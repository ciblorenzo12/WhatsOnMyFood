package com.example.myapplication.api;

import android.graphics.Bitmap;
import okhttp3.Call;

public class SecureAiService {

    private static final RunpodLlmClient llmClient = new RunpodLlmClient();

    public interface AiCallback {
        void onResult(String result);
        void onError(String error);
    }

    /**
     * Centralized method to analyze product using the FastAPI Runpod Proxy.
     */
    public static Call analyzeProduct(String prompt, AiCallback callback) {
        return analyzeProduct(prompt, null, callback);
    }

    public static Call analyzeProduct(String prompt, Bitmap bitmap, AiCallback callback) {
        return llmClient.askBitwise(prompt, bitmap, new RunpodLlmClient.LlmCallback() {
            @Override
            public void onResult(String text) {
                callback.onResult(text);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Simplified deep explanation call.
     */
    public static Call getDeepExplanation(String productName, String nutrition, AiCallback callback) {
        String prompt = "Act as Bitwise AI (Food Scientist). Friendly summary of this product. Use English.\n\n" +
                        "Product: " + productName + "\n" +
                        "Data: " + nutrition + "\n\n" +
                        "Format:\n" +
                        "- Verdict: 1 simple sentence.\n" +
                        "- Key Insights: 2-3 bullet points.\n" +
                        "- Bitwise Tip: 1 advice sentence.";
        
        return analyzeProduct(prompt, callback);
    }
}
