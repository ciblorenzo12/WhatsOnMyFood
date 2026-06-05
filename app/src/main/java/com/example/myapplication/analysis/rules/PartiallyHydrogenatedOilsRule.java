package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PartiallyHydrogenatedOilsRule implements ProductAnalysisRule {

    private static final List<String> TRANS_FATS = Arrays.asList(
            "partially hydrogenated oil",
            "partially hydrogenated",
            "hydrogenated vegetable oil",
            "fully hydrogenated oil"
    );

    private static final String EXPLANATION = "Contains hydrogenated or partially hydrogenated oil, a trans-fat concern. This is different from plain canola, sunflower, soybean, corn, or vegetable oil, which are refined oils but not automatically hydrogenated.";

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
