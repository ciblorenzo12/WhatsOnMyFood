package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProductNameOcrValidatorTest {

    @Test
    public void acceptsFrontLabelProductNames() {
        ProductNameOcrValidator.Result nutella = ProductNameOcrValidator.validate("nutella\nHAZELNUT SPREAD");
        ProductNameOcrValidator.Result cheerios = ProductNameOcrValidator.validate("General Mills\nCHEERIOS\nOriginal");

        assertTrue(nutella.readable);
        assertTrue(cheerios.readable);
        assertFalse(nutella.bestCandidate.isEmpty());
    }

    @Test
    public void rejectsIngredientAndNutritionPanels() {
        assertFalse(ProductNameOcrValidator.validate("INGREDIENTS: sugar, cocoa, palm oil").readable);
        assertFalse(ProductNameOcrValidator.validate("Nutrition Facts\nCalories 190\nTotal Fat 11g").readable);
        assertFalse(ProductNameOcrValidator.validate("NET WT 12 OZ\nKEEP REFRIGERATED").readable);
    }

    @Test
    public void mergerKeepsProductIdentityBeforeOneIngredientSection() {
        String merged = SupplementalOcrMerger.merge(
                "NUTELLA\nHazelnut Spread",
                "INGREDIENTS: Sugar, palm oil, hazelnuts"
        );

        assertTrue(merged.startsWith("NUTELLA"));
        assertEquals(1, merged.split("Ingredients:", -1).length - 1);
        assertTrue(merged.contains("Sugar, palm oil, hazelnuts"));
    }
}
