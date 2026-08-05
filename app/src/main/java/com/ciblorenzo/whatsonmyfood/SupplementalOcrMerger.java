package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;

import java.util.Locale;

/** Combines separate front-label and ingredient-label OCR passes for analysis. */
public final class SupplementalOcrMerger {

    private SupplementalOcrMerger() {
    }

    public static String merge(String productText, String ingredientText) {
        String identity = textBeforeIngredientPanel(IngredientOcrHeuristics.trimUiNoise(productText));
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

    private static String textBeforeIngredientPanel(String text) {
        if (text == null || text.isEmpty()) return "";
        String lower = text.toLowerCase(Locale.US);
        String[] markers = {"ingredient list", "ingredients list", "ingredients:", "ingredients.", "ingredients\n", "ingredientes:", "ingrÃ©dients:"};
        int stop = -1;
        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index >= 0 && (stop < 0 || index < stop)) stop = index;
        }
        return (stop >= 0 ? text.substring(0, stop) : text).trim();
    }
}
