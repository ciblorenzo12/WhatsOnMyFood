package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JunkOilsRule implements ProductAnalysisRule {

    private static final List<String> JUNK_OILS = Arrays.asList("vegetable oil", "corn oil", "soybean oil", "palm oil", "palm kernel oil", "shortening");
    private static final String EXPLANATION = "⚠️ Made with refined industrial oils (corn/soy/palm) in a fried snack. These ultra-processed snacks tend to be high in calories and low in nutrients. Frequent intake is associated with poorer diet quality overall. Treat as an occasional snack, not a daily food.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            boolean isFriedSnack = productWithDetails.product.categories != null && (productWithDetails.product.categories.toLowerCase().contains("chips") || productWithDetails.product.categories.toLowerCase().contains("fries"));
            if (isFriedSnack) {
                for (Ingredient ingredient : productWithDetails.ingredients) {
                    if (ingredient.text != null) {
                        String lowerCaseIngredient = ingredient.text.toLowerCase();
                        for (String oil : JUNK_OILS) {
                            if (lowerCaseIngredient.contains(oil)) {
                                results.add(new AnalysisResult("Fried with industrial oils", AnalysisResult.WarningLevel.WARNING, 15, oil, EXPLANATION));
                                return results;
                            }
                        }
                    }
                }
            }
        }
        return results;
    }
}
