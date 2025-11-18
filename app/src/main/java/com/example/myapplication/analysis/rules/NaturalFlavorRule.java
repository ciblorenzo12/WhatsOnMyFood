package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class NaturalFlavorRule implements ProductAnalysisRule {

    private static final String NATURAL_FLAVOR_SINGULAR = "natural flavor";
    private static final String NATURAL_FLAVOR_PLURAL = "natural flavors";
    private static final String EXPLANATION = "❌ Contains 'natural flavors'. This is a catch-all term for proprietary chemical mixtures derived from natural sources. While not necessarily harmful, it hides the true ingredients, preventing you from knowing exactly what you’re eating. Choose products with clearly listed flavors like 'vanilla extract' or 'lemon juice' instead.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    // Prioritize checking for the plural form first
                    if (lowerCaseIngredient.contains(NATURAL_FLAVOR_PLURAL)) {
                        results.add(new AnalysisResult("Contains 'Natural Flavors'", AnalysisResult.WarningLevel.SEVERE, 20, NATURAL_FLAVOR_PLURAL, EXPLANATION));
                        break; // Found it, no need to check further
                    } else if (lowerCaseIngredient.contains(NATURAL_FLAVOR_SINGULAR)) {
                        results.add(new AnalysisResult("Contains 'Natural Flavor'", AnalysisResult.WarningLevel.SEVERE, 20, NATURAL_FLAVOR_SINGULAR, EXPLANATION));
                        break; // Found it, no need to check further
                    }
                }
            }
        }
        return results;
    }
}
