package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Validates that OCR contains a usable ingredient label before scoring begins. */
public final class IngredientLabelValidator {

    public enum FailureReason {
        NONE,
        EMPTY_TEXT,
        OCR_PLACEHOLDER,
        NUTRITION_PANEL_ONLY,
        HEADING_WITHOUT_INGREDIENTS,
        NO_RELIABLE_INGREDIENT_LIST
    }

    public static final class Result {
        public final boolean readable;
        public final String cleanedText;
        public final List<String> ingredients;
        public final FailureReason failureReason;

        private Result(boolean readable, String cleanedText, List<String> ingredients, FailureReason failureReason) {
            this.readable = readable;
            this.cleanedText = cleanedText;
            this.ingredients = Collections.unmodifiableList(ingredients);
            this.failureReason = failureReason;
        }
    }

    private IngredientLabelValidator() {
    }

    public static Result validate(String ocrText) {
        String cleaned = IngredientOcrHeuristics.trimUiNoise(ocrText);
        if (cleaned.isEmpty()) return failed(cleaned, FailureReason.EMPTY_TEXT);

        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.equals("image upload") || lower.contains("ocr failed")) {
            return failed(cleaned, FailureReason.OCR_PLACEHOLDER);
        }

        boolean hasMarker = hasIngredientMarker(cleaned);
        List<String> candidates = IngredientTextParser.parseIngredientCandidates(cleaned);
        if (hasMarker && candidates.isEmpty()) {
            return failed(cleaned, FailureReason.HEADING_WITHOUT_INGREDIENTS);
        }

        if (!hasMarker && looksLikeNutritionPanel(lower)) {
            return failed(cleaned, FailureReason.NUTRITION_PANEL_ONLY);
        }

        int confidence = IngredientOcrHeuristics.confidence(cleaned);
        boolean delimiterRichList = candidates.size() >= 2 && (cleaned.contains(",") || cleaned.contains(";"));
        // Some single-purpose products print the formula without an Ingredients heading.
        // Require both delimiters and ingredient-like vocabulary so marketing copy stays rejected.
        boolean readable = !candidates.isEmpty() && (hasMarker || (confidence >= 16 && delimiterRichList));
        return readable
                ? new Result(true, cleaned, candidates, FailureReason.NONE)
                : failed(cleaned, FailureReason.NO_RELIABLE_INGREDIENT_LIST);
    }

    public static boolean hasIngredientMarker(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("ingredients")
                || lower.contains("ingredient list")
                || lower.contains("ingrédients")
                || lower.contains("ingredientes")
                || lower.contains("contains:")
                || lower.contains("contient:")
                || lower.contains("contient :")
                || lower.contains("contiene:");
    }

    private static boolean looksLikeNutritionPanel(String lower) {
        return lower.contains("nutrition facts")
                || lower.contains("serving size")
                || lower.contains("daily value")
                || lower.contains("calories");
    }

    private static Result failed(String cleaned, FailureReason reason) {
        return new Result(false, cleaned, Collections.emptyList(), reason);
    }
}
