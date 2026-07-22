package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GoodOilsRule implements ProductAnalysisRule {

    private static final List<String> GOOD_OILS = Arrays.asList("olive oil", "avocado oil", "coconut oil");
    private static final String EXPLANATION = "Contains olive, avocado, or coconut oil. These oils receive a positive adjustment under this app's oil policy.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) return results;

        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            String normalized = ingredient.text.toLowerCase(Locale.US);
            for (String goodOil : GOOD_OILS) {
                if (normalized.contains(goodOil)) {
                    results.add(new AnalysisResult(
                            "Contains beneficial oil",
                            AnalysisResult.WarningLevel.POSITIVE,
                            -5,
                            goodOil,
                            EXPLANATION
                    ));
                    return results;
                }
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Beneficial oil: restores up to 5 points when the ingredient list names olive, avocado, or coconut oil. Multiple matching oils count only once, and the final score cannot exceed 100.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.OILS;
    }
}
