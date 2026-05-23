package com.example.myapplication.analysis;

import android.graphics.Bitmap;
import okhttp3.Call;
import java.util.List;

/**
 * Legacy wrapper for BitwiseAiCore to maintain backward compatibility.
 */
public class OpenAIAnalysisService {

    public interface AnalysisCallback {
        void onResult(String jsonResult);
        void onError(Throwable t);
    }

    private Call activeCall;

    public void analyzeWithRules(String productData, List<String> rules, AnalysisCallback callback) {
        analyzeWithRules(productData, rules, null, callback);
    }

    public void analyzeWithRules(String productData, List<String> rules, Bitmap bitmap, AnalysisCallback callback) {
        // Delegate to the new centralized Core
        BitwiseAiCore.startAnalysis(null, productData, bitmap, new BitwiseAiCore.AiCallback() {
            @Override
            public void onResult(String jsonResult) {
                callback.onResult(jsonResult);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void cancelActiveCall() {
        // Logic handled by individual calls now, but kept for signature compatibility
    }
}
