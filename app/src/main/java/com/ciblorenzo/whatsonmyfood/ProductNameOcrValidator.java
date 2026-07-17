package com.ciblorenzo.whatsonmyfood;

import java.util.Locale;

/** Detects whether an OCR pass contains useful front-label product identity text. */
public final class ProductNameOcrValidator {

    public static final class Result {
        public final boolean readable;
        public final String cleanedText;
        public final String bestCandidate;
        public final int confidence;

        private Result(boolean readable, String cleanedText, String bestCandidate, int confidence) {
            this.readable = readable;
            this.cleanedText = cleanedText;
            this.bestCandidate = bestCandidate;
            this.confidence = confidence;
        }
    }

    private ProductNameOcrValidator() {
    }

    public static Result validate(String ocrText) {
        String cleaned = IngredientOcrHeuristics.trimUiNoise(ocrText);
        String best = "";
        int bestScore = 0;

        for (String line : cleaned.split("\\r?\\n")) {
            String candidate = line.replaceAll("[^\\p{L}\\p{N}&'+\\- ]+", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            int score = scoreCandidate(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return new Result(bestScore >= 6, cleaned, best, bestScore);
    }

    private static int scoreCandidate(String candidate) {
        if (candidate.length() < 3 || candidate.length() > 55) return 0;
        String lower = candidate.toLowerCase(Locale.ROOT);
        if (isPackageMetadata(lower)) return 0;

        int letters = 0;
        int digits = 0;
        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (Character.isLetter(c)) letters++;
            else if (Character.isDigit(c)) digits++;
        }
        if (letters < 3 || digits > letters * 2) return 0;

        int words = candidate.split(" ").length;
        if (words > 9) return 0;

        int score = 3;
        if (letters >= 5) score += 2;
        if (words <= 5) score += 2;
        if (candidate.equals(candidate.toUpperCase(Locale.ROOT))) score += 1;
        if (lower.matches(".*\\b(original|classic|vanilla|chocolate|strawberry|cheese|cereal|water|yogurt|biscuits?|chips|spread)\\b.*")) {
            score += 1;
        }
        return score;
    }

    private static boolean isPackageMetadata(String lower) {
        return lower.startsWith("ingredients")
                || lower.startsWith("ingredient list")
                || lower.startsWith("ingr\u00e9dients")
                || lower.startsWith("ingredientes")
                || lower.startsWith("nutrition")
                || lower.startsWith("serving")
                || lower.startsWith("calories")
                || lower.startsWith("total fat")
                || lower.startsWith("contains")
                || lower.startsWith("distributed by")
                || lower.startsWith("manufactured by")
                || lower.startsWith("directions")
                || lower.startsWith("warning")
                || lower.startsWith("keep refrigerated")
                || lower.matches(".*\\b(net wt|fl oz|daily value|barcode|www\\.)\\b.*");
    }
}
