package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class HighSodiumRule implements ProductAnalysisRule {

    private static final double SODIUM_THRESHOLD_MG_PER_100G = 600; // NHS high sodium threshold (1.5g salt ≈ 600mg sodium)
    private static final String EXPLANATION = "❌ High in sodium. Eating too much sodium (from salt) can raise your blood pressure, which increases your risk of heart disease and stroke.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null && productWithDetails.nutriments.sodium != null) {
            // The API provides sodium in g, so we convert to mg for comparison
            double sodiumInMg = productWithDetails.nutriments.sodium * 1000;
            if (sodiumInMg > SODIUM_THRESHOLD_MG_PER_100G) {
                results.add(new AnalysisResult("High sodium content", AnalysisResult.WarningLevel.WARNING, 20, null, EXPLANATION));
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "High sodium: subtracts 20 points when nutrition data reports more than 600 mg sodium per 100 g.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.SODIUM;
    }
}
