package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.List;

public class HighSodiumRule implements ProductAnalysisRule {

    private static final double SODIUM_THRESHOLD_MG_PER_100G = 600;
    private static final String EXPLANATION = "❌ Very high in sodium: about %.0f mg per 100g. High salt intake is linked with higher blood pressure and cardiovascular risk. If you eat several salty foods like this each day, it can add up quickly.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null) {
            // The salt value in openfoodfacts is in grams, so we convert to mg
            double sodiumInMg = productWithDetails.nutriments.salt * 1000 / 2.5; // Approximation from salt to sodium
            if (sodiumInMg > SODIUM_THRESHOLD_MG_PER_100G) {
                results.add(new AnalysisResult("High sodium content", AnalysisResult.WarningLevel.SEVERE, 20, null, String.format(EXPLANATION, sodiumInMg)));
            }
        }
        return results;
    }
}
