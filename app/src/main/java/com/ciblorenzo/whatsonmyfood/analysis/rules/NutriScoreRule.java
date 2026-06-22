package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NutriScoreRule implements ProductAnalysisRule {
    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null
                || productWithDetails.product == null
                || productWithDetails.product.nutriscoreGrade == null) {
            return results;
        }

        String grade = productWithDetails.product.nutriscoreGrade.trim().toLowerCase(Locale.US);
        if (!grade.equals("d") && !grade.equals("e")) return results;

        AnalysisResult result = new AnalysisResult(
                "Low Nutri-Score grade",
                grade.equals("e") ? AnalysisResult.WarningLevel.SEVERE : AnalysisResult.WarningLevel.WARNING,
                grade.equals("e") ? 25 : 15,
                "Nutri-Score " + grade.toUpperCase(Locale.US),
                "Open Food Facts reports a low Nutri-Score grade for this product. Nutri-Score summarizes nutrition quality using nutrients to limit, such as sugar, saturated fat, salt, and calories, balanced against positive components."
        );
        if (productWithDetails.product.barcode != null && !productWithDetails.product.barcode.trim().isEmpty()) {
            result.setSourceUrl("https://world.openfoodfacts.org/product/" + productWithDetails.product.barcode);
            result.setVisualQuote("Open Food Facts API field: nutriscore_grade = " + grade);
        } else {
            result.setSourceUrl("https://world.openfoodfacts.org/nutriscore");
        }
        results.add(result);
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Flags Nutri-Score D/E from Open Food Facts as a nutrition quality concern.";
    }
}
