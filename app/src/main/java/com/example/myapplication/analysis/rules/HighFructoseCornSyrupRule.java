package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class HighFructoseCornSyrupRule implements ProductAnalysisRule {

    private static final String HFCS = "high fructose corn syrup";
    private static final String EXPLANATION = "❌ Uses high-fructose corn syrup as a main sweetener. Heavy intake of added sugars (especially in drinks and sweets) is associated with weight gain, fatty liver, and higher risk of metabolic disease. Whole-food or minimal-sugar options are a better choice.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (int i = 0; i < productWithDetails.ingredients.size(); i++) {
                Ingredient ingredient = productWithDetails.ingredients.get(i);
                if (ingredient.text != null && ingredient.text.toLowerCase().contains(HFCS)) {
                    if (i < 3) { // Check if it's in the top 3 ingredients
                        results.add(new AnalysisResult("Contains high-fructose corn syrup", AnalysisResult.WarningLevel.SEVERE, 25, HFCS, EXPLANATION));
                    } else {
                        results.add(new AnalysisResult("Contains high-fructose corn syrup", AnalysisResult.WarningLevel.WARNING, 15, HFCS, EXPLANATION));
                    }
                    break;
                }
            }
        }
        return results;
    }
}
