package com.example.myapplication;

import com.example.myapplication.analysis.rules.AnalysisResult;

import java.util.List;

public interface ProductAnalysisRule {
    List<AnalysisResult> evaluate(ProductWithDetails productWithDetails);
}
