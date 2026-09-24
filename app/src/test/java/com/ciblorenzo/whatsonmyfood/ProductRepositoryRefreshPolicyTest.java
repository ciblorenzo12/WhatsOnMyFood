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

    @Test
    public void refreshWithUnknownNutritionClearsDerivedClaimsButKeepsShopperState() {
        ProductWithDetails saved = new ProductWithDetails();
        saved.product = product("usda", "Old product", "Old healthy explanation", 100, 40);
        saved.product.isFavorite = true;
        ProductWithDetails refreshed = new ProductWithDetails();
        refreshed.product = product("usda", "Updated product", null, null, 0);

        ProductRefreshPolicy.preserveLocalState(refreshed, saved);

        assertNull(refreshed.product.healthScore);
        assertNull(refreshed.product.aiInsight);
        assertTrue(refreshed.product.isFavorite);
        assertEquals(Integer.valueOf(40), refreshed.product.userIngredientRiskScore);
    }

    @Test
    public void refreshWithComparableCoreNutritionRetainsExistingLocalBehavior() {
        ProductWithDetails saved = new ProductWithDetails();
        saved.product = product("usda", "Old product", "Saved explanation", 75, 40);
        ProductWithDetails refreshed = new ProductWithDetails();
        refreshed.product = product("usda", "Updated product", null, null, 0);
        refreshed.nutriments = new com.google.gson.Gson().fromJson(
                "{\"sugars_100g\":1,\"saturated-fat_100g\":0,\"sodium_100g\":0.1}", Nutriments.class);

        ProductRefreshPolicy.preserveLocalState(refreshed, saved);

        assertEquals(Integer.valueOf(75), refreshed.product.healthScore);
        assertEquals("Saved explanation", refreshed.product.aiInsight);
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
