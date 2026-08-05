function createRateLimiter({ windowMs = 60_000, maxRequests = 20, now = Date.now } = {}) {
  const buckets = new Map();
  let checks = 0;

  return {
    check(key) {
      const timestamp = now();
      checks += 1;
      if (checks % 500 === 0) {
        for (const [bucketKey, bucket] of buckets) {
          if (timestamp >= bucket.resetAt) buckets.delete(bucketKey);
        }
      }
      const current = buckets.get(key);
      if (!current || timestamp >= current.resetAt) {
        buckets.set(key, { count: 1, resetAt: timestamp + windowMs });
        return { allowed: true, remaining: Math.max(0, maxRequests - 1), retryAfterSeconds: 0 };
      }

      if (current.count >= maxRequests) {
        return {
          allowed: false,
          remaining: 0,
          retryAfterSeconds: Math.max(1, Math.ceil((current.resetAt - timestamp) / 1000)),
        };
      }

      current.count += 1;
      return { allowed: true, remaining: Math.max(0, maxRequests - current.count), retryAfterSeconds: 0 };
    },
  };
}

function rateLimitBucketKey(clientKey, pathname) {
  const client = String(clientKey || "unknown");
  const path = String(pathname || "");
  if (/^\/api\/retail\/products\/[^/]+\/ingredients\/rag$/.test(path)) {
    return `${client}|ingredient-rag`;
  }
  if (path === "/v1/billing/google-play/verify") {
    return `${client}|billing`;
  }
  return `${client}|bitwise-analysis`;
}

module.exports = { createRateLimiter, rateLimitBucketKey };
