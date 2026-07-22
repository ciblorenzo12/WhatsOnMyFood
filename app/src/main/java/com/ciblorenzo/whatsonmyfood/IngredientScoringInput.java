package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Selects parsed label fields for scoring while keeping OCR authoritative over model guesses. */
public final class IngredientScoringInput {

    public static final class Selection {
        public final List<String> ingredients;
        public final List<String> containsAllergens;
        public final List<String> mayContainAllergens;

        private Selection(List<String> ingredients, List<String> containsAllergens, List<String> mayContainAllergens) {
            this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
            this.containsAllergens = Collections.unmodifiableList(new ArrayList<>(containsAllergens));
            this.mayContainAllergens = Collections.unmodifiableList(new ArrayList<>(mayContainAllergens));
        }
    }

    private IngredientScoringInput() {
    }

    public static List<String> select(String ocrText, List<String> modelIngredients) {
        return selectWithAllergens(ocrText, modelIngredients).ingredients;
    }

    public static Selection selectWithAllergens(String ocrText, List<String> modelIngredients) {
        IngredientLabelValidator.Result validation = IngredientLabelValidator.validate(ocrText);
        if (validation.readable
                && IngredientLabelValidator.hasIngredientMarker(validation.cleanedText)
                && !validation.ingredients.isEmpty()) {
            return new Selection(validation.ingredients, validation.containsAllergens, validation.mayContainAllergens);
        }

        if (modelIngredients == null || modelIngredients.isEmpty()) {
            return validation.readable
                    ? new Selection(validation.ingredients, validation.containsAllergens, validation.mayContainAllergens)
                    : new Selection(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        StringBuilder combinedModelText = new StringBuilder();
        int firstIngredientMarker = firstIngredientMarker(modelIngredients);
        for (int i = 0; i < modelIngredients.size(); i++) {
            if (firstIngredientMarker >= 0 && i < firstIngredientMarker) continue;
            String modelIngredient = modelIngredients.get(i);
            if (modelIngredient == null || modelIngredient.trim().isEmpty()) continue;
            if (isEditorOrPromptNoise(modelIngredient)) continue;
            if (combinedModelText.length() > 0) combinedModelText.append(", ");
            combinedModelText.append(modelIngredient.trim());
        }
        IngredientTextParser.ParsedLabel parsedLabel = IngredientTextParser.parseLabel(combinedModelText.toString());
        return new Selection(
                parsedLabel.ingredients,
                parsedLabel.containsAllergens,
                parsedLabel.mayContainAllergens
        );
    }

    private static int firstIngredientMarker(List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (value == null) continue;
            String lower = value.toLowerCase(java.util.Locale.US);
            if (lower.contains("ingredients")
                    || lower.contains("ingredient list")
                    || lower.contains("ingrédients")
                    || lower.contains("ingredientes")) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isEditorOrPromptNoise(String value) {
        String normalized = value.toLowerCase(java.util.Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        if (normalized.contains("ingredients")) return false;

        return normalized.equals("file")
                || normalized.equals("fess")
                || normalized.equals("view")
                || normalized.equals("edit")
                || normalized.equals("edit view")
                || normalized.equals("config")
                || normalized.equals("env")
                || normalized.equals("new t")
                || normalized.equals("a r")
                || normalized.equals("plain text")
                || normalized.equals("appjs")
                || normalized.equals("app js")
                || normalized.contains("requirement")
                || normalized.contains("characters")
                || normalized.matches(".*\\b(?:ln|line|col)\\s*\\d+.*")
                || normalized.contains("windows crlf")
                || normalized.contains("build and run")
                || normalized.contains("clear search");
    }
}
