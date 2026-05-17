package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class OrganicMilkRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "✅ Contains organic milk. This is a good sign, as it avoids potential exposure to growth hormones and pesticides used in conventional dairy farming.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    if (lowerCaseIngredient.contains("organic") && lowerCaseIngredient.contains("milk")) {
                        results.add(new AnalysisResult("Contains Organic Milk", AnalysisResult.WarningLevel.POSITIVE, -10, "organic milk", EXPLANATION));
                        break;
                    }
                }
            }
        }
        return results;
    }
}
