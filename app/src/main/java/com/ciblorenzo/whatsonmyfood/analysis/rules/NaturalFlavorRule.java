package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class NaturalFlavorRule implements ProductAnalysisRule {

    private static final String NATURAL_FLAVOR_SINGULAR = "natural flavor";
    private static final String NATURAL_FLAVOR_PLURAL = "natural flavors";
    private static final String EXPLANATION = "Contains natural flavors. This is a broad label term, so it is worth noting, but it is not automatically a high-concern ingredient by itself. Products with clearly listed flavors such as vanilla extract or lemon juice are more transparent.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) {
            return results;
        }

        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            String lowerCaseIngredient = ingredient.text.toLowerCase();
            if (lowerCaseIngredient.contains(NATURAL_FLAVOR_PLURAL)) {
                results.add(new AnalysisResult("Contains Natural Flavors", AnalysisResult.WarningLevel.WARNING, 5, NATURAL_FLAVOR_PLURAL, EXPLANATION));
                break;
            } else if (lowerCaseIngredient.contains(NATURAL_FLAVOR_SINGULAR)) {
                results.add(new AnalysisResult("Contains Natural Flavor", AnalysisResult.WarningLevel.WARNING, 5, NATURAL_FLAVOR_SINGULAR, EXPLANATION));
                break;
            }
        }

        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Natural flavor: subtracts 5 points when the ingredient list uses the broad term natural flavor or natural flavors. Repeated mentions count only once.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.FLAVORS;
    }
}
