const test = require("node:test");
const assert = require("node:assert/strict");

const { RetailerService, resultMode } = require("../src/retailerService");

test("alternatives identify mock provider results", async () => {
  const service = new RetailerService([{
    name: "MockRetailerProvider",
    async getAlternatives(query) {
      return [{ productName: "Sample swap", category: query.category }];
    },
  }]);

  const response = await service.getAlternatives({ barcode: "123", category: "snacks" });

  assert.equal(response.resultMode, "mock");
  assert.equal(response.results[0].providerName, "MockRetailerProvider");
});

test("alternatives identify live and mixed provider results", async () => {
  const service = new RetailerService([
    {
      name: "LiveRetailerProvider",
      async getAlternatives() {
        return [{ productName: "Live swap" }];
      },
    },
    {
      name: "MockRetailerProvider",
      async getAlternatives() {
        return [{ productName: "Sample swap" }];
      },
    },
  ]);

  const response = await service.getAlternatives({ barcode: "456" });

  assert.equal(response.resultMode, "mixed");
  assert.deepEqual(
    response.results.map((item) => item.providerName),
    ["LiveRetailerProvider", "MockRetailerProvider"],
  );
});

test("empty and incomplete provider responses are safe", async () => {
  const service = new RetailerService([
    { name: "BrokenProvider", async getAlternatives() { return null; } },
    { name: "EmptyProvider", async getAlternatives() { return []; } },
  ]);

  const response = await service.getAlternatives({ barcode: "789" });

  assert.equal(response.resultMode, "empty");
  assert.deepEqual(response.results, []);
  assert.equal(resultMode([{ providerName: "LiveRetailerProvider" }]), "live");
});
