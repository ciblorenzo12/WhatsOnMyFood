package com.example.myapplication.analysis.rules;

import com.example.myapplication.Nutriments;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for the AddedSugarRule.
 * Verifies that the rule applies the correct penalty and handles edge cases.
 */
public class AddedSugarRuleTest {

    private AddedSugarRule rule;

    @Before
    public void setUp() {
        rule = new AddedSugarRule();
    }

    /**
     * Helper method to create a Nutriments object for testing.
     * This encapsulates the massive constructor and makes tests cleaner.
     * @param addedSugarValue The value to use for the addedSugars field.
     */
    private Nutriments createNutrimentsWithAddedSugar(Double addedSugarValue) {
        return new Nutriments(
            "test_barcode",
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, addedSugarValue, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    @Test
    public void evaluate_withAddedSugar_returnsSevereResultWithCorrectPenalty() {
        // Arrange
        ProductWithDetails product = new ProductWithDetails();
        product.nutriments = createNutrimentsWithAddedSugar(10.0); // 10g of added sugar

        // Act
        List<AnalysisResult> results = rule.evaluate(product);

        // Assert
        assertFalse("Rule should trigger for added sugar", results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(AnalysisResult.WarningLevel.SEVERE, results.get(0).getLevel());
        assertEquals(50, results.get(0).getScorePenalty());
    }

    @Test
    public void evaluate_withZeroAddedSugar_returnsEmptyList() {
        // Arrange
        ProductWithDetails product = new ProductWithDetails();
        product.nutriments = createNutrimentsWithAddedSugar(0.0); // 0g of added sugar

        // Act
        List<AnalysisResult> results = rule.evaluate(product);

        // Assert
        assertTrue("Rule should not trigger for zero added sugar", results.isEmpty());
    }

    @Test
    public void evaluate_withNullNutriments_returnsEmptyList() {
        // Arrange
        ProductWithDetails product = new ProductWithDetails();
        product.nutriments = null;

        // Act
        List<AnalysisResult> results = rule.evaluate(product);

        // Assert
        assertTrue("Rule should not trigger for null nutriments", results.isEmpty());
    }

    @Test
    public void evaluate_withNullAddedSugarField_returnsEmptyList() {
        // Arrange
        ProductWithDetails product = new ProductWithDetails();
        product.nutriments = createNutrimentsWithAddedSugar(null); // Null added sugar value

        // Act
        List<AnalysisResult> results = rule.evaluate(product);

        // Assert
        assertTrue("Rule should handle null added sugar value gracefully", results.isEmpty());
    }
}
