const assert = require("node:assert/strict");
const { Readable } = require("node:stream");
const { afterEach, test } = require("node:test");

const {
  handleBitwiseAnalysis,
  groundingSources,
  attachVerifiedSources,
  productContextForFactCheck,
  factCheckSourcesForPrompt,
  groundedResponsePrompt,
  urlContextSources,
  buildSourceAwarePrompt,
  validateProviderOutput,
  sourceVerification,
} = require("../src/bitwiseGemini");

const originalFetch = global.fetch;
const originalApiKey = process.env.GEMINI_API_KEY;

function request(body, token = "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG") {
  const stream = Readable.from([JSON.stringify(body)]);
  stream.headers = { "x-app-token": token };
  return stream;
}

function validProviderContent(overrides = {}) {
  return {
    product_name: "Oat cereal",
    brand: "Test",
    product_type: "food",
    verdict: "HEALTHY",
    verdict_reason: "Whole-grain oats are the primary ingredient.",
    ingredients: ["oats"],
    ingredients_source: "label",
    ingredient_confidence: "high",
    summary: "<b>Why this rating</b><br>Whole-grain oats are the main ingredient, so the explanation stays tied to the supplied label. "
      + "<br><br><b>Portion guidance</b><br>Use the serving printed on the package and consider the rest of the meal. "
      + "<br><br><b>Fact check</b><br>The label guidance was checked against the verified nutrition source, while individual needs may differ.",
    findings: [{
      rule: "Whole grain base",
      impact: "positive",
      triggering_ingredient: "oats",
      explanation: "Whole-grain oats are the first listed ingredient.",
      source_url: "",
    }],
    sources: [],
    ...overrides,
  };
}

afterEach(() => {
  global.fetch = originalFetch;
  if (originalApiKey === undefined) delete process.env.GEMINI_API_KEY;
  else process.env.GEMINI_API_KEY = originalApiKey;
});

test("uses the local analysis when Gemini is not configured", async () => {
  delete process.env.GEMINI_API_KEY;
  const result = await handleBitwiseAnalysis(request({ prompt: "Ingredients: water, sugar" }));
  assert.equal(result.status, 200);
  assert.equal(result.body.provider, "local-fallback");
  const content = JSON.parse(result.body.content);
  assert.equal(content.ingredients[0], "water");
  assert.equal(content.verdict, "HEALTHY");
});

test("local fallback does not mark natural flavors healthy", async () => {
  delete process.env.GEMINI_API_KEY;
  const result = await handleBitwiseAnalysis(request({
    prompt: "Product: Organic Yogurt. Ingredients: organic yogurt, natural flavors",
  }));

  const content = JSON.parse(result.body.content);
  assert.equal(content.verdict, "NOT_HEALTHY");
  assert.deepEqual(content.ingredients, ["organic yogurt", "natural flavors"]);
});

test("does not classify a solid food as a drink just because water is an ingredient", async () => {
  delete process.env.GEMINI_API_KEY;
  const result = await handleBitwiseAnalysis(request({
    prompt: "Product: Tofu. Brand: Mori-Nu. Ingredients: water, soybeans, calcium sulfate.",
    productContext: { raw: "Name: Tofu\nBrand: Mori-Nu\nIngredients: water, soybeans, calcium sulfate" },
    rules: [],
  }));

  const content = JSON.parse(result.body.content);
  assert.equal(content.product_name, "Tofu");
  assert.equal(content.product_type, "food");
  assert.doesNotMatch(content.summary, /\bdrink\b/i);
});

test("accepts protected structured product and deterministic rule context", async () => {
  delete process.env.GEMINI_API_KEY;
  const result = await handleBitwiseAnalysis(request({
    requestVersion: 1,
    prompt: "Product: Oat cereal. Ingredients: oats, sugar, salt.",
    productContext: { raw: "Name: Oat cereal\nIngredients: oats, sugar, salt" },
    rules: ["Flag added sugar", "Keep deterministic findings visible"],
  }));

  assert.equal(result.status, 200);
  assert.equal(result.body.provider, "local-fallback");
});

