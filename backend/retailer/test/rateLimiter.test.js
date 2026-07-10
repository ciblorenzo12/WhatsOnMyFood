const test = require("node:test");
const assert = require("node:assert/strict");
const { createRateLimiter } = require("../src/rateLimiter");

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
