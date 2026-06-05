package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BadVegetableOilsRule implements ProductAnalysisRule {

    private static final List<String> REFINED_OILS = Arrays.asList(
            "vegetable oil", "sunflower oil", "canola oil", "soybean oil", "palm oil", "corn oil", "palm kernel oil", "shortening"
    );

    private static final String EXPLANATION = "Contains a refined seed/vegetable oil. This is a moderate watch item in packaged snacks, especially when paired with refined flour or high sodium. It is not the same thing as partially hydrogenated oil unless the label explicitly says hydrogenated or partially hydrogenated.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) {
            return results;
        }

        List<String> foundOils = new ArrayList<>();
        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            String lowerCaseIngredient = ingredient.text.toLowerCase();
            for (String oil : REFINED_OILS) {
                if (lowerCaseIngredient.contains(oil) && !foundOils.contains(oil)) {
                    foundOils.add(oil);
                }
            }
        }

        if (!foundOils.isEmpty()) {
            AnalysisResult result = new AnalysisResult("Contains refined oil", AnalysisResult.WarningLevel.WARNING, 10, String.join(", ", foundOils), EXPLANATION);
            result.setSourceUrl("https://www.fda.gov/food/food-additives-petitions/trans-fat");
            results.add(result);
        }

        return results;
    }
}