test("rejects invalid tokens and malformed structured context", async () => {
  const unauthorized = await handleBitwiseAnalysis(request({ prompt: "Ingredients: oats" }, "wrong-token"));
  assert.equal(unauthorized.status, 401);

  const malformed = await handleBitwiseAnalysis(request({
    requestVersion: 1,
    prompt: "Ingredients: oats",
    productContext: "not-an-object",
    rules: ["valid", 42],
  }));
  assert.equal(malformed.status, 400);
});

test("grounds the fact check before generating the structured shopper response", async () => {
  process.env.GEMINI_API_KEY = "test-google-key";
  let callCount = 0;
  global.fetch = async (url, options) => {
    callCount += 1;
    assert.match(url, /gemini-3\.1-pro-preview:generateContent$/);
    assert.equal(options.headers["x-goog-api-key"], "test-google-key");
    const body = JSON.parse(options.body);
    assert.equal(body.contents[0].parts[1].inlineData.data, "image-data");

    assert.deepEqual(body.tools, [{ url_context: {} }]);
    assert.equal(body.generationConfig.responseMimeType, undefined);
    assert.equal(body.generationConfig.responseFormat.text.mimeType, "APPLICATION_JSON");
    assert.equal(body.generationConfig.responseFormat.text.schema.properties.verdict.type, "string");
    assert.equal(body.generationConfig.thinkingConfig.thinkingLevel, "low");
    assert.equal(body.generationConfig.maxOutputTokens, 8192);
    assert.equal(body.generationConfig.temperature, undefined);
    assert.match(body.systemInstruction.parts[0].text, /warm, evidence-aware food-label assistant/i);
    assert.match(body.systemInstruction.parts[0].text, /evidence-checking stage/i);
    assert.match(body.systemInstruction.parts[0].text, /Do not claim to be a doctor/i);
    assert.match(body.contents[0].parts[0].text, /Use URL Context now/i);
    assert.match(body.contents[0].parts[0].text, /FDA.*Nutrition Facts Label/i);
    assert.match(body.contents[0].parts[0].text, /Source status: updated_from_product_database/i);
    assert.match(body.contents[0].parts[0].text, /Normalized ingredients: bovine collagen peptides/i);
    assert.match(body.contents[0].parts[0].text, /Product database identity is trusted; nutrition details remain label-limited/i);
    assert.match(body.contents[0].parts[0].text, /Preserve the deterministic protein finding/i);
    assert.match(body.contents[0].parts[0].text, /Do not contradict deterministic findings/i);
    assert.doesNotMatch(body.contents[0].parts[0].text, /Added Sugars on the Nutrition Facts Label/i);
    assert.doesNotMatch(body.contents[0].parts[0].text, /Sodium in Your Diet/i);
    return {
      ok: true,
      text: async () => JSON.stringify({
        candidates: [{
          content: { parts: [{ text: JSON.stringify(validProviderContent({
            sources: [{ name: "Invented", url: "https://bad.example" }],
          })) }] },
          urlContextMetadata: {
            urlMetadata: [{
              retrievedUrl: "https://www.fda.gov/food/nutrition-facts-label/how-understand-and-use-nutrition-facts-label",
              urlRetrievalStatus: "URL_RETRIEVAL_STATUS_SUCCESS",
            }],
          },
        }],
      }),
    };
  };

  const result = await handleBitwiseAnalysis(request({
    prompt: "Analyze this label. Generic rules mention sugar and sodium.",
    productContext: {
      raw: "Product: Collagen Peptides. Ingredients: bovine collagen peptides.",
      normalizedIngredients: ["bovine collagen peptides"],
      sourceStatus: "updated_from_product_database",
      uncertainty: "Product database identity is trusted; nutrition details remain label-limited.",
    },
    rules: ["Preserve the deterministic protein finding"],
    image: { mimeType: "image/jpeg", data: "image-data" },
  }));
  assert.equal(result.status, 200);
  assert.equal(result.body.provider, "google-gemini");
  assert.equal(callCount, 1);
  const content = JSON.parse(result.body.content);
  assert.equal(content.verdict, "HEALTHY");
  assert.equal(content.fact_check_status, "grounded");
  assert.equal(content.sources.length, 1);
  assert.deepEqual({
    name: content.sources[0].name,
    url: content.sources[0].url,
    visual_quote: content.sources[0].visual_quote,
    search_query: content.sources[0].search_query,
  }, {
    name: "FDA - How to Understand and Use the Nutrition Facts Label",
    url: "https://www.fda.gov/food/nutrition-facts-label/how-understand-and-use-nutrition-facts-label",
    visual_quote: "Used by Gemini to fact-check the product explanation.",
    search_query: "",
  });
  assert.ok(content.sources[0].verification.score >= 90);
  assert.equal(content.sources[0].verification.level, "very_strong");
});

