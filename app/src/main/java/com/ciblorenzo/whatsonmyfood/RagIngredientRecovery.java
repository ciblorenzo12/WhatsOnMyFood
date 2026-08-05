package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Validates and converts a RAG response into deterministic ingredient entities. */
public final class RagIngredientRecovery {

    public static final class Result {
        public final List<Ingredient> ingredients;

        private Result(List<Ingredient> ingredients) {
            this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        }

        public boolean recovered() {
            return !ingredients.isEmpty();
        }
    }

    private RagIngredientRecovery() {
    }

    public static Result fromResponse(
            String barcode,
            ProductResponse response,
            Nutriments nutriments,
            String languageCode
    ) {
        if (response == null || response.status != 1 || response.product == null) {
            return empty();
        }
        return fromText(barcode, localizedIngredients(response.product, languageCode), nutriments);
    }

    public static Result fromText(String barcode, String ingredientsText, Nutriments nutriments) {
        if (isBlank(barcode) || isBlank(ingredientsText)) return empty();

        boolean hasAddedSugars = nutriments != null
                && nutriments.addedSugars != null
                && nutriments.addedSugars > 0;
        List<String> sugarKeywords = Arrays.asList(
                "sugar", "syrup", "juice", "sweetener", "fructose", "dextrose", "cane"
        );
        List<Ingredient> ingredients = new ArrayList<>();
        int rank = 0;
        for (String ingredientText : IngredientTextParser.parseIngredientCandidates(ingredientsText)) {
            String formatted = formatIngredientText(ingredientText, sugarKeywords, hasAddedSugars);
            if (!formatted.isEmpty()) {
                ingredients.add(new Ingredient(barcode, formatted, rank++));
            }
        }

        if (ingredients.size() == 1 && isLikelyWarningOnlyIngredient(ingredients.get(0).text)) {
            return empty();
        }
        return ingredients.isEmpty() ? empty() : new Result(ingredients);
    }

    private static String localizedIngredients(ProductResponse.ProductData product, String languageCode) {
        if (product == null) return "";
        String structured = structuredIngredientsText(product.ingredients);
        if ("es".equals(languageCode)) {
            return firstNonEmpty(
                    product.ingredientsTextEs,
                    product.ingredientsTextEn,
                    product.ingredientsText,
                    structured,
                    product.ingredientsTextWithAllergensEs,
                    product.ingredientsTextWithAllergensEn,
                    product.ingredientsTextWithAllergens
            );
        }
        if ("fr".equals(languageCode)) {
            return firstNonEmpty(
                    product.ingredientsTextFr,
                    product.ingredientsTextEn,
                    product.ingredientsText,
                    structured,
                    product.ingredientsTextWithAllergensFr,
                    product.ingredientsTextWithAllergensEn,
                    product.ingredientsTextWithAllergens
            );
        }
        return firstNonEmpty(
                product.ingredientsTextEn,
                product.ingredientsText,
                structured,
                product.ingredientsTextWithAllergensEn,
                product.ingredientsTextWithAllergens,
                product.ingredientsTextEs,
                product.ingredientsTextFr
        );
    }

    private static String structuredIngredientsText(ProductResponse.IngredientsData[] ingredients) {
        if (ingredients == null || ingredients.length == 0) return "";
        List<ProductResponse.IngredientsData> sorted = new ArrayList<>(Arrays.asList(ingredients));
        sorted.sort((left, right) -> Integer.compare(
                left != null ? left.rank : Integer.MAX_VALUE,
                right != null ? right.rank : Integer.MAX_VALUE
        ));
        List<String> values = new ArrayList<>();
        for (ProductResponse.IngredientsData ingredient : sorted) {
            if (ingredient != null && !isBlank(ingredient.text)) values.add(ingredient.text.trim());
        }
        return String.join(", ", values);
    }

    private static String formatIngredientText(
            String ingredientText,
            List<String> sugarKeywords,
            boolean hasAddedSugars
    ) {
        String cleaned = IngredientTextParser.cleanIngredientText(ingredientText).replace("_", "").trim();
        if (cleaned.isEmpty()) return "";
        String formatted = cleaned.substring(0, 1).toUpperCase(Locale.US)
                + cleaned.substring(1).toLowerCase(Locale.US);
        boolean isSugar = sugarKeywords.stream().anyMatch(formatted.toLowerCase(Locale.US)::contains);
        if (isSugar) formatted += hasAddedSugars ? " (Added Sugar)" : " (Sugar)";
        return formatted;
    }

    private static boolean isLikelyWarningOnlyIngredient(String ingredientText) {
        if (isBlank(ingredientText)) return true;
        String normalized = ingredientText.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
        return normalized.equals("phenylalanine")
                || normalized.equals("no calories")
                || normalized.equals("no sugar")
                || normalized.equals("zero calories")
                || normalized.equals("zero sugar");
    }

    private static Result empty() {
        return new Result(Collections.emptyList());
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
