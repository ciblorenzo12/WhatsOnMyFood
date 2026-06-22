package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.List;

public interface ProductAnalysisRule {
    List<AnalysisResult> evaluate(ProductWithDetails productWithDetails);
    default String getRuleDescription() {
        return "This rule checks for " + this.getClass().getSimpleName().replace("Rule", "") + ".";
    }
}
