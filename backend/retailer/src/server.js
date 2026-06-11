const http = require("http");
const { URL } = require("url");
const { createProviderRegistry } = require("./providerRegistry");
const { RetailerService } = require("./retailerService");
const { handleBitwiseCompletion } = require("./bitwiseFallback");

const service = new RetailerService(createProviderRegistry());
const port = Number(process.env.PORT || 8787);

function writeJson(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
  });
  res.end(JSON.stringify(body, null, 2));
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
    if (req.method === "GET" && url.pathname === "/health") {
      writeJson(res, 200, { ok: true, service: "retailer-backend" });
      return;
    }

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
