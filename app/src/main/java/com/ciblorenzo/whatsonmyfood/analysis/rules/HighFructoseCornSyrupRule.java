package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class HighFructoseCornSyrupRule implements ProductAnalysisRule {

    private static final String HFCS = "high fructose corn syrup";
    private static final String EXPLANATION = "❌ Uses high-fructose corn syrup as a main sweetener. Heavy intake of added sugars (especially in drinks and sweets) is associated with weight gain, fatty liver, and higher risk of metabolic disease. Whole-food or minimal-sugar options are a better choice.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (int i = 0; i < productWithDetails.ingredients.size(); i++) {
                Ingredient ingredient = productWithDetails.ingredients.get(i);
                if (ingredient.text != null && ingredient.text.toLowerCase().contains(HFCS)) {
                    if (i < 3) { // Check if it's in the top 3 ingredients
                        results.add(new AnalysisResult("Contains high-fructose corn syrup", AnalysisResult.WarningLevel.SEVERE, 25, HFCS, EXPLANATION));
                    } else {
                        results.add(new AnalysisResult("Contains high-fructose corn syrup", AnalysisResult.WarningLevel.WARNING, 15, HFCS, EXPLANATION));
                    }
                    break;
                }
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "High-fructose corn syrup: subtracts 25 points when it is among the first three ingredients, or 15 points when it appears later. It shares the added-sugar group, so only the largest sugar penalty counts.";
    }

    @Override
    public String getScoringGroup() {
        return "added_sugar";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.SUGAR;
    }
}
