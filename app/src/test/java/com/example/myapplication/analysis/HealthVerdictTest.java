package com.example.myapplication.analysis;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class HealthVerdictTest {

    @Test
    public void fromResults_withSevereSignal_returnsNotHealthy() {
        HealthVerdict verdict = HealthVerdict.fromResults(
                Collections.singletonList(new AnalysisResult("Artificial color", AnalysisResult.WarningLevel.SEVERE, 20, "Red 40", "Concern")),
                4
        );

        assertEquals(HealthVerdict.Status.NOT_HEALTHY, verdict.getStatus());
    }

    @Test
    public void fromResults_withOnlyPositiveAndInfo_returnsHealthy() {
        HealthVerdict verdict = HealthVerdict.fromResults(
                Arrays.asList(
                        new AnalysisResult("Short label", AnalysisResult.WarningLevel.POSITIVE, -10, null, "Good"),
                        new AnalysisResult("Contains milk", AnalysisResult.WarningLevel.INFO, 0, "milk", "Allergen note")
                ),
                3
        );

        assertEquals(HealthVerdict.Status.HEALTHY, verdict.getStatus());
    }

    @Test
    public void fromResults_withoutIngredients_returnsReview() {
        HealthVerdict verdict = HealthVerdict.fromResults(Collections.emptyList(), 0);

        assertEquals(HealthVerdict.Status.REVIEW, verdict.getStatus());
    }

    @Test
    public void fromAiVerdict_withHealthyVerdict_returnsHealthyLabel() {
        HealthVerdict verdict = HealthVerdict.fromAiVerdict(
                "HEALTHY",
                "No high-concern ingredients found.",
                Collections.singletonList(new AnalysisResult("Clean ingredient list", AnalysisResult.WarningLevel.POSITIVE, 0, null, "Good")),
                3
        );

        assertEquals(HealthVerdict.Status.HEALTHY, verdict.getStatus());
        assertEquals("Healthy", verdict.getLabel());
    }

    @Test
    public void fromAiVerdict_withSevereFallback_keepsNotHealthy() {
        HealthVerdict verdict = HealthVerdict.fromAiVerdict(
                "HEALTHY",
                "AI missed the rule finding.",
                Collections.singletonList(new AnalysisResult("Artificial color", AnalysisResult.WarningLevel.SEVERE, 20, "Red 40", "Concern")),
                3
        );

        assertEquals(HealthVerdict.Status.NOT_HEALTHY, verdict.getStatus());
        assertEquals("Not Healthy", verdict.getLabel());
    }
}
