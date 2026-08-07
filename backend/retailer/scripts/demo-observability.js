const {
  buildDiagnostic,
  formatDiagnostic,
} = require("../src/privacySafeObservability");

const scenarios = [
  {
    correlationId: "demo-ai-success-01",
    route: "/v1/bitwise/analyze",
    status: 200,
    latencyMs: 842,
    outcome: "success",
    errorCategory: "none",
  },
  {
    correlationId: "demo-ai-timeout-02",
    route: "/v1/bitwise/analyze",
    status: 0,
    latencyMs: 45000,
    outcome: "failure",
    errorCategory: "timeout",
  },
  {
    correlationId: "demo-rag-limit-03",
    route: "/api/retail/products/012345678905/ingredients/rag",
    status: 429,
    latencyMs: 31,
    outcome: "rate_limited",
    errorCategory: "rate_limit",
  },
  {
    correlationId: "demo-rag-provider-04",
    route: "/api/retail/products/012345678905/ingredients/rag",
    status: 503,
    latencyMs: 1217,
    outcome: "failure",
    errorCategory: "provider_unavailable",
  },
];

const forbiddenFixtures = [
  "012345678905",
  "Organic Yogurt",
  "full prompt text",
  "super-secret-token",
  "data:image/jpeg;base64",
];
const lines = scenarios.map((scenario) => formatDiagnostic(buildDiagnostic(scenario)));
const consoleOutput = lines.join("\n");
const leaked = forbiddenFixtures.filter((fixture) => consoleOutput.includes(fixture));

console.log("M3-06 privacy-safe observability console demonstration");
lines.forEach((line) => console.log(line));

if (leaked.length > 0) {
  console.error("FAIL: a forbidden diagnostic value was exposed.");
  process.exitCode = 1;
} else {
  console.log("PASS: only allowlisted diagnostic fields were printed; sensitive request data was excluded.");
}
