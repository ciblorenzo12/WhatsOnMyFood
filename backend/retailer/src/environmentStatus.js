const DEFAULT_MODEL = "gemini-3.1-pro-preview";

function providerConfigured(env = process.env) {
  return Boolean(env.GEMINI_API_KEY || env.GOOGLE_API_KEY);
}

function foodRecallProviderConfigured(env = process.env) {
  return Boolean(String(env.OPENFDA_API_KEY || "").trim());
}

function publicBaseUrl(env = process.env) {
  return String(env.PUBLIC_BASE_URL || "").trim().replace(/\/+$/, "");
}

function healthPayload(env = process.env) {
  return {
    ok: true,
    service: "retailer-backend",
    bitwiseProvider: providerConfigured(env) ? "google-gemini" : "local-fallback",
    model: env.GEMINI_MODEL || DEFAULT_MODEL,
    foodRecallProvider: "openfda",
    foodRecallKeyConfigured: foodRecallProviderConfigured(env),
  };
}

function readinessResult({
  env = process.env,
  ragProviderCount = 0,
  appTokenConfigured = false,
  playBillingConfigured = false,
} = {}) {
  const baseUrl = publicBaseUrl(env);
  const checks = {
    publicHttpsConfigured: /^https:\/\//i.test(baseUrl),
    aiProviderCredentialConfigured: providerConfigured(env),
    appAuthenticationConfigured: Boolean(appTokenConfigured),
    ragProviderConfigured: ragProviderCount > 0,
    foodRecallCredentialConfigured: foodRecallProviderConfigured(env),
  };
  const ok = Object.values(checks).every(Boolean);

  return {
    status: ok ? 200 : 503,
    body: {
      ok,
      service: "retailer-backend",
      environment: env.NODE_ENV || "production",
      publicBaseUrl: baseUrl,
      model: env.GEMINI_MODEL || DEFAULT_MODEL,
      playBillingConfigured: Boolean(playBillingConfigured),
      checks,
      endpoints: {
        health: "/health",
        readiness: "/ready",
        aiAnalysis: "/v1/bitwise/analyze",
        ragIngredients: "/api/retail/products/:barcode/ingredients/rag",
        foodRecalls: "/v1/food-recalls",
      },
    },
  };
}

module.exports = {
  healthPayload,
  foodRecallProviderConfigured,
  providerConfigured,
  publicBaseUrl,
  readinessResult,
};
