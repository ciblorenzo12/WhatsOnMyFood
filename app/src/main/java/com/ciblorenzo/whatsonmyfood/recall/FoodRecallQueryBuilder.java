package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Produces a narrow openFDA search while leaving final identity decisions to the matcher. */
public final class FoodRecallQueryBuilder {
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "and", "the", "with", "from", "for", "food", "foods", "brand", "original",
            "organic", "natural", "style", "flavor", "flavored", "product"
    ));

    private FoodRecallQueryBuilder() {}

    public static String build(Product product) {
        if (product == null) throw new IllegalArgumentException("Product is required");
        String namePhrase = phrase(significantTokens(product.productName), 3);
        String brandPhrase = phrase(significantTokens(product.brands), 2);
        List<String> clauses = new ArrayList<>();
        if (!namePhrase.isEmpty()) {
            clauses.add("product_description:\"" + namePhrase + "\"");
        }
        if (!brandPhrase.isEmpty() && !brandPhrase.equals(namePhrase)) {
            clauses.add("product_description:\"" + brandPhrase + "\"");
        }
        if (clauses.isEmpty()) {
            throw new IllegalArgumentException("Product name or brand is required");
        }
        return String.join(" OR ", clauses);
    }

    static List<String> significantTokens(String value) {
        String normalized = normalize(value);
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token) && !tokens.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    static String normalize(String value) {
        if (value == null) return "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String phrase(List<String> tokens, int limit) {
        if (tokens.isEmpty()) return "";
        return String.join(" ", tokens.subList(0, Math.min(limit, tokens.size())));
    }
}
