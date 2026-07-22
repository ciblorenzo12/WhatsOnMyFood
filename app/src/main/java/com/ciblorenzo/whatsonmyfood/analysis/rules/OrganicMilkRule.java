package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class OrganicMilkRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "✅ Contains organic milk. This is a good sign, as it avoids potential exposure to growth hormones and pesticides used in conventional dairy farming.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    if (lowerCaseIngredient.contains("organic") && lowerCaseIngredient.contains("milk")) {
                        results.add(new AnalysisResult("Contains Organic Milk", AnalysisResult.WarningLevel.POSITIVE, -10, "organic milk", EXPLANATION));
                        break;
                    }
                }
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Organic milk: restores up to 10 points when an ingredient names organic milk. Organic bonuses do not stack, and the final score cannot exceed 100.";
    }

    @Override
    public String getScoringGroup() {
        return "organic_ingredients";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.POSITIVE_INGREDIENT_SIGNALS;
    }
}
