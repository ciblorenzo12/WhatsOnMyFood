const { analyzePrompt } = require("./bitwiseFallback");

const DEFAULT_MODEL = "gemini-3.1-pro-preview";
const DEFAULT_APP_TOKEN = "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG";
const MAX_BODY_BYTES = 8 * 1024 * 1024;
const HEALTH_EDUCATOR_INSTRUCTION = [
  "You are Bitwise, a warm, evidence-aware food-label assistant.",
  "Write like a thoughtful nutrition educator speaking to a real shopper: clear, calm, and conversational.",
  "Do not claim to be a doctor, dietitian, clinician, or medical professional, and do not diagnose, treat, or give personalized medical advice.",
  "Explain what the label suggests in everyday language, including useful context: one ingredient or one product does not determine a person's health.",
  "Be specific about what is present on the label, acknowledge uncertainty honestly, and avoid fear-based wording.",
  "End with a practical, non-judgmental takeaway that helps the shopper decide what to do next.",
  "For allergies, pregnancy, medical conditions, or medication questions, advise the shopper to check with a qualified healthcare professional rather than guessing.",
].join(" ");

const FACT_CHECKER_INSTRUCTION = [
  "You are the evidence-checking stage for Bitwise, a consumer food-label assistant.",
  "You must use Google Search on every request to verify the nutrition and ingredient claims that are relevant to the supplied product label.",
  "Prefer authoritative primary sources such as FDA, USDA, NIH, WHO, EFSA, and established medical or public-health organizations.",
  "Check the reason for the proposed health verdict and find practical serving or portion context when the label provides enough information.",
  "Do not diagnose, prescribe, or invent a serving amount. If the package serving size is unavailable, say that portion advice must stay general.",
  "Return a concise evidence memo for another model to use. Clearly separate verified facts, label-specific limits, and uncertainty.",
].join(" ");

const BITWISE_RESPONSE_SCHEMA = {
  type: "object",
  properties: {
    product_name: { type: "string" },
    brand: { type: "string" },
    product_type: {
      type: "string",
      enum: ["food", "beverage", "supplement", "oral_care", "personal_care", "unknown"],
    },
    verdict: {
      type: "string",
      enum: ["HEALTHY", "NOT_HEALTHY", "APPROVED", "NOT_APPROVED", "REVIEW"],
    },
    verdict_reason: { type: "string" },
    ingredients: { type: "array", items: { type: "string" } },
    ingredients_source: { type: "string", enum: ["label", "product_identity", "unknown"] },
    ingredient_confidence: { type: "string", enum: ["high", "medium", "low"] },
    summary: {
      type: "string",
      description: "A 100-160 word shopper explanation with Why this rating, Portion guidance, and Fact check HTML sections.",
    },
    findings: {
      type: "array",
      maxItems: 5,
      items: {
        type: "object",
        properties: {
          rule: { type: "string" },
          impact: { type: "string", enum: ["positive", "neutral", "warning", "negative"] },
          triggering_ingredient: { type: "string" },
          explanation: { type: "string" },
          source_url: { type: "string" },
          visual_quote: { type: "string" },
        },
        required: ["rule", "impact", "triggering_ingredient", "explanation", "source_url"],
      },
    },
    sources: {
      type: "array",
      items: {
        type: "object",
        properties: {
          name: { type: "string" },
          url: { type: "string" },
          visual_quote: { type: "string" },
          search_query: { type: "string" },
        },
        required: ["name", "url"],
      },
    },
  },
  required: [
    "product_name",
    "brand",
    "product_type",
    "verdict",
    "verdict_reason",
    "ingredients",
    "ingredients_source",
    "ingredient_confidence",
    "summary",
    "findings",
    "sources",
  ],
};

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    let size = 0;
    req.setEncoding("utf8");
    req.on("data", (chunk) => {
      size += Buffer.byteLength(chunk);
      if (size > MAX_BODY_BYTES) {
        reject(new Error("Bitwise request is too large"));
        req.destroy();
        return;
      }
      body += chunk;
    });
    req.on("end", () => {
      try {
        resolve(JSON.parse(body || "{}"));
      } catch (_error) {
        reject(new Error("Bitwise request must contain valid JSON"));
      }
    });
    req.on("error", reject);
  });
}

