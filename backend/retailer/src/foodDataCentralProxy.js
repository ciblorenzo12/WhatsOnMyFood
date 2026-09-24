const { createHash } = require("node:crypto");

const SOURCE = "USDA FoodData Central";
const API_ROOT = "https://api.nal.usda.gov/fdc/v1/";
const DEFAULT_TIMEOUT_MS = 9_000;
const MAX_RESPONSE_BYTES = 1_048_576;
const PAGE_SIZE = 200;
const MAX_PAGES = 2;
const MAX_DETAILS = 4;
const SAFE_ERROR = Symbol("safe_provider_error");

function createLookupState() {
  return { cache: new Map(), inFlight: new Map() };
}
const sharedState = createLookupState();

function safeText(value, limit = 500) {
  return typeof value === "string" ? value.trim().slice(0, limit) : "";
}

function normalizeGtin(value) {
  if (typeof value !== "string" || !/^(?:\d{8}|\d{12}|\d{13}|\d{14})$/.test(value) || /^0+$/.test(value)) return null;
  let sum = 0;
  for (let index = value.length - 2, weight = 3; index >= 0; index -= 1, weight = 4 - weight) {
    sum += Number(value[index]) * weight;
  }
  if ((10 - sum % 10) % 10 !== Number(value.at(-1))) return null;
  return value.padStart(14, "0");
}

function barcodeVariants(canonical) {
  return [8, 12, 13, 14].map(length => canonical.slice(-length))
    .filter(value => normalizeGtin(value) === canonical);
}

function failure(code, status, message, headers) {
  const error = new Error(message);
  Object.assign(error, { code, status, headers, [SAFE_ERROR]: true });
  return error;
}

function invalidPayload() {
  return failure("invalid_upstream_response", 502, "USDA returned an incomplete or invalid response. Please try again later.");
}

function isUsMarket(value) {
  return /^(?:united states(?: of america)?|us|usa)$/i.test(safeText(value));
}

function positiveInteger(value) {
  return Number.isSafeInteger(value) && value > 0;
}

function nonnegativeNumber(value) {
  return typeof value === "number" && Number.isFinite(value) && value >= 0;
}

// Nutrient IDs are USDA nutrient IDs (not the individual foodNutrient row IDs).
// Full food details report these per 100 units, not per serving. Gram-based
// nutrition fits the app's contract; mL data must not be relabeled as grams.
const NUTRIENTS = new Map([
  [1003, ["proteins_100g", "g"]],
  [1004, ["fat_100g", "g"]],
  [1005, ["carbohydrates_100g", "g"]],
  [1008, ["energy-kcal_100g", "kcal"]],
  [1062, ["energy-kj_100g", "kj"]],
  [1063, ["sugars_100g", "g"]],
  [2000, ["sugars_100g", "g"]],
  [1235, ["added-sugars_100g", "g"]],
  [1079, ["fiber_100g", "g"]],
  [1093, ["sodium_100g", "g"]],
  [1253, ["cholesterol_100g", "g"]],
  [1257, ["trans-fat_100g", "g"]],
  [1258, ["saturated-fat_100g", "g"]],
  [1292, ["monounsaturated-fat_100g", "g"]],
  [1293, ["polyunsaturated-fat_100g", "g"]],
  [1087, ["calcium_100g", "g"]],
  [1089, ["iron_100g", "g"]],
  [1092, ["potassium_100g", "g"]],
]);

function mapNutriments(food) {
  const basis = safeText(food.servingSizeUnit).toLowerCase();
  const nutrients = {};
  const warnings = [];
  if (basis !== "g") {
    warnings.push("Per-100g nutrition is unavailable: the source uses a volume or unspecified serving basis.");
    return { nutrients, warnings, nutrientBasis: basis === "ml" ? "per100ml_not_converted" : "unknown" };
  }
  if (food.foodNutrients != null && !Array.isArray(food.foodNutrients)) throw invalidPayload();
  const conflicts = new Set();
  for (const row of food.foodNutrients || []) {
    const mapping = NUTRIENTS.get(row?.nutrient?.id);
    if (!mapping || !nonnegativeNumber(row.amount)) continue;
    const [key, expectedUnit] = mapping;
    const unit = safeText(row.nutrient.unitName).toLowerCase();
    let value = row.amount;
    if (unit !== expectedUnit) {
      if (expectedUnit === "g" && unit === "mg") value /= 1000;
      else if (expectedUnit === "g" && (unit === "ug" || unit === "µg")) value /= 1_000_000;
      else continue;
    }
    if (Object.hasOwn(nutrients, key) && Math.abs(nutrients[key] - value) > 1e-8) conflicts.add(key);
    nutrients[key] = value;
  }
  for (const key of conflicts) delete nutrients[key];
  if (conflicts.size) warnings.push("Conflicting nutrient values were left unknown.");
  if (Object.hasOwn(nutrients, "sodium_100g")) nutrients.salt_100g = nutrients.sodium_100g * 2.5;
  return { nutrients, warnings, nutrientBasis: "per100g" };
}

