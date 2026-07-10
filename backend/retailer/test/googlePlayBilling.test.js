const assert = require("node:assert/strict");
const { test } = require("node:test");

const { evaluateSubscription } = require("../src/googlePlayBilling");

const PRODUCT_ID = "bitwise_plus_monthly";
const ACCOUNT_ID = "a".repeat(64);

function subscription(state, expiryTime, overrides = {}) {
  return {
    subscriptionState: state,
    acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
    externalAccountIdentifiers: { obfuscatedExternalAccountId: ACCOUNT_ID },
    lineItems: [{ productId: PRODUCT_ID, expiryTime }],
    ...overrides,
  };
}

test("grants an active account-bound subscription", () => {
  const result = evaluateSubscription(
    subscription("SUBSCRIPTION_STATE_ACTIVE", "2030-01-01T00:00:00Z"),
    PRODUCT_ID,
    ACCOUNT_ID,
    Date.parse("2029-01-01T00:00:00Z"),
  );
  assert.equal(result.verified, true);
  assert.equal(result.entitlementActive, true);
});

test("keeps access after cancellation until the paid period expires", () => {
  const result = evaluateSubscription(
    subscription("SUBSCRIPTION_STATE_CANCELED", "2030-01-01T00:00:00Z"),
    PRODUCT_ID,
    ACCOUNT_ID,
    Date.parse("2029-01-01T00:00:00Z"),
  );
  assert.equal(result.entitlementActive, true);
});

test("does not grant pending, expired, wrong-product, or wrong-account purchases", () => {
  const now = Date.parse("2029-01-01T00:00:00Z");
  assert.equal(evaluateSubscription(
    subscription("SUBSCRIPTION_STATE_PENDING", "2030-01-01T00:00:00Z"), PRODUCT_ID, ACCOUNT_ID, now,
  ).entitlementActive, false);
  assert.equal(evaluateSubscription(
    subscription("SUBSCRIPTION_STATE_ACTIVE", "2028-01-01T00:00:00Z"), PRODUCT_ID, ACCOUNT_ID, now,
  ).entitlementActive, false);
  assert.equal(evaluateSubscription(
    subscription("SUBSCRIPTION_STATE_ACTIVE", "2030-01-01T00:00:00Z"), "another_product", ACCOUNT_ID, now,
  ).entitlementActive, false);
  assert.equal(evaluateSubscription(
    subscription("SUBSCRIPTION_STATE_ACTIVE", "2030-01-01T00:00:00Z"), PRODUCT_ID, "b".repeat(64), now,
  ).entitlementActive, false);
});
