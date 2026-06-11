const https = require("https");
const querystring = require("querystring");

const DEFAULT_ENDPOINT = "https://sellingpartnerapi-na.amazon.com";
const DEFAULT_MARKETPLACE_ID = "ATVPDKIKX0DER";
const LWA_TOKEN_URL = "https://api.amazon.com/auth/o2/token";

let cachedToken = null;

class AmazonSpApiProvider {
  constructor() {
    this.name = "AmazonSpApiProvider";
    this.endpoint = withoutTrailingSlash(process.env.AMAZON_SP_API_ENDPOINT || DEFAULT_ENDPOINT);
    this.marketplaceId = process.env.AMAZON_SP_API_MARKETPLACE_ID || DEFAULT_MARKETPLACE_ID;
  }

  async getProduct(query) {
    const barcode = digitsOnly(query.barcode);
    if (!barcode) return null;

    const catalog = await this.searchCatalogByUpc(barcode);
    const item = firstItem(catalog);
    if (!item) return null;

    const asin = item.asin || "";
    const pricing = asin ? await this.getPricing(asin) : null;
    return mapCatalogItemToProductResponse(item, pricing, barcode);
  }

  async getAvailability(query) {
    const product = await this.getProduct(query);
    if (!product || !product.product) return [];

    const price = amazonPrice(product.product);
    return [
      {
        retailerName: "Amazon",
        providerName: this.name,
        availabilityStatus: product.product.amazon_asin ? "Product match" : "Search available",
        price: formatPrice(price),
        distance: "Delivery",
        fulfillment: "Shipping",
        productUrl: product.product.amazon_product_url || amazonSearchUrl(query),
        note: "Check Amazon for current price and delivery options.",
        available: true,
        priceValue: numericPrice(price),
        distanceValue: 0.0,
      },
    ];
  }

  async getAlternatives() {
    return [];
  }

  async searchCatalogByUpc(upc) {
    const url = new URL(`${this.endpoint}/catalog/2022-04-01/items`);
    url.searchParams.set("identifiers", upc);
    url.searchParams.set("identifiersType", "UPC");
    url.searchParams.set("marketplaceIds", this.marketplaceId);
    url.searchParams.set("includedData", "summaries,images,attributes,identifiers");
    url.searchParams.set("pageSize", "1");

    return amazonJsonRequest("GET", url);
  }

  async getPricing(asin) {
    const url = new URL(`${this.endpoint}/products/pricing/v0/price`);
    url.searchParams.set("MarketplaceId", this.marketplaceId);
    url.searchParams.set("ItemType", "Asin");
    url.searchParams.set("Asins", asin);

    return amazonJsonRequest("GET", url);
  }
}

function hasAmazonCredentials() {
  return Boolean(
    process.env.AMAZON_SP_API_CLIENT_ID
      && process.env.AMAZON_SP_API_CLIENT_SECRET
      && process.env.AMAZON_SP_API_REFRESH_TOKEN
  );
}

async function amazonJsonRequest(method, url, body) {
  const token = await getLwaAccessToken();
  const headers = {
    "Accept": "application/json",
    "Host": url.host,
    "User-Agent": sanitizeHeader(process.env.AMAZON_SP_API_USER_AGENT || "YourHealthyPantry/1.0 (Language=JavaScript)"),
    "x-amz-date": amzDate(new Date()),
    "x-amz-access-token": token,
  };

  return requestJson(method, url, headers, body);
}

async function getLwaAccessToken() {
  const now = Date.now();
  if (cachedToken && cachedToken.accessToken && cachedToken.expiresAt > now + 60000) {
    return cachedToken.accessToken;
  }

  const body = querystring.stringify({
    grant_type: "refresh_token",
    refresh_token: process.env.AMAZON_SP_API_REFRESH_TOKEN,
    client_id: process.env.AMAZON_SP_API_CLIENT_ID,
    client_secret: process.env.AMAZON_SP_API_CLIENT_SECRET,
  });

  const url = new URL(LWA_TOKEN_URL);
  const payload = await requestJson("POST", url, {
    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
    "Content-Length": Buffer.byteLength(body),
  }, body);

  if (!payload || !payload.access_token) {
    throw new Error("Amazon SP-API LWA token response did not include an access token.");
  }

  const expiresIn = Number(payload.expires_in || 3600);
  cachedToken = {
    accessToken: payload.access_token,
    expiresAt: now + Math.max(60, expiresIn) * 1000,
  };
  return cachedToken.accessToken;
}

function requestJson(method, url, headers, body) {
  return new Promise((resolve, reject) => {
    const request = https.request(url, { method, headers }, (response) => {
      let responseBody = "";
      response.setEncoding("utf8");
      response.on("data", (chunk) => {
        responseBody += chunk;
      });
      response.on("end", () => {
        if (response.statusCode === 404) {
          resolve(null);
          return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`Amazon SP-API request failed with HTTP ${response.statusCode}: ${responseBody}`));
          return;
        }
        if (!responseBody.trim()) {
          resolve(null);
          return;
        }
        try {
          resolve(JSON.parse(responseBody));
        } catch (error) {
          reject(error);
        }
      });
    });

    request.on("error", reject);
    if (body) request.write(body);
    request.end();
  });
}

