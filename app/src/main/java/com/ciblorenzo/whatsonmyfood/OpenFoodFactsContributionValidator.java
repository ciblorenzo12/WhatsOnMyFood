package com.ciblorenzo.whatsonmyfood;

import java.util.List;

public final class OpenFoodFactsContributionValidator {
    private OpenFoodFactsContributionValidator() {
    }

    public static String joinSuggestedIngredients(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String ingredient : ingredients) {
            if (ingredient == null || ingredient.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(", ");
            builder.append(ingredient.trim());
        }
        return builder.toString();
    }

    public static ValidationResult validate(
            String barcode,
            String ingredients,
            String userId,
            String password,
            boolean labelConfirmed
    ) {
        return validate(barcode, ingredients, ingredients, userId, password, labelConfirmed);
    }

    public static ValidationResult validate(
            String barcode,
            String originalIngredients,
            String englishIngredients,
            String userId,
            String password,
            boolean labelConfirmed
    ) {
        if (barcode == null || !barcode.trim().matches("[0-9]{8,14}")) {
            return ValidationResult.failure(Field.BARCODE);
        }
        if (originalIngredients == null || originalIngredients.trim().length() < 2) {
            return ValidationResult.failure(Field.ORIGINAL_INGREDIENTS);
        }
        if (englishIngredients == null || englishIngredients.trim().length() < 2) {
            return ValidationResult.failure(Field.INGREDIENTS);
        }
        if (userId == null || userId.trim().isEmpty()) {
            return ValidationResult.failure(Field.USER_ID);
        }
        if (password == null || password.isEmpty()) {
            return ValidationResult.failure(Field.PASSWORD);
        }
        if (!labelConfirmed) {
            return ValidationResult.failure(Field.CONFIRMATION);
        }
        return ValidationResult.success();
    }

    public enum Field {
        NONE,
        BARCODE,
        ORIGINAL_INGREDIENTS,
        INGREDIENTS,
        USER_ID,
        PASSWORD,
        CONFIRMATION
    }

    public static final class ValidationResult {
        public final boolean valid;
        public final Field field;

        private ValidationResult(boolean valid, Field field) {
            this.valid = valid;
            this.field = field;
        }

        static ValidationResult success() {
            return new ValidationResult(true, Field.NONE);
        }

        static ValidationResult failure(Field field) {
            return new ValidationResult(false, field);
        }
    }
}
