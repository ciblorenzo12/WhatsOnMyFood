package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class MilkRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "⚠️ Contains conventional milk. Look for milk that is certified organic, non-GMO, and ideally 100% grass-fed to avoid potential hormones, antibiotics, and inflammatory A1 casein.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    if (lowerCaseIngredient.contains("milk")) {
                        boolean isGoodMilk = lowerCaseIngredient.contains("organic") || 
                                             lowerCaseIngredient.contains("non-gmo") || 
                                             lowerCaseIngredient.contains("grass-fed");

                        if (!isGoodMilk) {
                            results.add(new AnalysisResult("Contains conventional milk", AnalysisResult.WarningLevel.WARNING, 15, "milk", EXPLANATION));
                            break; // Found it, no need to check further
                        }
                    }
                }
            }
        }
        return results;
    }
}
