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

        assertEquals(100, rating.score);
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

    @Test
    public void localIntermediatePointsAreNormalizedBeforeAssigningLevel() {
        SourceReliabilityEvaluator.Rating rating = SourceReliabilityEvaluator.evaluate(
                "FDA - Sodium", "https://www.fda.gov/nutrition/sodium", "unrelated topic");

        // 30 + 24 + 14 + 10 + 8 = 86 raw points out of 92.
        assertEquals(93, rating.score);
        assertEquals(SourceReliabilityEvaluator.Level.VERY_STRONG, rating.level);
    }

    @Test
    public void legacyServerScoresAreNormalizedOnlyForTheV1Method() {
        assertEquals(100, SourceReliabilityEvaluator.fromServerScore(99, "source_quality_v1").score);
        assertEquals(90, SourceReliabilityEvaluator.fromServerScore(89, "source_quality_v1").score);
        assertEquals(SourceReliabilityEvaluator.Level.VERY_STRONG,
                SourceReliabilityEvaluator.fromServerScore(89, "source_quality_v1").level);
        assertEquals(89, SourceReliabilityEvaluator.fromServerScore(89, "source_quality_v2").score);
        assertEquals(89, SourceReliabilityEvaluator.fromServerScore(89, "").score);
        assertEquals(89, SourceReliabilityEvaluator.fromServerScore(89, "future_method").score);
        assertEquals(100, SourceReliabilityEvaluator.fromServerScore(100, "source_quality_v2").score);
    }
}
