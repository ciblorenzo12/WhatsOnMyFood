const test = require("node:test");
const assert = require("node:assert/strict");
const { createRateLimiter, rateLimitBucketKey } = require("../src/rateLimiter");

test("limits requests per key and resets after the window", () => {
  let timestamp = 1_000;
  const limiter = createRateLimiter({ windowMs: 10_000, maxRequests: 2, now: () => timestamp });

  assert.equal(limiter.check("client-a").allowed, true);
  assert.equal(limiter.check("client-a").allowed, true);
  const blocked = limiter.check("client-a");
  assert.equal(blocked.allowed, false);
  assert.equal(blocked.retryAfterSeconds, 10);
  assert.equal(limiter.check("client-b").allowed, true);

  timestamp += 10_000;
  assert.equal(limiter.check("client-a").allowed, true);
});

test("separates ingredient recovery, recalls, analysis, and billing limits", () => {
  const client = "203.0.113.10";

  assert.equal(
    rateLimitBucketKey(client, "/api/retail/products/012345678905/ingredients/rag"),
    `${client}|ingredient-rag`,
  );
  assert.equal(
    rateLimitBucketKey(client, "/v1/bitwise/analyze"),
    `${client}|bitwise-analysis`,
  );
  assert.equal(
    rateLimitBucketKey(client, "/v1/chat/completions"),
    `${client}|bitwise-analysis`,
  );
  assert.equal(
    rateLimitBucketKey(client, "/v1/billing/google-play/verify"),
    `${client}|billing`,
  );
  assert.equal(
    rateLimitBucketKey(client, "/v1/food-recalls"),
    `${client}|food-recalls`,
  );
});
