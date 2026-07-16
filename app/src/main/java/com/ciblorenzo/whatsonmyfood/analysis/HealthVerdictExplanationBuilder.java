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
        builder.append(escape(productName)).append(" is marked <b>Not Healthy</b> because ");
        builder.append(explainConcern(concerns.get(0)));
        for (int i = 0; i < Math.min(2, concerns.size()); i++) {
            if (i == 0) continue;
            AnalysisResult concern = concerns.get(i);
            builder.append(" A second concern is that ").append(explainConcern(concern));
        }

        builder.append("<br><br><b>Bottom line</b><br>");
        builder.append("This is not a safety warning about one serving. When choosing between similar products, compare the ingredient list with added sugar, sodium, and serving size to find the option that best fits your routine.");
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

    private static String explainConcern(AnalysisResult concern) {
        String ingredient = concern.getTriggeringIngredient() != null ? concern.getTriggeringIngredient().trim() : "";
        String explanation = shorten(concern.getExplanation());
        if (!ingredient.isEmpty()) {
            return "the label lists <b>" + escape(ingredient) + "</b>. "
                    + (explanation.isEmpty() ? "That ingredient is the specific reason this result needs a closer look." : escape(explanation));
        }
        if (!explanation.isEmpty()) {
            return escape(explanation);
        }
        String message = concern.getMessage() != null ? concern.getMessage().trim() : "a specific label concern";
        return "the label shows <b>" + escape(message) + "</b>.";
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
