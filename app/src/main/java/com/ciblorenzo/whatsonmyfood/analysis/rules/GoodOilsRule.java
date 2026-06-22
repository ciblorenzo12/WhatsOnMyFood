package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GoodOilsRule implements ProductAnalysisRule {

    private static final List<String> GOOD_OILS = Arrays.asList("olive oil", "avocado oil", "coconut oil");
    private static final String EXPLANATION = "✅ Contains beneficial oils like olive oil or avocado oil. These are healthy fats that are great for you!";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String goodOil : GOOD_OILS) {
                        if (lowerCaseIngredient.contains(goodOil)) {
                            results.add(new AnalysisResult("Contains beneficial oils", AnalysisResult.WarningLevel.POSITIVE, -5, goodOil, EXPLANATION));
                        }
                    }
                }
            }
        }
        return results;
    }
}
