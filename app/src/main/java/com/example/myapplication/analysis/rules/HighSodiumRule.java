package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class HighSodiumRule implements ProductAnalysisRule {

    private static final double SODIUM_THRESHOLD_MG_PER_100G = 600; // NHS high sodium threshold (1.5g salt ≈ 600mg sodium)
    private static final String EXPLANATION = "❌ High in sodium. Eating too much sodium (from salt) can raise your blood pressure, which increases your risk of heart disease and stroke.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null && productWithDetails.nutriments.sodium != null) {
            // The API provides sodium in g, so we convert to mg for comparison
            double sodiumInMg = productWithDetails.nutriments.sodium * 1000;
            if (sodiumInMg > SODIUM_THRESHOLD_MG_PER_100G) {
                results.add(new AnalysisResult("High sodium content", AnalysisResult.WarningLevel.WARNING, 20, null, EXPLANATION));
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Detects products with high sodium content (above 600mg per 100g). Excessive sodium is linked to high blood pressure and heart disease.";
    }
}
