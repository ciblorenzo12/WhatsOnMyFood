class RetailerService {
  constructor(providers) {
    this.providers = providers;
  }

  async getAvailability(query) {
    const providerResults = await Promise.all(
      this.providers.map((provider) => this.safeProviderCall(provider, "getAvailability", query)),
    );
    const results = this.filterAvailability(flatten(providerResults));

    return {
      barcode: query.barcode,
      providerMode: this.providerMode(),
      generatedAt: new Date().toISOString(),
      results,
    };
  }

  async getAlternatives(query) {
    const providerResults = await Promise.all(
      this.providers.map((provider) => this.safeProviderCall(provider, "getAlternatives", query)),
    );

    return {
      barcode: query.barcode,
      providerMode: this.providerMode(),
      generatedAt: new Date().toISOString(),
      results: flatten(providerResults),
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
    for (const provider of this.providers) {
      if (typeof provider.getProduct !== "function") continue;
      const productResponse = await this.safeProviderCall(provider, "getProduct", query);
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
  return items.reduce((all, item) => all.concat(item), []);
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
};
