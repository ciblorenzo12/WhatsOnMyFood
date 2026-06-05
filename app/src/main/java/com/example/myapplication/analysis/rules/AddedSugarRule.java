package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class AddedSugarRule implements ProductAnalysisRule {

    static final double FDA_ADDED_SUGAR_DAILY_VALUE_G = 50.0;
    private static final String UNDER_DAILY_VALUE_EXPLANATION = "Added sugar is present (%.1f g), but it is under the FDA Daily Value of %.0f g for a 2,000 calorie diet. Organic sugar is still sugar, but this amount is within the suggested daily limit.";
    private static final String OVER_DAILY_VALUE_EXPLANATION = "Added sugar is very high (%.1f g), which is above the FDA Daily Value of %.0f g for a 2,000 calorie diet. Organic sugar still counts toward the same daily added sugar limit.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null
                || productWithDetails.nutriments == null
                || productWithDetails.nutriments.addedSugars == null) {
            return results;
        }

        double addedSugarAmount = productWithDetails.nutriments.addedSugars;
        if (addedSugarAmount <= 0) {
            return results;
        }

        boolean overDailyValue = addedSugarAmount > FDA_ADDED_SUGAR_DAILY_VALUE_G;
        results.add(new AnalysisResult(
                overDailyValue ? "Added sugar above daily value" : "Added sugar within daily value",
                overDailyValue ? AnalysisResult.WarningLevel.SEVERE : AnalysisResult.WarningLevel.POSITIVE,
                overDailyValue ? 50 : 0,
                "sugar",
                String.format(
                        overDailyValue ? OVER_DAILY_VALUE_EXPLANATION : UNDER_DAILY_VALUE_EXPLANATION,
                        addedSugarAmount,
                        FDA_ADDED_SUGAR_DAILY_VALUE_G
                )
        ));
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Compares added sugar to the FDA Daily Value of 50g. Amounts over the daily value are severe; lower amounts are highlighted as within the suggested limit.";
    }
}
