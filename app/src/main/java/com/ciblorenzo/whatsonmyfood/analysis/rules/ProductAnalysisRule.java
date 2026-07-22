package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.List;

public interface ProductAnalysisRule {
    enum RuleCategory {
        SUGAR,
        SODIUM,
        OILS,
        ADDITIVES_AND_PRESERVATIVES,
        FLAVORS,
        PROCESSING_LEVEL,
        POSITIVE_INGREDIENT_SIGNALS,
        GENERAL_NUTRITION,
        INGREDIENT_SOURCING,
        ALLERGENS
    }

    List<AnalysisResult> evaluate(ProductWithDetails productWithDetails);

    String getRuleDescription();

    RuleCategory getRuleCategory();

    default String getScoringGroup() {
        return getClass().getSimpleName();
    }
}
