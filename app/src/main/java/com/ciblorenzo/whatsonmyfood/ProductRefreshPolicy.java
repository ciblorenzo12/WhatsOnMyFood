package com.ciblorenzo.whatsonmyfood;

/** Defines which device-owned product fields survive a refresh from external data sources. */
public final class ProductRefreshPolicy {

    private ProductRefreshPolicy() {
    }

    public static void preserveLocalState(ProductWithDetails refreshed, ProductWithDetails saved) {
        if (refreshed == null || refreshed.product == null) return;
        preserveLocalState(refreshed.product, saved == null ? null : saved.product);
        if (!NutritionScoreCoverage.hasCoreNutrition(refreshed)) {
            // A score or explanation from an older, fuller record is not current evidence.
            refreshed.product.healthScore = null;
            refreshed.product.aiInsight = null;
        }
    }

    public static void preserveLocalState(Product refreshedProduct, Product savedProduct) {
        if (refreshedProduct == null || savedProduct == null) return;
        if (!refreshedProduct.barcode.equals(savedProduct.barcode)) {
            throw new IllegalArgumentException("Refresh products must use the same barcode");
        }

        refreshedProduct.isFavorite = savedProduct.isFavorite;
        refreshedProduct.healthScore = savedProduct.healthScore;
        refreshedProduct.aiInsight = savedProduct.aiInsight;
        refreshedProduct.userIngredientRiskScore = savedProduct.userIngredientRiskScore;
    }
}
