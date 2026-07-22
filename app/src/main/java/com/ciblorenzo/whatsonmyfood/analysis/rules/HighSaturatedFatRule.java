package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class HighSaturatedFatRule implements ProductAnalysisRule {

    private static final double SAT_FAT_THRESHOLD_G_PER_100G = 5.0; // NHS high saturated fat threshold
    private static final String EXPLANATION = "❌ High in saturated fat. A diet high in saturated fat can raise the level of cholesterol in the blood, which increases the risk of heart disease.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null && productWithDetails.nutriments.saturatedFat != null) {
            if (productWithDetails.nutriments.saturatedFat > SAT_FAT_THRESHOLD_G_PER_100G) {
                results.add(new AnalysisResult("High saturated fat content", AnalysisResult.WarningLevel.WARNING, 20, null, EXPLANATION));
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "High saturated fat: subtracts 20 points when nutrition data reports more than 5 g of saturated fat per 100 g.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.GENERAL_NUTRITION;
    }
}
