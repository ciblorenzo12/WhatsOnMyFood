package com.example.myapplication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OpenFoodFactsApiClient {

    private static final String API_URL = "https://world.openfoodfacts.org/api/v0/product/";
    private final OkHttpClient client;
    private final Gson gson;

    public OpenFoodFactsApiClient(File cacheDir) {
        Cache cache = new Cache(new File(cacheDir, "http_cache"), 10 * 1024 * 1024); // 10MB cache

        this.client = new OkHttpClient.Builder()
                .cache(cache)
                .build();

        this.gson = new GsonBuilder().create();
    }

    public ProductResponse getProduct(String barcode) throws IOException {
        Request request = new Request.Builder()
                .url(API_URL + barcode + ".json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return gson.fromJson(response.body().charStream(), ProductResponse.class);
        }
    }
}
