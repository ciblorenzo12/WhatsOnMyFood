package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SugarAsMainIngredientRule implements ProductAnalysisRule {

    private static final List<String> SUGAR_NAMES = Arrays.asList("sugar", "cane sugar", "cane juice", "honey", "agave", "brown rice syrup", "corn syrup", "maltodextrin", "sucrose", "fructose");
    private static final String EXPLANATION = "❌ Sugar is one of the main ingredients (top three on the label). That means the product is more of a sweet treat than a nourishing food. Having these occasionally is fine, but they shouldn’t be everyday staples.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            Double addedSugars = productWithDetails.nutriments == null ? null : productWithDetails.nutriments.addedSugars;
            if (addedSugars != null && addedSugars > 0 && addedSugars <= AddedSugarRule.FDA_ADDED_SUGAR_DAILY_VALUE_G) {
                return results;
            }
            for (int i = 0; i < Math.min(3, productWithDetails.ingredients.size()); i++) {
                Ingredient ingredient = productWithDetails.ingredients.get(i);
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase(Locale.US);
                    if (isNutritionClaimNotIngredient(lowerCaseIngredient)) {
                        continue;
                    }
                    for (String sugarName : SUGAR_NAMES) {
                        if (lowerCaseIngredient.contains(sugarName)) {
                            results.add(new AnalysisResult("Sugar is a main ingredient", AnalysisResult.WarningLevel.SEVERE, 20, ingredient.text, EXPLANATION));
                            return results;
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

    private boolean isNutritionClaimNotIngredient(String normalizedIngredient) {
        return normalizedIngredient.matches("^(no|zero)\\s+(added\\s+)?(sugar|calories)\\b.*")
                || normalizedIngredient.matches("^sugar\\s*free\\b.*")
                || normalizedIngredient.matches("^\\d+\\s*(g|mg)?\\s*(added\\s+)?sugar\\b.*");
    }
}
