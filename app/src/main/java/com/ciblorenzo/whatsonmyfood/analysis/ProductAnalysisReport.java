package com.ciblorenzo.whatsonmyfood.analysis;

import java.util.List;

public class ProductAnalysisReport {

    private final int startingScore;
    private final int rawScore;
    private final int overallScore;
    private final List<AnalysisResult> results;
    private List<String> aiTriggeringIngredients;

    public ProductAnalysisReport(int overallScore, List<AnalysisResult> results) {
        this(100, overallScore, overallScore, results);
    }

    public ProductAnalysisReport(int startingScore, int rawScore, int overallScore, List<AnalysisResult> results) {
        this.startingScore = startingScore;
        this.rawScore = rawScore;
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

    public int getStartingScore() {
        return startingScore;
    }

    public int getRawScore() {
        return rawScore;
    }

    public String getScoreExplanation() {
        int penalties = 0;
        int positiveAdjustments = 0;
        if (results != null) {
            for (AnalysisResult result : results) {
                if (result == null) continue;
                int adjustment = result.getScoreAdjustment();
                if (adjustment < 0) penalties += Math.abs(adjustment);
                if (adjustment > 0) positiveAdjustments += adjustment;
            }
        }

        StringBuilder explanation = new StringBuilder("Starts at ")
                .append(startingScore)
                .append(", subtracts ").append(penalties).append(" penalty points")
                .append(", and restores ").append(positiveAdjustments).append(" positive-adjustment points")
                .append(". Raw score: ").append(rawScore).append(".");
        if (rawScore != overallScore) {
            explanation.append(" The final score is limited to 0-100, so it is ")
                    .append(overallScore).append(".");
        } else {
            explanation.append(" Final score: ").append(overallScore).append(".");
        }
        return explanation.toString();
    }
}
