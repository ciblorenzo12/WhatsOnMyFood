package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class NonOrganicWheatRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "⚠️ Contains conventional (non-organic) wheat. Conventional wheat is often grown using pesticides like glyphosate. Choosing organic wheat is a way to reduce exposure to these synthetic chemicals.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    if (lowerCaseIngredient.contains("wheat") && !lowerCaseIngredient.contains("organic")) {
                        results.add(new AnalysisResult("Contains non-organic wheat", AnalysisResult.WarningLevel.WARNING, 20, "wheat", EXPLANATION));
                        break; // Found it, no need to check further
                    }
                }
            }
        }
        return results;
    }
}
