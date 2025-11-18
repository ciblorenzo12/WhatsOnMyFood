package com.example.myapplication.analysis.rules;

import androidx.annotation.Nullable;

public class AnalysisResult {

    public enum WarningLevel {
        INFO, WARNING, SEVERE
    }

    private final String message;
    private final WarningLevel level;
    private final int scorePenalty;
    private final String triggeringIngredient;
    private final String explanation;

    public AnalysisResult(String message, WarningLevel level, int scorePenalty, @Nullable String triggeringIngredient, String explanation) {
        this.message = message;
        this.level = level;
        this.scorePenalty = scorePenalty;
        this.triggeringIngredient = triggeringIngredient;
        this.explanation = explanation;
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
