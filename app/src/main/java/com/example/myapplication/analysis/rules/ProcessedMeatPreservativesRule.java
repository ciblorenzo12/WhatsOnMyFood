package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessedMeatPreservativesRule implements ProductAnalysisRule {

    private static final List<String> PRESERVATIVES = Arrays.asList("sodium nitrite", "sodium nitrate", "potassium nitrite");
    private static final String EXPLANATION = "❌ Contains processed meat preservatives (nitrites/nitrates) in a processed meat product. Frequent intake of processed meats has been linked with higher risk of colorectal cancer and heart disease. It’s best to limit these and favor fresh, minimally processed protein sources.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            boolean hasPreservative = false;
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String preservative : PRESERVATIVES) {
                        if (lowerCaseIngredient.contains(preservative)) {
                            hasPreservative = true;
                            break;
                        }
                    }
                }
                if(hasPreservative) break;
            }
            if (hasPreservative) {
                results.add(new AnalysisResult("Contains nitrites/nitrates", AnalysisResult.WarningLevel.SEVERE, 25, null, EXPLANATION));
            }
        }
        return results;
    }
}
