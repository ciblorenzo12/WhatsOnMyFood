package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class ShortIngredientListRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "✅ Very short, clean ingredient list made of recognizable foods. This is closer to home cooking and usually a good sign of minimal processing.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null
                && !productWithDetails.ingredients.isEmpty()
                && productWithDetails.ingredients.size() <= 5) {
            results.add(new AnalysisResult("Short ingredient list", AnalysisResult.WarningLevel.POSITIVE, -10, null, EXPLANATION));
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Short ingredient list: restores up to 10 points when the product has one to five listed ingredients. A missing ingredient list does not qualify, and the final score cannot exceed 100.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.POSITIVE_INGREDIENT_SIGNALS;
    }
}
