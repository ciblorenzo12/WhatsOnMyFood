package com.ciblorenzo.whatsonmyfood;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Repairs common ingredient fragments returned by product databases or older cached scans. */
public final class IngredientListSanitizer {

    private IngredientListSanitizer() {
    }

    public static void sanitize(ProductWithDetails product) {
        if (product == null || product.ingredients == null || product.ingredients.isEmpty()) return;
        product.ingredients = sanitize(product.ingredients);
    }

    static List<Ingredient> sanitize(List<Ingredient> source) {
        List<Ingredient> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (source == null) return result;

        for (int i = 0; i < source.size(); i++) {
            Ingredient ingredient = source.get(i);
            if (ingredient == null || ingredient.text == null) continue;

            String text = ingredient.text.replaceAll("\\s+", " ").trim();
            String normalized = normalize(text);
            if (text.isEmpty() || isSectionHeadingOrFunction(normalized)) continue;

            Ingredient next = i + 1 < source.size() ? source.get(i + 1) : null;
            String nextText = next != null && next.text != null
                    ? next.text.replaceAll("\\s+", " ").trim()
                    : "";
            String nextNormalized = normalize(nextText);

            if (isGenericBVitamin(normalized)) {
                if (nextNormalized.equals("niacinamide") || nextNormalized.equals("niacin")) {
                    text = "B vitamin (" + nextText.toLowerCase(Locale.US) + ")";
                    i++;
                } else if (nextNormalized.equals("folic acid")) {
                    text = "B vitamin (folic acid)";
                    i++;
                } else {
                    continue;
                }
            } else if (isMatchingVitaminForm(normalized, nextNormalized)) {
                text = text + " (" + nextText.toLowerCase(Locale.US) + ")";
                i++;
            }

            text = canonicalizeVitaminName(text);

            String key = normalize(text);
            if (!key.isEmpty() && seen.add(key)) {
                result.add(new Ingredient(ingredient.barcode, text, result.size()));
            }
        }
        return result;
    }

    private static boolean isSectionHeadingOrFunction(String value) {
        return value.equals("vitamins")
                || value.equals("minerals")
                || value.equals("vitamins and minerals")
                || value.equals("added to preserve freshness")
                || value.equals("to preserve freshness");
    }

    private static boolean isGenericBVitamin(String value) {
        return value.equals("a b vitamin")
                || value.equals("a b vitamins")
                || value.equals("b vitamin")
                || value.equals("b vitamins");
    }

    private static String canonicalizeVitaminName(String value) {
        if (value == null || value.isEmpty()) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)^vitamin\\s+([a-z](?:\\d{1,2})?)(.*)$")
                .matcher(value.trim());
        if (!matcher.matches()) return value.trim();
        return "Vitamin " + matcher.group(1).toUpperCase(Locale.US) + matcher.group(2);
    }

    private static boolean isMatchingVitaminForm(String vitamin, String form) {
        if (form.isEmpty()) return false;
        if (vitamin.equals("vitamin c")) {
            return form.equals("sodium ascorbate") || form.equals("ascorbic acid");
        }
        if (vitamin.equals("vitamin b6")) return form.equals("pyridoxine hydrochloride");
        if (vitamin.equals("vitamin a")) return form.equals("palmitate") || form.equals("vitamin a palmitate");
        if (vitamin.equals("vitamin b1")) {
            return form.equals("thiamin mononitrate") || form.equals("thiamine mononitrate");
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
