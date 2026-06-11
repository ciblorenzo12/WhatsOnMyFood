package com.example.myapplication.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AnalysisResultDeduplicator {

    private AnalysisResultDeduplicator() {
    }

    public static List<AnalysisResult> deduplicate(List<AnalysisResult> results) {
        Map<String, AnalysisResult> unique = new LinkedHashMap<>();
        if (results == null) {
            return new ArrayList<>();
        }

        for (AnalysisResult result : results) {
            if (result == null || isBlank(result.getMessage())) {
                continue;
            }

            String key = keyFor(result);
            AnalysisResult existing = unique.get(key);
            if (existing == null) {
                unique.put(key, result);
            } else {
                unique.put(key, stronger(existing, result));
            }
        }

        return new ArrayList<>(unique.values());
    }

    private static AnalysisResult stronger(AnalysisResult first, AnalysisResult second) {
        if (severityRank(second.getLevel()) > severityRank(first.getLevel())) {
            carrySource(second, first);
            return second;
        }
        if (severityRank(second.getLevel()) == severityRank(first.getLevel())
                && Math.abs(second.getScorePenalty()) > Math.abs(first.getScorePenalty())) {
            carrySource(second, first);
            return second;
        }
        carrySource(first, second);
        return first;
    }

    private static void carrySource(AnalysisResult target, AnalysisResult source) {
        if (target == null || source == null) return;
        if (isBlank(target.getSourceUrl()) && !isBlank(source.getSourceUrl())) {
            target.setSourceUrl(source.getSourceUrl());
            target.setVisualQuote(source.getVisualQuote());
        }
    }

    private static int severityRank(AnalysisResult.WarningLevel level) {
        if (level == null) return 0;
        switch (level) {
            case SEVERE:
                return 4;
            case WARNING:
                return 3;
            case INFO:
                return 2;
            case POSITIVE:
                return 1;
            default:
                return 0;
        }
    }

    private static String keyFor(AnalysisResult result) {
        String message = normalize(result.getMessage());
        String trigger = normalize(result.getTriggeringIngredient());

        if (message.contains("added sugar") || trigger.contains("added sugar")) {
            return "added_sugar";
        }
        if (message.contains("organic ingredient")) {
            return "organic_ingredients";
        }
        if (message.contains("natural flavor")) {
            return "natural_flavor";
        }
        if (message.contains("high fructose corn syrup")) {
            return "high_fructose_corn_syrup";
        }
        if (message.contains("trans fat") || trigger.contains("partially hydrogenated")) {
            return "trans_fat";
        }

        return message + "|" + trigger;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
