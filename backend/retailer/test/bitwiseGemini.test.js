const assert = require("node:assert/strict");
const { Readable } = require("node:stream");
const { afterEach, test } = require("node:test");

const { handleBitwiseAnalysis } = require("../src/bitwiseGemini");

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

test("sends text and image input to Gemini and returns JSON content", async () => {
  process.env.GEMINI_API_KEY = "test-google-key";
  global.fetch = async (url, options) => {
    assert.match(url, /gemini-2\.5-flash:generateContent$/);
    assert.equal(options.headers["x-goog-api-key"], "test-google-key");
    const body = JSON.parse(options.body);
    assert.equal(body.generationConfig.responseMimeType, "application/json");
    assert.equal(body.generationConfig.temperature, 0.35);
    assert.match(body.systemInstruction.parts[0].text, /warm, evidence-aware food-label assistant/i);
    assert.match(body.systemInstruction.parts[0].text, /Do not claim to be a doctor/i);
    assert.equal(body.contents[0].parts[1].inlineData.data, "image-data");
    return {
      ok: true,
      text: async () => JSON.stringify({
        candidates: [{ content: { parts: [{ text: '{"verdict":"HEALTHY"}' }] } }],
      }),
    };
  };

  const result = await handleBitwiseAnalysis(request({
    prompt: "Analyze this label",
    image: { mimeType: "image/jpeg", data: "image-data" },
  }));
  assert.equal(result.status, 200);
  assert.equal(result.body.provider, "google-gemini");
  assert.equal(JSON.parse(result.body.content).verdict, "HEALTHY");
});

test("rejects a request with the wrong app token", async () => {
  const result = await handleBitwiseAnalysis(request({ prompt: "Analyze" }, "wrong-token"));
  assert.equal(result.status, 401);
});
