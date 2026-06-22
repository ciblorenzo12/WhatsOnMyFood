package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArtificialSweetenersRule implements ProductAnalysisRule {

    private static final List<String> ARTIFICIAL_SWEETENERS = Arrays.asList("sucralose", "acesulfame potassium", "aspartame", "saccharin", "neotame", "advantame");
    private static final List<String> SUGAR_ALCOHOLS = Arrays.asList("sorbitol", "maltitol", "xylitol", "erythritol");
    private static final String EXPLANATION_ARTIFICIAL = "Contains an artificial sweetener. This is a caution signal for people who prefer minimally processed products, but it should be explained separately from added sugar.";
    private static final String EXPLANATION_ALCOHOLS = "Contains a sugar alcohol. Sugar alcohols are lower-sugar sweeteners and can be useful in some products, but they may cause digestive discomfort for sensitive people.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) {
            return results;
        }

        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            String lowerCaseIngredient = ingredient.text.toLowerCase();
            for (String sweetener : ARTIFICIAL_SWEETENERS) {
                if (lowerCaseIngredient.contains(sweetener)) {
                    results.add(new AnalysisResult("Contains " + sweetener, AnalysisResult.WarningLevel.WARNING, 5, sweetener, EXPLANATION_ARTIFICIAL));
                }
            }
            for (String alcohol : SUGAR_ALCOHOLS) {
                if (lowerCaseIngredient.contains(alcohol)) {
                    results.add(new AnalysisResult("Contains " + alcohol, AnalysisResult.WarningLevel.INFO, 0, alcohol, EXPLANATION_ALCOHOLS));
                }
            }
        }

        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Detects artificial sweeteners and sugar alcohols, separating caution signals from neutral informational notes.";
    }
}
