package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.List;

public class HighSugarRule implements ProductAnalysisRule {

    private static final double SUGAR_THRESHOLD_G_PER_100G = 0.1;
    private static final String EXPLANATION = "❌ Contains sugar. Diets high in added sugar are linked to weight gain, insulin resistance, and dental problems. Try to keep most snacks under ~5 g added sugar per serving, and drinks as close to 0 as possible.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null) {
            if (productWithDetails.nutriments.sugar > SUGAR_THRESHOLD_G_PER_100G) {
                results.add(new AnalysisResult("Contains sugar", AnalysisResult.WarningLevel.SEVERE, 30, "sugar", EXPLANATION));
            }
        }
        return results;
    }
}
