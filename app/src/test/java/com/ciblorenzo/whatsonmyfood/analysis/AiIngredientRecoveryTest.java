package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AiIngredientRecoveryTest {

    @Test
    public void parsesAndDeduplicatesIdentityBasedIngredients() {
        String json = "{\"ingredients_source\":\"product_identity\",\"ingredient_confidence\":\"medium\","
                + "\"ingredients\":[\"Water\",\"water\",\"Sea salt\",\"Not listed\"]}";

        AiIngredientRecovery.Recovery recovery = AiIngredientRecovery.parse(json);

        assertEquals("product_identity", recovery.source);
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
    public void splitsACommaSeparatedModelItemWithoutBreakingParentheses() {
        AiIngredientRecovery.Recovery recovery = AiIngredientRecovery.parse(
                "{\"ingredients_source\":\"product_identity\",\"ingredient_confidence\":\"high\","
                        + "\"ingredients\":[\"Oatmilk (Filtered Water, Oats), Cane Sugar, Sea Salt\"]}"
        );

        assertEquals(3, recovery.ingredients.size());
        assertEquals("Oatmilk (Filtered Water, Oats)", recovery.ingredients.get(0));
        assertEquals("medium", recovery.confidence);
    }
}
