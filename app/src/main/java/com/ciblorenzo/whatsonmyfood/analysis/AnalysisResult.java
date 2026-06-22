package com.ciblorenzo.whatsonmyfood.analysis;

import androidx.annotation.Nullable;

public class AnalysisResult {

    public enum WarningLevel {
        POSITIVE, INFO, WARNING, SEVERE
    }

    private final String message;
    private final WarningLevel level;
    private final int scorePenalty;
    private final String triggeringIngredient;
    private final String explanation;
    private String sourceUrl;
    private String visualQuote;

    public AnalysisResult(String message, WarningLevel level, int scorePenalty, @Nullable String triggeringIngredient, String explanation) {
        this.message = message;
        this.level = level;
        this.scorePenalty = scorePenalty;
        this.triggeringIngredient = triggeringIngredient;
        this.explanation = explanation;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setVisualQuote(String visualQuote) {
        this.visualQuote = visualQuote;
    }

    public String getVisualQuote() {
        return visualQuote;
    }

    public String getMessage() {
        return message;
    }

    public WarningLevel getLevel() {
        return level;
    }

    public int getScorePenalty() {
        return scorePenalty;
    }

    @Nullable
    public String getTriggeringIngredient() {
        return triggeringIngredient;
    }
    
    public String getExplanation() {
        return explanation;
    }
}
