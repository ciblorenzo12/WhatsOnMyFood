package com.ciblorenzo.whatsonmyfood;

import java.text.Normalizer;
import java.util.List;
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

    /**
     * Keeps OCR focused on the ingredient-label block instead of returning text from the
     * surrounding screen or package. The full OCR text remains a fallback when ML Kit did
     * not separate the label into a reliable block.
     */
    public static String selectIngredientRegion(List<String> textBlocks, String fullText) {
        String preparedFullText = prepareRecognizedText(fullText);
        if (textBlocks == null || textBlocks.isEmpty()) return preparedFullText;

        String bestBlock = "";
        int bestScore = Integer.MIN_VALUE;
        boolean bestHasHeading = false;
        for (String block : textBlocks) {
            String prepared = prepareRecognizedText(block);
            if (prepared.isEmpty()) continue;

            String lower = prepared.toLowerCase(Locale.ROOT);
            boolean hasHeading = lower.contains("ingredients")
                    || lower.contains("ingredient list")
                    || lower.contains("ingrÃ©dients")
                    || lower.contains("ingredientes");
            int score = confidence(prepared);
            if (hasHeading) score += 100;

            if (score > bestScore) {
                bestScore = score;
                bestBlock = prepared;
                bestHasHeading = hasHeading;
            }
        }

        return bestHasHeading ? bestBlock : preparedFullText;
    }

    public static String prepareRecognizedText(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        normalized = normalized.replaceAll("(?i)\\bingred[1l|]ents\\b", "ingredients");
        // OCR commonly confuses a lowercase L with 1 or | inside ordinary words.
        normalized = normalized.replaceAll("(?i)(?<=[a-z])[1|](?=[a-z])", "l");
        normalized = normalized.replaceAll("(?i)(?<=[a-z])\\|(?=\\s|[,.;:]|$)", "l");
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
