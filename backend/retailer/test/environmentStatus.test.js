const assert = require("node:assert/strict");
const { test } = require("node:test");

const { healthPayload, readinessResult } = require("../src/environmentStatus");

test("reports a ready hosted environment without exposing secrets", () => {
  const secret = "private-provider-value-never-return";
  const env = {
    NODE_ENV: "staging",
    PUBLIC_BASE_URL: "https://staging.example.test/",
    GEMINI_API_KEY: secret,
    GEMINI_MODEL: "gemini-3.1-pro-preview",
  };
  const readiness = readinessResult({
    env,
    ragProviderCount: 1,
    appTokenConfigured: true,
  });

  assert.equal(readiness.status, 200);
  assert.equal(readiness.body.ok, true);
  assert.equal(readiness.body.publicBaseUrl, "https://staging.example.test");
  assert.equal(readiness.body.checks.aiProviderCredentialConfigured, true);
  assert.equal(readiness.body.checks.ragProviderConfigured, true);
  assert.doesNotMatch(JSON.stringify(readiness.body), new RegExp(secret));
});

test("readiness fails when HTTPS or the provider credential is missing", () => {
  const readiness = readinessResult({
    env: { PUBLIC_BASE_URL: "http://localhost:8787" },
    ragProviderCount: 1,
    appTokenConfigured: true,
  });

  assert.equal(readiness.status, 503);
  assert.equal(readiness.body.ok, false);
  assert.equal(readiness.body.checks.publicHttpsConfigured, false);
  assert.equal(readiness.body.checks.aiProviderCredentialConfigured, false);
  assert.equal(healthPayload({}).bitwiseProvider, "local-fallback");
});
