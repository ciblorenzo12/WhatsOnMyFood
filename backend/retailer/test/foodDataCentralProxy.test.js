const assert = require("node:assert/strict");
const { test } = require("node:test");
const {
  handleFoodDataCentralLookup, normalizeGtin, mapNutriments, createLookupState,
} = require("../src/foodDataCentralProxy");

const BARCODE = "012345678905";
const CANONICAL = "00012345678905";
const TEST_KEY = "unit-test-placeholder-not-a-real-key";
const request = (barcode = BARCODE) => new URL(`https://backend.test/v1/food-data/usda?barcode=${encodeURIComponent(barcode)}`);
const json = (body, status = 200, headers = {}) => new Response(JSON.stringify(body), { status, headers });
const nutrient = (id, amount, unitName = "g") => ({ nutrient: { id, unitName }, amount });
const record = (overrides = {}) => ({
  fdcId: 123, dataType: "Branded", description: "Example oatmeal", gtinUpc: BARCODE,
  publicationDate: "2026-09-01", marketCountry: "United States", brandOwner: "Example Foods",
  servingSize: 40, servingSizeUnit: "g", ingredients: "Oats, sugar.",
  foodNutrients: [nutrient(1008, 350, "kcal"), nutrient(2000, 18), nutrient(1235, 12), nutrient(1093, 400, "mg")],
  ...overrides,
});
function options(fetchImpl, extra = {}) {
  return { env: { FDC_API_KEY: TEST_KEY }, fetchImpl, state: createLookupState(), ...extra };
}
function oneProduct(food = record(), searchFood = food) {
  return options(async url => url.pathname.endsWith("/search")
    ? json({ totalHits: 1, foods: [searchFood] }) : json(food));
}

test("validates check digits and only equates full zero-padded GTINs", () => {
  for (const value of [BARCODE, `0${BARCODE}`, CANONICAL]) assert.equal(normalizeGtin(value), CANONICAL);
  assert.equal(normalizeGtin("96385074"), "00000096385074");
  for (const value of ["", "00000000", "123", "012345678906", "01234567890a", ` ${BARCODE}`, 12345678905]) {
    assert.equal(normalizeGtin(value), null);
  }
  assert.notEqual(normalizeGtin("112345678902"), CANONICAL);
});

test("rejects invalid or duplicate barcodes before calling the provider", async () => {
  let called = false;
  const config = options(async () => { called = true; throw Error("not expected"); });
  assert.equal((await handleFoodDataCentralLookup(request("012345678906"), config)).status, 400);
  assert.equal((await handleFoodDataCentralLookup(new URL(`${request()}&barcode=${BARCODE}`), config)).status, 400);
  assert.equal(called, false);
});

test("requires a non-demo backend FDC_API_KEY and ignores request keys", async () => {
  for (const key of [undefined, "", "  ", "DEMO_KEY", "demo_key"]) {
    const result = await handleFoodDataCentralLookup(new URL(`${request()}&api_key=not-accepted`), options(
      async () => { throw Error("not expected"); }, { env: { FDC_API_KEY: key } },
    ));
    assert.equal(result.status, 503);
    assert.equal(result.body.code, "provider_not_configured");
  }
});

