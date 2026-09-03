const assert = require("node:assert/strict");
const { test } = require("node:test");

const {
  RESULT_LIMIT,
  buildOpenFdaUrl,
  buildSearchQuery,
  fetchFoodRecalls,
  handleFoodRecallCheck,
} = require("../src/foodRecallProxy");

test("builds a narrow bounded openFDA query on the backend", () => {
  const query = buildSearchQuery({
    productName: "Straus Organic Cookie Dough Ice Cream",
    brand: "Straus Family Creamery",
  });

  assert.equal(
    query,
    'product_description:"straus cookie dough" OR product_description:"straus family"',
  );
  const url = buildOpenFdaUrl({ productName: "Oat Cereal", brand: "Sample Foods" }, "secret-key");
  assert.equal(url.protocol, "https:");
  assert.equal(url.searchParams.get("limit"), String(RESULT_LIMIT));
  assert.equal(url.searchParams.get("api_key"), "secret-key");
});

test("uses the server key upstream but never returns it to the client", async () => {
  let requestedUrl;
  const payload = await fetchFoodRecalls(
    { productName: "Oat Cereal", brand: "Sample Foods" },
    {
      apiKey: "server-only-key",
      fetchImpl: async (url) => {
        requestedUrl = String(url);
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify({
            meta: { last_updated: "2026-09-02" },
            results: [{
              recall_number: "F-0001-2026",
              product_description: "Oat cereal",
              status: "Ongoing",
              ignored_private_field: "not forwarded",
            }],
          }),
        };
      },
    },
  );

  assert.match(requestedUrl, /api_key=server-only-key/);
  assert.doesNotMatch(JSON.stringify(payload), /server-only-key/);
  assert.equal(payload.results[0].recall_number, "F-0001-2026");
  assert.equal(payload.results[0].ignored_private_field, undefined);
});

test("retries a temporary upstream failure once", async () => {
  let attempts = 0;
  const payload = await fetchFoodRecalls(
    { productName: "Oat Cereal", brand: "" },
    {
      apiKey: "key",
      sleep: async () => {},
      fetchImpl: async () => {
        attempts += 1;
        if (attempts === 1) return { ok: false, status: 503 };
        return { ok: true, status: 200, text: async () => '{"results":[]}' };
      },
    },
  );

  assert.equal(attempts, 2);
  assert.deepEqual(payload.results, []);
});

test("maps invalid product input and unavailable upstream to safe responses", async () => {
  const missing = await handleFoodRecallCheck(new URL("https://backend.test/v1/food-recalls"));
  assert.equal(missing.status, 400);

  const unavailable = await handleFoodRecallCheck(
    new URL("https://backend.test/v1/food-recalls?productName=Oat%20Cereal"),
    {
      apiKey: "key",
      sleep: async () => {},
      fetchImpl: async () => ({ ok: false, status: 429 }),
    },
  );
  assert.equal(unavailable.status, 503);
  assert.doesNotMatch(JSON.stringify(unavailable.body), /429|key/);
});

test("refuses a recall request when the backend key is not configured", async () => {
  const result = await handleFoodRecallCheck(
    new URL("https://backend.test/v1/food-recalls?productName=Oat%20Cereal"),
    { apiKey: "" },
  );

  assert.equal(result.status, 503);
  assert.match(result.body.error, /not configured/i);
});
