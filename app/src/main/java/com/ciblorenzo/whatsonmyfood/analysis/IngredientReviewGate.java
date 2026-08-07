package com.ciblorenzo.whatsonmyfood.analysis;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;

import java.util.List;

/** Prevents Bitwise review requests until a usable ingredient list is available. */
public final class IngredientReviewGate {

    private IngredientReviewGate() {
    }

    public static boolean hasUsableIngredients(ProductWithDetails product) {
        if (product == null || product.ingredients == null) return false;
        for (Ingredient ingredient : product.ingredients) {
            if (ingredient != null && ingredient.text != null && !ingredient.text.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static List<String> parseManualIngredients(String rawText) {
        return IngredientTextParser.parseIngredientCandidates(rawText == null ? "" : rawText);
    }
}
