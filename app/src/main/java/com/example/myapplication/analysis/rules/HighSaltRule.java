package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.List;

public class HighSaltRule implements ProductAnalysisRule {

    private static final double SALT_THRESHOLD_G_PER_100G = 1.5; // NHS high salt threshold
    private static final String EXPLANATION = "❌ High in salt. Eating too much salt can raise your blood pressure, which increases your risk of heart disease and stroke.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.nutriments != null) {
            if (productWithDetails.nutriments.salt > SALT_THRESHOLD_G_PER_100G) {
                results.add(new AnalysisResult("High salt content", AnalysisResult.WarningLevel.WARNING, 20, null, EXPLANATION));
            }
        }
        return results;
    }
}
