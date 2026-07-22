package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IngredientTextParserTest {

    @Test
    public void parseIngredientCandidates_removesPromptMetadataAndProductText() {
        String text = "response_language: English, TRADER JOE'S, ROSE WATER, FACIAL, TONER, HYDRATE& REFRESH, NE 4 FL OZ (18)";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertTrue(ingredients.contains("rose water"));
        assertFalse(ingredients.contains("english"));
        assertFalse(ingredients.contains("trader joe's"));
        assertFalse(ingredients.contains("facial"));
        assertFalse(ingredients.contains("toner"));
    }

    @Test
    public void parseIngredientCandidates_extractsOnlyIngredientSection() {
        String text = "Front label Amazing Drink\nIngredients: Water, Cane Sugar, Citric Acid\nNutrition Facts Calories 80";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertEquals(3, ingredients.size());
        assertEquals("water", ingredients.get(0));
        assertEquals("cane sugar", ingredients.get(1));
        assertEquals("citric acid", ingredients.get(2));
    }

    @Test
    public void parseIngredientCandidates_stopsBeforeAllergensSection() {
        String text = "whole grain wheat, canola oil, sea salt.\nAllergens: gluten";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertEquals(3, ingredients.size());
        assertEquals("whole grain wheat", ingredients.get(0));
        assertEquals("canola oil", ingredients.get(1));
        assertEquals("sea salt", ingredients.get(2));
    }

    @Test
    public void parseIngredientCandidates_filtersNutritionClaimsFromIngredients() {
        String text = "PHENYLALANINE, NO CALORIES, NO SUGAR";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertEquals(1, ingredients.size());
        assertEquals("phenylalanine", ingredients.get(0));
    }

    @Test
    public void parseIngredientCandidates_stopsBeforePhenylketonuricsWarning() {
        String text = "CARBONATED WATER, CARAMEL COLOR, ASPARTAME, PHOSPHORIC ACID, POTASSIUM BENZOATE (TO PROTECT TASTE), NATURAL FLAVORS, CITRIC ACID, CAFFEINE. PHENYLKETONURICS: CONTAINS PHENYLALANINE NO CALORIES, NO SUGAR";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertEquals(8, ingredients.size());
        assertEquals("carbonated water", ingredients.get(0));
        assertEquals("caramel color", ingredients.get(1));
        assertEquals("aspartame", ingredients.get(2));
        assertEquals("caffeine", ingredients.get(7));
        assertFalse(ingredients.contains("no calories"));
        assertFalse(ingredients.contains("no sugar"));
    }

    @Test
    public void parseLabel_detectsContainsStatementWithoutAddingAllergensAsIngredients() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "Ingredients: oats, cocoa, salt. Contains: milk, wheat."
        );

        assertEquals(3, label.ingredients.size());
        assertEquals("salt", label.ingredients.get(2));
        assertEquals(java.util.Arrays.asList("milk", "wheat"), label.containsAllergens);
        assertTrue(label.mayContainAllergens.isEmpty());
        assertFalse(label.ingredients.contains("milk"));
        assertFalse(label.ingredients.contains("wheat"));
    }

    @Test
    public void parseLabel_detectsMayContainSeparatelyFromContains() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "Ingredients: oats, cocoa. Contains milk and soy. May contain: peanuts; tree nuts."
        );

        assertEquals(java.util.Arrays.asList("milk", "soy"), label.containsAllergens);
        assertEquals(java.util.Arrays.asList("peanuts", "tree nuts"), label.mayContainAllergens);
        assertEquals(java.util.Arrays.asList("oats", "cocoa"), label.ingredients);
    }

    @Test
    public void parseLabel_preservesNestedIngredientGroupsAndTheirInternalDelimiters() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "Ingredients: chocolate (sugar, cocoa butter, milk powder), wafer (wheat flour; palm oil), salt; Contains: milk, wheat"
        );

        assertEquals(3, label.ingredients.size());
        assertEquals("chocolate (sugar, cocoa butter, milk powder)", label.ingredients.get(0));
        assertEquals("wafer (wheat flour; palm oil)", label.ingredients.get(1));
        assertEquals("salt", label.ingredients.get(2));
        assertEquals(java.util.Arrays.asList("milk", "wheat"), label.containsAllergens);
    }

    @Test
    public void parseLabel_doesNotTreatContainsTwoPercentAsAnAllergenStatement() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "Ingredients: flour; contains 2% or less of: salt, yeast. Contains: wheat."
        );

        assertEquals(java.util.Arrays.asList("flour", "contains 2% or less of: salt", "yeast"), label.ingredients);
        assertEquals(java.util.Collections.singletonList("wheat"), label.containsAllergens);
    }

    @Test
    public void parseLabel_separatesInlineAllergenHeadingFromLastIngredient() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "Ingredients: sugar, palm oil, hazelnuts, vanillin an artificial flavor allergens: milk, nuts, soybeans"
        );

        assertEquals(
                java.util.Arrays.asList("sugar", "palm oil", "hazelnuts", "vanillin an artificial flavor"),
                label.ingredients
        );
        assertEquals(java.util.Arrays.asList("milk", "nuts", "soybeans"), label.containsAllergens);
        assertTrue(label.mayContainAllergens.isEmpty());
    }

    @Test
    public void parseLabel_removesNutritionAndServingSections() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "Ingredients. Water, lemon juice, sea salt. Nutrition Facts Calories 20 Serving Size 1 bottle"
        );

        assertEquals(java.util.Arrays.asList("water", "lemon juice", "sea salt"), label.ingredients);
    }

    @Test
    public void parseLabel_removesPackageDirectionsStorageAndWarnings() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "Ingredients: oats, honey, cinnamon. Directions: Add milk and stir. "
                        + "Storage instructions: Keep refrigerated. Warning: Do not consume seal."
        );

        assertEquals(java.util.Arrays.asList("oats", "honey", "cinnamon"), label.ingredients);
    }

    @Test
    public void parseLabel_removesAiPromptMetadataButKeepsPayload() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "response_language: English\n"
                        + "scan_mode: ingredients\n"
                        + "image_attached: true\n"
                        + "task: Parse the scanned label and return JSON.\n"
                        + "detected_ingredient_label:\n"
                        + "Water, cane sugar, citric acid\n"
                        + "baseline_health_score: 100"
        );

        assertEquals(java.util.Arrays.asList("water", "cane sugar", "citric acid"), label.ingredients);
    }

    @Test
    public void parseLabel_repairsCorruptedQuotedIngredientHeading() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "\u00e2\u20ac\u0153Ingredients.\u00e2\u20ac\u009d Water, cocoa, sea salt"
        );

        assertEquals(java.util.Arrays.asList("water", "cocoa", "sea salt"), label.ingredients);
    }

    @Test
    public void parseLabel_preservesIngredientNamesThatResemblePackageLanguage() {
        IngredientTextParser.ParsedLabel label = IngredientTextParser.parseLabel(
                "Ingredients: nutritional yeast, cooking oil, cultured dextrose, calcium carbonate, natural flavor (vanilla)"
        );

        assertEquals(
                java.util.Arrays.asList(
                        "nutritional yeast",
                        "cooking oil",
                        "cultured dextrose",
                        "calcium carbonate",
                        "natural flavor (vanilla)"
                ),
                label.ingredients
        );
    }
}
