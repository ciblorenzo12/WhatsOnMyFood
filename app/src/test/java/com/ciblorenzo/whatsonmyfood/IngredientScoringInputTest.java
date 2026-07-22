package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IngredientScoringInputTest {

    @Test
    public void parsedOcrFieldsOverrideModelIngredientGuesses() {
        String ocr = "Ingredients: whole grain oats, sugar, corn starch, salt. Nutrition Facts Calories 140";
        List<String> modelGuess = Arrays.asList("rice flour", "canola oil", "artificial flavor");

        assertEquals(
                Arrays.asList("whole grain oats", "sugar", "corn starch", "salt"),
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

    @Test
    public void modelFallbackCropsEditorChromeAroundEmbeddedIngredientHeading() {
        List<String> noisyModelItems = Arrays.asList(
                "de",
                "new t",
                "requirement analysis",
                "view",
                "edit",
                "file",
                "a€oIngredients. afd Water",
                "cocoa",
                "saltwater",
                "cocoa",
                "salt",
                "Plain text",
                "56 characters",
                "Ln 1 Col 56",
                "Windows (CRLF)"
        );

        assertEquals(
                Arrays.asList("water", "cocoa", "saltwater", "salt"),
                IngredientScoringInput.select("Image upload (OCR failed)", noisyModelItems)
        );
    }

    @Test
    public void modelFallbackFiltersEditorItemsWithoutDroppingLaterIngredients() {
        List<String> noisyModelItems = Arrays.asList(
                "file",
                "fess",
                "edit view",
                "config",
                "â€ ingredients:â€ water",
                "requirement",
                "env",
                "saltwater",
                "cocoa)",
                "plain text",
                "appjs",
                "salt",
                "build and run",
                "requirement requirement"
        );

        assertEquals(
                Arrays.asList("water", "saltwater", "cocoa", "salt"),
                IngredientScoringInput.select("Image upload (OCR failed)", noisyModelItems)
        );
    }

    @Test
    public void allergensReachAnalysisInputWithoutJoiningIngredients() {
        IngredientScoringInput.Selection selection = IngredientScoringInput.selectWithAllergens(
                "Ingredients: oats, cocoa. Contains: milk, soy. May contain: peanuts.",
                Arrays.asList("invented ingredient")
        );

        assertEquals(Arrays.asList("oats", "cocoa"), selection.ingredients);
        assertEquals(Arrays.asList("milk", "soy"), selection.containsAllergens);
        assertEquals(Arrays.asList("peanuts"), selection.mayContainAllergens);
        assertFalse(selection.ingredients.contains("milk"));
        assertFalse(selection.ingredients.contains("peanuts"));
    }
}
