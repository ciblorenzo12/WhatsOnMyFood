package com.ciblorenzo.whatsonmyfood.analysis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

public class HealthVerdictExplanationBuilderTest {
    @Test
    public void namesTheActualIngredientAndExplainsWhyItMatters() {
        AnalysisResult concern = new AnalysisResult(
                "Contains refined oil",
                AnalysisResult.WarningLevel.WARNING,
                10,
                "sunflower oil",
                "Sunflower oil is a refined oil; the concern is greater when a snack is also high in sodium, added sugar, or refined flour."
        );

        String explanation = HealthVerdictExplanationBuilder.buildNotHealthyExplanation(
                null,
                Collections.singletonList(concern)
        );

        assertTrue(explanation.contains("sunflower oil"));
        assertTrue(explanation.contains("refined oil"));
        assertFalse(explanation.toLowerCase().contains("app's rules"));
        assertFalse(explanation.toLowerCase().contains("open food facts"));
        assertFalse(explanation.contains("Why this rating"));
    }
}
