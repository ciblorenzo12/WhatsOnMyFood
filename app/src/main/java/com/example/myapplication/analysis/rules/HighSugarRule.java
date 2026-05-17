package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class HighSugarRule implements ProductAnalysisRule {

    private static final double SUGAR_THRESHOLD_G_PER_100G = 22.5; // NHS high total sugar threshold
    private static final String EXPLANATION = "❌ High in sugar. Diets high in sugar are linked to weight gain, insulin resistance, and dental problems.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null && productWithDetails.nutriments.sugars != null) {
            if (productWithDetails.nutriments.sugars > SUGAR_THRESHOLD_G_PER_100G) {
                results.add(new AnalysisResult("High sugar content", AnalysisResult.WarningLevel.WARNING, 15, null, EXPLANATION));
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Detects products with high total sugar content (above 22.5g per 100g). High sugar intake is linked to obesity and metabolic issues.";
    }
}
