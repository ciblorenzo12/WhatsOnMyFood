package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HighSugarRule implements ProductAnalysisRule {

    private static final double SUGAR_THRESHOLD_G_PER_100G = 22.5; // NHS high total sugar threshold
    private static final String SOURCE_URL = "https://www.nhs.uk/live-well/eat-well/food-guidelines-and-food-labels/how-to-read-food-labels/";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null && productWithDetails.nutriments.sugars != null) {
            if (productWithDetails.nutriments.sugars > SUGAR_THRESHOLD_G_PER_100G) {
                AnalysisResult result = new AnalysisResult(
                        "High sugar content",
                        AnalysisResult.WarningLevel.WARNING,
                        15,
                        "total sugar",
                        String.format(
                                Locale.US,
                                "The nutrition data reports %.1f g of total sugar per 100 g. More than %.1f g per 100 g is considered high for a food, so this is better treated as an occasional choice rather than an everyday option.",
                                productWithDetails.nutriments.sugars,
                                SUGAR_THRESHOLD_G_PER_100G
                        )
                );
                result.setSourceUrl(SOURCE_URL);
                results.add(result);
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
