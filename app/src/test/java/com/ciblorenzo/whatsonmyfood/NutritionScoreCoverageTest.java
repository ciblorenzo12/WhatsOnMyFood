package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.analysis.rules.HighSodiumRule;
import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;

public class NutritionScoreCoverageTest {
    @Test
    public void absentOrEmptyNutritionCannotSupportAnOverallScore() {
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(null));
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(new ProductWithDetails()));
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(details("{}")));
    }

    @Test
    public void allThreeCoreValuesAreRequiredButReportedZeroIsValid() {
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(details(
                "{\"sugars_100g\":0,\"saturated-fat_100g\":0}")));
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(details(
                "{\"sugars_100g\":0,\"sodium_100g\":0}")));
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(details(
                "{\"saturated-fat_100g\":0,\"sodium_100g\":0}")));
        assertTrue(NutritionScoreCoverage.hasCoreNutrition(details(
                "{\"sugars_100g\":0,\"saturated-fat_100g\":0,\"sodium_100g\":0}")));
    }

    @Test
    public void invalidAmountsDoNotCountAsKnownNutrition() {
        ProductWithDetails data = details(
                "{\"sugars_100g\":2,\"saturated-fat_100g\":1,\"sodium_100g\":0.1}");
        data.nutriments.sugars = Double.NaN;
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(data));
        data.nutriments.sugars = Double.POSITIVE_INFINITY;
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(data));
        data.nutriments.sugars = -1.0;
        assertFalse(NutritionScoreCoverage.hasCoreNutrition(data));
    }

    @Test
    public void saltEquivalentSupportsCoverageAndStillTriggersSodiumWarning() {
        ProductWithDetails data = details(
                "{\"sugars_100g\":2,\"saturated-fat_100g\":1,\"salt_100g\":2}");
        assertTrue(NutritionScoreCoverage.hasCoreNutrition(data));
        assertEquals(0.8, NutritionScoreCoverage.sodiumForAnalysis(data.nutriments), 0.00001);
        assertEquals(1, new HighSodiumRule().evaluate(data).size());
        assertNull(data.nutriments.sodium);
    }

    @Test
    public void reportedSodiumTakesPrecedenceOverDerivedSaltEquivalent() {
        ProductWithDetails data = details(
                "{\"sugars_100g\":2,\"saturated-fat_100g\":1,\"sodium_100g\":0.1,\"salt_100g\":2}");
        assertEquals(0.1, NutritionScoreCoverage.sodiumForAnalysis(data.nutriments), 0.00001);
        assertTrue(new HighSodiumRule().evaluate(data).isEmpty());
    }

    private ProductWithDetails details(String nutrition) {
        ProductWithDetails data = new ProductWithDetails();
        data.nutriments = new Gson().fromJson(nutrition, Nutriments.class);
        return data;
    }
}