function mapProduct(food, canonical) {
  const { nutrients, warnings, nutrientBasis } = mapNutriments(food);
  const servingUnit = safeText(food.servingSizeUnit, 10);
  const serving = nonnegativeNumber(food.servingSize) && food.servingSize > 0 && servingUnit
    ? `${food.servingSize} ${servingUnit}` : safeText(food.householdServingFullText, 160);
  return {
    status: 1,
    code: canonical,
    source: SOURCE,
    provenance: {
      fdcId: food.fdcId,
      gtinUpc: safeText(food.gtinUpc, 14),
      publicationDate: safeText(food.publicationDate, 40),
      modifiedDate: safeText(food.modifiedDate, 40),
      marketCountry: "United States",
      dataType: "Branded",
      dataSource: safeText(food.dataSource, 100),
      nutrientBasis,
      sourceUrl: `https://fdc.nal.usda.gov/food-details/${food.fdcId}/nutrients`,
      warnings,
    },
    product: {
      product_name: safeText(food.description),
      brands: [...new Set([safeText(food.brandName), safeText(food.brandOwner)].filter(Boolean))].join(", "),
      quantity: safeText(food.packageWeight, 160),
      categories: safeText(food.brandedFoodCategory),
      countries: "United States",
      countries_tags: ["en:united-states"],
      serving_size: serving,
      ingredients_text: safeText(food.ingredients, 20_000),
      nutriments: nutrients,
    },
  };
}

function retryAfterSeconds(response, now) {
  const value = response.headers?.get("retry-after") || "";
  let seconds = /^\d+$/.test(value) ? Number(value) : Math.ceil((Date.parse(value) - now()) / 1000);
  if (!Number.isFinite(seconds) || seconds < 1) seconds = 60;
  return String(Math.min(seconds, 3600));
}

async function readBoundedJson(response, signal, maxBytes) {
  const declaredSize = Number(response.headers?.get("content-length"));
  if (declaredSize > maxBytes || !response.body?.getReader) {
    if (response.body?.cancel) void response.body.cancel().catch(() => {});
    throw invalidPayload();
  }
  if (signal.aborted) {
    void response.body.cancel().catch(() => {});
    throw failure("upstream_timeout", 504, "The USDA lookup timed out. Please try again.");
  }
  const reader = response.body.getReader();
  const cancel = () => { void reader.cancel().catch(() => {}); };
  signal.addEventListener("abort", cancel, { once: true });
  const chunks = [];
  let bytes = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (signal.aborted) throw failure("upstream_timeout", 504, "The USDA lookup timed out. Please try again.");
      if (done) break;
      bytes += value.byteLength;
      if (bytes > maxBytes) {
        cancel();
        throw invalidPayload();
      }
      chunks.push(Buffer.from(value));
    }
    try {
      return JSON.parse(Buffer.concat(chunks).toString("utf8"));
    } catch {
      throw invalidPayload();
    }
  } finally {
    signal.removeEventListener("abort", cancel);
    reader.releaseLock();
  }
}

async function requestJson(path, context, body) {
  const url = new URL(path, API_ROOT);
  url.searchParams.set("api_key", context.apiKey);
  // Redirects are not followed: the provider credential must stay at this host.
  const response = await context.fetchImpl(url, {
    method: body ? "POST" : "GET",
    redirect: "error",
    headers: { Accept: "application/json", ...(body ? { "Content-Type": "application/json" } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}),
    signal: context.signal,
  });
  if (!response.ok) {
    if (response.body?.cancel) void response.body.cancel().catch(() => {});
    if (response.status === 429) {
      throw failure("rate_limited", 429, "USDA is temporarily busy. Please try again later.", {
        "Retry-After": retryAfterSeconds(response, context.now),
      });
    }
    // Do not read or expose upstream error bodies; they may include request URLs.
    throw failure("upstream_unavailable", 502, "USDA is temporarily unavailable. Please try again later.");
  }
  return readBoundedJson(response, context.signal, context.maxResponseBytes);
}

function newestFirst(a, b) {
  const aTime = Date.parse(a.publicationDate || a.publishedDate) || 0;
  const bTime = Date.parse(b.publicationDate || b.publishedDate) || 0;
  return bTime - aTime || b.fdcId - a.fdcId;
}

