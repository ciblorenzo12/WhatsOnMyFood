package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArtificialSweetenersRule implements ProductAnalysisRule {

    private static final List<String> ARTIFICIAL_SWEETENERS = Arrays.asList("sucralose", "acesulfame potassium", "aspartame", "saccharin", "neotame", "advantame");
    private static final List<String> SUGAR_ALCOHOLS = Arrays.asList("sorbitol", "maltitol", "xylitol", "erythritol");
    private static final String EXPLANATION_ARTIFICIAL = "⚠️ Contains artificial sweeteners (like sucralose / aspartame). These reduce calories but their long-term impact on metabolism and gut microbiome is still being debated. Some people also experience headaches or digestive issues. If you’re aiming for ‘clean’ eating, you might prefer products that aren’t artificially sweetened.";
    private static final String EXPLANATION_ALCOHOLS = "⚠️ Contains sugar alcohols (like sorbitol / erythritol), which can cause digestive issues for some people.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String sweetener : ARTIFICIAL_SWEETENERS) {
                        if (lowerCaseIngredient.contains(sweetener)) {
                            results.add(new AnalysisResult("Contains " + sweetener, AnalysisResult.WarningLevel.WARNING, 10, sweetener, EXPLANATION_ARTIFICIAL));
                        }
                    }
                    for (String alcohol : SUGAR_ALCOHOLS) {
                        if (lowerCaseIngredient.contains(alcohol)) {
                            results.add(new AnalysisResult("Contains " + alcohol, AnalysisResult.WarningLevel.INFO, 10, alcohol, EXPLANATION_ALCOHOLS));
                        }
                    }
                }
            }
        }
        return results;
    }
}
