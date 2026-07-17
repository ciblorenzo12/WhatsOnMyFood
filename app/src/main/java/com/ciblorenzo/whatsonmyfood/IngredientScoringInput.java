package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Selects parsed label fields for scoring while keeping OCR authoritative over model guesses. */
public final class IngredientScoringInput {

    private IngredientScoringInput() {
    }

    public static List<String> select(String ocrText, List<String> modelIngredients) {
        IngredientLabelValidator.Result validation = IngredientLabelValidator.validate(ocrText);
        if (validation.readable
                && IngredientLabelValidator.hasIngredientMarker(validation.cleanedText)
                && !validation.ingredients.isEmpty()) {
            return validation.ingredients;
        }

        if (modelIngredients == null || modelIngredients.isEmpty()) {
            return validation.readable ? validation.ingredients : Collections.emptyList();
        }
        List<String> parsed = new ArrayList<>();
        for (String modelIngredient : modelIngredients) {
            parsed.addAll(IngredientTextParser.parseIngredientCandidates(modelIngredient));
        }
        return parsed;
    }
}
