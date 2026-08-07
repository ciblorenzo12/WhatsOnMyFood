const assert = require("node:assert/strict");
const { test } = require("node:test");

const { OpenFoodFactsProvider, digitsOnly } = require("../src/providers/openFoodFactsProvider");

test("retrieves product identity and ingredients for hosted RAG recovery", async () => {
  let requestedUrl = "";
  const provider = new OpenFoodFactsProvider(async (url, options) => {
    requestedUrl = url;
    assert.match(options.headers["User-Agent"], /WhatsOnMyFood/);
    return {
      ok: true,
      json: async () => ({
        status: 1,
        code: "016000275260",
        product: {
          product_name: "Cheerios",
          brands: "General Mills",
          ingredients_text_en: "Whole grain oats, corn starch, sugar, salt",
        },
      }),
    };
  });

  const result = await provider.getProduct({ barcode: "0 16000-27526 0" });
  assert.equal(result.status, 1);
  assert.equal(result.product.product_name, "Cheerios");
  assert.match(result.product.ingredients_text_en, /whole grain oats/i);
  assert.match(requestedUrl, /016000275260\.json/);
});

test("rejects empty barcodes and missing products", async () => {
  const provider = new OpenFoodFactsProvider(async () => ({
    ok: true,
    json: async () => ({ status: 0 }),
  }));

  assert.equal(digitsOnly("UPC 123-45"), "12345");
  assert.equal(await provider.getProduct({ barcode: "" }), null);
  assert.equal(await provider.getProduct({ barcode: "0000000000000" }), null);
});
