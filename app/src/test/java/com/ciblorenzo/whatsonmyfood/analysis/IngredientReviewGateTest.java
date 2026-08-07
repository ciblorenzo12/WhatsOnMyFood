package com.ciblorenzo.whatsonmyfood.analysis;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IngredientReviewGateTest {

    @Test
    public void blocksReviewUntilProductHasUsableIngredients() {
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = new ArrayList<>();

        assertFalse(IngredientReviewGate.hasUsableIngredients(product));

        product.ingredients = Arrays.asList(new Ingredient("012345678905", "whole grain oats", 0));
        assertTrue(IngredientReviewGate.hasUsableIngredients(product));
    }

    @Test
    public void parsesTypedIngredientListBeforeReview() {
        assertEquals(
                Arrays.asList("whole grain oats", "sugar", "salt"),
                IngredientReviewGate.parseManualIngredients("Ingredients: whole grain oats, sugar, salt.")
        );
        assertTrue(IngredientReviewGate.parseManualIngredients("  ").isEmpty());
    }
}
