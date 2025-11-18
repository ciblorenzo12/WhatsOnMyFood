package com.example.myapplication;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OpenFoodFactsApiClient implements BarcodeApiClient {

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public OpenFoodFactsApiClient(File cacheDir) {
        Cache cache = new Cache(new File(cacheDir, "http-cache"), 10 * 1024 * 1024); // 10MB cache
        this.client = new OkHttpClient.Builder()
                .cache(cache)
                .build();
    }

    @Override
    public ProductResponse getProduct(String barcode) throws IOException {
        Request request = new Request.Builder()
                .url("https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            return gson.fromJson(response.body().charStream(), ProductResponse.class);
        }
    }
}
