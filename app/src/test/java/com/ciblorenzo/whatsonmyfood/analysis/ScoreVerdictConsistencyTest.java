package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class ScoreVerdictConsistencyTest {
    @Test public void positivesCannotCancelBlockingFindingIntoPerfectScore() {
        ProductAnalysisReport report = new ProductAnalysisReport(100, 110, 100, Arrays.asList(
                new AnalysisResult("High sugar content", AnalysisResult.WarningLevel.WARNING, 15, "sugar", "High sugar"),
                new AnalysisResult("Whole grains", AnalysisResult.WarningLevel.POSITIVE, -25, "grains", "Whole grains")));
        assertEquals(69, report.getOverallScore());
        assertEquals(110, report.getRawScore());
        assertEquals(HealthVerdict.Status.NOT_HEALTHY, HealthVerdict.fromReport(report, 5).getStatus());
        assertTrue(report.getScoreExplanation().contains("Blocking rule findings"));
    }
    @Test public void lowNumericScoreCannotHaveHealthyVerdict() {
        assertEquals(HealthVerdict.Status.NOT_HEALTHY, HealthVerdict.fromReport(
                new ProductAnalysisReport(40, Collections.emptyList()), 3).getStatus());
    }
    @Test public void missingAnalysisNeedsReviewAndCleanAnalysisStaysHealthy() {
        assertEquals(HealthVerdict.Status.REVIEW, HealthVerdict.fromReport(null, 3).getStatus());
        ProductAnalysisReport clean = new ProductAnalysisReport(100, Collections.emptyList());
        assertEquals(HealthVerdict.Status.HEALTHY, HealthVerdict.fromReport(clean, 3).getStatus());
        assertEquals(HealthVerdict.Status.REVIEW, HealthVerdict.fromReport(clean, 0).getStatus());
    }
}
