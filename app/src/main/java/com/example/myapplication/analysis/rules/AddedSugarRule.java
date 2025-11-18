package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.List;

public class AddedSugarRule implements ProductAnalysisRule {

    private static final String EXPLANATION_RED = "❌ High in added sugar: about %.1f g per serving (≈ %.1f teaspoons). Diets high in added sugar are linked to weight gain, insulin resistance, and dental problems. Try to keep most snacks under ~5 g added sugar per serving, and drinks as close to 0 as possible.";
    private static final String EXPLANATION_YELLOW = "Contains a moderate amount of added sugar.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null) {
            double addedSugar = productWithDetails.nutriments.sugar; // Using total sugars as a proxy
            if (addedSugar > 12) {
                results.add(new AnalysisResult("High added sugar", AnalysisResult.WarningLevel.SEVERE, 20, null, String.format(EXPLANATION_RED, addedSugar, addedSugar/4)));
            } else if (addedSugar > 5) {
                results.add(new AnalysisResult("Moderate added sugar", AnalysisResult.WarningLevel.WARNING, 10, null, EXPLANATION_YELLOW));
            }
        }
        return results;
    }
}
