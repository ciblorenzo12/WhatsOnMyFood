package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArtificialFlavorRuleTest {
    private ArtificialFlavorRule rule;

    @Before
    public void setUp() {
        rule = new ArtificialFlavorRule();
    }

    @Test
    public void evaluate_withArtificialMangoFlavor_returnsWarningResult() {
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = Collections.singletonList(new Ingredient("test_barcode", "Artificial mango flavor", 1));

        List<AnalysisResult> results = rule.evaluate(product);

        assertFalse(results.isEmpty());
        assertEquals(AnalysisResult.WarningLevel.WARNING, results.get(0).getLevel());
        assertEquals("Artificial mango flavor", results.get(0).getTriggeringIngredient());
    }

    @Test
    public void evaluate_withNoArtificialFlavor_returnsEmptyList() {
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = Collections.singletonList(new Ingredient("test_barcode", "Mango puree", 1));

        assertTrue(rule.evaluate(product).isEmpty());
    }
}
