package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        List<String> parsed = new ArrayList<>();
        Set<String> containsAllergens = new LinkedHashSet<>();
        Set<String> mayContainAllergens = new LinkedHashSet<>();
        for (String modelIngredient : modelIngredients) {
            IngredientTextParser.ParsedLabel parsedLabel = IngredientTextParser.parseLabel(modelIngredient);
            parsed.addAll(parsedLabel.ingredients);
            containsAllergens.addAll(parsedLabel.containsAllergens);
            mayContainAllergens.addAll(parsedLabel.mayContainAllergens);
        }
        return new Selection(parsed, new ArrayList<>(containsAllergens), new ArrayList<>(mayContainAllergens));
    }
}
