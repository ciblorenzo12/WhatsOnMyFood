package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AnalysisResultDeduplicatorTest {

    @Test
    public void deduplicate_collapsesRepeatedOrganicFindings() {
        List<AnalysisResult> results = AnalysisResultDeduplicator.deduplicate(Arrays.asList(
                new AnalysisResult("Contains organic ingredients", AnalysisResult.WarningLevel.POSITIVE, -5, "organic", "Good"),
                new AnalysisResult("Contains organic ingredients", AnalysisResult.WarningLevel.POSITIVE, -5, "organic", "Good"),
                new AnalysisResult("Contains organic ingredients", AnalysisResult.WarningLevel.POSITIVE, -5, "organic", "Good")
        ));

        assertEquals(1, results.size());
        assertEquals("Contains organic ingredients", results.get(0).getMessage());
    }

    @Test
    public void deduplicate_keepsAddedSugarSevere() {
        List<AnalysisResult> results = AnalysisResultDeduplicator.deduplicate(Arrays.asList(
                new AnalysisResult("Added sugar", AnalysisResult.WarningLevel.WARNING, 0, "sugar", "AI caution"),
                new AnalysisResult("Contains Added Sugar", AnalysisResult.WarningLevel.SEVERE, 50, "sugar", "Rule caution")
        ));

        assertEquals(1, results.size());
        assertEquals(AnalysisResult.WarningLevel.SEVERE, results.get(0).getLevel());
    }

    @Test
    public void deduplicate_preservesAddedSugarWarningLevelWhenNoStrongerRuleExists() {
        List<AnalysisResult> results = AnalysisResultDeduplicator.deduplicate(Arrays.asList(
                new AnalysisResult("Added sugar", AnalysisResult.WarningLevel.WARNING, 0, "sugar", "AI caution")
        ));

        assertEquals(1, results.size());
        assertEquals(AnalysisResult.WarningLevel.WARNING, results.get(0).getLevel());
    }

    @Test
    public void deduplicate_explicitScoringGroupKeepsLargestPenalty() {
        AnalysisResult smallerSevere = new AnalysisResult(
                "Sugar near top", AnalysisResult.WarningLevel.SEVERE, 20, "sugar", "Near top"
        );
        smallerSevere.setScoringGroup("added_sugar");
        AnalysisResult largerWarning = new AnalysisResult(
                "Added sugar amount", AnalysisResult.WarningLevel.WARNING, 50, "sugar", "Above threshold"
        );
        largerWarning.setScoringGroup("added_sugar");

        List<AnalysisResult> results = AnalysisResultDeduplicator.deduplicate(Arrays.asList(smallerSevere, largerWarning));

        assertEquals(1, results.size());
        assertEquals(50, results.get(0).getScorePenalty());
    }

    @Test
    public void scoringExplanationAlwaysUsesActualAdjustment() {
        AnalysisResult penalty = new AnalysisResult("Concern", AnalysisResult.WarningLevel.WARNING, 15, null, "Reason");
        AnalysisResult positive = new AnalysisResult("Positive", AnalysisResult.WarningLevel.POSITIVE, -10, null, "Reason");
        AnalysisResult information = new AnalysisResult("Info", AnalysisResult.WarningLevel.INFO, 0, null, "Reason");

        assertEquals("Subtracts 15 points. Reason", penalty.getScoringExplanation());
        assertEquals("Restores up to 10 points after penalties; the final score cannot exceed 100. Reason", positive.getScoringExplanation());
        assertEquals("Does not change the score. Reason", information.getScoringExplanation());
    }

    @Test
    public void ruleEffectIsAlwaysDerivedFromActualScoreAdjustment() {
        AnalysisResult negative = new AnalysisResult("Negative", AnalysisResult.WarningLevel.INFO, 5, null, "Reason");
        AnalysisResult informational = new AnalysisResult("Info", AnalysisResult.WarningLevel.SEVERE, 0, null, "Reason");
        AnalysisResult positive = new AnalysisResult("Positive", AnalysisResult.WarningLevel.INFO, -5, null, "Reason");

        assertEquals(AnalysisResult.RuleEffect.NEGATIVE, negative.getRuleEffect());
        assertEquals(AnalysisResult.RuleEffect.INFORMATIONAL, informational.getRuleEffect());
        assertEquals(AnalysisResult.RuleEffect.POSITIVE, positive.getRuleEffect());
    }
}
