package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RefinedFlourRule implements ProductAnalysisRule {

    private static final List<String> REFINED_FLOUR_TERMS = Arrays.asList(
            "enriched flour",
            "wheat flour",
            "white flour",
            "bleached flour",
            "unbleached flour",
            "refined flour"
    );

    private static final String EXPLANATION = "Refined flour appears near the top of the ingredient list. Refined grains usually have less fiber and fewer naturally occurring nutrients than whole grains.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) {
            return results;
        }

        for (int i = 0; i < Math.min(3, productWithDetails.ingredients.size()); i++) {
            Ingredient ingredient = productWithDetails.ingredients.get(i);
            if (ingredient == null || ingredient.text == null) continue;

            String lowerCaseIngredient = ingredient.text.toLowerCase();
            if (lowerCaseIngredient.contains("whole")) {
                continue;
            }

            for (String flourTerm : REFINED_FLOUR_TERMS) {
                if (lowerCaseIngredient.contains(flourTerm)) {
                    results.add(new AnalysisResult(
                            "Refined flour is a main ingredient",
                            AnalysisResult.WarningLevel.WARNING,
                            10,
                            ingredient.text,
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
        return "Detects refined flour in the first three ingredients unless it is clearly whole grain.";
    }
}
