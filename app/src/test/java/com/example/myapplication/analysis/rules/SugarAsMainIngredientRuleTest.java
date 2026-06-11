package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.Nutriments;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SugarAsMainIngredientRuleTest {

    @Test
    public void evaluate_withOrganicCaneSugarUnderAddedSugarDailyValue_doesNotReturnSevereMainIngredientWarning() {
        ProductWithDetails product = productWithSugar(10.0);

        List<AnalysisResult> results = new SugarAsMainIngredientRule().evaluate(product);

        assertTrue(results.isEmpty());
    }

    @Test
    public void evaluate_withOrganicCaneSugarAboveAddedSugarDailyValue_returnsWarning() {
        ProductWithDetails product = productWithSugar(51.0);

        List<AnalysisResult> results = new SugarAsMainIngredientRule().evaluate(product);

        assertFalse(results.isEmpty());
    }

    @Test
    public void evaluate_withNoSugarClaim_doesNotReturnMainIngredientWarning() {
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = new ArrayList<>();
        product.ingredients.add(new Ingredient("test_barcode", "No sugar (Sugar)", 0));

        List<AnalysisResult> results = new SugarAsMainIngredientRule().evaluate(product);

        assertTrue(results.isEmpty());
    }

    private ProductWithDetails productWithSugar(Double addedSugarValue) {
        ProductWithDetails product = new ProductWithDetails();
        product.ingredients = new ArrayList<>();
        product.ingredients.add(new Ingredient("test_barcode", "Organic cane sugar (Added Sugar)", 0));
        product.nutriments = new Nutriments(
                "test_barcode",
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, addedSugarValue,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        );
        return product;
    }
}
