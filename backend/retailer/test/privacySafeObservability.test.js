const assert = require("node:assert/strict");
const { EventEmitter } = require("node:events");
const test = require("node:test");

const {
  buildDiagnostic,
  classifyErrorCategory,
  createRequestObserver,
  formatDiagnostic,
  normalizeRoute,
} = require("../src/privacySafeObservability");

test("normalizes the RAG route without retaining the product barcode", () => {
  const route = normalizeRoute("/api/retail/products/012345678905/ingredients/rag");

  assert.equal(route, "/api/retail/products/:barcode/ingredients/rag");
  assert.equal(route.includes("012345678905"), false);
});

test("prints only the allowlisted privacy-safe diagnostic fields", () => {
  const diagnostic = buildDiagnostic({
    correlationId: "test-request-123",
    route: "/api/retail/products/012345678905/ingredients/rag",
    status: 503,
    latencyMs: 251.6,
    outcome: "failure",
    errorCategory: "provider_unavailable",
    prompt: "full prompt text",
    token: "super-secret-token",
    image: "data:image/jpeg;base64,private",
    productName: "Organic Yogurt",
  });
  const line = formatDiagnostic(diagnostic);

  assert.deepEqual(Object.keys(diagnostic), [
    "event", "correlationId", "route", "outcome", "status", "latencyMs", "errorCategory",
  ]);
  assert.equal(line.includes("012345678905"), false);
  assert.equal(line.includes("full prompt text"), false);
  assert.equal(line.includes("super-secret-token"), false);
  assert.equal(line.includes("data:image"), false);
  assert.equal(line.includes("Organic Yogurt"), false);
});

test("classifies timeout, rate-limit, and provider failures", () => {
  const timeout = new Error("request timed out");
  timeout.code = "ETIMEDOUT";

  assert.equal(classifyErrorCategory({ error: timeout }), "timeout");
  assert.equal(classifyErrorCategory({ status: 429 }), "rate_limit");
  assert.equal(classifyErrorCategory({ error: new Error("Provider returned 429") }), "rate_limit");
  assert.equal(classifyErrorCategory({ status: 503 }), "provider_unavailable");
});

test("preserves a safe correlation ID across the response and emitted event", () => {
  const res = new EventEmitter();
  res.statusCode = 200;
  res.headers = {};
  res.setHeader = (name, value) => { res.headers[name] = value; };
  const lines = [];
  let timestamp = 1000;
  const observer = createRequestObserver({
    req: { headers: { "x-correlation-id": "android-request-123" } },
    res,
    pathname: "/v1/bitwise/analyze",
    logger: (line) => lines.push(line),
    now: () => timestamp,
  });
  timestamp = 1125;
  observer.setResult("success", "none");
  res.emit("finish");

  assert.equal(observer.correlationId, "android-request-123");
  assert.equal(res.headers["X-Correlation-ID"], "android-request-123");
  assert.equal(lines.length, 1);
  assert.match(lines[0], /"latencyMs":125/);
  assert.match(lines[0], /"outcome":"success"/);
});
