package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class HighSaltRule implements ProductAnalysisRule {

    private static final double SALT_THRESHOLD_G_PER_100G = 1.5; // NHS high salt threshold
    private static final String EXPLANATION = "❌ High in salt. Eating too much salt can raise your blood pressure, which increases your risk of heart disease and stroke.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null && productWithDetails.nutriments.sodium != null) {
            // The API provides sodium, so we convert the threshold from salt to sodium for comparison
            double sodium_threshold_g = SALT_THRESHOLD_G_PER_100G / 2.5;
            if (productWithDetails.nutriments.sodium > sodium_threshold_g) {
                results.add(new AnalysisResult("High salt content", AnalysisResult.WarningLevel.WARNING, 20, null, EXPLANATION));
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "High salt: subtracts 20 points when sodium is above 0.6 g per 100 g, the equivalent of 1.5 g salt. This legacy rule is not part of the active rule set because the sodium rule covers the same signal.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.SODIUM;
    }
}
