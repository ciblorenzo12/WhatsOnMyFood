package com.example.myapplication.analysis;

import com.example.myapplication.analysis.rules.AnalysisResult;

import java.util.List;

public class ProductAnalysisReport {

    private final int overallScore;
    private final List<AnalysisResult> results;

    public ProductAnalysisReport(int overallScore, List<AnalysisResult> results) {
        this.overallScore = overallScore;
        this.results = results;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public List<AnalysisResult> getResults() {
        return results;
    }
}
