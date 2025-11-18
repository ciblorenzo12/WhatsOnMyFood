package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.List;

public class UltraProcessedFoodRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "❌ This is an ultra-processed food (NOVA 4). These are industrial formulations with five or more ingredients. They typically contain substances not found in home cooking, such as additives to imitate sensory qualities of fresh foods. High consumption is linked to poorer health outcomes.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.product != null && "4".equals(productWithDetails.product.novaGroup)) {
            results.add(new AnalysisResult("Ultra-processed food (NOVA 4)", AnalysisResult.WarningLevel.SEVERE, 25, null, EXPLANATION));
        }
        return results;
    }
}
