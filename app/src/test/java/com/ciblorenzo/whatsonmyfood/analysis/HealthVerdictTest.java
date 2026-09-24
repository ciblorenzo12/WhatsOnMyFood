package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class HealthVerdictTest {

    @Test
    public void incompleteNutritionCannotTurnNoWarningsIntoHealthy() {
        ProductAnalysisReport report = new ProductAnalysisReport(100, Collections.emptyList());
        HealthVerdict verdict = HealthVerdict.fromReport(report, 1, false);
        assertEquals(HealthVerdict.Status.REVIEW, verdict.getStatus());
    }

    @Test
    public void incompleteNutritionStillKeepsAnActualHighSugarFinding() {
        ProductAnalysisReport report = new ProductAnalysisReport(85, Collections.singletonList(
                new AnalysisResult("High sugar content", AnalysisResult.WarningLevel.WARNING,
                        15, null, "25 g per 100 g")));
        assertEquals(HealthVerdict.Status.NOT_HEALTHY,
                HealthVerdict.fromReport(report, 2, false).getStatus());
    }

    @Test
    public void completeCoreNutritionKeepsExistingVerdictBehavior() {
        ProductAnalysisReport report = new ProductAnalysisReport(100, Collections.emptyList());
        assertEquals(HealthVerdict.Status.HEALTHY,
                HealthVerdict.fromReport(report, 1, true).getStatus());
    }

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
    public void fromResults_withHighSugarAndPositiveFinding_returnsNotHealthy() {
        HealthVerdict verdict = HealthVerdict.fromResults(
                Arrays.asList(
                        new AnalysisResult("High sugar content", AnalysisResult.WarningLevel.WARNING, 15, "total sugar", "25 g per 100 g"),
                        new AnalysisResult("Short ingredient list", AnalysisResult.WarningLevel.POSITIVE, -10, null, "Short label")
                ),
                3
        );

        assertEquals(HealthVerdict.Status.NOT_HEALTHY, verdict.getStatus());
        assertEquals("Not Healthy", verdict.getLabel());
    }

    @Test
    public void fromAiVerdict_cannotOverrideHighSugarWithHealthy() {
        HealthVerdict verdict = HealthVerdict.fromAiVerdict(
                "HEALTHY",
                "The ingredient list is short.",
                Arrays.asList(
                        new AnalysisResult("High sugar content", AnalysisResult.WarningLevel.WARNING, 15, "total sugar", "25 g per 100 g"),
                        new AnalysisResult("Short ingredient list", AnalysisResult.WarningLevel.POSITIVE, -10, null, "Short label")
                ),
                3
        );

        assertEquals(HealthVerdict.Status.NOT_HEALTHY, verdict.getStatus());
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

    @Test
    public void fromAiVerdict_withNaturalFlavorWarning_rejectsHealthyOverride() {
        HealthVerdict verdict = HealthVerdict.fromAiVerdict(
                "HEALTHY",
                "The base ingredient is organic yogurt.",
                Arrays.asList(
                        new AnalysisResult("Organic yogurt", AnalysisResult.WarningLevel.POSITIVE, 0, "organic yogurt", "Positive base"),
                        new AnalysisResult("Contains Natural Flavors", AnalysisResult.WarningLevel.WARNING, 5, "natural flavors", "Broad label term")
                ),
                2
        );

        assertEquals(HealthVerdict.Status.NOT_HEALTHY, verdict.getStatus());
        assertEquals("Not Healthy", verdict.getLabel());
    }
}