function cleanModelJson(text) {
  const cleaned = String(text || "")
    .replace(/^\s*```(?:json)?\s*/i, "")
    .replace(/\s*```\s*$/i, "")
    .trim();
  JSON.parse(cleaned);
  return cleaned;
}

function geminiParts(prompt, image) {
  const parts = [{ text: prompt }];
  if (image && image.data) {
    parts.push({
      inlineData: {
        mimeType: image.mimeType || "image/jpeg",
        data: image.data,
      },
    });
  }
  return parts;
}

function productContextForFactCheck(prompt) {
  const text = String(prompt || "");
  const detectedMarker = "DETECTED INGREDIENT LABEL:";
  const detectedIndex = text.indexOf(detectedMarker);
  if (detectedIndex >= 0) {
    const outputIndex = text.indexOf("Return valid JSON only", detectedIndex);
    return text.slice(detectedIndex, outputIndex >= 0 ? outputIndex : undefined).trim().slice(0, 12000);
  }

  let context = text;
  const schemaIndex = context.search(/Use (?:this )?exact JSON shape/i);
  if (schemaIndex > 0) context = context.slice(0, schemaIndex);
  const productIndex = context.search(/\bProduct(?: name)?:/i);
  if (productIndex > 0) context = context.slice(productIndex);
  return context.trim().slice(0, 12000);
}

function factCheckPrompt(prompt) {
  return "Use Google Search now and return a short fact-check memo supported by 2-4 authoritative web sources. "
    + "Verify the specific reason for the product verdict and any portion guidance that can be supported by the printed serving size. "
    + "Search authoritative health or food-regulatory sources first. Do not merely repeat the product analysis request.\n\n"
    + `PRODUCT LABEL DATA TO VERIFY:\n${productContextForFactCheck(prompt)}`;
}

function modelText(result) {
  return (result.candidates || [])
    .flatMap((candidate) => candidate.content?.parts || [])
    .map((part) => part.text || "")
    .join("")
    .trim();
}

