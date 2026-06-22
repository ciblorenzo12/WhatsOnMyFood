package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class OrganicWheatRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "✅ Contains organic wheat. Choosing organic wheat helps reduce exposure to synthetic pesticides like glyphosate, which are commonly used in conventional wheat farming. It's a great choice for cleaner eating.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    if (lowerCaseIngredient.contains("organic") && lowerCaseIngredient.contains("wheat")) {
                        results.add(new AnalysisResult("Contains Organic Wheat", AnalysisResult.WarningLevel.POSITIVE, -10, ingredient.text, EXPLANATION));
                        // Found it, no need to check further in this product
                        break;
                    }
                }
            }
        }
        return results;
    }
}
