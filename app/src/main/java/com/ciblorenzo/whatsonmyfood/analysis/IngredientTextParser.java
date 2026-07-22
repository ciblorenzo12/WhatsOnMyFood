package com.ciblorenzo.whatsonmyfood.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IngredientTextParser {

    private static final int MAX_INGREDIENTS = 80;

    private static final Pattern METADATA_LINE = Pattern.compile(
            "(?im)^\\s*(response_language|language|scan_mode|image_attached|available_barcode|baseline_health_score|contains_allergens|may_contain_allergens|product_name|brand|category|quantity|serving_size|source|model|temperature)\\s*[:=].*$"
    );
    private static final Pattern PAYLOAD_FIELD = Pattern.compile(
            "(?im)^\\s*(detected_ingredient_label|product_ocr_text|ocr_text)\\s*[:=]\\s*(.*)$"
    );
    private static final Pattern PROMPT_INSTRUCTION_LINE = Pattern.compile(
            "(?im)^\\s*(task\\s*:|instructions?\\s*:|system_prompt\\s*:|user_prompt\\s*:|you are\\b|your task\\b|analy[sz]e\\b|parse\\b|return\\b|respond\\b|output\\b|use visible\\b|do not\\b|never\\b|important\\s*:).*$"
    );
    private static final Pattern FIELD_PREFIX = Pattern.compile("(?i)^\\s*[a-z][a-z_ ]{2,30}\\s*[:=]\\s*");
    private static final Pattern MEASUREMENT = Pattern.compile("(?i).*\\b\\d+(\\.\\d+)?\\s*(fl\\s*oz|oz|ml|l|g|kg|lb|ct|count)\\b.*");
    private static final Pattern INGREDIENT_HEADING = Pattern.compile(
            "(?i)(?:\\b|a[\\p{S}\\p{Punct}]+[a-z]?)(ingredient\\s+list|ingredients|ingrédients|ingredientes)\\b[\\t \"'\\u2018\\u2019\\u201c\\u201d]*[:.\\-\\u2013\\u2014]?[\\t \"'\\u2018\\u2019\\u201c\\u201d]*"
    );
    private static final Pattern ALLERGEN_HEADING = Pattern.compile(
            "(?im)(^|[.,;][\\t ]*|\\r?\\n[\\t ]*|[\\t ]+(?=allergens?\\s*:))(may[\\t ]+contain|contains(?![\\t ]+(?:less[\\t ]+than[\\t ]+)?\\d)|allergens?|allergènes|alérgenos)\\s*:?[\\t ]*"
    );

    public static final class ParsedLabel {
        public final List<String> ingredients;
        public final List<String> containsAllergens;
        public final List<String> mayContainAllergens;

        private ParsedLabel(List<String> ingredients, List<String> containsAllergens, List<String> mayContainAllergens) {
            this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
            this.containsAllergens = Collections.unmodifiableList(new ArrayList<>(containsAllergens));
            this.mayContainAllergens = Collections.unmodifiableList(new ArrayList<>(mayContainAllergens));
        }
    }

    private static final class AllergenSection {
        final int start;
        final int contentStart;
        final boolean advisory;

        AllergenSection(int start, int contentStart, boolean advisory) {
            this.start = start;
            this.contentStart = contentStart;
            this.advisory = advisory;
        }
    }

    private IngredientTextParser() {
    }

    public static ParsedLabel parseLabel(String text) {
        if (text == null) {
            return new ParsedLabel(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        String source = stripPromptMetadata(text);
        List<AllergenSection> sections = findAllergenSections(source);
        List<String> containsAllergens = new ArrayList<>();
        List<String> mayContainAllergens = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            AllergenSection section = sections.get(i);
            int sectionEnd = i + 1 < sections.size() ? sections.get(i + 1).start : source.length();
            String content = source.substring(section.contentStart, sectionEnd);
            content = cutAtStopMarker(content);
            int sentenceEnd = content.indexOf('.');
            if (sentenceEnd >= 0) content = content.substring(0, sentenceEnd);
            addUniqueAllergens(section.advisory ? mayContainAllergens : containsAllergens, content);
        }

        String ingredientSection = extractIngredientSection(source, sections);
        return new ParsedLabel(
                parseIngredientsFromSection(ingredientSection),
                containsAllergens,
                mayContainAllergens
        );
    }

    public static List<String> parseIngredientCandidates(String text) {
        return parseLabel(text).ingredients;
    }

    public static String stripPromptMetadata(String text) {
        if (text == null) return "";
        String cleaned = repairCommonMojibake(text)
                .replaceAll("(?i)\\bresponse_language\\s*[:=]\\s*[^,\\n]+,?\\s*", " ");

        Matcher payloadMatcher = PAYLOAD_FIELD.matcher(cleaned);
        StringBuffer unwrapped = new StringBuffer();
        while (payloadMatcher.find()) {
            payloadMatcher.appendReplacement(unwrapped, Matcher.quoteReplacement(payloadMatcher.group(2)));
        }
        payloadMatcher.appendTail(unwrapped);

        cleaned = METADATA_LINE.matcher(unwrapped.toString()).replaceAll(" ");
        cleaned = PROMPT_INSTRUCTION_LINE.matcher(cleaned).replaceAll(" ");
        return cleaned.replaceAll("(?im)^\\s*product data\\s*:\\s*$", " ").trim();
    }

    public static String trimToLikelyIngredientList(String text) {
        if (text == null) return "";
        String source = stripPromptMetadata(text);
        return extractIngredientSection(source, findAllergenSections(source));
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
        if (isNonIngredientLine(lower, cleaned)) return "";
        return cleaned;
    }

    private static String extractIngredientSection(String source, List<AllergenSection> sections) {
        Matcher ingredientMatcher = INGREDIENT_HEADING.matcher(source);
        boolean hasIngredientHeading = ingredientMatcher.find();
        int ingredientStart = hasIngredientHeading ? ingredientMatcher.end() : 0;
        int ingredientEnd = source.length();
        for (AllergenSection section : sections) {
            if (section.start >= ingredientStart) {
                ingredientEnd = Math.min(ingredientEnd, section.start);
                break;
            }
        }

        String ingredientSection = source.substring(Math.min(ingredientStart, ingredientEnd), ingredientEnd);
        if (hasIngredientHeading) ingredientSection = stripLeadingEncodingArtifact(ingredientSection);
        return cutAtStopMarker(ingredientSection).trim();
    }

    private static String stripLeadingEncodingArtifact(String text) {
        String trimmed = text == null ? "" : text.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace <= 0) return trimmed;

        String token = trimmed.substring(0, firstSpace);
        String lower = token.toLowerCase(Locale.US);
        boolean encodingArtifact = token.indexOf('\u20ac') >= 0
                || token.indexOf('\ufffd') >= 0
                || token.indexOf('\u25a1') >= 0
                || lower.startsWith("â")
                || lower.equals("afd");
        return encodingArtifact ? trimmed.substring(firstSpace + 1).trim() : trimmed;
    }

    private static List<String> parseIngredientsFromSection(String source) {
        List<String> parsed = new ArrayList<>();
        for (String part : splitAtTopLevel(source)) {
            String cleaned = cleanIngredientText(part);
            if (!cleaned.isEmpty()) parsed.add(cleaned);
        }
        List<String> normalized = IngredientNormalizer.normalizeAndDeduplicate(parsed);
        if (normalized.size() <= MAX_INGREDIENTS) return normalized;
        return new ArrayList<>(normalized.subList(0, MAX_INGREDIENTS));
    }

    private static List<String> splitAtTopLevel(String source) {
        List<String> parts = new ArrayList<>();
        if (source == null || source.isEmpty()) return parts;

        int depth = 0;
        int start = 0;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (current == '(' || current == '[' || current == '{') {
                depth++;
            } else if (current == ')' || current == ']' || current == '}') {
                if (depth > 0) depth--;
            } else if (depth == 0 && (current == ',' || current == ';' || current == '\n' || current == '\r' || current == '\u2022')) {
                if (i > start) parts.add(source.substring(start, i));
                start = i + 1;
            }
        }
        if (start < source.length()) parts.add(source.substring(start));
        return parts;
    }

    private static List<AllergenSection> findAllergenSections(String source) {
        List<AllergenSection> sections = new ArrayList<>();
        Matcher matcher = ALLERGEN_HEADING.matcher(source);
        while (matcher.find()) {
            String heading = matcher.group(2).toLowerCase(Locale.US);
            sections.add(new AllergenSection(matcher.start(), matcher.end(), heading.startsWith("may")));
        }
        return sections;
    }

    private static void addUniqueAllergens(List<String> target, String content) {
        if (content == null) return;
        String normalizedContent = content
                .replaceFirst("(?i)^traces[\\t ]+of[\\t ]+", "")
                .replaceFirst("(?i)^the[\\t ]+following[\\t ]+", "");
        Set<String> seen = new LinkedHashSet<>();
        for (String existing : target) seen.add(normalizeKey(existing));

        for (String grouped : splitAtTopLevel(normalizedContent)) {
            String[] joined = grouped.split("(?i)\\s+and\\s+");
            for (String candidate : joined) {
                String cleaned = candidate
                        .replaceFirst("(?i)^[a-z]{2}:", "")
                        .replaceAll("^[\\s\\-*:]+|[\\s.!]+$", "")
                        .replaceAll("\\s+", " ")
                        .trim();
                if (cleaned.length() >= 2 && seen.add(normalizeKey(cleaned))) target.add(cleaned);
            }
        }
    }

    private static String cutAtStopMarker(String text) {
        String lower = text.toLowerCase(Locale.US);
        String[] stopMarkers = {
                "nutrition facts",
                "nutrition information",
                "nutritional information",
                "supplement facts",
                "serving size",
                "servings per container",
                "amount per serving",
                "phenylketonurics:",
                "phenylketonurics",
                "directions",
                "preparation instructions",
                "cooking instructions",
                "usage instructions",
                "storage instructions",
                "how to prepare",
                "keep refrigerated",
                "refrigerate after opening",
                "shake well",
                "warning",
                "warnings",
                "caution",
                "plain text",
                "characters",
                "windows (crlf",
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
            if (index >= 0 && (stop == -1 || index < stop)) stop = index;
        }
        return stop >= 0 ? text.substring(0, stop) : text;
    }

    private static String repairCommonMojibake(String text) {
        return text
                .replace("\u00e2\u20ac\u0153", "\"")
                .replace("\u00e2\u20ac\u009d", "\"")
                .replace("\u00e2\u20ac\u02dc", "'")
                .replace("\u00e2\u20ac\u2122", "'");
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

        if (MEASUREMENT.matcher(lower).matches() && !lower.contains("vitamin")) return true;

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
