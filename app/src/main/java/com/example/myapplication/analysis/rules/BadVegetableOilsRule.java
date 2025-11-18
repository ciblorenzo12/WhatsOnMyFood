package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BadVegetableOilsRule implements ProductAnalysisRule {

    private static final List<String> BAD_OILS = Arrays.asList(
            "vegetable oil", "sunflower oil", "canola oil", "soybean oil", "palm oil", "corn oil", "palm kernel oil", "shortening"
    );

    private static final String EXPLANATION = "⚠️ Contains refined industrial oils (like soy/corn/palm/canola). These are often found in ultra-processed foods and can contribute to an imbalanced omega-6 to omega-3 ratio in the diet if consumed excessively.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    // We simply check for the presence of bad oils.
                    // The separate GoodOilsRule provides the counter-balance with positive points.
                    for (String badOil : BAD_OILS) {
                        if (lowerCaseIngredient.contains(badOil)) {
                            results.add(new AnalysisResult("Contains " + badOil, AnalysisResult.WarningLevel.SEVERE, 50, badOil, EXPLANATION));
                        }
                    }
                }
            }
        }
        return results;
    }
}
