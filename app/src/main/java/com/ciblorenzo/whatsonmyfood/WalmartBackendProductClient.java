package com.ciblorenzo.whatsonmyfood;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WalmartBackendProductClient implements BarcodeApiClient {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    public ProductResponse getProduct(String barcode) throws IOException {
        String baseUrl = BuildConfig.RETAILER_BACKEND_BASE_URL;
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }

        HttpUrl base = HttpUrl.parse(baseUrl.trim());
        if (base == null) {
            return null;
        }

        HttpUrl url = base.newBuilder()
                .addPathSegment("api")
                .addPathSegment("retail")
                .addPathSegment("products")
                .addPathSegment(barcode)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Walmart backend lookup failed with HTTP " + response.code());
            }
            return gson.fromJson(response.body().string(), ProductResponse.class);
        }
    }
}
