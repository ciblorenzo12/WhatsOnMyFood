package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RagIngredientRecoveryTest {

    @Test
    public void validResponseIsParsedNormalizedDeduplicatedAndRanked() {
        ProductResponse response = new ProductResponse();
        response.status = 1;
        response.product = new ProductResponse.ProductData();
        response.product.ingredientsTextEn =
                "Ingredients: Water, CANE SUGAR, water, E330. Contains: milk.";

        RagIngredientRecovery.Result recovery = RagIngredientRecovery.fromResponse(
                "012345678905",
                response,
                null,
                "en"
        );

        assertTrue(recovery.recovered());
        List<String> ingredientNames = recovery.ingredients.stream()
                .map(ingredient -> ingredient.text)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("Water", "Cane sugar (Sugar)", "Citric acid"), ingredientNames);
        assertEquals(0, recovery.ingredients.get(0).rank);
        assertEquals(2, recovery.ingredients.get(2).rank);
    }

    @Test
    public void emptyInvalidAndWarningOnlyResponsesDoNotProduceIngredients() {
        assertFalse(RagIngredientRecovery.fromResponse(
                "012345678905", null, null, "en"
        ).recovered());

        ProductResponse unavailable = new ProductResponse();
        unavailable.status = 0;
        assertFalse(RagIngredientRecovery.fromResponse(
                "012345678905", unavailable, null, "en"
        ).recovered());

        ProductResponse empty = new ProductResponse();
        empty.status = 1;
        empty.product = new ProductResponse.ProductData();
        empty.product.ingredientsTextEn = "   ";
        assertFalse(RagIngredientRecovery.fromResponse(
                "012345678905", empty, null, "en"
        ).recovered());

        assertFalse(RagIngredientRecovery.fromText(
                "012345678905", "Phenylalanine", null
        ).recovered());
    }
}
