package com.ciblorenzo.whatsonmyfood.analysis;

import java.util.List;

public class ProductAnalysisReport {

    private final int overallScore;
    private final List<AnalysisResult> results;
    private List<String> aiTriggeringIngredients;

    public ProductAnalysisReport(int overallScore, List<AnalysisResult> results) {
        this.overallScore = overallScore;
        this.results = results;
    }

    public void setAiTriggeringIngredients(List<String> aiTriggeringIngredients) {
        this.aiTriggeringIngredients = aiTriggeringIngredients;
    }

    public List<String> getAiTriggeringIngredients() {
        return aiTriggeringIngredients;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public List<AnalysisResult> getResults() {
        return results;
    }
}
