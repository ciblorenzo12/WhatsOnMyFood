package com.ciblorenzo.whatsonmyfood.analysis;

import android.graphics.Bitmap;

import java.util.List;

public class BitwiseAnalysisService {

    public interface AnalysisCallback {
        void onResult(String jsonResult);
        void onError(Throwable error);
    }

    public void analyzeWithRules(String productData, List<String> rules, AnalysisCallback callback) {
        analyzeWithRules(productData, rules, null, callback);
    }

    public void analyzeWithRules(String productData, List<String> rules, Bitmap bitmap, AnalysisCallback callback) {
        BitwiseAiCore.startAnalysis(null, productData, bitmap, new BitwiseAiCore.AiCallback() {
            @Override
            public void onResult(String jsonResult) {
                callback.onResult(jsonResult);
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    public void cancelActiveCall() {
        // Calls are owned by the screen lifecycle through BitwiseAiCore.
    }
}
