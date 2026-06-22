package com.ciblorenzo.whatsonmyfood.analysis;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HealthVerdictExplanationBuilder {
    private HealthVerdictExplanationBuilder() {
    }

    public static String buildNotHealthyExplanation(ProductWithDetails product, List<AnalysisResult> results) {
        List<AnalysisResult> concerns = collectConcerns(results);
        if (concerns.isEmpty()) return "";

        String productName = product != null
                && product.product != null
                && product.product.productName != null
                && !product.product.productName.trim().isEmpty()
                ? product.product.productName.trim()
                : "This product";

        StringBuilder builder = new StringBuilder();
        builder.append("<b>Why this rating</b><br>");
        builder.append(escape(productName)).append(" is marked <b>Not Healthy</b> because ");
        builder.append(primaryReason(concerns)).append("<br><br>");

        builder.append("<b>What to watch</b><br>");
        for (int i = 0; i < Math.min(2, concerns.size()); i++) {
            AnalysisResult concern = concerns.get(i);
            builder.append(escape(concern.getMessage()));
            if (concern.getTriggeringIngredient() != null && !concern.getTriggeringIngredient().trim().isEmpty()) {
                builder.append(": ").append(escape(concern.getTriggeringIngredient().trim()));
            }
            builder.append(". ");
            builder.append(escape(shorten(concern.getExplanation()))).append("<br>");
        }

        builder.append("<br><b>Recommendation</b><br>");
        builder.append("Use it occasionally instead of as an everyday pantry staple, and compare it with a similar product that has fewer processing warnings, less added sugar, lower sodium, or a shorter ingredient list.");
        return builder.toString();
    }

    public static JSONArray buildSources(List<AnalysisResult> results) {
        JSONArray sources = new JSONArray();
        List<String> seen = new ArrayList<>();
        if (results == null) return sources;

        for (AnalysisResult result : results) {
            if (result == null || result.getSourceUrl() == null || result.getSourceUrl().trim().isEmpty()) continue;
            String url = result.getSourceUrl().trim();
            if (seen.contains(url)) continue;
            seen.add(url);

            try {
                JSONObject source = new JSONObject();
                source.put("name", sourceName(url, result));
                source.put("url", url);
                source.put("visual_quote", result.getVisualQuote() != null ? result.getVisualQuote() : "");
                source.put("search_query", result.getMessage());
                sources.put(source);
            } catch (Exception ignored) {
            }
        }
        return sources;
    }

    public static boolean isContradictingNotHealthy(String summary) {
        if (summary == null) return false;
        String text = summary.toLowerCase(Locale.US);
        return text.contains("reasonable everyday choice")
                || text.contains("low-concern")
                || text.contains("no major high-concern")
                || text.contains("solid choice");
    }

    private static List<AnalysisResult> collectConcerns(List<AnalysisResult> results) {
        List<AnalysisResult> concerns = new ArrayList<>();
        if (results == null) return concerns;
        for (AnalysisResult result : results) {
            if (result == null || result.getLevel() == null) continue;
            if (result.getLevel() == AnalysisResult.WarningLevel.SEVERE
                    || result.getLevel() == AnalysisResult.WarningLevel.WARNING) {
                concerns.add(result);
            }
        }
        return concerns;
    }

    private static String primaryReason(List<AnalysisResult> concerns) {
        AnalysisResult primary = concerns.get(0);
        String message = primary.getMessage() != null ? primary.getMessage() : "it has one or more caution signals";
        if (message.toLowerCase(Locale.US).contains("nova 4")) {
            return "Open Food Facts classifies it as <b>NOVA Group 4</b>, the highest processing category for ultra-processed foods.";
        }
        if (primary.getTriggeringIngredient() != null && !primary.getTriggeringIngredient().trim().isEmpty()) {
            return "the ingredient list contains <b>" + escape(primary.getTriggeringIngredient().trim()) + "</b>, a concern flagged by the app's rules.";
        }
        return "the app found <b>" + escape(message) + "</b> in the product data.";
    }

    private static String sourceName(String url, AnalysisResult result) {
        String lower = url.toLowerCase(Locale.US);
        if (lower.contains("openfoodfacts.org")) return "Open Food Facts";
        if (lower.contains("fda.gov")) return "FDA";
        if (lower.contains("who.int")) return "WHO";
        if (result.getMessage() != null && !result.getMessage().trim().isEmpty()) return result.getMessage().trim();
        return "Source";
    }

    private static String shorten(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String trimmed = value.trim();
        return trimmed.length() <= 220 ? trimmed : trimmed.substring(0, 217) + "...";
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
