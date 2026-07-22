package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WholeGrainsRule implements ProductAnalysisRule {

    private static final List<String> WHOLE_GRAINS = Arrays.asList("whole oats", "whole wheat", "brown rice", "quinoa", "whole grain");
    private static final String EXPLANATION = "✅ Uses whole grains (like oats / brown rice) as a main ingredient. Whole grains provide more fiber, vitamins, and minerals than refined flour and help support more stable blood sugar and better satiety.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (int i = 0; i < Math.min(3, productWithDetails.ingredients.size()); i++) {
                Ingredient ingredient = productWithDetails.ingredients.get(i);
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String grain : WHOLE_GRAINS) {
                        if (lowerCaseIngredient.contains(grain)) {
                            results.add(new AnalysisResult("Made with whole grains", AnalysisResult.WarningLevel.POSITIVE, -10, ingredient.text, EXPLANATION));
                            return results;
                        }
                    }
                }
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Whole grains: restores up to 10 points when a named whole grain appears among the first three ingredients. Multiple whole grains count only once, and the final score cannot exceed 100.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.POSITIVE_INGREDIENT_SIGNALS;
    }
}
