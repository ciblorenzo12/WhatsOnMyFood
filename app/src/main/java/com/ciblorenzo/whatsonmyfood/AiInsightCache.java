package com.ciblorenzo.whatsonmyfood;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Stable local representation for an accepted Bitwise explanation and its sources. */
public final class AiInsightCache {

    public static final String PREFIX = "BITWISE_AI_CACHE_V11:";
    private static final String FAMILY_PREFIX = "BITWISE_AI_CACHE_";

    public static final class Decoded {
        public final boolean usable;
        public final String summary;
        public final String sourcesJson;

        private Decoded(boolean usable, String summary, String sourcesJson) {
            this.usable = usable;
            this.summary = summary;
            this.sourcesJson = sourcesJson;
        }
    }

    private AiInsightCache() {
    }

    public static String encode(String summary, String sourcesJson) {
        String cleanSummary = summary == null ? "" : summary.trim();
        JsonArray sources = parseSources(sourcesJson);
        if (cleanSummary.isEmpty() || sources.size() == 0) return "";

        JsonObject cache = new JsonObject();
        cache.addProperty("summary", cleanSummary);
        cache.add("sources", sources);
        return PREFIX + cache;
    }

    public static Decoded decode(String stored) {
        if (stored == null || !stored.startsWith(FAMILY_PREFIX)) return empty();
        int separator = stored.indexOf(':');
        if (separator < 0 || separator + 1 >= stored.length()) return empty();

        try {
            JsonObject root = JsonParser.parseString(stored.substring(separator + 1)).getAsJsonObject();
            JsonElement summaryValue = root.get("summary");
            String summary = summaryValue != null && summaryValue.isJsonPrimitive()
                    ? summaryValue.getAsString().trim()
                    : "";
            JsonArray sources = root.has("sources") && root.get("sources").isJsonArray()
                    ? root.getAsJsonArray("sources")
                    : new JsonArray();
            return summary.isEmpty() || sources.size() == 0
                    ? empty()
                    : new Decoded(true, summary, sources.toString());
        } catch (Exception ignored) {
            return empty();
        }
    }

    private static JsonArray parseSources(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json == null ? "[]" : json);
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        } catch (Exception ignored) {
            return new JsonArray();
        }
    }

    private static Decoded empty() {
        return new Decoded(false, "", "[]");
    }
}
