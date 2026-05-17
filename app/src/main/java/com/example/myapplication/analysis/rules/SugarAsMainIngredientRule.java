package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SugarAsMainIngredientRule implements ProductAnalysisRule {

    private static final List<String> SUGAR_NAMES = Arrays.asList("sugar", "cane sugar", "cane juice", "honey", "agave", "brown rice syrup", "corn syrup", "maltodextrin", "sucrose", "fructose");
    private static final String EXPLANATION = "❌ Sugar is one of the main ingredients (top three on the label). That means the product is more of a sweet treat than a nourishing food. Having these occasionally is fine, but they shouldn’t be everyday staples.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (int i = 0; i < Math.min(3, productWithDetails.ingredients.size()); i++) {
                Ingredient ingredient = productWithDetails.ingredients.get(i);
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String sugarName : SUGAR_NAMES) {
                        if (lowerCaseIngredient.contains(sugarName)) {
                            // CORRECTED: Use the actual ingredient text for highlighting
                            results.add(new AnalysisResult("Sugar is a main ingredient", AnalysisResult.WarningLevel.SEVERE, 20, ingredient.text, EXPLANATION));
                            return results; // Stop after finding the first one
                        }
                    }
                }
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Detects if sugar or sweeteners are among the top three ingredients. High sugar content at the top of the list indicates a low-nutrient product.";
    }
}
