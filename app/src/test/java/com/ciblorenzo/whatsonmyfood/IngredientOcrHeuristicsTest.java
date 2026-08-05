package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IngredientOcrHeuristicsTest {

    @Test
    public void selectsIngredientBlockAndIgnoresSurroundingScreenText() {
        String label = "Ingredients: Whole grain oats, corn starch, sugar, salt, "
                + "tripotassium phosphate, vitamin E (mixed tocopherols). Vitamins and minerals: "
                + "calcium carbonate, iron and zinc, vitamin C, vitamin B6, vitamin D3.";
        String surroundingScreen = "Habitos realistas\nZoom\nAlteracion do logo\nSearch";

        assertEquals(
                label,
                IngredientOcrHeuristics.selectIngredientRegion(
                        Arrays.asList(surroundingScreen, label, "View marketplace comparison"),
                        surroundingScreen + "\n" + label + "\nView marketplace comparison"
                )
        );
    }

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

    @Test
    public void repairsDigitAndPipeSubstitutionsInsideIngredientWords() {
        assertEquals(
                "partially hydrogenated oil, artificial flavor",
                IngredientOcrHeuristics.prepareRecognizedText(
                        "partia1ly hydrogenated oil, artificia| flavor"
                )
        );
    }
}
