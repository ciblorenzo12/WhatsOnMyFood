package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class MilkRule implements ProductAnalysisRule {

    private static final String CONVENTIONAL_EXPLANATION = "Contains milk, but the product does not show an organic or Non-GMO dairy claim. Conventional dairy is a watch item because the app cannot confirm cleaner dairy sourcing from this label.";
    private static final String CLAIMED_EXPLANATION = "Contains milk. This is mainly an allergen and dietary-preference note. The product also shows organic or Non-GMO language, so milk is not treated as the main health concern.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) {
            return results;
        }

        boolean hasCleanerDairyClaim = hasOrganicOrNonGmoClaim(productWithDetails);
        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            if (ingredient.text.toLowerCase().contains("milk")) {
                if (hasCleanerDairyClaim || ingredient.text.toLowerCase().contains("organic")) {
                    results.add(new AnalysisResult("Contains milk", AnalysisResult.WarningLevel.INFO, 0, ingredient.text, CLAIMED_EXPLANATION));
                } else {
                    results.add(new AnalysisResult("Conventional milk without organic/Non-GMO claim", AnalysisResult.WarningLevel.SEVERE, 25, ingredient.text, CONVENTIONAL_EXPLANATION));
                }
                break;
            }
        }

        return results;
    }

    private boolean hasOrganicOrNonGmoClaim(ProductWithDetails productWithDetails) {
        if (productWithDetails == null || productWithDetails.product == null) {
            return false;
        }
        String labels = productWithDetails.product.labels == null ? "" : productWithDetails.product.labels.toLowerCase();
        return labels.contains("organic")
                || labels.contains("non gmo")
                || labels.contains("non-gmo")
                || labels.contains("nongmo")
                || labels.contains("no gmos")
                || labels.contains("gmo free");
    }
}
