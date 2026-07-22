package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddedSugarRule implements ProductAnalysisRule {

    static final double FDA_ADDED_SUGAR_DAILY_VALUE_G = 50.0;
    private static final String UNDER_DAILY_VALUE_EXPLANATION = "Added sugar is present (%.1f g), but it is under the FDA Daily Value of %.0f g for a 2,000 calorie diet. Organic sugar is still sugar, but this amount is within the suggested daily limit.";
    private static final String OVER_DAILY_VALUE_EXPLANATION = "Added sugar is very high (%.1f g), which is above the FDA Daily Value of %.0f g for a 2,000 calorie diet. Organic sugar still counts toward the same daily added sugar limit.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null
                || productWithDetails.nutriments == null
                || productWithDetails.nutriments.addedSugars == null) {
            addIngredientTextAddedSugarResult(productWithDetails, results);
            return results;
        }

        double addedSugarAmount = productWithDetails.nutriments.addedSugars;
        if (addedSugarAmount <= 0) {
            addIngredientTextAddedSugarResult(productWithDetails, results);
            return results;
        }

        boolean overDailyValue = addedSugarAmount > FDA_ADDED_SUGAR_DAILY_VALUE_G;
        AnalysisResult result = new AnalysisResult(
                overDailyValue ? "Added sugar above daily value" : "Added sugar within daily value",
                overDailyValue ? AnalysisResult.WarningLevel.SEVERE : AnalysisResult.WarningLevel.WARNING,
                overDailyValue ? 50 : 10,
                "sugar",
                String.format(
                        overDailyValue ? OVER_DAILY_VALUE_EXPLANATION : UNDER_DAILY_VALUE_EXPLANATION,
                        addedSugarAmount,
                        FDA_ADDED_SUGAR_DAILY_VALUE_G
                )
        );
        result.setSourceUrl("https://www.fda.gov/food/nutrition-facts-label/added-sugars-nutrition-facts-label");
        results.add(result);
        addIngredientTextAddedSugarResult(productWithDetails, results);
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Added sugar: subtracts 10 points when present, 20 points when it is among the first three ingredients, or 50 points when reported added sugar is above 50 g. Only the largest added-sugar penalty is counted.";
    }

    @Override
    public String getScoringGroup() {
        return "added_sugar";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.SUGAR;
    }

    private void addIngredientTextAddedSugarResult(ProductWithDetails productWithDetails, List<AnalysisResult> results) {
        if (productWithDetails == null || productWithDetails.ingredients == null) return;
        for (int i = 0; i < productWithDetails.ingredients.size(); i++) {
            if (productWithDetails.ingredients.get(i) == null || productWithDetails.ingredients.get(i).text == null) continue;
            String ingredient = productWithDetails.ingredients.get(i).text.trim();
            String normalized = ingredient.toLowerCase(Locale.US);
            if (normalized.contains("added sugar") || normalized.matches(".*\\bsugar\\b.*")) {
                AnalysisResult result = new AnalysisResult(
                        i < 3 ? "Added sugar near top of ingredient list" : "Contains added sugar",
                        i < 3 ? AnalysisResult.WarningLevel.SEVERE : AnalysisResult.WarningLevel.WARNING,
                        i < 3 ? 20 : 10,
                        ingredient,
                        "The ingredient list includes added sugar. Because ingredients are listed by weight, sugar appearing near the top means it is a major part of the product."
                );
                result.setSourceUrl("https://www.fda.gov/food/nutrition-facts-label/added-sugars-nutrition-facts-label");
                results.add(result);
                return;
            }
        }
    }
}
