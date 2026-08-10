package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProductFindingsDisplayTest {

    @Test
    public void fromReport_keepsWarningInformationalAndPositiveFindings() {
        ProductFindingsDisplay display = ProductFindingsDisplay.fromReport(
                report(
                        finding("Added sugar", AnalysisResult.WarningLevel.WARNING, 8, "sugar"),
                        finding("Allergen information", AnalysisResult.WarningLevel.INFO, 0, "oats"),
                        finding("Whole grain oats", AnalysisResult.WarningLevel.POSITIVE, -5, "oats")
                ),
                true
        );

        assertEquals(ProductFindingsDisplay.State.CONTENT, display.getState());
        assertEquals(3, display.getFindings().size());
        assertTrue(display.hasFindings());
    }

    @Test
    public void combine_removesOverlappingRuleAndAiCards() {
        ProductFindingsDisplay display = ProductFindingsDisplay.combine(
                report(finding("Contains Added Sugar", AnalysisResult.WarningLevel.SEVERE, 30, "sugar")),
                Arrays.asList(
                        finding("Added sugar", AnalysisResult.WarningLevel.WARNING, 0, "sugar"),
                        finding("Whole grain oats", AnalysisResult.WarningLevel.POSITIVE, -5, "oats")
                ),
                true
        );

        assertEquals(2, display.getFindings().size());
        assertEquals(AnalysisResult.WarningLevel.SEVERE, display.getFindings().get(0).getLevel());
    }

    @Test
    public void fromReport_usesNoFindingsStateForACompletedEmptyReport() {
        ProductFindingsDisplay display = ProductFindingsDisplay.fromReport(
                new ProductAnalysisReport(100, Collections.emptyList()),
                true
        );

        assertEquals(ProductFindingsDisplay.State.NO_FINDINGS, display.getState());
        assertFalse(display.hasFindings());
    }

    @Test
    public void fromReport_explainsWhenIngredientsAreRequired() {
        ProductFindingsDisplay display = ProductFindingsDisplay.fromReport(null, false);

        assertEquals(ProductFindingsDisplay.State.INGREDIENTS_REQUIRED, display.getState());
        assertTrue(display.getFindings().isEmpty());
    }

    @Test
    public void fromReport_explainsWhenAnalysisIsUnavailable() {
        ProductFindingsDisplay display = ProductFindingsDisplay.fromReport(null, true);

        assertEquals(ProductFindingsDisplay.State.ANALYSIS_UNAVAILABLE, display.getState());
        assertTrue(display.getFindings().isEmpty());
    }

    private static ProductAnalysisReport report(AnalysisResult... findings) {
        return new ProductAnalysisReport(75, Arrays.asList(findings));
    }

    private static AnalysisResult finding(
            String message,
            AnalysisResult.WarningLevel level,
            int scorePenalty,
            String ingredient
    ) {
        return new AnalysisResult(message, level, scorePenalty, ingredient, "Useful explanation.");
    }
}
