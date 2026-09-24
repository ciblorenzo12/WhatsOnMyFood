const assert = require("node:assert/strict");
const http = require("node:http");
const { once } = require("node:events");
const test = require("node:test");
const { handleRequest, isProtectedEndpoint } = require("../src/server");
const { healthPayload, readinessResult } = require("../src/environmentStatus");
const { rateLimitBucketKey } = require("../src/rateLimiter");
const { normalizeRoute } = require("../src/privacySafeObservability");

test("USDA route requires app authentication and validates before contacting a provider", async t => {
  const savedToken = process.env.BITWISE_APP_TOKEN;
  const savedKey = process.env.FDC_API_KEY;
  process.env.BITWISE_APP_TOKEN = "local-fixture-token";
  delete process.env.FDC_API_KEY;
  const server = http.createServer(handleRequest).listen(0, "127.0.0.1");
  t.after(() => {
    server.closeAllConnections();
    server.close();
    if (savedToken === undefined) delete process.env.BITWISE_APP_TOKEN;
    else process.env.BITWISE_APP_TOKEN = savedToken;
    if (savedKey === undefined) delete process.env.FDC_API_KEY;
    else process.env.FDC_API_KEY = savedKey;
  });
  await once(server, "listening");
  const base = `http://127.0.0.1:${server.address().port}/v1/food-data/usda`;
  const unauthorized = await fetch(`${base}?barcode=012345678905`);
  assert.equal(unauthorized.status, 401);
  assert.deepEqual(await unauthorized.json(), { error: "Unauthorized" });
  const headers = { "X-APP-TOKEN": "local-fixture-token" };
  const invalid = await fetch(`${base}?barcode=123`, { headers });
  assert.equal(invalid.status, 400);
  assert.equal((await invalid.json()).code, "invalid_barcode");
  const unconfigured = await fetch(`${base}?barcode=012345678905`, { headers });
  assert.equal(unconfigured.status, 503);
  assert.equal((await unconfigured.json()).code, "provider_not_configured");
});

test("USDA is protected, independently rate limited and privacy safely observed", () => {
  assert.equal(isProtectedEndpoint("/v1/food-data/usda"), true);
  assert.equal(normalizeRoute("/v1/food-data/usda"), "/v1/food-data/usda");
  assert.notEqual(rateLimitBucketKey("client", "/v1/food-data/usda"),
    rateLimitBucketKey("client", "/v1/bitwise/analyze"));
});

test("health reports USDA configuration without disclosing a key or making it mandatory", () => {
  assert.equal(healthPayload({}).usdaKeyConfigured, false);
  assert.equal(healthPayload({ FDC_API_KEY: " " }).usdaKeyConfigured, false);
  assert.equal(healthPayload({ FDC_API_KEY: " demo_key " }).usdaKeyConfigured, false);
  const payload = healthPayload({ FDC_API_KEY: "private-fixture" });
  assert.equal(payload.usdaKeyConfigured, true);
  assert.equal(JSON.stringify(payload).includes("private-fixture"), false);
  const ready = readinessResult({
    env: { GEMINI_API_KEY: "ai-fixture", OPENFDA_API_KEY: "fda-fixture", PUBLIC_BASE_URL: "https://example.test" },
    ragProviderCount: 1, appTokenConfigured: true
  });
  assert.equal(ready.status, 200);
  assert.equal(ready.body.usdaKeyConfigured, false);
  assert.equal(ready.body.endpoints.usdaFoodData, "/v1/food-data/usda");
});
