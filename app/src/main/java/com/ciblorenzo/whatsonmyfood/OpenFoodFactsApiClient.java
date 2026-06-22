package com.ciblorenzo.whatsonmyfood;

import android.util.Log;

import com.google.gson.Gson;

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

    public boolean addProduct(String barcode, String productName, String brand, String ingredients) throws IOException {
        String url = "https://world.openfoodfacts.org/cgi/product_jqm2.pl";

        RequestBody formBody = new FormBody.Builder()
                .add("code", barcode)
                .add("product_name", productName)
                .add("brands", brand)
                .add("ingredients_text", ingredients)
                .add("lang", languageCode)
                // Required for authenticated write operations, but we can try without for now.
                // .add("user_id", "YOUR_USER_ID") 
                // .add("password", "YOUR_PASSWORD")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("User-Agent", "WhatsOnMyFood - Android - Version 1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to submit product, code: " + response.code());
                return false;
            }

            String responseBody = response.body().string();
            Log.d(TAG, "Add product response: " + responseBody);

            // The response is a simple JSON object, check the status field.
            return responseBody.contains("\"status\":1");
        } catch (Exception e) {
            Log.e(TAG, "Error submitting product", e);
            return false;
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
