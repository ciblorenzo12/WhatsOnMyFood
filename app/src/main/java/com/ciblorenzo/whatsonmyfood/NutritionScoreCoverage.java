package com.ciblorenzo.whatsonmyfood;

/** Minimum comparable data needed before showing an overall nutrition-based score. */
public final class NutritionScoreCoverage {
    private NutritionScoreCoverage() { }

    public static boolean hasCoreNutrition(ProductWithDetails product) {
        Nutriments nutrients = product == null ? null : product.nutriments;
        return nutrients != null
                && knownAmount(nutrients.sugars)
                && knownAmount(nutrients.saturatedFat)
                && sodiumForAnalysis(nutrients) != null;
    }

    public static Double sodiumForAnalysis(Nutriments nutrients) {
        if (nutrients == null) return null;
        if (knownAmount(nutrients.sodium)) return nutrients.sodium;
        // Both model fields use grams per 100 g; salt is approximately 2.5 x sodium.
        return knownAmount(nutrients.salt) ? nutrients.salt / 2.5 : null;
    }

    private static boolean knownAmount(Double amount) {
        return amount != null && !amount.isNaN() && !amount.isInfinite() && amount >= 0;
    }
}
