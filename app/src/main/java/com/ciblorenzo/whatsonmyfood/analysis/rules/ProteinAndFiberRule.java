package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class ProteinAndFiberRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "✅ Solid balance of protein and fiber, which helps you stay full longer and supports healthy digestion, especially when paired with low added sugar.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null && 
            productWithDetails.nutriments.proteins != null && 
            productWithDetails.nutriments.fiber != null && 
            productWithDetails.nutriments.sugars != null) {
            
            if (productWithDetails.nutriments.proteins >= 10 && productWithDetails.nutriments.fiber >= 5 && productWithDetails.nutriments.sugars < 10) {
                results.add(new AnalysisResult("Good source of protein & fiber", AnalysisResult.WarningLevel.POSITIVE, -15, null, EXPLANATION));
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Protein and fiber: restores up to 15 points when the product has at least 10 g protein and 5 g fiber, with under 10 g sugar. The final score cannot exceed 100.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.POSITIVE_INGREDIENT_SIGNALS;
    }
}
