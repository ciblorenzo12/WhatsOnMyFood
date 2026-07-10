const { analyzePrompt } = require("./bitwiseFallback");

const DEFAULT_MODEL = "gemini-2.5-flash";
const DEFAULT_APP_TOKEN = "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG";
const MAX_BODY_BYTES = 8 * 1024 * 1024;

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
  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;
  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": apiKey,
    },
    body: JSON.stringify({
      contents: [{ role: "user", parts: geminiParts(prompt, image) }],
      generationConfig: {
        temperature: 0.1,
        topP: 0.9,
        maxOutputTokens: 4096,
        responseMimeType: "application/json",
      },
    }),
  });

  const responseText = await response.text();
  if (!response.ok) {
    throw new Error(`Gemini API returned ${response.status}: ${responseText.slice(0, 500)}`);
  }

  const result = JSON.parse(responseText);
  const content = (result.candidates || [])
    .flatMap((candidate) => candidate.content?.parts || [])
    .map((part) => part.text || "")
    .join("")
    .trim();
  if (!content) {
    const blockReason = result.promptFeedback?.blockReason;
    throw new Error(blockReason ? `Gemini blocked the request: ${blockReason}` : "Gemini returned an empty response");
  }

  return {
    content: cleanModelJson(content),
    provider: "google-gemini",
    model,
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
};
