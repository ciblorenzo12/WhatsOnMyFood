package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.List;

public class ProteinAndFiberRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "✅ Solid balance of protein and fiber, which helps you stay full longer and supports healthy digestion, especially when paired with low added sugar.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null) {
            if (productWithDetails.nutriments.proteins >= 10 && productWithDetails.nutriments.fiber >= 5 && productWithDetails.nutriments.sugar < 10) {
                results.add(new AnalysisResult("Good source of protein & fiber", AnalysisResult.WarningLevel.INFO, -15, null, EXPLANATION));
            }
        }
        return results;
    }
}
