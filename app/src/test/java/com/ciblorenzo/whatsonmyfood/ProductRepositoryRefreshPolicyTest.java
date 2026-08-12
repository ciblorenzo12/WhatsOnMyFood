package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProductRepositoryRefreshPolicyTest {

    @Test
    public void refreshKeepsLocalFieldsWhileAcceptingNewExternalFields() {
        Product saved = product("m4-06-product", "Old product name", "Saved insight", 91, 60);
        saved.isFavorite = true;
        Product refreshed = product("m4-06-product", "Updated product name", null, null, 0);

        ProductRefreshPolicy.preserveLocalState(refreshed, saved);

        assertEquals("Updated product name", refreshed.productName);
        assertTrue(refreshed.isFavorite);
        assertEquals(Integer.valueOf(91), refreshed.healthScore);
        assertEquals("Saved insight", refreshed.aiInsight);
        assertEquals(Integer.valueOf(60), refreshed.userIngredientRiskScore);
    }

    @Test
    public void productWithoutSavedStateKeepsItsRefreshDefaults() {
        Product refreshed = product("m4-06-new", "New product", null, null, 0);

        ProductRefreshPolicy.preserveLocalState(refreshed, null);

        assertFalse(refreshed.isFavorite);
        assertNull(refreshed.healthScore);
        assertNull(refreshed.aiInsight);
        assertEquals(Integer.valueOf(0), refreshed.userIngredientRiskScore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mismatchedBarcodeCannotCopyAnotherProductsState() {
        Product saved = product("m4-06-saved", "Saved", "Insight", 80, 20);
        Product refreshed = product("m4-06-other", "Other", null, null, 0);
        ProductRefreshPolicy.preserveLocalState(refreshed, saved);
    }

    private Product product(
            String barcode,
            String name,
            String insight,
            Integer healthScore,
            Integer userScore
    ) {
        return new Product(
                barcode, name, "Test Foods", "12 oz", "", "", "Box", "Test",
                "1 serving", "b", "2", "b", insight, healthScore, userScore
        );
    }
}
