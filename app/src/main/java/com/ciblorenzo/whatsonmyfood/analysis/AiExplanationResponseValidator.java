package com.ciblorenzo.whatsonmyfood.analysis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Ensures only complete, source-backed product explanations reach the UI or local cache. */
public final class AiExplanationResponseValidator {

    private static final Set<String> VERDICTS = new HashSet<>(Arrays.asList(
            "HEALTHY", "NOT_HEALTHY", "APPROVED", "NOT_APPROVED", "REVIEW"
    ));
    private static final Set<String> IMPACTS = new HashSet<>(Arrays.asList(
            "positive", "neutral", "warning", "negative"
    ));
    private static final Pattern[] UNSAFE_MEDICAL_CLAIMS = new Pattern[]{
            Pattern.compile("\\b(?:this product|this ingredient|it)\\s+(?:will|can)\\s+(?:cure|treat|prevent|reverse)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:cures?|treats?|prevents?|reverses?)\\s+(?:diabetes|cancer|hypertension|disease|a medical condition)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\byou\\s+(?:have|likely have|are suffering from)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:stop|start|change|skip)\\s+(?:taking\\s+)?(?:your\\s+)?(?:medication|medicine|insulin|prescription)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bguaranteed\\s+(?:weight loss|health benefit|blood sugar control)\\b", Pattern.CASE_INSENSITIVE)
    };

    public static final class Result {
        public final boolean usable;
        public final String json;
        public final String error;

        private Result(boolean usable, String json, String error) {
            this.usable = usable;
            this.json = json;
            this.error = error;
        }
    }

    private AiExplanationResponseValidator() {
    }

    public static Result validate(String json) {
        try {
            String raw = json == null ? "" : json.trim();
            if (raw.isEmpty() || raw.matches("(?is)^\\s*(?:<!doctype\\s+html|<html|<body|<script)\\b.*")) {
                return invalid("Bitwise returned an invalid explanation.");
            }
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) return invalid("Bitwise returned an invalid explanation.");

            JsonObject root = parsed.getAsJsonObject();
            String verdict = stringValue(root, "verdict").toUpperCase(Locale.US);
            if (!VERDICTS.contains(verdict)) {
                return invalid("Bitwise returned an unsupported product verdict.");
            }

            String summary = stringValue(root, "summary");
            String unsupportedTags = summary
                    .replaceAll("(?i)</?b>", "")
                    .replaceAll("(?i)<br\\s*/?>", "");
            String readableSummary = summary
                    .replaceAll("(?i)<br\\s*/?>", " ")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (unsupportedTags.matches("(?s).*<[^>]+>.*")
                    || readableSummary.length() < 80
                    || !endsWithSentencePunctuation(readableSummary)
                    || !summary.toLowerCase(Locale.US).contains("why this rating")
                    || !summary.toLowerCase(Locale.US).contains("portion guidance")
                    || !summary.toLowerCase(Locale.US).contains("fact check")) {
                return invalid("Bitwise returned an incomplete explanation. Please try again.");
            }

            JsonArray ingredients = arrayValue(root, "ingredients");
            if (ingredients == null || ingredients.size() > 100) {
                return invalid("Bitwise returned invalid ingredient details.");
            }
            for (JsonElement ingredient : ingredients) {
                if (ingredient == null || !ingredient.isJsonPrimitive()
                        || !ingredient.getAsJsonPrimitive().isString()
                        || ingredient.getAsString().trim().isEmpty()) {
                    return invalid("Bitwise returned invalid ingredient details.");
                }
            }

            JsonArray findings = arrayValue(root, "findings");
            if (findings == null || findings.size() > 5) {
                return invalid("Bitwise returned invalid finding details.");
            }
            for (JsonElement finding : findings) {
                if (finding == null || !finding.isJsonObject()) {
                    return invalid("Bitwise returned invalid finding details.");
                }
                JsonObject findingObject = finding.getAsJsonObject();
                if (stringValue(findingObject, "rule").isEmpty()
                        || stringValue(findingObject, "explanation").isEmpty()
                        || !IMPACTS.contains(stringValue(findingObject, "impact").toLowerCase(Locale.US))) {
                    return invalid("Bitwise returned invalid finding details.");
                }
            }

            JsonArray sources = arrayValue(root, "sources");
            if (!hasDisplayableSource(sources)) {
                return invalid("Bitwise could not verify this explanation with a usable source.");
            }

            StringBuilder claimText = new StringBuilder(stringValue(root, "verdict_reason"))
                    .append(' ').append(summary);
            for (JsonElement finding : findings) {
                claimText.append(' ').append(stringValue(finding.getAsJsonObject(), "explanation"));
            }
            for (Pattern unsafeClaim : UNSAFE_MEDICAL_CLAIMS) {
                if (unsafeClaim.matcher(claimText).find()) {
                    return invalid("Bitwise returned an unsafe medical claim.");
                }
            }

            return new Result(true, root.toString(), "");
        } catch (Exception ignored) {
            return invalid("Bitwise returned an invalid explanation.");
        }
    }

    private static boolean hasDisplayableSource(JsonArray sources) {
        if (sources == null) return false;
        for (JsonElement sourceElement : sources) {
            if (sourceElement == null || !sourceElement.isJsonObject()) continue;
            String url = stringValue(sourceElement.getAsJsonObject(), "url").toLowerCase(Locale.US);
            if (url.startsWith("https://")) return true;
        }
        return false;
    }

    private static boolean endsWithSentencePunctuation(String value) {
        return value.endsWith(".") || value.endsWith("!") || value.endsWith("?");
    }

    private static String stringValue(JsonObject root, String key) {
        JsonElement value = root.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString().trim()
                : "";
    }

    private static JsonArray arrayValue(JsonObject root, String key) {
        JsonElement value = root.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static Result invalid(String error) {
        return new Result(false, "", error);
    }
}
