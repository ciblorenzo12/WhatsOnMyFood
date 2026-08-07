const crypto = require("crypto");

const CORRELATION_HEADER = "x-correlation-id";
const LOG_PREFIX = "[privacy-safe-request]";
const SAFE_CATEGORIES = new Set([
  "none",
  "authentication",
  "configuration",
  "invalid_request",
  "invalid_response",
  "not_found",
  "provider_unavailable",
  "rate_limit",
  "server_error",
  "timeout",
]);
const SAFE_OUTCOMES = new Set([
  "success",
  "empty_result",
  "fallback_success",
  "failure",
  "rate_limited",
]);

function normalizeRoute(pathname) {
  if (pathname === "/v1/bitwise/analyze") return "/v1/bitwise/analyze";
  if (pathname === "/v1/chat/completions") return "/v1/chat/completions";
  if (/^\/api\/retail\/products\/[^/]+\/ingredients\/rag$/.test(pathname)) {
    return "/api/retail/products/:barcode/ingredients/rag";
  }
  return "";
}

function safeCorrelationId(value) {
  const candidate = String(value || "").trim();
  return /^[A-Za-z0-9_-]{8,64}$/.test(candidate) ? candidate : crypto.randomUUID();
}

function classifyErrorCategory({ status = 0, error, category = "" } = {}) {
  if (SAFE_CATEGORIES.has(category)) return category;
  const code = String(error?.code || "").toUpperCase();
  const name = String(error?.name || "").toLowerCase();
  const message = String(error?.message || error || "").toLowerCase();

  if (status === 429) return "rate_limit";
  if (status === 401 || status === 403) return "authentication";
  if (status === 404) return "not_found";
  if (status === 400 || status === 413 || status === 422) return "invalid_request";
  if (code.includes("TIMEOUT") || name.includes("timeout") || message.includes("timed out") || message.includes("timeout")) {
    return "timeout";
  }
  if (/\b429\b/.test(message) || message.includes("rate limit")) return "rate_limit";
  if (message.includes("invalid json") || message.includes("html") || message.includes("malformed")
      || message.includes("empty response") || message.includes("unusable")) {
    return "invalid_response";
  }
  if ([502, 503, 504].includes(status) || /\b(?:502|503|504)\b/.test(message)
      || code === "ECONNREFUSED" || code === "ENOTFOUND"
      || message.includes("provider") || message.includes("unavailable") || message.includes("gemini")) {
    return "provider_unavailable";
  }
  if (status >= 500) return "server_error";
  return status >= 400 || error ? "server_error" : "none";
}

function buildDiagnostic({ correlationId, route, status = 0, latencyMs = 0, outcome, errorCategory } = {}) {
  const safeRoute = normalizeRoute(route) || "protected_route";
  const safeStatus = Number.isInteger(Number(status)) ? Number(status) : 0;
  const safeOutcome = SAFE_OUTCOMES.has(outcome)
    ? outcome
    : (safeStatus >= 200 && safeStatus < 400 ? "success" : "failure");
  const safeCategory = SAFE_CATEGORIES.has(errorCategory)
    ? errorCategory
    : classifyErrorCategory({ status: safeStatus });

  return {
    event: "protected_request",
    correlationId: safeCorrelationId(correlationId),
    route: safeRoute,
    outcome: safeOutcome,
    status: safeStatus,
    latencyMs: Math.max(0, Math.round(Number(latencyMs) || 0)),
    errorCategory: safeCategory,
  };
}

function formatDiagnostic(diagnostic) {
  return `${LOG_PREFIX} ${JSON.stringify(diagnostic)}`;
}

function createRequestObserver({ req, res, pathname, logger = console.log, now = Date.now }) {
  const route = normalizeRoute(pathname);
  if (!route) {
    return {
      correlationId: "",
      setResult() {},
      setError() {},
    };
  }

  const correlationId = safeCorrelationId(req.headers[CORRELATION_HEADER]);
  const startedAt = now();
  let outcome = "";
  let errorCategory = "";
  res.setHeader("X-Correlation-ID", correlationId);

  res.once("finish", () => {
    const status = Number(res.statusCode || 0);
    logger(formatDiagnostic(buildDiagnostic({
      correlationId,
      route,
      status,
      latencyMs: now() - startedAt,
      outcome,
      errorCategory: errorCategory || classifyErrorCategory({ status }),
    })));
  });

  return {
    correlationId,
    setResult(nextOutcome, nextCategory = "") {
      outcome = SAFE_OUTCOMES.has(nextOutcome) ? nextOutcome : "";
      errorCategory = SAFE_CATEGORIES.has(nextCategory) ? nextCategory : "";
    },
    setError(error, category = "") {
      outcome = "failure";
      errorCategory = classifyErrorCategory({ error, category });
    },
  };
}

module.exports = {
  CORRELATION_HEADER,
  LOG_PREFIX,
  buildDiagnostic,
  classifyErrorCategory,
  createRequestObserver,
  formatDiagnostic,
  normalizeRoute,
  safeCorrelationId,
};
