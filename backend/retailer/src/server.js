const http = require("http");
const { URL } = require("url");
const { createProviderRegistry } = require("./providerRegistry");
const { RetailerService } = require("./retailerService");
const { handleBitwiseCompletion } = require("./bitwiseFallback");
const { handleBitwiseAnalysis } = require("./bitwiseGemini");
const { handleGooglePlayVerification } = require("./googlePlayBilling");
const { createRateLimiter } = require("./rateLimiter");

const service = new RetailerService(createProviderRegistry());
const port = Number(process.env.PORT || 8787);
const configuredProtectedLimit = Number(process.env.PROTECTED_RATE_LIMIT_PER_MINUTE || 20);
const protectedRateLimiter = createRateLimiter({
  maxRequests: Number.isFinite(configuredProtectedLimit) && configuredProtectedLimit > 0
    ? Math.floor(configuredProtectedLimit)
    : 20,
});

function writeJson(res, status, body, extraHeaders = {}) {
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    ...extraHeaders,
  });
  res.end(JSON.stringify(body, null, 2));
}

function clientAddress(req) {
  const forwarded = String(req.headers["x-forwarded-for"] || "").split(",")[0].trim();
  return forwarded || req.socket.remoteAddress || "unknown";
}

function isProtectedEndpoint(pathname) {
  return pathname === "/v1/bitwise/analyze"
    || pathname === "/v1/billing/google-play/verify"
    || pathname === "/v1/chat/completions";
}

function playBillingConfigured() {
  return Boolean(
    process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
      || (process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL
        && process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY),
  );
}

function buildQuery(url, barcode) {
  return {
    barcode,
    productName: url.searchParams.get("productName") || "",
    brand: url.searchParams.get("brand") || "",
    category: url.searchParams.get("category") || "",
    zipCode: url.searchParams.get("zip") || "",
    latitude: url.searchParams.get("lat") || "",
    longitude: url.searchParams.get("lng") || "",
  };
}

async function handleRequest(req, res) {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const availabilityMatch = url.pathname.match(/^\/api\/retail\/products\/([^/]+)\/availability$/);
  const alternativesMatch = url.pathname.match(/^\/api\/retail\/products\/([^/]+)\/alternatives$/);
  const ingredientRagMatch = url.pathname.match(/^\/api\/retail\/products\/([^/]+)\/ingredients\/rag$/);
  const productMatch = url.pathname.match(/^\/api\/retail\/products\/([^/]+)$/);

  try {
    if (isProtectedEndpoint(url.pathname)) {
      const rateLimit = protectedRateLimiter.check(clientAddress(req));
      if (!rateLimit.allowed) {
        writeJson(
          res,
          429,
          { error: "Too many requests. Try again shortly." },
          { "Retry-After": String(rateLimit.retryAfterSeconds) },
        );
        return;
      }
    }

    if (req.method === "GET" && url.pathname === "/health") {
      writeJson(res, 200, {
        ok: true,
        service: "retailer-backend",
        bitwiseProvider: process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY ? "google-gemini" : "local-fallback",
        playBillingConfigured: playBillingConfigured(),
      });
      return;
    }

    if (req.method === "POST" && url.pathname === "/v1/bitwise/analyze") {
      const result = await handleBitwiseAnalysis(req);
      writeJson(res, result.status, result.body);
      return;
    }

    if (req.method === "POST" && url.pathname === "/v1/billing/google-play/verify") {
      const result = await handleGooglePlayVerification(req);
      writeJson(res, result.status, result.body);
      return;
    }

    // Kept temporarily so older installed app versions still receive Bitwise responses.
    if (req.method === "POST" && url.pathname === "/v1/chat/completions") {
      const result = await handleBitwiseCompletion(req, writeJson);
      writeJson(res, result.status, result.body);
      return;
    }

    if (productMatch) {
      const query = buildQuery(url, decodeURIComponent(productMatch[1]));
      writeJson(res, 200, await service.getProduct(query));
      return;
    }

    if (availabilityMatch) {
      const query = buildQuery(url, decodeURIComponent(availabilityMatch[1]));
      writeJson(res, 200, await service.getAvailability(query));
      return;
    }

    if (alternativesMatch) {
      const query = buildQuery(url, decodeURIComponent(alternativesMatch[1]));
      writeJson(res, 200, await service.getAlternatives(query));
      return;
    }

    if (ingredientRagMatch) {
      const query = buildQuery(url, decodeURIComponent(ingredientRagMatch[1]));
      writeJson(res, 200, await service.getIngredientRag(query));
      return;
    }

    writeJson(res, 404, {
      error: "Not found",
      endpoints: [
        "/api/retail/products/:barcode",
        "/api/retail/products/:barcode/availability",
        "/api/retail/products/:barcode/alternatives",
        "/api/retail/products/:barcode/ingredients/rag",
        "/v1/bitwise/analyze",
        "/v1/billing/google-play/verify",
      ],
    });
  } catch (error) {
    writeJson(res, 500, {
      error: error.message || "Retailer provider error",
    });
  }
}

http.createServer(handleRequest).listen(port, () => {
  console.log(`Retailer mock backend listening on http://localhost:${port}`);
});
