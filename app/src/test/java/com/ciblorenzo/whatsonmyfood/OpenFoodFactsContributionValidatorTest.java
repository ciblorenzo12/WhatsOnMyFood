package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpenFoodFactsContributionValidatorTest {

    @Test
    public void requiresPackageLabelConfirmation() {
        OpenFoodFactsContributionValidator.ValidationResult result =
                OpenFoodFactsContributionValidator.validate(
                        "0123456789012",
                        "Water, oats, sea salt",
                        "contributor",
                        "password",
                        false
                );

        assertFalse(result.valid);
        assertEquals(OpenFoodFactsContributionValidator.Field.CONFIRMATION, result.field);
    }

    @Test
    public void acceptsAConfirmedContribution() {
        OpenFoodFactsContributionValidator.ValidationResult result =
                OpenFoodFactsContributionValidator.validate(
                        "0123456789012",
                        "Water, oats, sea salt",
                        "contributor",
                        "password",
                        true
                );

        assertTrue(result.valid);
    }

    @Test
    public void joinsEditableSuggestionsWithoutBlankItems() {
        assertEquals(
                "Water, Oats, Sea salt",
                OpenFoodFactsContributionValidator.joinSuggestedIngredients(
                        Arrays.asList("Water", " ", "Oats", null, "Sea salt")
                )
        );
    }

    @Test
    public void requiresAnEnglishTranslationSeparatelyFromTheOriginalLabel() {
        OpenFoodFactsContributionValidator.ValidationResult result =
                OpenFoodFactsContributionValidator.validate(
                        "0123456789012",
                        "Agua, azucar, sal",
                        "",
                        "contributor",
                        "password",
                        true
                );

        assertEquals(OpenFoodFactsContributionValidator.Field.INGREDIENTS, result.field);
    }

    @Test
    public void normalizesLanguageTagsForOpenFoodFactsFields() {
        assertEquals("es", OpenFoodFactsApiClient.ingredientLanguageCode("es"));
        assertEquals("zh", OpenFoodFactsApiClient.ingredientLanguageCode("zh-Latn"));
        assertEquals(null, OpenFoodFactsApiClient.ingredientLanguageCode("unknown"));
    }
}