test("uses protected Branded search plus details and returns only mapped safe fields", async () => {
  const calls = [];
  const food = record({ privateField: "not forwarded", imageUrl: "https://not-supported.test/image" });
  const result = await handleFoodDataCentralLookup(request(), options(async (url, init) => {
    calls.push({ url, init });
    assert.equal(url.origin, "https://api.nal.usda.gov");
    assert.equal(url.searchParams.get("api_key"), TEST_KEY);
    assert.equal(init.redirect, "error");
    return url.pathname.endsWith("/search") ? json({ totalHits: 1, foods: [food] }) : json(food);
  }));
  assert.equal(calls.length, 2);
  const query = JSON.parse(calls[0].init.body);
  assert.deepEqual(query.dataType, ["Branded"]);
  assert.equal(query.sortBy, "publishedDate");
  assert.equal(query.pageSize, 200);
  assert.match(query.query, /"012345678905"/);
  assert.equal(calls[0].init.method, "POST");
  assert.equal(calls[1].url.pathname, "/fdc/v1/food/123");
  assert.equal(calls[1].url.searchParams.get("format"), "full");
  assert.equal(result.status, 200);
  assert.equal(result.body.status, 1);
  assert.equal(result.body.code, BARCODE);
  assert.equal(result.body.source, "USDA FoodData Central");
  assert.equal(result.body.provenance.nutrientBasis, "per100g");
  assert.equal(result.body.product.serving_size, "40 g");
  assert.equal(result.body.product.nutriments.sugars_100g, 18);
  assert.equal(result.body.product.nutriments["added-sugars_100g"], 12);
  assert.equal(result.body.product.nutriments.sodium_100g, 0.4);
  assert.equal(result.body.product.nutriments.salt_100g, 1);
  assert.equal(result.body.product.nutriments["energy-kcal_100g"], 350);
  for (const field of ["image_url", "nutriscore_grade", "nova_group", "allergens", "allergens_tags"]) {
    assert.equal(result.body.product[field], undefined);
  }
  assert.doesNotMatch(JSON.stringify(result), new RegExp(TEST_KEY));
  assert.equal(result.body.product.privateField, undefined);
});

test("never uses a fuzzy/suffix barcode match, non-Branded record, or other market", async () => {
  let calls = 0;
  const foods = [record({ gtinUpc: "112345678902" }), record({ dataType: "Foundation" }), record({ marketCountry: "New Zealand" })];
  const result = await handleFoodDataCentralLookup(request(), options(async () => {
    calls += 1; return json({ totalHits: foods.length, foods });
  }));
  assert.equal(calls, 1);
  assert.equal(result.body.status, 0);
});

test("sorts exact matches newest first and verifies both detail ID and barcode", async () => {
  const foods = [record({ fdcId: 100, publicationDate: "2025-01-01" }), record({ fdcId: 200, publicationDate: "2026-01-01" })];
  const ids = [];
  const result = await handleFoodDataCentralLookup(request(), options(async url => {
    if (url.pathname.endsWith("/search")) return json({ totalHits: foods.length, foods });
    ids.push(url.pathname); return json(foods[1]);
  }));
  assert.equal(result.body.provenance.fdcId, 200);
  assert.deepEqual(ids, ["/fdc/v1/food/200"]);
  for (const food of [record({ fdcId: 321 }), record({ gtinUpc: "112345678902" }), record({ description: "" })]) {
    assert.equal((await handleFoodDataCentralLookup(request(), oneProduct(food, record()))).status, 502);
  }
});

test("search market may be missing, but details must explicitly confirm the US market", async () => {
  const search = record({ marketCountry: undefined });
  assert.equal((await handleFoodDataCentralLookup(request(), oneProduct(record(), search))).body.status, 1);
  for (const marketCountry of [undefined, "", "New Zealand"]) {
    const result = await handleFoodDataCentralLookup(request(), oneProduct(record({ marketCountry }), search));
    assert.equal(result.status, 200);
    assert.equal(result.body.status, 0);
  }
});

test("missing nutrition stays unknown, explicit zero remains zero, and unit errors are ignored", () => {
  const { nutrients } = mapNutriments(record({ foodNutrients: [
    nutrient(2000, 0), nutrient(1235, null), nutrient(1004, "7"), nutrient(1003, -1),
    nutrient(1008, 200, "g"), nutrient(1093, 0.3, "g"), nutrient(1089, 2000, "µg"),
  ] }));
  assert.equal(nutrients.sugars_100g, 0);
  assert.equal(nutrients["added-sugars_100g"], undefined);
  assert.equal(nutrients.fat_100g, undefined);
  assert.equal(nutrients.proteins_100g, undefined);
  assert.equal(nutrients["energy-kcal_100g"], undefined);
  assert.equal(nutrients.sodium_100g, 0.3);
  assert.equal(nutrients.iron_100g, 0.002);
  assert.deepEqual(mapNutriments(record({ foodNutrients: undefined })).nutrients, {});
});

