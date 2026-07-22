package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

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
        return "High total sugar: subtracts 15 points when nutrition data reports more than 22.5 g total sugar per 100 g. This can stack with added sugar because total sugar is a separate nutrition signal.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.SUGAR;
    }
}
