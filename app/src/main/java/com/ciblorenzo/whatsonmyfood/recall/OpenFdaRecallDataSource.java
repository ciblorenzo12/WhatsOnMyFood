package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.api.ResilientRequestPolicy;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** HTTPS client for the FDA Recall Enterprise System data published through openFDA. */
public final class OpenFdaRecallDataSource implements FoodRecallDataSource {
    static final String ENDPOINT = "https://api.fda.gov/food/enforcement.json";
    static final int RESULT_LIMIT = 100;
    private static final int MAX_ATTEMPTS = 2;
    private final OkHttpClient client;

    public OpenFdaRecallDataSource() {
        this(new OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build());
    }

    OpenFdaRecallDataSource(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public FoodRecallDataset search(Product product) throws IOException {
        HttpUrl url = buildUrl(product);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "WhatsOnMyFood-Android")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 404) {
                    return new FoodRecallDataset(Collections.emptyList(), "");
                }
                if (isTransient(response.code()) && attempt + 1 < MAX_ATTEMPTS) {
                    ResilientRequestPolicy.waitBeforeRetry();
                    continue;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    throw new FoodRecallServiceException(
                            "openFDA returned HTTP " + response.code(),
                            isTransient(response.code())
                    );
                }
                return parseResponse(response.body().string());
            }
        }
        throw new FoodRecallServiceException("openFDA is temporarily unavailable", true);
    }

    HttpUrl buildUrl(Product product) {
        HttpUrl endpoint = HttpUrl.parse(ENDPOINT);
        if (endpoint == null) throw new IllegalStateException("Invalid openFDA endpoint");
        HttpUrl.Builder builder = endpoint.newBuilder()
                .addQueryParameter("search", FoodRecallQueryBuilder.build(product))
                .addQueryParameter("sort", "report_date:desc")
                .addQueryParameter("limit", String.valueOf(RESULT_LIMIT));
        if (BuildConfig.OPENFDA_API_KEY != null && !BuildConfig.OPENFDA_API_KEY.trim().isEmpty()) {
            builder.addQueryParameter("api_key", BuildConfig.OPENFDA_API_KEY.trim());
        }
        return builder.build();
    }

    static FoodRecallDataset parseResponse(String body) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) throw new IOException("openFDA returned an invalid response");
            JsonObject root = parsed.getAsJsonObject();
            String lastUpdated = "";
            if (root.has("meta") && root.get("meta").isJsonObject()) {
                lastUpdated = text(root.getAsJsonObject("meta"), "last_updated");
            }
            List<FoodRecallRecord> records = new ArrayList<>();
            JsonArray results = root.has("results") && root.get("results").isJsonArray()
                    ? root.getAsJsonArray("results")
                    : new JsonArray();
            for (JsonElement element : results) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                records.add(new FoodRecallRecord(
                        text(item, "recall_number"),
                        text(item, "product_description"),
                        text(item, "recalling_firm"),
                        text(item, "classification"),
                        text(item, "reason_for_recall"),
                        text(item, "code_info"),
                        text(item, "report_date"),
                        text(item, "status")
                ));
            }
            return new FoodRecallDataset(records, lastUpdated);
        } catch (RuntimeException error) {
            throw new IOException("openFDA returned malformed JSON", error);
        }
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean isTransient(int statusCode) {
        return statusCode == 429 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }
}
