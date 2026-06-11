const { createWalmartAuthHeaders } = require("../walmartSignature");
const https = require("https");

const WALMART_AFFILIATES_BASE =
  "https://developer.api.walmart.com/api-proxy/service/affil/product/v2";

class WalmartAffiliatesProvider {
  constructor() {
    this.name = "WalmartAffiliatesProvider";
  }

  async getProduct(query) {
    const barcode = digitsOnly(query.barcode);
    if (!barcode) return null;

    const url = new URL(`${WALMART_AFFILIATES_BASE}/items`);
    url.searchParams.set("upc", barcode);

    const payload = await getJson(url, createWalmartAuthHeaders());
    const item = firstItem(payload);
    return item ? mapItemToProductResponse(item, barcode) : null;
  }

  async getAvailability(query) {
    const product = await this.getProduct(query);
    if (!product || !product.product) return [];

    return [
      {
        retailerName: "Walmart",
        providerName: this.name,
        availabilityStatus: "Product match",
        price: formatPrice(walmartPrice(product.product)),
        distance: "Online",
        fulfillment: "Shipping or pickup where available",
        productUrl: product.product.walmartProductUrl || walmartSearchUrl(query),
        note: "Check Walmart for current stock and delivery options.",
        available: true,
        priceValue: numericPrice(walmartPrice(product.product)),
        distanceValue: 0.0,
      },
    ];
  }

  async getAlternatives() {
    return [];
  }
}

function firstItem(payload) {
  if (!payload) return null;
  if (Array.isArray(payload.items) && payload.items.length > 0) return payload.items[0];
  if (Array.isArray(payload) && payload.length > 0) return payload[0];
  if (payload.item) return payload.item;
  return null;
}

function getJson(url, headers) {
  return new Promise((resolve, reject) => {
    const request = https.get(url, { headers }, (response) => {
      let body = "";

      response.setEncoding("utf8");
      response.on("data", (chunk) => {
        body += chunk;
      });
      response.on("end", () => {
        if (response.statusCode === 404) {
          resolve(null);
          return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`Walmart product lookup failed with HTTP ${response.statusCode}: ${body}`));
          return;
        }

        try {
          resolve(JSON.parse(body));
        } catch (error) {
          reject(error);
        }
      });
    });

    request.on("error", reject);
  });
}

function mapItemToProductResponse(item, barcode) {
  const ingredientsText = extractIngredientsText(item);

  return {
    status: 1,
    product: {
      product_name: firstNonEmpty(item.name, item.productName, item.title),
      product_name_en: firstNonEmpty(item.name, item.productName, item.title),
      brands: firstNonEmpty(item.brandName, item.brand, item.manufacturer),
      quantity: firstNonEmpty(item.size, item.netWeight, item.packageSize),
      image_url: firstNonEmpty(item.largeImage, item.mediumImage, item.thumbnailImage, item.imageUrl),
      image_front_url: firstNonEmpty(item.largeImage, item.mediumImage, item.thumbnailImage, item.imageUrl),
      categories: firstNonEmpty(item.categoryPath, item.categoryNode, item.categoryName),
      categories_en: firstNonEmpty(item.categoryPath, item.categoryNode, item.categoryName),
      countries: "United States",
      countries_tags: ["en:united-states"],
      ingredients_text: ingredientsText,
      ingredients_text_en: ingredientsText,
      walmart_item_id: firstNonEmpty(item.itemId, item.usItemId, item.productId),
      walmart_product_url: firstNonEmpty(item.productTrackingUrl, item.productUrl, item.productPageUrl),
      walmart_sale_price: item.salePrice || item.price || null,
    },
  };
}

function extractIngredientsText(item) {
  const direct = firstNonEmpty(
    item.ingredients,
    item.ingredient,
    item.ingredientsText,
    item.ingredientStatement,
    item.ingredientList,
  );
  if (direct) return normalizeIngredientValue(direct);

  const found = findIngredientLikeValue(item, new Set());
  return normalizeIngredientValue(found);
}

function findIngredientLikeValue(value, seen) {
  if (!value || typeof value !== "object") return "";
  if (seen.has(value)) return "";
  seen.add(value);

  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findIngredientLikeValue(item, seen);
      if (found) return found;
    }
    return "";
  }

  for (const [key, child] of Object.entries(value)) {
    if (/ingredient/i.test(key) && typeof child === "string" && child.trim()) {
      return child;
    }
    if (/ingredient/i.test(key) && Array.isArray(child) && child.length > 0) {
      return child.map(normalizeIngredientValue).filter(Boolean).join(", ");
    }
  }

  for (const child of Object.values(value)) {
    const found = findIngredientLikeValue(child, seen);
    if (found) return found;
  }

  return "";
}

function normalizeIngredientValue(value) {
  if (!value) return "";
  if (Array.isArray(value)) {
    return value.map(normalizeIngredientValue).filter(Boolean).join(", ");
  }
  if (typeof value === "object") {
    return firstNonEmpty(value.name, value.text, value.value);
  }
  return String(value).replace(/\s+/g, " ").trim();
}

function walmartSearchUrl(query) {
  const search = [query.brand, query.productName, query.barcode].filter(Boolean).join(" ");
  return `https://www.walmart.com/search?q=${encodeURIComponent(search)}`;
}

function formatPrice(value) {
  if (typeof value === "number") return `$${value.toFixed(2)}`;
  if (typeof value === "string" && value.trim()) return value.trim().startsWith("$") ? value.trim() : `$${value.trim()}`;
  return "See Walmart";
}

function walmartPrice(product) {
  if (!product) return null;
  return product.walmartSalePrice || product.walmart_sale_price || null;
}

function numericPrice(value) {
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = Number(value.replace(/[^0-9.]+/g, ""));
    return Number.isFinite(parsed) ? parsed : 0.0;
  }
  return 0.0;
}

function firstNonEmpty(...values) {
  for (const value of values) {
    if (value == null) continue;
    const text = typeof value === "string" ? value.trim() : String(value).trim();
    if (text) return text;
  }
  return "";
}

function digitsOnly(value) {
  return String(value || "").replace(/\D/g, "");
}

module.exports = {
  WalmartAffiliatesProvider,
};
