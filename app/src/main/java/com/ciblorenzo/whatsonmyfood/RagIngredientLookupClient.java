package com.ciblorenzo.whatsonmyfood;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RagIngredientLookupClient {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    private final Gson gson = new Gson();

    public ProductResponse getIngredients(String barcode, String productName, String brand) throws IOException {
        String baseUrl = BuildConfig.RETAILER_BACKEND_BASE_URL;
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }

        HttpUrl base = HttpUrl.parse(baseUrl.trim());
        if (base == null) {
            return null;
        }

        HttpUrl.Builder urlBuilder = base.newBuilder()
                .addPathSegment("api")
                .addPathSegment("retail")
                .addPathSegment("products")
                .addPathSegment(barcode)
                .addPathSegment("ingredients")
                .addPathSegment("rag");

        if (productName != null && !productName.trim().isEmpty()) {
            urlBuilder.addQueryParameter("productName", productName.trim());
        }
        if (brand != null && !brand.trim().isEmpty()) {
            urlBuilder.addQueryParameter("brand", brand.trim());
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Accept", "application/json")
                .addHeader("X-APP-TOKEN", BuildConfig.BITWISE_APP_TOKEN)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("RAG ingredient lookup failed with HTTP " + response.code());
            }
            try {
                return gson.fromJson(response.body().string(), ProductResponse.class);
            } catch (JsonParseException error) {
                throw new IOException("RAG ingredient lookup returned invalid JSON", error);
            }
        }
    }
}
