package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArtificialFlavorRule implements ProductAnalysisRule {
    private static final String EXPLANATION = "The label lists an artificial flavor. Artificial flavors are allowed food additives, but they are a processing marker and do not add nutritional value. They should not be described as absent when they appear on the label.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) return results;

        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            String text = ingredient.text.trim();
            String normalized = text.toLowerCase(Locale.US);
            if (normalized.contains("artificial") && (normalized.contains("flavor") || normalized.contains("flavour"))) {
                AnalysisResult result = new AnalysisResult(
                        "Contains artificial flavor",
                        AnalysisResult.WarningLevel.WARNING,
                        10,
                        text,
                        EXPLANATION
                );
                result.setSourceUrl("https://www.fda.gov/food/food-additives-petitions/food-additive-status-list");
                results.add(result);
                return results;
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Artificial flavor: subtracts 10 points when an ingredient contains artificial plus flavor or flavour. The penalty is applied once.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.FLAVORS;
    }
}