function groundingSources(result) {
  const candidate = result.candidates?.[0];
  const metadata = candidate?.groundingMetadata || {};
  const queries = Array.isArray(metadata.webSearchQueries) ? metadata.webSearchQueries : [];
  const seen = new Set();

  return (metadata.groundingChunks || [])
    .map((chunk) => chunk?.web)
    .filter((web) => web && typeof web.uri === "string" && /^https?:\/\//i.test(web.uri))
    .filter((web) => {
      if (seen.has(web.uri)) return false;
      seen.add(web.uri);
      return true;
    })
    .slice(0, 4)
    .map((web) => ({
      name: String(web.title || "Fact-check source").trim(),
      url: web.uri,
      visual_quote: "Used by Gemini to fact-check the product explanation.",
      search_query: String(queries[0] || "").trim(),
    }));
}

function groundedAnalysisPrompt(prompt, evidence, sources) {
  const sourceList = sources.map((source, index) =>
    `${index + 1}. ${source.name}: ${source.url}`
  ).join("\n");

  return `${prompt}\n\nGEMINI FACT-CHECK EVIDENCE:\n${evidence}\n\n`
    + `SOURCES ACTUALLY USED BY THE FACT-CHECK:\n${sourceList || "No grounded web source was returned."}\n\n`
    + "Use the fact-check evidence to verify the verdict explanation and portion guidance. "
    + "Do not introduce a health claim that is not supported by the label or the evidence above. "
    + "Do not invent citations. The server will attach the verified source list to the final response.";
}

function attachVerifiedSources(content, sources) {
  const parsed = JSON.parse(content);
  parsed.fact_check_status = sources.length > 0 ? "grounded" : "source_unavailable";
  parsed.sources = sources;

  const allowedUrls = new Set(sources.map((source) => source.url));
  if (Array.isArray(parsed.findings)) {
    parsed.findings = parsed.findings.map((finding) => {
      if (!finding || typeof finding !== "object") return finding;
      if (!allowedUrls.has(finding.source_url)) finding.source_url = "";
      return finding;
    });
  }

  return JSON.stringify(parsed);
}

function analysisGenerationConfig(usesGemini3Defaults) {
  const config = {
    ...(usesGemini3Defaults ? {} : { temperature: 0.3, topP: 0.9 }),
    maxOutputTokens: 4096,
  };
  if (usesGemini3Defaults) {
    config.responseFormat = {
      text: {
        mimeType: "APPLICATION_JSON",
        schema: BITWISE_RESPONSE_SCHEMA,
      },
    };
  } else {
    config.responseMimeType = "application/json";
  }
  return config;
}

function fallbackResponse(prompt, reason) {
  return {
    status: 200,
    body: {
      content: JSON.stringify(analyzePrompt(prompt)),
      provider: "local-fallback",
      fallbackReason: reason,
    },
  };
}

async function requestGemini(prompt, image) {
  const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY;
  const model = process.env.GEMINI_MODEL || DEFAULT_MODEL;
  const usesGemini3Defaults = /^gemini-3(?:\.|-)/i.test(model);
  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;
  const factCheckResponse = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": apiKey,
    },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: FACT_CHECKER_INSTRUCTION }] },
      contents: [{ role: "user", parts: geminiParts(factCheckPrompt(prompt), image) }],
      tools: [{ google_search: {} }],
      generationConfig: {
        ...(usesGemini3Defaults ? {} : { temperature: 0.1, topP: 0.8 }),
        maxOutputTokens: 1400,
      },
    }),
  });

  const factCheckText = await factCheckResponse.text();
  if (!factCheckResponse.ok) {
    throw new Error(`Gemini fact-check returned ${factCheckResponse.status}: ${factCheckText.slice(0, 500)}`);
  }

  const factCheckResult = JSON.parse(factCheckText);
  const evidence = modelText(factCheckResult);
  const sources = groundingSources(factCheckResult);
  if (!evidence) {
    throw new Error("Gemini fact-check returned an empty response");
  }

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": apiKey,
    },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: HEALTH_EDUCATOR_INSTRUCTION }] },
      contents: [{ role: "user", parts: geminiParts(groundedAnalysisPrompt(prompt, evidence, sources), image) }],
      generationConfig: analysisGenerationConfig(usesGemini3Defaults),
    }),
  });

  const responseText = await response.text();
  if (!response.ok) {
    throw new Error(`Gemini analysis returned ${response.status}: ${responseText.slice(0, 500)}`);
  }

  const result = JSON.parse(responseText);
  const content = modelText(result);
  if (!content) {
    const blockReason = result.promptFeedback?.blockReason;
    throw new Error(blockReason ? `Gemini blocked the request: ${blockReason}` : "Gemini returned an empty response");
  }

  return {
    content: attachVerifiedSources(cleanModelJson(content), sources),
    provider: "google-gemini",
    model,
    factCheck: sources.length > 0 ? "grounded" : "source-unavailable",
  };
}

async function handleBitwiseAnalysis(req) {
  const expectedToken = process.env.BITWISE_APP_TOKEN || DEFAULT_APP_TOKEN;
  if (expectedToken && req.headers["x-app-token"] !== expectedToken) {
    return { status: 401, body: { error: "Unauthorized" } };
  }

  const body = await readJsonBody(req);
  const prompt = typeof body.prompt === "string" ? body.prompt.trim() : "";
  if (!prompt) {
    return { status: 400, body: { error: "A prompt is required" } };
  }

  if (!process.env.GEMINI_API_KEY && !process.env.GOOGLE_API_KEY) {
    return fallbackResponse(prompt, "GEMINI_API_KEY is not configured on the server");
  }

  try {
    return { status: 200, body: await requestGemini(prompt, body.image) };
  } catch (error) {
    console.error("Gemini request failed:", error.message);
    return fallbackResponse(prompt, "The Gemini service was unavailable");
  }
}

module.exports = {
  handleBitwiseAnalysis,
  requestGemini,
  HEALTH_EDUCATOR_INSTRUCTION,
  FACT_CHECKER_INSTRUCTION,
  groundingSources,
  attachVerifiedSources,
  BITWISE_RESPONSE_SCHEMA,
  productContextForFactCheck,
};