function mapCatalogItemToProductResponse(item, pricing, barcode) {
  const summary = firstSummary(item);
  const attributes = item.attributes || {};
  const title = firstNonEmpty(summary.itemName, attributeValue(attributes.item_name), attributeValue(attributes.title));
  const brand = firstNonEmpty(summary.brandName, attributeValue(attributes.brand), attributeValue(attributes.manufacturer));
  const imageUrl = firstImageUrl(item);
  const price = firstPrice(pricing);
  const asin = item.asin || "";

  return {
    status: 1,
    product: {
      product_name: title,
      product_name_en: title,
      brands: brand,
      quantity: firstNonEmpty(attributeValue(attributes.size), attributeValue(attributes.unit_count)),
      image_url: imageUrl,
      image_front_url: imageUrl,
      categories: firstNonEmpty(summary.browseClassification && summary.browseClassification.displayName),
      categories_en: firstNonEmpty(summary.browseClassification && summary.browseClassification.displayName),
      countries: "United States",
      countries_tags: ["en:united-states"],
      ingredients_text: firstNonEmpty(attributeValue(attributes.ingredients), attributeValue(attributes.ingredients_text)),
      ingredients_text_en: firstNonEmpty(attributeValue(attributes.ingredients), attributeValue(attributes.ingredients_text)),
      amazon_asin: asin,
      amazon_upc: barcode,
      amazon_product_url: asin ? `https://www.amazon.com/dp/${encodeURIComponent(asin)}` : "",
      amazon_price: price,
    },
  };
}

function firstItem(payload) {
  if (!payload) return null;
  if (Array.isArray(payload.items) && payload.items.length > 0) return payload.items[0];
  return null;
}

function firstSummary(item) {
  if (!item || !Array.isArray(item.summaries) || item.summaries.length === 0) return {};
  return item.summaries[0] || {};
}

function firstImageUrl(item) {
  if (!item || !Array.isArray(item.images)) return "";
  for (const imageGroup of item.images) {
    const images = imageGroup && Array.isArray(imageGroup.images) ? imageGroup.images : [];
    for (const image of images) {
      const link = firstNonEmpty(image.link, image.url);
      if (link) return link;
    }
  }
  return "";
}

function firstPrice(pricing) {
  const products = pricing && pricing.payload && Array.isArray(pricing.payload) ? pricing.payload : [];
  for (const product of products) {
    const price = product && product.Product && product.Product.Offers
      && product.Product.Offers[0]
      && product.Product.Offers[0].BuyingPrice
      && product.Product.Offers[0].BuyingPrice.ListingPrice;
    if (price && price.Amount != null) return price.Amount;
  }
  return null;
}

function amazonPrice(product) {
  if (!product) return null;
  return product.amazonPrice || product.amazon_price || null;
}

function attributeValue(values) {
  if (!Array.isArray(values) || values.length === 0) return "";
  const first = values[0];
  if (first == null) return "";
  if (typeof first === "string" || typeof first === "number") return String(first);
  if (first.value != null) return String(first.value);
  if (first.displayValue != null) return String(first.displayValue);
  if (first.name != null) return String(first.name);
  return "";
}

function amazonSearchUrl(query) {
  const search = [query.brand, query.productName, query.barcode].filter(Boolean).join(" ");
  return `https://www.amazon.com/s?k=${encodeURIComponent(search)}`;
}

function formatPrice(value) {
  if (typeof value === "number") return `$${value.toFixed(2)}`;
  if (typeof value === "string" && value.trim()) return value.trim().startsWith("$") ? value.trim() : `$${value.trim()}`;
  return "See Amazon";
}

function numericPrice(value) {
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = Number(value.replace(/[^0-9.]+/g, ""));
    return Number.isFinite(parsed) ? parsed : 0.0;
  }
  return 0.0;
}

function firstNonEmpty() {
  for (let i = 0; i < arguments.length; i += 1) {
    const value = arguments[i];
    if (value == null) continue;
    const text = typeof value === "string" ? value.trim() : String(value).trim();
    if (text) return text;
  }
  return "";
}

function digitsOnly(value) {
  return String(value || "").replace(/\D/g, "");
}

function withoutTrailingSlash(value) {
  return String(value || "").replace(/\/+$/, "");
}

function amzDate(date) {
  return date.toISOString().replace(/[:-]|\.\d{3}/g, "");
}

function sanitizeHeader(value) {
  return String(value || "").replace(/[\r\n]+/g, " ").trim();
}

module.exports = {
  AmazonSpApiProvider,
  hasAmazonCredentials,
};
