package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreservativesRule implements ProductAnalysisRule {

    private static final List<String> PRESERVATIVES = Arrays.asList(
            "bha",
            "bht",
            "tbhq",
            "sodium benzoate",
            "potassium benzoate",
            "potassium sorbate",
            "calcium propionate",
            "sodium metabisulfite",
            "calcium disodium edta"
    );

    private static final String EXPLANATION = "Contains shelf-life preservatives. These are common in packaged foods, but their presence usually points to a more processed product.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) {
            return results;
        }

        List<String> foundPreservatives = new ArrayList<>();
        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            String lowerCaseIngredient = ingredient.text.toLowerCase();
            for (String preservative : PRESERVATIVES) {
                if (lowerCaseIngredient.contains(preservative) && !foundPreservatives.contains(preservative)) {
                    foundPreservatives.add(preservative);
                }
            }
        }

        if (!foundPreservatives.isEmpty()) {
            results.add(new AnalysisResult(
                    "Contains preservatives",
                    AnalysisResult.WarningLevel.WARNING,
                    10,
                    String.join(", ", foundPreservatives),
                    EXPLANATION
            ));
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Detects common preservatives such as BHA, BHT, TBHQ, benzoates, sorbates, propionates, sulfites, and EDTA.";
    }
}
