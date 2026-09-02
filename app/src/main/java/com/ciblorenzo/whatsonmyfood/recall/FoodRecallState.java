package com.ciblorenzo.whatsonmyfood.recall;

import java.util.Locale;

/** User-visible states for the food recall flow. */
public enum FoodRecallState {
    READY,
    CHECKING,
    NO_KNOWN_MATCH,
    POSSIBLE_MATCH,
    CONFIRMED_MATCH,
    STALE,
    UNAVAILABLE,
    ERROR;

    public static FoodRecallState fromName(String value) {
        if (value == null || value.trim().isEmpty()) return READY;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return READY;
        }
    }
}
