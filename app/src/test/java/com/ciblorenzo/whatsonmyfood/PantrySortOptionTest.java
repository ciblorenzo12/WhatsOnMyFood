package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PantrySortOptionTest {

    @Test
    public void persistedValuesRestoreEachSupportedSortOption() {
        assertEquals(PantrySortOption.RECENT, PantrySortOption.fromPreference("recent"));
        assertEquals(PantrySortOption.NAME, PantrySortOption.fromPreference("name"));
        assertEquals(PantrySortOption.HEALTH_SCORE, PantrySortOption.fromPreference("health_score"));
    }

    @Test
    public void missingOrInvalidPreferenceFallsBackToRecent() {
        assertEquals(PantrySortOption.RECENT, PantrySortOption.fromPreference(null));
        assertEquals(PantrySortOption.RECENT, PantrySortOption.fromPreference("unsupported"));
    }
}
