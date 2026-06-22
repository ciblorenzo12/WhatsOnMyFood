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

        assertTrue(ingredients.contains("ROSE WATER"));
        assertFalse(ingredients.contains("English"));
        assertFalse(ingredients.contains("TRADER JOE'S"));
        assertFalse(ingredients.contains("FACIAL"));
        assertFalse(ingredients.contains("TONER"));
    }

    @Test
    public void parseIngredientCandidates_extractsOnlyIngredientSection() {
        String text = "Front label Amazing Drink\nIngredients: Water, Cane Sugar, Citric Acid\nNutrition Facts Calories 80";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertEquals(3, ingredients.size());
        assertEquals("Water", ingredients.get(0));
        assertEquals("Cane Sugar", ingredients.get(1));
        assertEquals("Citric Acid", ingredients.get(2));
    }

    @Test
    public void parseIngredientCandidates_stopsBeforeAllergensSection() {
        String text = "whole grain wheat, canola oil, sea salt.\nAllergens: gluten";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertEquals(3, ingredients.size());
        assertEquals("whole grain wheat", ingredients.get(0));
        assertEquals("canola oil", ingredients.get(1));
        assertEquals("sea salt.", ingredients.get(2));
    }

    @Test
    public void parseIngredientCandidates_filtersNutritionClaimsFromIngredients() {
        String text = "PHENYLALANINE, NO CALORIES, NO SUGAR";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertEquals(1, ingredients.size());
        assertEquals("PHENYLALANINE", ingredients.get(0));
    }

    @Test
    public void parseIngredientCandidates_stopsBeforePhenylketonuricsWarning() {
        String text = "CARBONATED WATER, CARAMEL COLOR, ASPARTAME, PHOSPHORIC ACID, POTASSIUM BENZOATE (TO PROTECT TASTE), NATURAL FLAVORS, CITRIC ACID, CAFFEINE. PHENYLKETONURICS: CONTAINS PHENYLALANINE NO CALORIES, NO SUGAR";

        List<String> ingredients = IngredientTextParser.parseIngredientCandidates(text);

        assertEquals(8, ingredients.size());
        assertEquals("CARBONATED WATER", ingredients.get(0));
        assertEquals("CARAMEL COLOR", ingredients.get(1));
        assertEquals("ASPARTAME", ingredients.get(2));
        assertEquals("CAFFEINE.", ingredients.get(7));
        assertFalse(ingredients.contains("NO CALORIES"));
        assertFalse(ingredients.contains("NO SUGAR"));
    }
}