test("never treats added sugar as total sugar or conflicting values as valid", () => {
  assert.deepEqual(mapNutriments(record({ foodNutrients: [nutrient(1235, 4)] })).nutrients, { "added-sugars_100g": 4 });
  const result = mapNutriments(record({ foodNutrients: [nutrient(2000, 3), nutrient(1063, 8)] }));
  assert.equal(result.nutrients.sugars_100g, undefined);
  assert.equal(result.warnings.length, 1);
});

test("volume and unknown bases never produce fabricated per-100g values", async () => {
  for (const servingSizeUnit of ["ml", "mL", undefined, "", "oz"]) {
    const result = await handleFoodDataCentralLookup(request(), oneProduct(record({ servingSizeUnit })));
    assert.equal(result.body.status, 1);
    assert.deepEqual(result.body.product.nutriments, {});
    assert.equal(result.body.provenance.warnings.length, 1);
  }
});

test("returns an explicit empty result only for a valid completed search", async () => {
  const result = await handleFoodDataCentralLookup(request(), options(async () => json({ totalHits: 0, foods: [] })));
  assert.equal(result.status, 200);
  assert.equal(result.body.status, 0);
  assert.equal(result.body.reason, "no_matching_us_product");
  for (const body of [{}, { foods: [] }, { totalHits: 1, foods: [] }, { totalHits: 0, foods: [{}] },
    { totalHits: 1, foods: [{}] }, { totalHits: 1, foods: [null] }, { totalHits: -1, foods: [] }]) {
    assert.equal((await handleFoodDataCentralLookup(request(), options(async () => json(body)))).status, 502);
  }
});

test("handles bounded pagination and rejects incomplete or oversized search sets", async () => {
  const irrelevant = record({ gtinUpc: "112345678902" });
  let calls = 0;
  const result = await handleFoodDataCentralLookup(request(), options(async (url, init) => {
    calls += 1;
    if (!url.pathname.endsWith("/search")) return json(record());
    const page = JSON.parse(init.body).pageNumber;
    return json({ totalHits: 201, foods: page === 1 ? Array(200).fill(irrelevant) : [record()] });
  }));
  assert.equal(calls, 3);
  assert.equal(result.body.status, 1);
  const bounded = await handleFoodDataCentralLookup(request(), options(async () => json({ totalHits: 401, foods: Array(200).fill(irrelevant) })));
  assert.equal(bounded.status, 502);
  assert.equal(bounded.body.code, "search_limit_exceeded");
});

test("rate limits return a safe bounded retry hint with no automatic retries", async () => {
  for (const [header, expected] of [["120", "120"], ["9999999", "3600"], ["-1", "60"], ["secret-provider-text", "60"]]) {
    let calls = 0;
    const result = await handleFoodDataCentralLookup(request(), options(async () => {
      calls += 1; return json({ error: TEST_KEY }, 429, { "Retry-After": header });
    }));
    assert.equal(calls, 1);
    assert.equal(result.status, 429);
    assert.equal(result.headers["Retry-After"], expected);
    assert.doesNotMatch(JSON.stringify(result), new RegExp(TEST_KEY));
  }
});

test("HTTP errors, invalid JSON, and thrown network errors cannot leak the key", async () => {
  for (const fetchImpl of [
    async () => json({ message: TEST_KEY }, 403),
    async () => json({ message: TEST_KEY }, 500),
    async () => new Response("bad JSON"),
    async () => { throw Object.assign(Error(TEST_KEY), { status: 401, code: TEST_KEY }); },
  ]) {
    const result = await handleFoodDataCentralLookup(request(), options(fetchImpl));
    assert.equal(result.status, 502);
    assert.doesNotMatch(JSON.stringify(result), new RegExp(TEST_KEY));
  }
});

