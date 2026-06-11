package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class TransFatNutritionRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "Contains measurable trans fat. Trans fats are a strong negative signal because they can raise LDL cholesterol and increase heart disease risk.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null
                && productWithDetails.nutriments != null
                && productWithDetails.nutriments.transFat != null
                && productWithDetails.nutriments.transFat > 0) {
            results.add(new AnalysisResult(
                    "Contains trans fat",
                    AnalysisResult.WarningLevel.SEVERE,
                    30,
                    null,
                    EXPLANATION
            ));
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Detects measurable trans fat from nutrition data and applies a severe penalty.";
    }
}
