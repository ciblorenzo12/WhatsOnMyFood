package com.ciblorenzo.whatsonmyfood.analysis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Ensures only complete, source-backed product explanations reach the UI or local cache. */
public final class AiExplanationResponseValidator {

    private static final Set<String> VERDICTS = new HashSet<>(Arrays.asList(
            "HEALTHY", "NOT_HEALTHY", "APPROVED", "NOT_APPROVED", "REVIEW"
    ));

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
            JsonElement parsed = JsonParser.parseString(json == null ? "" : json.trim());
            if (!parsed.isJsonObject()) return invalid("Bitwise returned an invalid explanation.");

            JsonObject root = parsed.getAsJsonObject();
            String verdict = stringValue(root, "verdict").toUpperCase(Locale.US);
            if (!VERDICTS.contains(verdict)) {
                return invalid("Bitwise returned an unsupported product verdict.");
            }

            String summary = stringValue(root, "summary");
            String readableSummary = summary
                    .replaceAll("(?i)<br\\s*/?>", " ")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (readableSummary.length() < 40 || !endsWithSentencePunctuation(readableSummary)) {
                return invalid("Bitwise returned an incomplete explanation. Please try again.");
            }

            JsonArray findings = arrayValue(root, "findings");
            if (findings == null || findings.size() > 5) {
                return invalid("Bitwise returned invalid finding details.");
            }
            for (JsonElement finding : findings) {
                if (finding == null || !finding.isJsonObject()) {
                    return invalid("Bitwise returned invalid finding details.");
                }
            }

            JsonArray sources = arrayValue(root, "sources");
            if (!hasDisplayableSource(sources)) {
                return invalid("Bitwise could not verify this explanation with a usable source.");
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
            if (url.startsWith("https://") || url.startsWith("http://")) return true;
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
