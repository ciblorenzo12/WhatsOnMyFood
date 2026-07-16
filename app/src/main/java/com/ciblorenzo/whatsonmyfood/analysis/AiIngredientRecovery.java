package com.ciblorenzo.whatsonmyfood.analysis;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AiIngredientRecovery {
    private static final int MAX_INGREDIENTS = 30;

    private AiIngredientRecovery() {
    }

    public static Recovery parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String source = stringValue(root, "ingredients_source").toLowerCase(Locale.US);
            String confidence = stringValue(root, "ingredient_confidence").toLowerCase(Locale.US);
            if (!source.equals("label") && !source.equals("product_identity")) {
                return Recovery.empty();
            }

            JsonArray array = root.getAsJsonArray("ingredients");
            if (array == null) return Recovery.empty();

            Set<String> seen = new LinkedHashSet<>();
            List<String> ingredients = new ArrayList<>();
            for (JsonElement element : array) {
                if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) continue;
                for (String part : splitTopLevelIngredients(element.getAsString())) {
                    String ingredient = cleanIngredient(part);
                    String key = ingredient.toLowerCase(Locale.US);
                    if (!ingredient.isEmpty() && seen.add(key)) {
                        ingredients.add(ingredient);
                        if (ingredients.size() == MAX_INGREDIENTS) break;
                    }
                }
                if (ingredients.size() == MAX_INGREDIENTS) break;
            }
            String normalizedConfidence = normalizeConfidence(confidence);
            if (source.equals("product_identity") && normalizedConfidence.equals("high")) {
                normalizedConfidence = "medium";
            }
            return ingredients.isEmpty() ? Recovery.empty() : new Recovery(source, normalizedConfidence, ingredients);
        } catch (Exception ignored) {
            return Recovery.empty();
        }
    }

    public static boolean shouldDisplay(ProductWithDetails product, Recovery recovery) {
        boolean hasVerifiedIngredients = product != null
                && product.ingredients != null
                && product.ingredients.stream().anyMatch(ingredient -> ingredient != null
                && ingredient.text != null
                && !ingredient.text.trim().isEmpty());
        return !hasVerifiedIngredients && recovery != null && !recovery.ingredients.isEmpty();
    }

    private static String stringValue(JsonObject root, String name) {
        JsonElement value = root.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
    }

    private static String cleanIngredient(String value) {
        if (value == null) return "";
        String cleaned = value.replaceFirst("^[\\s\\-*•]+", "").trim();
        if (cleaned.length() > 140) cleaned = cleaned.substring(0, 140).trim();
        String normalized = cleaned.toLowerCase(Locale.US);
        if (normalized.isEmpty()
                || normalized.equals("unknown")
                || normalized.equals("not listed")
                || normalized.equals("not available")) {
            return "";
        }
        return cleaned;
    }

    private static List<String> splitTopLevelIngredients(String value) {
        List<String> parts = new ArrayList<>();
        if (value == null) return parts;
        int depth = 0;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '(' || character == '[') depth++;
            else if ((character == ')' || character == ']') && depth > 0) depth--;
            else if ((character == ',' || character == ';') && depth == 0) {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    private static String normalizeConfidence(String value) {
        if (value.equals("high") || value.equals("medium") || value.equals("low")) return value;
        return "low";
    }

    public static final class Recovery {
        public final String source;
        public final String confidence;
        public final List<String> ingredients;

        Recovery(String source, String confidence, List<String> ingredients) {
            this.source = source;
            this.confidence = confidence;
            this.ingredients = ingredients;
        }

        static Recovery empty() {
            return new Recovery("unknown", "low", new ArrayList<>());
        }

        public String toDisplayText() {
            StringBuilder builder = new StringBuilder();
            if (source.equals("product_identity")) {
                builder.append("Ingredients recovered from a supporting service (")
                        .append(confidence)
                        .append(" confidence; verify on package)");
            } else {
                builder.append("Ingredients recovered from the label (verify on package)");
            }
            for (String ingredient : ingredients) {
                builder.append("\n- ").append(ingredient);
            }
            return builder.toString();
        }
    }
}
