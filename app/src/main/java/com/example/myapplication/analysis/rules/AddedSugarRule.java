package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class AddedSugarRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "❌ Contains added sugar (%.1f g). Any amount of added sugar is a significant negative signal. Diets high in added sugar are linked to numerous health problems. This product should be avoided or consumed in strict moderation.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null && productWithDetails.nutriments.addedSugars != null) {
            double addedSugarAmount = productWithDetails.nutriments.addedSugars;
            if (addedSugarAmount > 0) {
                results.add(new AnalysisResult(
                        "Contains Added Sugar", 
                        AnalysisResult.WarningLevel.SEVERE, 
                        50, 
                        "sugar",
                        String.format(EXPLANATION, addedSugarAmount)
                ));
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Detects any amount of added sugar. Added sugar is a significant negative signal linked to health problems.";
    }
}
