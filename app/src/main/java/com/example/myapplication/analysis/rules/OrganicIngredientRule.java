package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class OrganicIngredientRule implements ProductAnalysisRule {

    private static final String ORGANIC = "organic";
    private static final String EXPLANATION = "✅ Contains organic ingredients. This is a good sign, as organic foods are grown without synthetic pesticides and fertilizers.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null && ingredient.text.toLowerCase().contains(ORGANIC)) {
                    results.add(new AnalysisResult("Contains organic ingredients", AnalysisResult.WarningLevel.POSITIVE, -5, ORGANIC, EXPLANATION));
                    break;
                }
            }
        }
        return results;
    }
}