async function lookup(canonical, context) {
  const candidates = new Map();
  for (let page = 1; page <= MAX_PAGES; page += 1) {
    const payload = await requestJson("foods/search", context, {
      query: barcodeVariants(canonical).map(value => `"${value}"`).join(" OR "),
      dataType: ["Branded"],
      pageSize: PAGE_SIZE,
      pageNumber: page,
      sortBy: "publishedDate",
      sortOrder: "desc",
    });
    if (!payload || !Array.isArray(payload.foods) || payload.foods.length > PAGE_SIZE
        || !Number.isSafeInteger(payload.totalHits) || payload.totalHits < 0
        || payload.foods.length !== Math.min(PAGE_SIZE, Math.max(0, payload.totalHits - (page - 1) * PAGE_SIZE))) {
      throw invalidPayload();
    }
    for (const food of payload.foods) {
      if (!food || !positiveInteger(food.fdcId) || !safeText(food.dataType)
          || (food.dataType === "Branded" && typeof food.gtinUpc !== "string")) throw invalidPayload();
      if (normalizeGtin(food?.gtinUpc) !== canonical || food?.dataType !== "Branded") continue;
      // Older search responses can omit marketCountry, so details must verify it.
      if (safeText(food.marketCountry) && !isUsMarket(food.marketCountry)) continue;
      candidates.set(food.fdcId, food);
    }
    if (page * PAGE_SIZE >= payload.totalHits) break;
    if (payload.foods.length !== PAGE_SIZE || page === MAX_PAGES) {
      throw failure("search_limit_exceeded", 502, "USDA could not confirm a single product within the lookup limit.");
    }
  }
  const ordered = [...candidates.values()].sort(newestFirst);
  for (const candidate of ordered.slice(0, MAX_DETAILS)) {
    const food = await requestJson(`food/${candidate.fdcId}?format=full`, context);
    if (!food || food.fdcId !== candidate.fdcId || normalizeGtin(food.gtinUpc) !== canonical
        || food.dataType !== "Branded" || !safeText(food.description)) throw invalidPayload();
    // Fail closed for an unknown market rather than silently assuming a US label.
    if (!isUsMarket(food.marketCountry)) continue;
    return mapProduct(food, canonical);
  }
  if (ordered.length > MAX_DETAILS) {
    throw failure("search_limit_exceeded", 502, "USDA could not confirm a single product within the lookup limit.");
  }
  return { status: 0, code: canonical, source: SOURCE, reason: "no_matching_us_product" };
}

async function timedLookup(canonical, options) {
  const controller = new AbortController();
  let timeout;
  const timedOut = new Promise((resolve, reject) => {
    timeout = setTimeout(() => {
      reject(failure("upstream_timeout", 504, "The USDA lookup timed out. Please try again."));
      controller.abort();
    }, options.timeoutMs);
  });
  try {
    // The same deadline covers search, detail requests, and all response body reads.
    return await Promise.race([lookup(canonical, { ...options, signal: controller.signal }), timedOut]);
  } finally {
    clearTimeout(timeout);
  }
}

async function handleFoodDataCentralLookup(url, options = {}) {
  const requestedBarcode = url.searchParams.get("barcode");
  const canonical = normalizeGtin(requestedBarcode);
  if (!canonical || url.searchParams.getAll("barcode").length !== 1) {
    return { status: 400, body: { code: "invalid_barcode", error: "A valid GTIN-8, UPC, EAN-13, or GTIN-14 barcode is required." } };
  }
  // The key is server configuration only. No request field or Android key is used.
  const apiKey = safeText((options.env || process.env).FDC_API_KEY, 256);
  if (!apiKey || apiKey.toUpperCase() === "DEMO_KEY") {
    return { status: 503, body: { code: "provider_not_configured", error: "The USDA fallback is not configured." } };
  }
  const state = options.state || sharedState;
  const now = options.now || Date.now;
  const key = `${createHash("sha256").update(apiKey).digest("hex")}:${canonical}`;
  const respond = body => ({ status: 200, body: { ...structuredClone(body), code: requestedBarcode } });
  try {
    for (const [cacheKey, entry] of state.cache) {
      if (entry.expiresAt <= now()) state.cache.delete(cacheKey);
    }
    const cached = state.cache.get(key);
    if (cached) {
      state.cache.delete(key);
      state.cache.set(key, cached);
      return respond(cached.body);
    }
    let pending = state.inFlight.get(key);
    if (!pending) {
      if (state.inFlight.size >= 32) {
        throw failure("service_busy", 503, "The USDA fallback is busy. Please try again later.");
      }
      pending = timedLookup(canonical, {
        apiKey,
        fetchImpl: options.fetchImpl || globalThis.fetch,
        timeoutMs: Math.max(1, Math.min(options.timeoutMs || DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS)),
        maxResponseBytes: Math.max(1, Math.min(options.maxResponseBytes || MAX_RESPONSE_BYTES, MAX_RESPONSE_BYTES)),
        now,
      }).then(body => {
        const ttl = body.status === 1 ? 300_000 : 30_000;
        state.cache.set(key, { body, expiresAt: now() + ttl });
        while (state.cache.size > 256) state.cache.delete(state.cache.keys().next().value);
        return body;
      }).finally(() => state.inFlight.delete(key));
      state.inFlight.set(key, pending);
    }
    return respond(await pending);
  } catch (error) {
    const safe = error?.[SAFE_ERROR] === true;
    return {
      status: safe ? error.status : 502,
      body: {
        code: safe ? error.code : "upstream_unavailable",
        error: safe ? error.message : "USDA is temporarily unavailable. Please try again later.",
      },
      ...(safe && error.headers ? { headers: error.headers } : {}),
    };
  }
}

module.exports = { handleFoodDataCentralLookup, normalizeGtin, mapNutriments, createLookupState };
