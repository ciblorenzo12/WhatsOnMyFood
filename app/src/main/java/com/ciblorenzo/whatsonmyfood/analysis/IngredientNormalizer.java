package com.ciblorenzo.whatsonmyfood.analysis;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Produces stable ingredient names without guessing that similar names are equivalent. */
public final class IngredientNormalizer {

    private static final Map<String, String> ADDITIVE_ALIASES = createAdditiveAliases();

    private IngredientNormalizer() {
    }

    public static String normalize(String ingredient) {
        if (ingredient == null) return "";

        String normalized = Normalizer.normalize(ingredient, Normalizer.Form.NFKC)
                .replace('\u00a0', ' ')
                .replace('\u2019', '\'')
                .replaceAll("^[\\s,;:.!?*\\u2022\\u00b7\\\"'\\u201c\\u201d]+", "")
                .replaceAll("[\\s,;:.!?*\\u2022\\u00b7\\\"'\\u201c\\u201d]+$", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.US);

        if (normalized.isEmpty()) return "";

        // Treat OCR/layout hyphens between words consistently while retaining values such as omega-3.
        normalized = normalized.replaceAll("(?<=\\p{L})[-\\u2010-\\u2015](?=\\p{L})", " ");
        normalized = normalized
                .replaceAll("\\s*\\(\\s*", " (")
                .replaceAll("\\s*,\\s*", ", ")
                .replaceAll("\\s*;\\s*", "; ")
                .replaceAll("\\s*\\)\\s*", ")")
                .replaceAll("\\s+", " ")
                .trim();

        String alias = ADDITIVE_ALIASES.get(normalized);
        if (alias != null) return alias;

        String compactCode = normalized.replaceAll("[\\s-]+", "");
        alias = ADDITIVE_ALIASES.get(compactCode);
        return alias != null ? alias : normalized;
    }

    public static List<String> normalizeAndDeduplicate(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String ingredient : ingredients) {
            String normalized = normalize(ingredient);
            if (!normalized.isEmpty() && seen.add(normalized)) result.add(normalized);
        }
        return result;
    }

    private static Map<String, String> createAdditiveAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("e300", "ascorbic acid");
        aliases.put("e202", "potassium sorbate");
        aliases.put("e211", "sodium benzoate");
        aliases.put("e330", "citric acid");
        aliases.put("citric acid (e330)", "citric acid");
        aliases.put("e330 (citric acid)", "citric acid");
        aliases.put("e415", "xanthan gum");
        aliases.put("e621", "monosodium glutamate");
        aliases.put("msg", "monosodium glutamate");
        aliases.put("monosodium glutamate (msg)", "monosodium glutamate");
        aliases.put("baking soda", "sodium bicarbonate");
        aliases.put("sodium hydrogen carbonate", "sodium bicarbonate");
        aliases.put("natural flavour", "natural flavor");
        aliases.put("natural flavours", "natural flavors");
        aliases.put("artificial flavour", "artificial flavor");
        aliases.put("artificial flavours", "artificial flavors");
        aliases.put("caramel colour", "caramel color");
        return Collections.unmodifiableMap(aliases);
    }
}
