const assert = require("node:assert/strict");
const { Readable } = require("node:stream");
const { afterEach, test } = require("node:test");

const {
  handleBitwiseAnalysis,
  groundingSources,
  attachVerifiedSources,
  productContextForFactCheck,
} = require("../src/bitwiseGemini");

const originalFetch = global.fetch;
const originalApiKey = process.env.GEMINI_API_KEY;

function request(body, token = "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG") {
  const stream = Readable.from([JSON.stringify(body)]);
  stream.headers = { "x-app-token": token };
  return stream;
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
  assert.equal(JSON.parse(result.body.content).ingredients[0], "water");
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

    if (callCount === 1) {
      assert.deepEqual(body.tools, [{ google_search: {} }]);
      assert.equal(body.generationConfig.responseMimeType, undefined);
      assert.equal(body.generationConfig.temperature, undefined);
      assert.match(body.systemInstruction.parts[0].text, /evidence-checking stage/i);
      assert.match(body.contents[0].parts[0].text, /Use Google Search now/i);
      assert.match(body.contents[0].parts[0].text, /2-4 authoritative web sources/i);
      return {
        ok: true,
        text: async () => JSON.stringify({
          candidates: [{
            content: { parts: [{ text: "FDA guidance supports checking added sugar against the label serving." }] },
            groundingMetadata: {
              webSearchQueries: ["FDA added sugar serving size"],
              groundingChunks: [{ web: { title: "FDA Nutrition Facts Label", uri: "https://example.gov/fda-label" } }],
            },
          }],
        }),
      };
    }

    assert.equal(body.tools, undefined);
    assert.equal(body.generationConfig.responseMimeType, undefined);
    assert.equal(body.generationConfig.responseFormat.text.mimeType, "APPLICATION_JSON");
    assert.equal(body.generationConfig.responseFormat.text.schema.properties.verdict.type, "string");
    assert.equal(body.generationConfig.temperature, undefined);
    assert.match(body.systemInstruction.parts[0].text, /warm, evidence-aware food-label assistant/i);
    assert.match(body.systemInstruction.parts[0].text, /Do not claim to be a doctor/i);
    assert.match(body.contents[0].parts[0].text, /GEMINI FACT-CHECK EVIDENCE/i);
    assert.match(body.contents[0].parts[0].text, /FDA Nutrition Facts Label/i);
    return {
      ok: true,
      text: async () => JSON.stringify({
        candidates: [{ content: { parts: [{ text: '{"verdict":"HEALTHY","findings":[],"sources":[{"name":"Invented","url":"https://bad.example"}]}' }] } }],
      }),
    };
  };

  const result = await handleBitwiseAnalysis(request({
    prompt: "Analyze this label",
    image: { mimeType: "image/jpeg", data: "image-data" },
  }));
  assert.equal(result.status, 200);
  assert.equal(result.body.provider, "google-gemini");
  assert.equal(callCount, 2);
  const content = JSON.parse(result.body.content);
  assert.equal(content.verdict, "HEALTHY");
  assert.equal(content.fact_check_status, "grounded");
  assert.deepEqual(content.sources, [{
    name: "FDA Nutrition Facts Label",
    url: "https://example.gov/fda-label",
    visual_quote: "Used by Gemini to fact-check the product explanation.",
    search_query: "FDA added sugar serving size",
  }]);
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

test("rejects a request with the wrong app token", async () => {
  const result = await handleBitwiseAnalysis(request({ prompt: "Analyze" }, "wrong-token"));
  assert.equal(result.status, 401);
});
