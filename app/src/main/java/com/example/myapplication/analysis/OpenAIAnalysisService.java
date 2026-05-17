package com.example.myapplication.analysis;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.example.myapplication.BitmapUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenAIAnalysisService {
    private static final String API_KEY = "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";
    private final OkHttpClient httpClient;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public OpenAIAnalysisService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
    }

    public interface AnalysisCallback {
        void onResult(String jsonResult);
        void onError(Throwable t);
    }

    public void analyzeWithRules(String productData, List<String> rules, Bitmap bitmap, AnalysisCallback callback) {
        executor.execute(() -> {
            try {
                if (API_KEY.contains("XXXXXXXXXXXX")) {
                    callback.onError(new Exception("OpenAI API Key not configured"));
                    return;
                }

                StringBuilder rulesPrompt = new StringBuilder("Analyze the following food product based on these specific rules:\n");
                for (String rule : rules) {
                    rulesPrompt.append("- ").append(rule).append("\n");
                }

                String instructions = "You are an expert nutrition and AI vision assistant. Your task is to analyze the provided image and OCR text to identify a food product and its nutritional quality.\n\n" +
                        "SCENARIOS:\n" +
                        "1. BARCODE/TEXT SCAN: If the OCR text contains a product name or ingredients, use that as the primary source of truth, but verify it against the image if available.\n" +
                        "2. IMAGE ONLY: If the OCR text is poor or missing but an image is provided, identify the product and its typical ingredients based on visual recognition.\n" +
                        "3. NON-FOOD ITEM: If the user scans something that is CLEARLY NOT a food product (like a shoe, a cat, or a computer), provide a funny, witty, or sarcastic response in the 'summary' field explaining why you can't eat it. Set 'score' to 0 and 'ingredients' to [].\n\n" +
                        "OUTPUT REQUIREMENTS:\n" +
                        "- Identify the product name and brand.\n" +
                        "- Provide a complete list of ingredients.\n" +
                        "- Estimate nutritional information (Energy, Fat, Sugars, Protein, Salt).\n" +
                        "- Calculate a refined health score (0-100) based on the provided rules.\n" +
                        "- In the 'summary', explain your reasoning and how rules were applied. Be witty if it's not food.\n" +
                        "- In the 'findings' array, link violations to exact 'triggering_ingredient' text.\n\n" +
                        "Output strictly JSON: {" +
                        "\"product_name\": \"\", " +
                        "\"brand\": \"\", " +
                        "\"ingredients\": [], " +
                        "\"nutrition\": {\"energy\": \"\", \"fat\": \"\", \"sugars\": \"\", \"protein\": \"\", \"salt\": \"\"}, " +
                        "\"score\": 0, " +
                        "\"summary\": \"\", " +
                        "\"findings\": [{\"rule\": \"\", \"explanation\": \"\", \"impact\": \"negative/positive/neutral\", \"triggering_ingredient\": \"\"}]" +
                        "}";

                String fullPrompt = rulesPrompt.toString() + instructions + "\n\nProduct Data:\n" + productData;

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "gpt-4o-mini");
                
                JSONArray messages = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                
                JSONArray contents = new JSONArray();
                
                // Text part
                JSONObject textContent = new JSONObject();
                textContent.put("type", "text");
                textContent.put("text", fullPrompt);
                contents.put(textContent);
                
                // Image part
                if (bitmap != null) {
                    byte[] bytes = BitmapUtils.bitmapToByteArray(bitmap);
                    String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);
                    
                    JSONObject imageContent = new JSONObject();
                    imageContent.put("type", "image_url");
                    JSONObject imageUrl = new JSONObject();
                    imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
                    imageContent.put("image_url", imageUrl);
                    contents.put(imageContent);
                }
                
                userMessage.put("content", contents);
                messages.put(userMessage);
                requestBody.put("messages", messages);
                requestBody.put("max_tokens", 1500);

                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .header("Authorization", "Bearer " + API_KEY)
                        .post(RequestBody.create(requestBody.toString(), JSON))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        Log.e("OpenAI", "Error Code: " + response.code());
                        Log.e("OpenAI", "Error Body: " + body);
                        callback.onError(new Exception("OpenAI Error " + response.code()));
                        return;
                    }
                    
                    JSONObject responseJson = new JSONObject(body);
                    if (responseJson.has("choices")) {
                        String resultText = responseJson.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        callback.onResult(cleanJson(resultText));
                    } else {
                        callback.onError(new Exception("Invalid OpenAI Response Format"));
                    }
                }
            } catch (Exception e) {
                Log.e("OpenAI", "Exception: ", e);
                callback.onError(e);
            }
        });
    }

    public void analyzeIngredients(String rawText, Bitmap bitmap, AnalysisCallback callback) {
        analyzeWithRules(rawText, new ArrayList<>(), bitmap, callback);
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        String clean = text.replaceAll("(?s)```(?:json)?\\s*(.*?)\\s*```", "$1").trim();
        int start = clean.indexOf("{");
        int end = clean.lastIndexOf("}");
        if (start != -1 && end != -1) {
            return clean.substring(start, end + 1);
        }
        return clean;
    }
}