test("deduplicates grounded web sources and rejects unverified finding links", () => {
  const sources = groundingSources({
    candidates: [{
      groundingMetadata: {
        webSearchQueries: ["authoritative nutrition guidance"],
        groundingChunks: [
          { web: { title: "FDA", uri: "https://example.gov/fda" } },
          { web: { title: "FDA duplicate", uri: "https://example.gov/fda" } },
          { web: { title: "Invalid", uri: "javascript:alert(1)" } },
        ],
      },
    }],
  });
  assert.equal(sources.length, 1);

  const content = JSON.parse(attachVerifiedSources(JSON.stringify({
    findings: [{ source_url: "https://invented.example/source" }],
    sources: [{ name: "Invented", url: "https://invented.example/source" }],
  }), sources));
  assert.equal(content.findings[0].source_url, "");
  assert.equal(content.sources[0].url, "https://example.gov/fda");
});

test("keeps JSON formatting instructions out of the fact-check query", () => {
  const context = productContextForFactCheck(
    "App instructions. Product: Plain almond butter. Ingredients: almonds. "
      + "Use this exact JSON shape: {\"summary\":\"\"}"
  );
  assert.match(context, /^Product: Plain almond butter/);
  assert.doesNotMatch(context, /JSON shape/i);
});

test("keeps deterministic rule descriptions out of source selection", () => {
  const context = productContextForFactCheck(
    "DETECTED INGREDIENT LABEL:\n\nOCR TEXT:\nProduct: Collagen Peptides. "
      + "Ingredients: bovine collagen peptides.\n\nDETERMINISTIC RULE CONTEXT:\n"
      + "Added sugar rule. High sodium rule. Artificial color rule. Return valid JSON only."
  );
  assert.match(context, /bovine collagen peptides/i);
  assert.doesNotMatch(context, /Added sugar rule/i);

  const keys = factCheckSourcesForPrompt(context).map((source) => source.key);
  assert.deepEqual(keys, ["nutrition_label", "healthy_diet"]);
});

test("does not duplicate product label context in the grounded request", () => {
  const prompt = "Product: Plain almond butter. Ingredients: almonds. Use this exact JSON shape: {}";
  const groundedPrompt = groundedResponsePrompt(prompt, [{ name: "FDA", url: "https://www.fda.gov/food" }]);
  assert.equal(groundedPrompt.split("Product: Plain almond butter").length - 1, 1);
  assert.doesNotMatch(groundedPrompt, /PRODUCT LABEL DATA TO FACT-CHECK/);
});

