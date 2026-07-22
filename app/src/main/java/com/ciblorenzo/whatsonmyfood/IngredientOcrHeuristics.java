package com.ciblorenzo.whatsonmyfood;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text cleanup and scoring shared by live and imported ingredient OCR. */
public final class IngredientOcrHeuristics {

    private static final Pattern UI_NOISE_LINE = Pattern.compile(
            "(?im)^\\s*(share|download(?:\\s+label)?|related searches|uploaded(?:\\s+successfully)?|submit a photo|view source|web results)\\b.*$"
    );

    private IngredientOcrHeuristics() {
    }

    public static int confidence(String text) {
        if (text == null) return Integer.MIN_VALUE;
        String lower = prepareRecognizedText(text).toLowerCase(Locale.ROOT);
        int score = 0;

        if (lower.contains("ingredients") || lower.contains("ingrédients") || lower.contains("ingredientes")) {
            score += 75;
        } else if (lower.contains("contains:") || lower.contains("contient:") || lower.contains("contiene:")) {
            score += 45;
        }

        String[] markers = {
                "water", "sugar", "sucrose", "syrup", "flour", "oil", "salt", "acid",
                "flavor", "gum", "lecithin", "starch", "dextrose", "maltodextrin",
                "citric", "natural flavor", "preservative", "color", "soy", "milk", "wheat"
        };
        for (String marker : markers) {
            if (lower.contains(marker)) score += 12;
        }

        score += Math.min(35, countOccurrences(lower, ',') * 4);
        score += Math.min(15, countOccurrences(lower, ';') * 5);

        String[] nutritionMarkers = {"calories", "serving size", "total fat", "cholesterol", "daily value"};
        for (String marker : nutritionMarkers) {
            if (lower.contains(marker)) score -= 20;
        }
        if (lower.length() < 18) score -= 15;
        return score;
    }

    public static String chooseBest(String originalText, String enhancedText) {
        String original = prepareRecognizedText(originalText);
        String enhanced = prepareRecognizedText(enhancedText);
        int originalScore = confidence(original);
        int enhancedScore = confidence(enhanced);
        if (enhancedScore > originalScore) return enhanced;
        if (enhancedScore == originalScore && enhanced.length() > original.length()) return enhanced;
        return original;
    }

    public static String prepareRecognizedText(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        normalized = normalized.replaceAll("(?i)\\bingred[1l|]ents\\b", "ingredients");
        normalized = normalized.replaceAll("(?i)([a-z])[-‐‑]\\s*\\n\\s*([a-z])", "$1$2");
        normalized = normalized.replaceAll("[ \\t]+", " ");
        normalized = normalized.replaceAll(" *\\n *", "\n");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    public static String trimUiNoise(String text) {
        String processed = prepareRecognizedText(text);
        Matcher noiseMatcher = UI_NOISE_LINE.matcher(processed);
        if (noiseMatcher.find() && noiseMatcher.start() > 20) {
            processed = processed.substring(0, noiseMatcher.start()).trim();
        }
        return processed;
    }

    private static int countOccurrences(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) count++;
        }
        return count;
    }
}
