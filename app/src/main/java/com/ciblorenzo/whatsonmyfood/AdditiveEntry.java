package com.ciblorenzo.whatsonmyfood;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Locale;

@Entity(tableName = "additives")
public class AdditiveEntry {
    public enum HealthStatus {
        RECOMMENDED,
        MODERATE,
        NOT_RECOMMENDED
    }

    @PrimaryKey
    @NonNull
    public final String name;
    public final String category;
    public final String aliases;
    public final String function;
    public final String explanation;
    public final String note;
    public final String sourceTitle;
    public final String sourceUrl;
    public final HealthStatus status;

    public boolean isValid() {
        return isMeaningful(name, 3)
                && isMeaningful(category, 2)
                && isMeaningful(function, 3);
    }

    @Ignore
    public AdditiveEntry(@NonNull String name, String category, String aliases, String function, String explanation, String note, String sourceTitle, String sourceUrl) {
        this(name, category, aliases, function, explanation, note, sourceTitle, sourceUrl, HealthStatus.MODERATE);
    }

    public AdditiveEntry(@NonNull String name, String category, String aliases, String function, String explanation, String note, String sourceTitle, String sourceUrl, HealthStatus status) {
        this.name = name;
        this.category = category;
        this.aliases = aliases;
        this.function = function;
        this.explanation = explanation;
        this.note = note;
        this.sourceTitle = sourceTitle;
        this.sourceUrl = sourceUrl;
        this.status = status;
    }

    public boolean matches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String normalizedQuery = query.toLowerCase();
        return contains(name, normalizedQuery)
                || contains(category, normalizedQuery)
                || contains(aliases, normalizedQuery)
                || contains(function, normalizedQuery)
                || contains(explanation, normalizedQuery)
                || contains(note, normalizedQuery)
                || isLikelyTypoMatch(name, normalizedQuery)
                || isLikelyTypoMatch(aliases, normalizedQuery);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    static boolean isLikelyTypoMatch(String value, String query) {
        String normalizedValue = normalizeSearchText(value);
        String normalizedQuery = normalizeSearchText(query);
        if (normalizedValue.length() < 5 || normalizedQuery.length() < 5) return false;
        int allowedDistance = Math.max(normalizedValue.length(), normalizedQuery.length()) >= 8 ? 2 : 1;
        return Math.abs(normalizedValue.length() - normalizedQuery.length()) <= allowedDistance
                && editDistance(normalizedValue, normalizedQuery, allowedDistance) <= allowedDistance;
    }

    private static boolean isMeaningful(String value, int minimumLength) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.US);
        return normalized.length() >= minimumLength
                && !normalized.equals("null")
                && !normalized.equals("none")
                && !normalized.equals("unknown")
                && !normalized.equals("n/a");
    }

    private static String normalizeSearchText(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
    }

    private static int editDistance(String left, String right, int limit) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) previous[column] = column;
        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            int rowMinimum = current[0];
            for (int column = 1; column <= right.length(); column++) {
                int substitution = previous[column - 1]
                        + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1);
                current[column] = Math.min(
                        Math.min(previous[column] + 1, current[column - 1] + 1),
                        substitution
                );
                rowMinimum = Math.min(rowMinimum, current[column]);
            }
            if (rowMinimum > limit) return limit + 1;
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
