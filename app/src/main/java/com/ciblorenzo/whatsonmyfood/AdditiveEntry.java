package com.ciblorenzo.whatsonmyfood;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

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
        if (name == null || name.trim().isEmpty() || name.length() < 3) return false;
        
        return category != null && !category.trim().isEmpty() &&
               function != null && !function.trim().isEmpty();
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
                || contains(note, normalizedQuery);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
