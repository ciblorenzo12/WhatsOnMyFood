package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class IngredientScoringInputTest {

    @Test
    public void parsedOcrFieldsOverrideModelIngredientGuesses() {
        String ocr = "Ingredients: whole grain oats, sugar, corn starch, salt. Nutrition Facts Calories 140";
        List<String> modelGuess = Arrays.asList("rice flour", "canola oil", "artificial flavor");

        assertEquals(
                Arrays.asList("whole grain oats", "sugar", "corn starch", "salt."),
                IngredientScoringInput.select(ocr, modelGuess)
        );
    }

    @Test
    public void modelParsingIsOnlyFallbackWhenOcrIsUnavailable() {
        assertEquals(
                Arrays.asList("water", "lemon juice", "salt"),
                IngredientScoringInput.select("Image upload (OCR failed)", Arrays.asList("water, lemon juice, salt"))
        );
    }

    @Test
    public void modelCleansHeadinglessButReadableOcr() {
        String ocr = "LIFEWTR PURIFIED WATER, MAGNESIUM SULFATE, POTASSIUM BICARBONATE";

        assertEquals(
                Arrays.asList("purified water", "magnesium sulfate", "potassium bicarbonate"),
                IngredientScoringInput.select(
                        ocr,
                        Arrays.asList("purified water", "magnesium sulfate", "potassium bicarbonate")
                )
        );
    }
}
