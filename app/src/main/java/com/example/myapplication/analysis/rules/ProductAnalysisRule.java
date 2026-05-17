package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.List;

public interface ProductAnalysisRule {
    List<AnalysisResult> evaluate(ProductWithDetails productWithDetails);
    default String getRuleDescription() {
        return "This rule checks for " + this.getClass().getSimpleName().replace("Rule", "") + ".";
    }
}
