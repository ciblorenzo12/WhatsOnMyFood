package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SourceStatusResolverTest {

    @Test
    public void freshCache_showsFreshCachedResult() {
        assertEquals(
                Arrays.asList(ProductRepository.SourceStatus.FRESH_CACHED_RESULT),
                SourceStatusResolver.forCachedResult(false)
        );
    }

    @Test
    public void staleCache_showsOutdatedWarning() {
        assertEquals(
                Arrays.asList(ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED),
                SourceStatusResolver.forCachedResult(true)
        );
    }

    @Test
    public void offlineCache_showsSavedResultAndOutdatedWarning() {
        assertEquals(
                Arrays.asList(
                        ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT,
                        ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED
                ),
                SourceStatusResolver.forSavedOfflineResult()
        );
    }

    @Test
    public void primaryDatabaseUpdate_showsOnlyUpdateMessage() {
        assertEquals(
                Arrays.asList(ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE),
                SourceStatusResolver.forUpdatedDatabaseResult(false, false)
        );
    }

    @Test
    public void fallbackWithRecoveredIngredients_showsBothAdditionalMessages() {
        List<ProductRepository.SourceStatus> statuses = SourceStatusResolver.forUpdatedDatabaseResult(true, true);

        assertEquals(
                Arrays.asList(
                        ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE,
                        ProductRepository.SourceStatus.FALLBACK_PRODUCT_SOURCE,
                        ProductRepository.SourceStatus.INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE
                ),
                statuses
        );
    }
}
