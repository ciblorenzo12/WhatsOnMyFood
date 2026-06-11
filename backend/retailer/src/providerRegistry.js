const { MockRetailerProvider } = require("./providers/mockRetailerProvider");
const { AmazonSpApiProvider, hasAmazonCredentials } = require("./providers/amazonSpApiProvider");
const { WalmartAffiliatesProvider } = require("./providers/walmartAffiliatesProvider");
const { hasWalmartCredentials } = require("./walmartSignature");

function createProviderRegistry() {
  const providers = [];

  if (hasWalmartCredentials()) {
    providers.push(new WalmartAffiliatesProvider());
  }

  if (hasAmazonCredentials()) {
    providers.push(new AmazonSpApiProvider());
  }

  providers.push(new MockRetailerProvider());
  return providers;
}

module.exports = {
  createProviderRegistry,
};
