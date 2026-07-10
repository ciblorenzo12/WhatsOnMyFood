package com.ciblorenzo.whatsonmyfood;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import okhttp3.Cache;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenFoodFactsApiClient implements BarcodeApiClient {

    private static final String TAG = "OpenFoodFactsApiClient";
    private static final String BASE_HOST = "world.openfoodfacts.org";
    private static final String PRODUCT_FIELDS = String.join(",",
            "status",
            "product_name",
            "product_name_en",
            "product_name_es",
            "product_name_fr",
            "generic_name",
            "generic_name_en",
            "generic_name_es",
            "generic_name_fr",
            "brands",
            "quantity",
            "image_url",
            "image_front_url",
            "labels",
            "labels_en",
            "labels_es",
            "labels_fr",
            "labels_tags",
            "packaging",
            "packaging_en",
            "packaging_es",
            "packaging_fr",
            "categories",
            "categories_en",
            "categories_es",
            "categories_fr",
            "countries",
            "countries_tags",
            "lang",
            "serving_size",
            "nutriscore_grade",
            "nova_group",
            "ecoscore_grade",
            "nutriments",
            "ingredients",
            "ingredients_text",
            "ingredients_text_en",
            "ingredients_text_es",
            "ingredients_text_fr",
            "ingredients_text_with_allergens",
            "ingredients_text_with_allergens_en",
            "ingredients_text_with_allergens_es",
            "ingredients_text_with_allergens_fr",
            "allergens",
            "allergens_tags"
    );
    private final OkHttpClient client;
    private final Gson gson;
    private final String languageCode;

    public OpenFoodFactsApiClient(File cacheDir) {
        this(cacheDir, "en");
    }

    public OpenFoodFactsApiClient(File cacheDir, String languageCode) {
        int cacheSize = 10 * 1024 * 1024; // 10 MB
        Cache cache = new Cache(cacheDir, cacheSize);

        this.client = new OkHttpClient.Builder()
                .cache(cache)
                .build();
        this.gson = new Gson();
        this.languageCode = normalizeLanguageCode(languageCode);
    }

    @Override
    public ProductResponse getProduct(String barcode) throws IOException {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host(BASE_HOST)
                .addPathSegment("api")
                .addPathSegment("v2")
                .addPathSegment("product")
                .addPathSegment(barcode + ".json")
                .addQueryParameter("lc", languageCode)
                .addQueryParameter("cc", "us")
                .addQueryParameter("fields", PRODUCT_FIELDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Language", languageCode)
                .addHeader("User-Agent", "WhatsOnMyFood/1.0 (Android)")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "API call failed with code: " + response.code());
                return null;
            }
            String responseBody = response.body().string();
            return gson.fromJson(responseBody, ProductResponse.class);
        }
    }

    public SubmissionResult updateIngredients(
            String barcode,
            String originalIngredients,
            String englishIngredients,
            String sourceLanguage,
            String userId,
            String password
    ) throws IOException {
        String url = "https://world.openfoodfacts.org/cgi/product_jqm2.pl";

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("code", barcode)
                .add("ingredients_text_en", englishIngredients)
                .add("user_id", userId)
                .add("password", password)
                .add("comment", "Original ingredients and English translation verified in What's On My Food");
        String sourceCode = ingredientLanguageCode(sourceLanguage);
        if (sourceCode != null && !sourceCode.equals("en")) {
            formBuilder.add("ingredients_text_" + sourceCode, originalIngredients);
        } else {
            formBuilder.add("ingredients_text", originalIngredients);
        }
        RequestBody formBody = formBuilder.build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "WhatsOnMyFood/1.0.1 (Android; https://github.com/ciblorenzo12/WhatsOnMyFood)")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to submit product, code: " + response.code());
                return SubmissionResult.failure(responseMessage(responseBody, response.code()));
            }

            Log.d(TAG, "Ingredient update response: " + responseBody);
            try {
                JsonObject body = JsonParser.parseString(responseBody).getAsJsonObject();
                int status = body.has("status") ? body.get("status").getAsInt() : 0;
                String message = body.has("status_verbose")
                        ? body.get("status_verbose").getAsString()
                        : "Open Food Facts did not accept the update.";
                return status == 1 ? SubmissionResult.success(message) : SubmissionResult.failure(message);
            } catch (Exception parseError) {
                Log.e(TAG, "Could not parse Open Food Facts response", parseError);
                return SubmissionResult.failure("Open Food Facts returned an unexpected response.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error submitting product", e);
            if (e instanceof IOException) throw (IOException) e;
            return SubmissionResult.failure("The update could not be submitted.");
        }
    }

    private String responseMessage(String responseBody, int statusCode) {
        try {
            JsonObject body = JsonParser.parseString(responseBody).getAsJsonObject();
            if (body.has("status_verbose")) return body.get("status_verbose").getAsString();
            if (body.has("error")) return body.get("error").getAsString();
        } catch (Exception ignored) {
            // Fall through to a concise HTTP error.
        }
        return "Open Food Facts rejected the update (HTTP " + statusCode + ").";
    }

    static String ingredientLanguageCode(String languageTag) {
        if (languageTag == null) return null;
        String code = languageTag.trim().toLowerCase(Locale.US).split("-")[0];
        return code.matches("[a-z]{2,3}") ? code : null;
    }

    public static final class SubmissionResult {
        public final boolean successful;
        public final String message;

        private SubmissionResult(boolean successful, String message) {
            this.successful = successful;
            this.message = message;
        }

        static SubmissionResult success(String message) {
            return new SubmissionResult(true, message);
        }

        static SubmissionResult failure(String message) {
            return new SubmissionResult(false, message);
        }
    }

    private String normalizeLanguageCode(String value) {
        if (value == null) return "en";
        String normalized = value.trim().toLowerCase(Locale.US);
        if (normalized.startsWith("es")) return "es";
        if (normalized.startsWith("fr")) return "fr";
        return "en";
    }
}
