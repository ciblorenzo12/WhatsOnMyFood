package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// CORRECTED: No longer imports the old AnalysisResult
public class ArtificialColorsRule implements ProductAnalysisRule {

    private static final List<String> ARTIFICIAL_COLORS = Arrays.asList(
            "red 40", "yellow 5", "yellow 6", "blue 1", "blue 2", "green 3", "caramel color"
    );

    private static final String EXPLANATION = "❌ Contains artificial food dyes like Red 40 / Yellow 5. These are synthetic colors often added to make ultra-processed foods more appealing. Some studies and regulatory reviews have raised concerns about their potential impact on sensitive children (e.g., hyperactivity) and they don’t add any nutritional value. Choosing products colored with real foods (like paprika, beet juice, turmeric) is usually a safer bet.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String color : ARTIFICIAL_COLORS) {
                        if (lowerCaseIngredient.contains(color)) {
                            // This now correctly uses AnalysisResult from its own package
                            results.add(new AnalysisResult("Contains " + color, AnalysisResult.WarningLevel.SEVERE, 20, color, EXPLANATION));
                        }
                    }
                }
            }
        }
        return results;
    }
}
