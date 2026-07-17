package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;

/** Combines separate front-label and ingredient-label OCR passes for analysis. */
public final class SupplementalOcrMerger {

    private SupplementalOcrMerger() {
    }

    public static String merge(String productText, String ingredientText) {
        String identity = IngredientOcrHeuristics.trimUiNoise(productText);
        String ingredients = IngredientTextParser.trimToLikelyIngredientList(
                IngredientOcrHeuristics.trimUiNoise(ingredientText)
        );

        StringBuilder merged = new StringBuilder();
        if (!identity.isEmpty()) {
            merged.append(identity.trim()).append('\n');
        }
        if (!ingredients.isEmpty()) {
            merged.append("Ingredients:\n").append(ingredients.trim());
        }
        return merged.toString().trim();
    }
}
