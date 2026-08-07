const OPEN_FOOD_FACTS_BASE_URL = "https://world.openfoodfacts.org/api/v2/product";

class OpenFoodFactsProvider {
  constructor(fetchImpl = global.fetch) {
    this.name = "OpenFoodFactsProvider";
    this.fetch = fetchImpl;
  }

  async getProduct(query) {
    const barcode = digitsOnly(query && query.barcode);
    if (!barcode || typeof this.fetch !== "function") return null;

    const fields = [
      "code",
      "product_name",
      "product_name_en",
      "brands",
      "ingredients_text",
      "ingredients_text_en",
    ].join(",");
    const response = await this.fetch(
      `${OPEN_FOOD_FACTS_BASE_URL}/${encodeURIComponent(barcode)}.json?fields=${encodeURIComponent(fields)}`,
      {
        headers: {
          Accept: "application/json",
          "User-Agent": "WhatsOnMyFood/0.13 (hosted ingredient recovery)",
        },
      },
    );
    if (!response.ok) {
      throw new Error(`Open Food Facts returned HTTP ${response.status}`);
    }

    const payload = await response.json();
    if (payload.status !== 1 || !payload.product) return null;
    return {
      status: 1,
      code: payload.code || barcode,
      product: payload.product,
    };
  }
}

function digitsOnly(value) {
  return String(value || "").replace(/\D/g, "");
}

module.exports = {
  OpenFoodFactsProvider,
  digitsOnly,
};
