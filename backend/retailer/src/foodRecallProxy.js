const OPENFDA_ENDPOINT = "https://api.fda.gov/food/enforcement.json";
const RESULT_LIMIT = 100;
const MAX_ATTEMPTS = 2;
const DEFAULT_TIMEOUT_MS = 10_000;
const STOP_WORDS = new Set([
  "and", "the", "with", "from", "for", "food", "foods", "brand", "original",
  "organic", "natural", "style", "flavor", "flavored", "product",
]);

function normalize(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/\p{M}+/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim()
    .replace(/\s+/g, " ");
}

function significantTokens(value) {
  const tokens = [];
  for (const token of normalize(value).split(" ")) {
    if (token.length >= 3 && !STOP_WORDS.has(token) && !tokens.includes(token)) {
      tokens.push(token);
    }
  }
  return tokens;
}

function phrase(value, limit) {
  return significantTokens(value).slice(0, limit).join(" ");
}

function buildSearchQuery({ productName, brand }) {
  const namePhrase = phrase(productName, 3);
  const brandPhrase = phrase(brand, 2);
  const clauses = [];
  if (namePhrase) clauses.push(`product_description:"${namePhrase}"`);
  if (brandPhrase && brandPhrase !== namePhrase) {
    clauses.push(`product_description:"${brandPhrase}"`);
  }
  if (!clauses.length) {
    const error = new Error("Product name or brand is required");
    error.code = "invalid_product";
    throw error;
  }
  return clauses.join(" OR ");
}

function buildOpenFdaUrl(query, apiKey = process.env.OPENFDA_API_KEY) {
  const url = new URL(OPENFDA_ENDPOINT);
  url.searchParams.set("search", buildSearchQuery(query));
  url.searchParams.set("sort", "report_date:desc");
  url.searchParams.set("limit", String(RESULT_LIMIT));
  if (String(apiKey || "").trim()) {
    url.searchParams.set("api_key", String(apiKey).trim());
  }
  return url;
}

function safeText(value) {
  return typeof value === "string" ? value.trim() : "";
}

function normalizePayload(payload) {
  const results = Array.isArray(payload?.results) ? payload.results : [];
  return {
    meta: { last_updated: safeText(payload?.meta?.last_updated) },
    results: results.slice(0, RESULT_LIMIT).map((record) => ({
      recall_number: safeText(record?.recall_number),
      product_description: safeText(record?.product_description),
      recalling_firm: safeText(record?.recalling_firm),
      classification: safeText(record?.classification),
      reason_for_recall: safeText(record?.reason_for_recall),
      code_info: safeText(record?.code_info),
      report_date: safeText(record?.report_date),
      status: safeText(record?.status),
    })),
  };
}

function isTransient(status) {
  return status === 429 || status === 502 || status === 503 || status === 504;
}

async function defaultSleep(delayMs) {
  await new Promise((resolve) => setTimeout(resolve, delayMs));
}

async function fetchAttempt(url, { fetchImpl, timeoutMs }) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetchImpl(url, {
      headers: {
        Accept: "application/json",
        "User-Agent": "WhatsOnMyFood-Backend",
      },
      signal: controller.signal,
    });
  } finally {
    clearTimeout(timeout);
  }
}

async function fetchFoodRecalls(query, {
  fetchImpl = globalThis.fetch,
  apiKey = process.env.OPENFDA_API_KEY,
  timeoutMs = DEFAULT_TIMEOUT_MS,
  sleep = defaultSleep,
} = {}) {
  const upstreamUrl = buildOpenFdaUrl(query, apiKey);
  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt += 1) {
    let response;
    try {
      response = await fetchAttempt(upstreamUrl, { fetchImpl, timeoutMs });
    } catch (error) {
      if (attempt + 1 < MAX_ATTEMPTS) {
        await sleep(250);
        continue;
      }
      const unavailable = new Error("FDA recall source is temporarily unavailable");
      unavailable.code = "upstream_unavailable";
      unavailable.cause = error;
      throw unavailable;
    }

    if (response.status === 404) {
      return normalizePayload({ results: [] });
    }
    if (isTransient(response.status) && attempt + 1 < MAX_ATTEMPTS) {
      await sleep(250);
      continue;
    }
    if (!response.ok) {
      const unavailable = new Error("FDA recall source request failed");
      unavailable.code = isTransient(response.status) ? "upstream_unavailable" : "upstream_error";
      unavailable.status = response.status;
      throw unavailable;
    }

    let payload;
    try {
      payload = JSON.parse(await response.text());
    } catch (error) {
      const invalid = new Error("FDA recall source returned invalid JSON");
      invalid.code = "invalid_upstream_response";
      throw invalid;
    }
    if (!payload || typeof payload !== "object" || !Array.isArray(payload.results)) {
      const invalid = new Error("FDA recall source returned an invalid response");
      invalid.code = "invalid_upstream_response";
      throw invalid;
    }
    return normalizePayload(payload);
  }
  throw new Error("FDA recall source request did not complete");
}

async function handleFoodRecallCheck(url, options = {}) {
  const query = {
    barcode: safeText(url.searchParams.get("barcode")).slice(0, 32),
    productName: safeText(url.searchParams.get("productName")).slice(0, 160),
    brand: safeText(url.searchParams.get("brand")).slice(0, 120),
  };
  try {
    // Validate the product first so callers receive a useful 400 response even when
    // a local development environment has not configured the provider yet.
    buildSearchQuery(query);
    const apiKey = options.apiKey === undefined
      ? process.env.OPENFDA_API_KEY
      : options.apiKey;
    if (!String(apiKey || "").trim()) {
      return { status: 503, body: { error: "The food recall service is not configured." } };
    }
    return {
      status: 200,
      body: await fetchFoodRecalls(query, { ...options, apiKey }),
    };
  } catch (error) {
    if (error.code === "invalid_product") {
      return { status: 400, body: { error: "A product name or brand is required." } };
    }
    if (error.code === "upstream_unavailable") {
      return { status: 503, body: { error: "The FDA recall source is temporarily unavailable." } };
    }
    return { status: 502, body: { error: "The FDA recall source returned an invalid response." } };
  }
}

module.exports = {
  OPENFDA_ENDPOINT,
  RESULT_LIMIT,
  buildOpenFdaUrl,
  buildSearchQuery,
  fetchFoodRecalls,
  handleFoodRecallCheck,
  normalizePayload,
};
