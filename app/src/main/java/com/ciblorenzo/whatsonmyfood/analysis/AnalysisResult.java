package com.ciblorenzo.whatsonmyfood.analysis;

import androidx.annotation.Nullable;

public class AnalysisResult {

    public enum WarningLevel {
        POSITIVE, INFO, WARNING, SEVERE
    }

    public enum RuleEffect {
        NEGATIVE, INFORMATIONAL, POSITIVE
    }

    private final String message;
    private final WarningLevel level;
    private final int scorePenalty;
    private final String triggeringIngredient;
    private final String explanation;
    private String scoringGroup;
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

    /**
     * Returns the signed adjustment used by the score calculation.
     * Negative values lower the score and positive values offset penalties.
     */
    public int getScoreAdjustment() {
        return -scorePenalty;
    }

    /** The effect category is derived from the actual score adjustment, never from display text. */
    public RuleEffect getRuleEffect() {
        if (scorePenalty > 0) return RuleEffect.NEGATIVE;
        if (scorePenalty < 0) return RuleEffect.POSITIVE;
        return RuleEffect.INFORMATIONAL;
    }

    public String getScoreImpactDescription() {
        if (scorePenalty > 0) {
            return "Subtracts " + scorePenalty + (scorePenalty == 1 ? " point." : " points.");
        }
        if (scorePenalty < 0) {
            int adjustment = Math.abs(scorePenalty);
            return "Restores up to " + adjustment + (adjustment == 1 ? " point" : " points")
                    + " after penalties; the final score cannot exceed 100.";
        }
        return "Does not change the score.";
    }

    public String getScoringExplanation() {
        if (explanation == null || explanation.trim().isEmpty()) {
            return getScoreImpactDescription();
        }
        return getScoreImpactDescription() + " " + explanation.trim();
    }

    public void setScoringGroup(String scoringGroup) {
        this.scoringGroup = scoringGroup;
    }

    public String getScoringGroup() {
        return scoringGroup;
    }

    @Nullable
    public String getTriggeringIngredient() {
        return triggeringIngredient;
    }
    
    public String getExplanation() {
        return explanation;
    }
}
