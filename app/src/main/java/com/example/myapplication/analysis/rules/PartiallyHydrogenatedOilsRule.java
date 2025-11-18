package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PartiallyHydrogenatedOilsRule implements ProductAnalysisRule {

    private static final List<String> TRANS_FATS = Arrays.asList("partially hydrogenated oil", "shortening");

    private static final String EXPLANATION = "❌ Contains partially hydrogenated oils, a source of industrial trans fats. These fats raise LDL (“bad”) cholesterol and are strongly linked with higher heart disease risk. Many countries have effectively banned them, so if they still appear on a label, it’s a product to avoid.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String transFat : TRANS_FATS) {
                        if (lowerCaseIngredient.contains(transFat)) {
                            results.add(new AnalysisResult("Contains trans fats", AnalysisResult.WarningLevel.SEVERE, 30, transFat, EXPLANATION));
                        }
                    }
                }
            }
        }
        return results;
    }
}
