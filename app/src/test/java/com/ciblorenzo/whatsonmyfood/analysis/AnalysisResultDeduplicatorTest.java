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
}
