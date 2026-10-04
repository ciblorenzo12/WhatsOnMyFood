package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class NonOrganicWheatRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "⚠️ Contains conventional (non-organic) wheat. Conventional wheat is often grown using pesticides like glyphosate. Choosing organic wheat is a way to reduce exposure to these synthetic chemicals.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        // Product labels and ingredient names are separate provider fields. A certified
        // product may list simply "whole grain wheat" without repeating "organic".
        if (productWithDetails != null && productWithDetails.product != null
                && OrganicClaim.isOrganic(productWithDetails.product.labels)) {
            return results;
        }
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient != null && ingredient.text != null) {
                    if (OrganicClaim.isWheat(ingredient.text) && !OrganicClaim.isOrganic(ingredient.text)) {
                        results.add(new AnalysisResult("Contains non-organic wheat", AnalysisResult.WarningLevel.WARNING, 20, ingredient.text, EXPLANATION));
                        break; // Found it, no need to check further
                    }
                }
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Conventional wheat: subtracts 20 points when wheat is listed without an organic claim on either the ingredient or the product label.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.INGREDIENT_SOURCING;
    }
}