test("builds a source-aware prompt from normalized ingredients, source status, uncertainty, and rules", () => {
  const prompt = buildSourceAwarePrompt(
    "Explain this product in plain language.",
    {
      raw: "Name: Oat cereal\nIngredients: oats, sugar, salt",
      normalizedIngredients: ["oats", "sugar", "salt"],
      sourceStatus: "ingredients_recovered_from_label_or_supporting_service",
      uncertainty: "Recovered ingredients require cautious attribution.",
    },
    ["Added sugar finding", "Preserve the deterministic verdict"],
  );

  assert.match(prompt, /SOURCE-AWARE REQUEST CONTEXT/);
  assert.match(prompt, /Normalized ingredients: oats, sugar, salt/);
  assert.match(prompt, /ingredients_recovered_from_label_or_supporting_service/);
  assert.match(prompt, /Recovered ingredients require cautious attribution/);
  assert.match(prompt, /Added sugar finding/);
  assert.match(prompt, /Do not contradict deterministic findings/);
  assert.match(prompt, /Do not diagnose, prescribe treatment, or make unsupported medical claims/);
});

test("validates grounded provider output and rejects blank, HTML, malformed, and unsafe claims", () => {
  const valid = validProviderContent({
    fact_check_status: "grounded",
    sources: [{ name: "FDA", url: "https://www.fda.gov/food" }],
  });
  assert.equal(JSON.parse(validateProviderOutput(JSON.stringify(valid))).verdict, "HEALTHY");

  assert.throws(() => validateProviderOutput(""), /blank or HTML/i);
  assert.throws(() => validateProviderOutput("<!DOCTYPE html><title>Starting</title>"), /blank or HTML/i);
  assert.throws(() => validateProviderOutput("not-json"), /malformed JSON/i);
  assert.throws(() => validateProviderOutput(JSON.stringify(validProviderContent({
    verdict: "REVIEW",
    fact_check_status: "grounded",
    sources: [{ name: "FDA", url: "https://www.fda.gov/food" }],
  }))), /unsupported verdict/i);
  assert.throws(() => validateProviderOutput(JSON.stringify(validProviderContent({
    ingredients: [],
    fact_check_status: "grounded",
    sources: [{ name: "FDA", url: "https://www.fda.gov/food" }],
  }))), /invalid ingredient data/i);
  assert.throws(() => validateProviderOutput(JSON.stringify(validProviderContent({
    verdict_reason: "This product can cure diabetes.",
    fact_check_status: "grounded",
    sources: [{ name: "FDA", url: "https://www.fda.gov/food" }],
  }))), /unsafe medical claim/i);
});

test("uses the controlled local fallback when provider output fails validation", async () => {
  process.env.GEMINI_API_KEY = "test-google-key";
  global.fetch = async () => ({
    ok: true,
    text: async () => JSON.stringify({
      candidates: [{
        content: { parts: [{ text: "<!DOCTYPE html><title>Provider startup</title>" }] },
      }],
    }),
  });

  const result = await handleBitwiseAnalysis(request({
    prompt: "Product: Oat cereal. Ingredients: oats, sugar, salt.",
    productContext: {
      raw: "Name: Oat cereal\nIngredients: oats, sugar, salt",
      normalizedIngredients: ["oats", "sugar", "salt"],
      sourceStatus: "updated_from_product_database",
      uncertainty: "Use only the supplied product record.",
    },
    rules: ["Flag added sugar"],
  }));

  assert.equal(result.status, 200);
  assert.equal(result.body.provider, "local-fallback");
  assert.match(result.body.fallbackReason, /unavailable/i);
  assert.equal(JSON.parse(result.body.content).product_name, "Oat cereal");
});

test("selects nutrition sources that match the scanned label", () => {
  const sources = factCheckSourcesForPrompt(
    "Product: Almond butter. Ingredients: almonds. Added sugar: 0 g. Sodium: 0 mg."
  );
  const keys = sources.map((source) => source.key);
  assert.ok(keys.includes("nutrition_label"));
  assert.ok(keys.includes("fats"));
  assert.ok(keys.includes("added_sugars"));
  assert.ok(keys.includes("sodium"));
});

