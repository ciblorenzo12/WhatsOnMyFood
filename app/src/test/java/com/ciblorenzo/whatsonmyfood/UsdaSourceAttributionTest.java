package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UsdaSourceAttributionTest {

    @Test
    public void savedUsdaRecord_retainsAttributionAlongsideEveryFreshnessState() {
        CacheMeta metadata = usdaMetadata("per100g");
        for (ProductRepository.SourceStatus freshness : Arrays.asList(
                ProductRepository.SourceStatus.FRESH_CACHED_RESULT,
                ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED,
                ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT,
                ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE)) {
            List<ProductRepository.SourceStatus> original = Collections.singletonList(freshness);
            List<ProductRepository.SourceStatus> actual = metadata.withPrimarySourceStatuses(original, true);

            assertEquals(Arrays.asList(freshness, ProductRepository.SourceStatus.USDA_FOOD_DATA_CENTRAL), actual);
            assertEquals(Collections.singletonList(freshness), original);
        }
    }

    @Test
    public void volumeUnknownAndMissingBasis_warnEvenIfSavedValuesExist() {
        for (String basis : Arrays.asList("per100ml_not_converted", "unknown", null, "")) {
            List<ProductRepository.SourceStatus> actual = usdaMetadata(basis)
                    .withPrimarySourceStatuses(Collections.emptyList(), true);

            assertEquals(Arrays.asList(
                    ProductRepository.SourceStatus.USDA_FOOD_DATA_CENTRAL,
                    ProductRepository.SourceStatus.USDA_PER_100G_NUTRITION_UNAVAILABLE), actual);
        }
    }

    @Test
    public void per100gWithoutNutritionValues_doesNotClaimUsableNutrition() {
        List<ProductRepository.SourceStatus> actual = usdaMetadata("per100g")
                .withPrimarySourceStatuses(Collections.emptyList(), false);

        assertTrue(actual.contains(ProductRepository.SourceStatus.USDA_PER_100G_NUTRITION_UNAVAILABLE));
    }

    @Test
    public void otherPrimaryProvider_doesNotAttributeMergedFieldsToUsda() {
        CacheMeta metadata = new CacheMeta("012345678905", 123L);
        metadata.sourceName = "OpenFoodFactsApiClient";
        metadata.usdaNutrientBasis = "per100g";

        List<ProductRepository.SourceStatus> actual = metadata.withPrimarySourceStatuses(
                Collections.singletonList(ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE), true);

        assertEquals(Collections.singletonList(ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE), actual);
        assertFalse(actual.contains(ProductRepository.SourceStatus.USDA_FOOD_DATA_CENTRAL));
    }

    @Test
    public void legacyMetadata_doesNotInventAnAttribution() {
        CacheMeta metadata = new CacheMeta("012345678905", 123L);

        assertNull(metadata.sourceName);
        assertNull(metadata.usdaNutrientBasis);
        assertEquals(Collections.singletonList(ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT),
                metadata.withPrimarySourceStatuses(
                        Collections.singletonList(ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT), false));
    }

    @Test
    public void repeatedAttribution_doesNotDuplicateMessages() {
        CacheMeta metadata = usdaMetadata("unknown");
        List<ProductRepository.SourceStatus> first = metadata.withPrimarySourceStatuses(null, false);

        assertEquals(first, metadata.withPrimarySourceStatuses(first, false));
    }

    private static CacheMeta usdaMetadata(String basis) {
        CacheMeta metadata = new CacheMeta("012345678905", 123L);
        metadata.sourceName = "FoodDataCentralClient";
        metadata.usdaNutrientBasis = basis;
        return metadata;
    }
}
