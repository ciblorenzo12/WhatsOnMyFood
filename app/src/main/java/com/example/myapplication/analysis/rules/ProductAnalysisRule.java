package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;

import java.util.List;

public interface ProductAnalysisRule {
    List<AnalysisResult> evaluate(ProductWithDetails productWithDetails);
}
