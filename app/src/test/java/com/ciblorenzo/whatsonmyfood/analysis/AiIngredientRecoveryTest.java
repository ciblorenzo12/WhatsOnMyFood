package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AiIngredientRecoveryTest {

    @Test
    public void parsesAndDeduplicatesLabelIngredients() {
        String json = "{\"ingredients_source\":\"label\",\"ingredient_confidence\":\"medium\","
                + "\"ingredients\":[\"Water\",\"water\",\"Sea salt\",\"Not listed\"]}";

        AiIngredientRecovery.Recovery recovery = AiIngredientRecovery.parse(json);

        assertEquals("label", recovery.source);
        assertEquals("medium", recovery.confidence);
        assertEquals(2, recovery.ingredients.size());
        assertTrue(recovery.toDisplayText().contains("verify on package"));
    }

    @Test
    public void rejectsIngredientsWithoutAnAcceptedSource() {
        AiIngredientRecovery.Recovery recovery = AiIngredientRecovery.parse(
                "{\"ingredients_source\":\"unknown\",\"ingredients\":[\"Sugar\"]}"
        );

        assertTrue(recovery.ingredients.isEmpty());
    }

    @Test
    public void rejectsIngredientsInferredFromProductIdentity() {
        AiIngredientRecovery.Recovery recovery = AiIngredientRecovery.parse(
                "{\"ingredients_source\":\"product_identity\",\"ingredient_confidence\":\"medium\","
                        + "\"product_name\":\"Classic Almond Butter\",\"brand\":\"Pantry Test Foods\","
                        + "\"ingredients\":[\"Almonds\",\"Sea salt\"]}"
        );

        assertTrue(recovery.ingredients.isEmpty());
    }

    @Test
    public void splitsACommaSeparatedModelItemWithoutBreakingParentheses() {
        AiIngredientRecovery.Recovery recovery = AiIngredientRecovery.parse(
                "{\"ingredients_source\":\"label\",\"ingredient_confidence\":\"high\","
                        + "\"ingredients\":[\"Oatmilk (Filtered Water, Oats), Cane Sugar, Sea Salt\"]}"
        );

        assertEquals(3, recovery.ingredients.size());
        assertEquals("Oatmilk (filtered water, oats)", recovery.ingredients.get(0));
        assertEquals("high", recovery.confidence);
    }

    @Test
    public void removesScanNoiseAndDeduplicatesNormalizedAliases() {
        AiIngredientRecovery.Recovery recovery = AiIngredientRecovery.parse(
                "{\"ingredients_source\":\"label\",\"ingredient_confidence\":\"medium\","
                        + "\"ingredients\":[\"Ingredients: Enriched wheat flour, CANE SUGAR, high-fructose corn syrup, E211\","
                        + "\"cane sugar\",\"sodium benzoate\",\"response_language: English\",\"Not listed\"]}"
        );

        assertEquals(4, recovery.ingredients.size());
        assertEquals("Enriched wheat flour", recovery.ingredients.get(0));
        assertEquals("Cane sugar", recovery.ingredients.get(1));
        assertEquals("High fructose corn syrup", recovery.ingredients.get(2));
        assertEquals("Sodium benzoate", recovery.ingredients.get(3));
    }
}
