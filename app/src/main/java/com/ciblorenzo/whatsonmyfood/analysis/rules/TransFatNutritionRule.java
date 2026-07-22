package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

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
        return "Measured trans fat: subtracts 30 points when nutrition data reports more than 0 g. It shares the trans-fat group with hydrogenated oil, so the concern counts only once.";
    }

    @Override
    public String getScoringGroup() {
        return "trans_fat";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.OILS;
    }
}
