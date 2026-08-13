package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SourceReliabilityEvaluatorTest {

    @Test
    public void officialNutritionGuidanceReceivesVeryStrongEstimate() {
        SourceReliabilityEvaluator.Rating rating = SourceReliabilityEvaluator.evaluate(
                "FDA - Added Sugars on the Nutrition Facts Label",
                "https://www.fda.gov/food/nutrition-facts-label/added-sugars-nutrition-facts-label",
                "added sugar nutrition label"
        );

        assertTrue(rating.score >= 90);
        assertEquals(SourceReliabilityEvaluator.Level.VERY_STRONG, rating.level);
    }

    @Test
    public void peerReviewedRepositoryReceivesVeryStrongEstimate() {
        SourceReliabilityEvaluator.Rating rating = SourceReliabilityEvaluator.evaluate(
                "Peer-reviewed nutrition study",
                "https://pmc.ncbi.nlm.nih.gov/articles/PMC10357061/",
                "nutrition study"
        );

        assertTrue(rating.score >= 90);
        assertEquals(SourceReliabilityEvaluator.Level.VERY_STRONG, rating.level);
    }

    @Test
    public void unclassifiedPublisherIsClearlyLimited() {
        SourceReliabilityEvaluator.Rating rating = SourceReliabilityEvaluator.evaluate(
                "Food opinion",
                "https://example.com/article",
                "food additives"
        );

        assertTrue(rating.score < 60);
        assertEquals(SourceReliabilityEvaluator.Level.LIMITED, rating.level);
    }

    @Test
    public void serverScoreIsClampedAndMappedConsistently() {
        assertEquals(100, SourceReliabilityEvaluator.fromServerScore(140).score);
        assertEquals(SourceReliabilityEvaluator.Level.STRONG,
                SourceReliabilityEvaluator.fromServerScore(82).level);
    }
}
