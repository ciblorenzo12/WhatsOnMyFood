const { classifyErrorCategory } = require("./privacySafeObservability");

class RetailerService {
  constructor(providers) {
    this.providers = providers;
  }

  async getAvailability(query) {
    const providerResults = await Promise.all(
      this.providers.map(async (provider) => this.annotateProviderResults(
        provider,
        await this.safeProviderCall(provider, "getAvailability", query),
      )),
    );
    const results = this.filterAvailability(flatten(providerResults));

    return {
      barcode: query.barcode,
      providerMode: this.providerMode(),
      resultMode: resultMode(results),
      generatedAt: new Date().toISOString(),
      results,
    };
  }

  async getAlternatives(query) {
    const providerResults = await Promise.all(
      this.providers.map(async (provider) => this.annotateProviderResults(
        provider,
        await this.safeProviderCall(provider, "getAlternatives", query),
      )),
    );
    const results = flatten(providerResults);

    return {
      barcode: query.barcode,
      providerMode: this.providerMode(),
      resultMode: resultMode(results),
      generatedAt: new Date().toISOString(),
      results,
    };
  }

  async getProduct(query) {
    for (const provider of this.providers) {
      if (typeof provider.getProduct !== "function") continue;
      const product = await this.safeProviderCall(provider, "getProduct", query);
      if (product && product.status === 1 && product.product) {
        return {
          ...product,
          source: provider.name,
          generatedAt: new Date().toISOString(),
        };
      }
    }

    return {
      status: 0,
      source: "none",
      generatedAt: new Date().toISOString(),
      product: null,
    };
  }

  async getIngredientRag(query) {
    const retrievals = [];
    const failureCategories = [];
    for (const provider of this.providers) {
      if (typeof provider.getProduct !== "function") continue;
      let productResponse;
      try {
        productResponse = await provider.getProduct(query);
      } catch (error) {
        failureCategories.push(classifyErrorCategory({ error }));
        continue;
      }
      const product = productResponse && productResponse.product;
      const ingredientsText = extractIngredientsText(product);
      if (!ingredientsText) continue;

      retrievals.push({
        provider: provider.name,
        productName: firstNonEmpty(product.product_name_en, product.product_name, query.productName),
        brand: firstNonEmpty(product.brands, query.brand),
        ingredientsText,
      });
    }

    const best = retrievals.find((item) => item.ingredientsText) || null;
    const errorCategory = failureCategories.includes("timeout")
      ? "timeout"
      : (failureCategories[0] || "none");
    return {
      status: best ? 1 : 0,
      barcode: query.barcode,
      source: best ? best.provider : "none",
      retrievalCount: retrievals.length,
      generatedAt: new Date().toISOString(),
      product: best ? {
        product_name: best.productName,
        product_name_en: best.productName,
        brands: best.brand,
        ingredients_text: best.ingredientsText,
        ingredients_text_en: best.ingredientsText,
      } : null,
      _diagnostic: {
        outcome: best ? "success" : (failureCategories.length > 0 ? "failure" : "empty_result"),
        errorCategory: best ? "none" : errorCategory,
      },
    };
  }

  providerMode() {
    return this.providers
      .map((provider) => provider.name)
      .join(",");
  }

  async safeProviderCall(provider, method, query) {
    if (!provider || typeof provider[method] !== "function") return [];
    try {
      return await provider[method](query);
    } catch (error) {
      console.warn(`${provider.name || "RetailerProvider"} ${method} failed: ${error.message || error}`);
      return method === "getProduct" ? null : [];
    }
  }

  annotateProviderResults(provider, results) {
    if (!Array.isArray(results)) return [];
    const providerName = provider && provider.name ? provider.name : "UnknownRetailerProvider";
    return results
      .filter((item) => item && typeof item === "object")
      .map((item) => ({
        ...item,
        providerName: firstNonEmpty(item.providerName, providerName),
      }));
  }

  filterAvailability(results) {
    const hasLiveWalmart = results.some((item) =>
      item && item.retailerName === "Walmart" && item.providerName === "WalmartAffiliatesProvider"
    );
    const hasLiveAmazon = results.some((item) =>
      item && item.retailerName === "Amazon" && item.providerName === "AmazonSpApiProvider"
    );
    if (!hasLiveWalmart && !hasLiveAmazon) return results;
    return results.filter((item) =>
      !(item && item.retailerName === "Walmart" && item.providerName !== "WalmartAffiliatesProvider")
        && !(item && item.retailerName === "Amazon" && item.providerName !== "AmazonSpApiProvider")
    );
  }
}

function flatten(items) {
  return items.reduce((all, item) => all.concat(Array.isArray(item) ? item : []), []);
}

function resultMode(results) {
  const providerNames = (Array.isArray(results) ? results : [])
    .map((item) => item && item.providerName)
    .filter(Boolean);
  if (providerNames.length === 0) return "empty";

  const hasMock = providerNames.some((name) => name === "MockRetailerProvider");
  const hasLive = providerNames.some((name) => name !== "MockRetailerProvider");
  if (hasLive && hasMock) return "mixed";
  return hasLive ? "live" : "mock";
}

function extractIngredientsText(product) {
  if (!product) return "";
  return firstNonEmpty(
    product.ingredients_text_en,
    product.ingredients_text,
    product.ingredientsText,
    product.ingredients,
  );
}

function firstNonEmpty(...values) {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) return value.trim();
  }
  return "";
}

module.exports = {
  RetailerService,
  resultMode,
};
