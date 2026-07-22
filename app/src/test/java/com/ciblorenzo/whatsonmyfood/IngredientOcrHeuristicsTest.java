package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IngredientOcrHeuristicsTest {

    @Test
    public void scoresClearIngredientTextAboveNutritionPanelText() {
        String ingredients = "INGREDIENTS: Whole grain oats, sugar, corn starch, salt, natural flavor.";
        String nutrition = "Nutrition Facts Calories 140 Serving size 1 cup Total Fat 2g Daily Value 3%";

        assertTrue(IngredientOcrHeuristics.confidence(ingredients)
                > IngredientOcrHeuristics.confidence(nutrition));
        assertTrue(IngredientOcrHeuristics.confidence(ingredients) > 25);
    }

    @Test
    public void repairsCommonLowQualityHeadingAndLineHyphenation() {
        String skewedOcr = "INGRED1ENTS: whole grain oat-\n flour, sugar, corn starch, salt";

        String prepared = IngredientOcrHeuristics.prepareRecognizedText(skewedOcr);

        assertTrue(prepared.startsWith("ingredients:"));
        assertTrue(prepared.contains("oatflour"));
        assertTrue(IngredientOcrHeuristics.confidence(prepared) > 25);
    }

    @Test
    public void selectsEnhancedPassWhenItRecoversIngredientContent() {
        String original = "NUTRITION FACTS CALORIES 120";
        String enhanced = "INGREDIENTS: water, tomato, olive oil, salt, citric acid";

        assertEquals(
                IngredientOcrHeuristics.prepareRecognizedText(enhanced),
                IngredientOcrHeuristics.chooseBest(original, enhanced)
        );
    }

    @Test
    public void removesWebPageNoiseAfterIngredientList() {
        String text = "Ingredients: oats, sugar, salt, natural flavor\nDownload label\nRelated searches";

        assertEquals(
                "Ingredients: oats, sugar, salt, natural flavor",
                IngredientOcrHeuristics.trimUiNoise(text)
        );
    }

    @Test
    public void preservesPromptTextAndIngredientPayloadWhenTaskMentionsLabel() {
        String text = "task: Parse the scanned label and return JSON.\n"
                + "detected_ingredient_label:\n"
                + "water, cane sugar, citric acid";

        assertEquals(
                IngredientOcrHeuristics.prepareRecognizedText(text),
                IngredientOcrHeuristics.trimUiNoise(text)
        );
    }
}
