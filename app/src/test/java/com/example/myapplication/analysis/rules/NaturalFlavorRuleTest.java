package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for the NaturalFlavorRule.
 * These tests verify that the rule correctly identifies both singular and plural forms
 * and applies the correct penalty, while ignoring unrelated ingredients.
 */
public class NaturalFlavorRuleTest {

    private NaturalFlavorRule rule;

    @Before
    public void setUp() {
        rule = new NaturalFlavorRule();
    }

    @Test
    public void evaluate_withSingularNaturalFlavor_returnsWarningResult() {
        // Arrange
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = Collections.singletonList(new Ingredient("test_barcode", "Contains natural flavor.", 1));

        // Act
        List<AnalysisResult> results = rule.evaluate(product);

        // Assert
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(AnalysisResult.WarningLevel.WARNING, results.get(0).getLevel());
        assertEquals(5, results.get(0).getScorePenalty());
        assertEquals("natural flavor", results.get(0).getTriggeringIngredient());
    }

    @Test
    public void evaluate_withPluralNaturalFlavors_returnsWarningResult() {
        // Arrange
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = Collections.singletonList(new Ingredient("test_barcode", "Water, natural flavors, salt.", 1));

        // Act
        List<AnalysisResult> results = rule.evaluate(product);

        // Assert
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(AnalysisResult.WarningLevel.WARNING, results.get(0).getLevel());
        assertEquals(5, results.get(0).getScorePenalty());
        assertEquals("natural flavors", results.get(0).getTriggeringIngredient());
    }

    @Test
    public void evaluate_withNoNaturalFlavor_returnsEmptyList() {
        // Arrange
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = Collections.singletonList(new Ingredient("test_barcode", "Organic milk, vitamin D3.", 1));

        // Act
        List<AnalysisResult> results = rule.evaluate(product);

        // Assert
        assertTrue("The result list should be empty when there is no natural flavor", results.isEmpty());
    }

    @Test
    public void evaluate_withNullIngredients_returnsEmptyList() {
        // Arrange
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = null;

        // Act
        List<AnalysisResult> results = rule.evaluate(product);

        // Assert
        assertTrue("The result list should be empty for a product with null ingredients", results.isEmpty());
    }
}
