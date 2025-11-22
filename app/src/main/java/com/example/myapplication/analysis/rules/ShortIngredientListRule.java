package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class ShortIngredientListRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "✅ Very short, clean ingredient list made of recognizable foods. This is closer to home cooking and usually a good sign of minimal processing.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null && productWithDetails.ingredients.size() <= 5) {
            results.add(new AnalysisResult("Short ingredient list", AnalysisResult.WarningLevel.INFO, -10, null, EXPLANATION));
        }
        return results;
    }
}