test("deadline includes both connection and a stalled body read", async () => {
  for (const fetchImpl of [
    async () => new Promise(() => {}),
    async () => new Response(new ReadableStream({ start(controller) { controller.enqueue(new TextEncoder().encode('{"foods":')); } })),
  ]) {
    const result = await handleFoodDataCentralLookup(request(), options(fetchImpl, { timeoutMs: 20 }));
    assert.equal(result.status, 504);
    assert.equal(result.body.code, "upstream_timeout");
  }
});

test("response-size bound works with Content-Length and chunked bodies", async () => {
  for (const headers of [{ "Content-Length": "50000" }, {}]) {
    const result = await handleFoodDataCentralLookup(request(), options(async () => new Response("x".repeat(500), { headers }), { maxResponseBytes: 100 }));
    assert.equal(result.status, 502);
  }
});

test("deduplicates canonical lookups and caches a private copy, not failures", async () => {
  let calls = 0;
  let time = 100;
  const config = options(async url => {
    calls += 1;
    await new Promise(resolve => setTimeout(resolve, 5));
    return url.pathname.endsWith("/search") ? json({ totalHits: 1, foods: [record()] }) : json(record());
  }, { now: () => time });
  const [first, second] = await Promise.all([
    handleFoodDataCentralLookup(request(), config), handleFoodDataCentralLookup(request(CANONICAL), config),
  ]);
  assert.equal(calls, 2);
  assert.equal(first.body.code, BARCODE);
  assert.equal(second.body.code, CANONICAL);
  first.body.product.nutriments.sugars_100g = 999;
  assert.equal((await handleFoodDataCentralLookup(request(), config)).body.product.nutriments.sugars_100g, 18);
  assert.equal(calls, 2);
  time += 300_001;
  await handleFoodDataCentralLookup(request(), config);
  assert.equal(calls, 4);
  config.env.FDC_API_KEY = "";
  assert.equal((await handleFoodDataCentralLookup(request(), config)).status, 503);

  let failures = 0;
  const bad = options(async () => { failures += 1; return json({}, 500); });
  await handleFoodDataCentralLookup(request(), bad);
  await handleFoodDataCentralLookup(request(), bad);
  assert.equal(failures, 2);
  assert.equal(bad.state.cache.size, 0);
  assert.equal(bad.state.inFlight.size, 0);
});

test("negative cache expires quickly and total cached entries stay bounded", async () => {
  let time = 1;
  let calls = 0;
  const config = options(async () => { calls += 1; return json({ totalHits: 0, foods: [] }); }, { now: () => time });
  await handleFoodDataCentralLookup(request(), config);
  await handleFoodDataCentralLookup(request(), config);
  assert.equal(calls, 1);
  time += 30_001;
  await handleFoodDataCentralLookup(request(), config);
  assert.equal(calls, 2);
  for (let index = 0; index < 256; index += 1) config.state.cache.set(`fixture-${index}`, { expiresAt: 999_999, body: {} });
  await handleFoodDataCentralLookup(request("96385074"), config);
  assert.equal(config.state.cache.size, 256);
});

test("parallel lookup count and details verification have hard bounds", async () => {
  const config = oneProduct();
  for (let index = 0; index < 32; index += 1) config.state.inFlight.set(`fixture-${index}`, Promise.resolve({}));
  const busy = await handleFoodDataCentralLookup(request(), config);
  assert.equal(busy.status, 503);
  assert.equal(busy.body.code, "service_busy");
  const foods = Array.from({ length: 5 }, (_, index) => record({ fdcId: index + 1, marketCountry: undefined }));
  let detailCalls = 0;
  const bounded = await handleFoodDataCentralLookup(request(), options(async url => {
    if (url.pathname.endsWith("/search")) return json({ totalHits: foods.length, foods });
    detailCalls += 1;
    return json(foods.find(food => food.fdcId === Number(url.pathname.split("/").at(-1))));
  }));
  assert.equal(detailCalls, 4);
  assert.equal(bounded.status, 502);
  assert.equal(bounded.body.code, "search_limit_exceeded");
});
