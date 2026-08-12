package com.ciblorenzo.whatsonmyfood;

import androidx.annotation.StringRes;

/** Supported, persisted sort choices for the pantry product list. */
public enum PantrySortOption {
    RECENT("recent", R.string.pantry_sort_recent),
    NAME("name", R.string.pantry_sort_name),
    HEALTH_SCORE("health_score", R.string.pantry_sort_health_score);

    private final String preferenceValue;
    @StringRes
    private final int labelResId;

    PantrySortOption(String preferenceValue, @StringRes int labelResId) {
        this.preferenceValue = preferenceValue;
        this.labelResId = labelResId;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }

    @StringRes
    public int getLabelResId() {
        return labelResId;
    }

    public static PantrySortOption fromPreference(String value) {
        for (PantrySortOption option : values()) {
            if (option.preferenceValue.equals(value)) return option;
        }
        return RECENT;
    }
}
