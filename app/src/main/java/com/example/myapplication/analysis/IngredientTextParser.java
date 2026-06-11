package com.example.myapplication.analysis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class IngredientTextParser {

    private static final int MAX_INGREDIENTS = 80;

    private static final Pattern METADATA_LINE = Pattern.compile(
            "(?im)^\\s*(response_language|language|baseline_health_score|detected_ingredient_label|product_ocr_text|ocr_text|product data)\\s*[:=].*$"
    );
    private static final Pattern FIELD_PREFIX = Pattern.compile("(?i)^\\s*[a-z][a-z_ ]{2,30}\\s*[:=]\\s*");
    private static final Pattern MEASUREMENT = Pattern.compile("(?i).*\\b\\d+(\\.\\d+)?\\s*(fl\\s*oz|oz|ml|l|g|kg|lb|ct|count)\\b.*");

    private IngredientTextParser() {
    }

    public static List<String> parseIngredientCandidates(String text) {
        List<String> parsed = new ArrayList<>();
        if (text == null) return parsed;

        String source = cutAtStopMarker(stripPromptMetadata(text));
        source = trimToLikelyIngredientList(source);
        source = cutAtStopMarker(source);

        String[] parts = source.split("[,;\\n\\u2022]+");
        Set<String> seen = new LinkedHashSet<>();
        for (String part : parts) {
            String cleaned = cleanIngredientText(part);
            if (!cleaned.isEmpty()) {
                String key = normalizeKey(cleaned);
                if (seen.add(key)) {
                    parsed.add(cleaned);
                }
            }
            if (parsed.size() >= MAX_INGREDIENTS) break;
        }

        return parsed;
    }

    public static String stripPromptMetadata(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("(?i)\\bresponse_language\\s*[:=]\\s*[^,\\n]+,?\\s*", " ");
        return METADATA_LINE.matcher(cleaned).replaceAll(" ").replace("PRODUCT DATA:", " ");
    }

    public static String trimToLikelyIngredientList(String text) {
        if (text == null) return "";
        String cleaned = stripPromptMetadata(text);
        String lower = cleaned.toLowerCase(Locale.US);
        String[] markers = {
                "ingredients:",
                "ingredient list:",
                "ingredients",
                "contains:",
                "contains"
        };
        int start = -1;
        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index >= 0 && (start == -1 || index < start)) {
                start = index + marker.length();
            }
        }
        return start >= 0 ? cleaned.substring(start) : cleaned;
    }

    public static String cleanIngredientText(String text) {
        if (text == null) return "";
        String cleaned = FIELD_PREFIX.matcher(text).replaceFirst("")
                .replaceAll("\\[[a-zA-Z:-]+\\]", "")
                .replaceAll("^[\\s\\-*:]+", "")
                .replaceAll("\\s+", " ")
                .trim();

        cleaned = cleaned.replaceAll("(?i)^and\\s+", "").trim();
        if (cleaned.length() < 2) return "";

        String lower = cleaned.toLowerCase(Locale.US);
        if (isNonIngredientLine(lower, cleaned)) {
            return "";
        }
        return cleaned;
    }

    private static String cutAtStopMarker(String text) {
        String lower = text.toLowerCase(Locale.US);
        String[] stopMarkers = {
                "nutrition facts",
                "supplement facts",
                "phenylketonurics:",
                "phenylketonurics",
                "directions",
                "warnings",
                "allergens:",
                "allergens",
                "distributed by",
                "manufactured by",
                "where can i buy",
                "compare store",
                "view marketplace",
                "better alternatives",
                "bottom line"
        };
        int stop = -1;
        for (String marker : stopMarkers) {
            int index = lower.indexOf(marker);
            if (index >= 0 && (stop == -1 || index < stop)) {
                stop = index;
            }
        }
        return stop >= 0 ? text.substring(0, stop) : text;
    }

    private static boolean isNonIngredientLine(String lower, String original) {
        if (lower.startsWith("response_language")
                || lower.startsWith("product_ocr_text")
                || lower.startsWith("ocr_text")
                || lower.startsWith("nutrition facts")
                || lower.startsWith("serving size")
                || lower.startsWith("calories")
                || lower.startsWith("no calories")
                || lower.startsWith("zero calories")
                || lower.startsWith("no sugar")
                || lower.startsWith("zero sugar")
                || lower.startsWith("sugar free")
                || lower.startsWith("no added sugar")
                || lower.startsWith("total fat")
                || lower.startsWith("daily value")
                || lower.startsWith("barcode")
                || lower.startsWith("source")
                || lower.startsWith("verdict")
                || lower.startsWith("what stands out")
                || lower.startsWith("bottom line")) {
            return true;
        }

        if (lower.contains("trader joe")
                || lower.equals("english")
                || lower.equals("facial")
                || lower.equals("toner")
                || lower.equals("hydrate")
                || lower.equals("refresh")
                || lower.contains("hydrate& refresh")
                || lower.contains("submit a photo")
                || lower.contains("not bobby")
                || lower.contains("bobby approved")) {
            return true;
        }

        if (MEASUREMENT.matcher(lower).matches() && !lower.contains("vitamin")) {
            return true;
        }

        int letters = 0;
        for (int i = 0; i < original.length(); i++) {
            if (Character.isLetter(original.charAt(i))) letters++;
        }
        return letters == 0;
    }

    private static String normalizeKey(String text) {
        return text.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