test("returns only URL context sources Gemini successfully retrieved", () => {
  const requested = factCheckSourcesForPrompt("Product: Water");
  const sources = urlContextSources({
    candidates: [{
      urlContextMetadata: {
        urlMetadata: [
          { retrievedUrl: requested[0].url, urlRetrievalStatus: "URL_RETRIEVAL_STATUS_SUCCESS" },
          { retrievedUrl: requested[1].url, urlRetrievalStatus: "URL_RETRIEVAL_STATUS_ERROR" },
        ],
      },
    }],
  }, requested);
  assert.equal(sources.length, 1);
  assert.equal(sources[0].url, requested[0].url);
});

test("recognizes successfully retrieved sources when Gemini normalizes the URL", () => {
  const requested = factCheckSourcesForPrompt("Product: Water");
  const sources = urlContextSources({
    candidates: [{
      urlContextMetadata: {
        urlMetadata: [{
          retrievedUrl: `${requested[0].url}/?utm_source=gemini#details`,
          urlRetrievalStatus: "URL_RETRIEVAL_STATUS_SUCCESS",
        }],
      },
    }],
  }, requested);
  assert.equal(sources.length, 1);
  assert.equal(sources[0].url, requested[0].url);
});

test("keeps the Pro response with curated sources when URL Context returns no metadata", async () => {
  process.env.GEMINI_API_KEY = "test-google-key";
  global.fetch = async () => ({
    ok: true,
    text: async () => JSON.stringify({
      candidates: [{
        content: {
          parts: [{
            text: JSON.stringify({
              product_name: "Oat cereal",
              brand: "Test",
              product_type: "food",
              verdict: "HEALTHY",
              verdict_reason: "Simple ingredient list.",
              ingredients: ["oats"],
              ingredients_source: "label",
              ingredient_confidence: "high",
              summary: "<b>Why this rating</b><br>Oats are the supplied main ingredient, so the explanation is limited to that product evidence. "
                + "<br><br><b>Portion guidance</b><br>Use the serving printed on the package and consider the full meal. "
                + "<br><br><b>Fact check</b><br>The claim was checked against the selected authoritative nutrition references.",
              findings: [],
              sources: [],
            }),
          }],
        },
      }],
    }),
  });

  const result = await handleBitwiseAnalysis(request({
    prompt: "Product: Oat cereal. Ingredients: oats.",
    productContext: { raw: "Name: Oat cereal\nBrand: Test\nIngredients: oats" },
    rules: [],
  }));

  assert.equal(result.status, 200);
  assert.equal(result.body.provider, "google-gemini");
  assert.equal(result.body.model, "gemini-3.1-pro-preview");
  assert.equal(result.body.factCheck, "authoritative-sources-selected");
  const content = JSON.parse(result.body.content);
  assert.equal(content.product_name, "Oat cereal");
  assert.equal(content.brand, "Test");
  assert.match(content.summary, /Why this rating/);
  assert.equal(content.fact_check_status, "authoritative_sources_selected");
  assert.ok(content.sources.length > 0);
  assert.match(content.sources[0].url, /^https:\/\//);
});

test("rejects a request with the wrong app token", async () => {
  const result = await handleBitwiseAnalysis(request({ prompt: "Analyze" }, "wrong-token"));
  assert.equal(result.status, 401);
});

test("rates an official topic-matched nutrition source without another model request", () => {
  const rating = sourceVerification({
    key: "added_sugars",
    name: "FDA - Added Sugars on the Nutrition Facts Label",
    url: "https://www.fda.gov/food/nutrition-facts-label/added-sugars-nutrition-facts-label",
  }, "Ingredients: cane sugar and corn syrup", "grounded");

  assert.ok(rating.score >= 90);
  assert.equal(rating.level, "very_strong");
  assert.equal(rating.method, "source_quality_v1");
  assert.match(rating.note, /not the probability/i);
});

test("does not overrate an unclassified publisher with weak claim fit", () => {
  const rating = sourceVerification({
    name: "Food opinion",
    url: "https://example.com/article",
  }, "Ingredients: sodium benzoate", "grounded");

  assert.ok(rating.score < 60);
  assert.equal(rating.level, "limited");
});
