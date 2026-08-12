package com.ciblorenzo.whatsonmyfood;

/** Defines which device-owned product fields survive a refresh from external data sources. */
public final class ProductRefreshPolicy {

    private ProductRefreshPolicy() {
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
