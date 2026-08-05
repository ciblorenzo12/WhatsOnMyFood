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
            if (!source.equals("label")) {
                return Recovery.empty();
            }

            JsonArray array = root.getAsJsonArray("ingredients");
            if (array == null) return Recovery.empty();

            StringBuilder recoveredText = new StringBuilder();
            for (JsonElement element : array) {
                if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) continue;
                String value = element.getAsString();
                if (value == null || value.trim().isEmpty()) continue;
                if (recoveredText.length() > 0) recoveredText.append(", ");
                recoveredText.append(value.trim());
            }

            Set<String> seen = new LinkedHashSet<>();
            List<String> ingredients = new ArrayList<>();
            for (String parsed : IngredientTextParser.parseIngredientCandidates(recoveredText.toString())) {
                String normalized = IngredientNormalizer.normalize(cleanIngredient(parsed));
                if (!normalized.isEmpty() && seen.add(normalized)) {
                    ingredients.add(formatForDisplay(normalized));
                    if (ingredients.size() == MAX_INGREDIENTS) break;
                }
            }
            String normalizedConfidence = normalizeConfidence(confidence);
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

    private static String formatForDisplay(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
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
            builder.append("Ingredients recovered from the label (verify on package)");
            for (String ingredient : ingredients) {
                builder.append("\n- ").append(ingredient);
            }
            return builder.toString();
        }
    }
}
