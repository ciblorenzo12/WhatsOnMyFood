package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class UltraProcessedFoodRule implements ProductAnalysisRule {

    private static final String EXPLANATION = "Open Food Facts returned nova_group = 4 for this product, which maps to NOVA 4: ultra-processed foods. Open Food Facts uses NOVA to classify products by processing level from 1 to 4; group 4 is the highest processing level.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null
                && productWithDetails.product != null
                && "4".equals(productWithDetails.product.novaGroup)) {
            AnalysisResult result = new AnalysisResult(
                    "Ultra-processed food (NOVA 4)",
                    AnalysisResult.WarningLevel.SEVERE,
                    25,
                    null,
                    EXPLANATION
            );
            result.setVisualQuote("Open Food Facts API field: nova_group = 4");
            if (productWithDetails.product.barcode != null && !productWithDetails.product.barcode.trim().isEmpty()) {
                result.setSourceUrl("https://world.openfoodfacts.org/product/" + productWithDetails.product.barcode);
            } else {
                result.setSourceUrl("https://wiki.openfoodfacts.org/Ultra-processed_foods_NOVA");
            }
            results.add(result);
        }
        return results;
    }
}
