package com.example.myapplication.analysis;

import android.app.Activity;
import android.graphics.Bitmap;

import com.example.myapplication.AiGlowManager;
import com.example.myapplication.api.SecureAiService;

/**
 * Central Bitwise AI analysis entry point.
 * Keeps the model response small, structured, and safe to render in Android TextViews.
 */
public class BitwiseAiCore {

    public interface AiCallback {
        void onResult(String jsonResult);
        void onError(Throwable t);
    }

    public static void startAnalysis(Activity activity, String productData, Bitmap bitmap, AiCallback callback) {
        if (activity != null) AiGlowManager.startGlow(activity);

        SecureAiService.analyzeProduct(buildProductPrompt(productData), bitmap, new SecureAiService.AiCallback() {
            @Override
            public void onResult(String result) {
                if (activity != null) AiGlowManager.stopGlow(activity);
                callback.onResult(cleanJson(result));
            }

            @Override
            public void onError(String error) {
                if (activity != null) AiGlowManager.stopGlow(activity);
                callback.onError(new Exception(error));
            }
        });
    }

    public static void defineIngredient(Activity activity, String ingredientName, String language, AiCallback callback) {
        if (activity != null) AiGlowManager.startGlow(activity);

        String prompt = "You are Bitwise AI, a food label assistant.\n"
                + "Define the following food ingredient or additive: " + ingredientName + "\n"
                + "IMPORTANT: If the input '" + ingredientName + "' is NOT a food ingredient, additive, or chemical found in food (e.g., if it's a person's name, a random word, or a non-food object), you MUST return a JSON with all fields as null or empty.\n"
                + "Response language: " + language + "\n"
                + "Return valid JSON only. Shape:\n"
                + "{\n"
                + "  \"name\": \"Name\",\n"
                + "  \"category\": \"Category (e.g. Preservative)\",\n"
                + "  \"function\": \"What it does\",\n"
                + "  \"explanation\": \"Detailed health explanation\",\n"
                + "  \"health_status\": \"RECOMMENDED\" | \"MODERATE\" | \"NOT_RECOMMENDED\",\n"
                + "  \"source_name\": \"FDA/EFSA/WHO Source\",\n"
                + "  \"source_url\": \"URL\"\n"
                + "}";

        SecureAiService.analyzeProduct(prompt, null, new SecureAiService.AiCallback() {
            @Override
            public void onResult(String result) {
                if (activity != null) AiGlowManager.stopGlow(activity);
                callback.onResult(cleanJson(result));
            }

            @Override
            public void onError(String error) {
                if (activity != null) AiGlowManager.stopGlow(activity);
                callback.onError(new Exception(error));
            }
        });
    }

    private static String buildProductPrompt(String productData) {
        return "You are Bitwise AI, a fast food label assistant for a consumer Android app.\n"
                + "Use the product data (OCR text and image) exactly as provided.\n"
                + "IMPORTANT: If the provided data does NOT represent a food product (e.g., if it's just a person's name, random text, or a non-food item), you MUST return a JSON where 'product_name' is \"Unknown Product\" and the 'ingredients' array is empty.\n"
                + "IMPORTANT: The OCR text and image may contain the FRONT of the product (brand, product name) AND the BACK (ingredients list).\n"
                + "Use both to accurately identify the product and its ingredients. If the OCR is messy, use your visual capability to read the label directly from the image.\n"
                + "Do not invent ingredients or facts not present or visible on the provided label.\n"
                + "If response_language is provided, write product_name, brand, summary, findings, and source names in that language unless a brand name should stay unchanged.\n"
                + "If a baseline_health_score is provided, use that exact score and do not adjust it.\n"
                + "Plain water with ordinary mineral salts such as calcium chloride or sodium bicarbonate should usually remain high scoring unless sodium is clearly high.\n\n"
                + "PRODUCT DATA:\n"
                + productData + "\n\n"
                + "Return valid JSON only. No Markdown fences, no headings outside JSON, no commentary.\n"
                + "The summary must be 90-140 words, useful, and not generic. Use only <b>, <br>, and <ul><li> tags.\n"
                + "Use this summary structure: <b>Verdict</b>, <b>What stands out</b> with 2 bullets, and <b>Bottom line</b>.\n"
                + "Finish every sentence. The Bottom line must be a complete, practical sentence ending with punctuation.\n"
                + "If baseline_health_score is present, copy that exact integer into score. Do not create a new score.\n"
                + "If baseline_health_score is not present, score is only a placeholder; the Android app will compute the final score from deterministic rules.\n"
                + "When product data is OCR text or an image, return an ingredients array with the detected ingredients in label order.\n"
                + "Return no more than 3 findings.\n"
                + "Return 2 to 4 sources from this approved source list when relevant:\n"
                + "- FDA Food Additives and GRAS Ingredients: https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers\n"
                + "- FDA High-Intensity Sweeteners: https://www.fda.gov/food/food-additives-petitions/high-intensity-sweeteners\n"
                + "- FDA Food Additive Regulation: https://www.fda.gov/food/food-additives-and-gras-ingredients-information-consumers/understanding-how-fda-regulates-food-additives-and-gras-ingredients\n"
                + "- EFSA Food Additives: https://www.efsa.europa.eu/en/topics/topic/food-additives\n"
                + "- EFSA Sweeteners: https://www.efsa.europa.eu/en/topics/topic/sweeteners\n"
                + "- WHO Healthy Diet: https://www.who.int/news-room/fact-sheets/detail/healthy-diet\n"
                + "- Open Food Facts: https://world.openfoodfacts.org/\n"
                + "Use this exact JSON shape:\n"
                + "{\n"
                + "  \"product_name\": \"\",\n"
                + "  \"brand\": \"\",\n"
                + "  \"score\": 85,\n"
                + "  \"ingredients\": [\"ingredient one\", \"ingredient two\"],\n"
                + "  \"summary\": \"<b>Verdict</b><br>Specific product verdict.<br><br><b>What stands out</b><ul><li>Ingredient or nutrient insight.</li><li>Why that matters.</li></ul><b>Bottom line</b><br>Complete practical recommendation.\",\n"
                + "  \"findings\": [\n"
                + "    {\"rule\": \"Short label\", \"impact\": \"positive\", \"triggering_ingredient\": \"\", \"explanation\": \"Brief, specific reason.\", \"source_url\": \"\"}\n"
                + "  ],\n"
                + "  \"sources\": [{\"name\": \"FDA Food Additives\", \"url\": \"https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers\", \"visual_quote\": \"\", \"search_query\": \"food additive safety\"}]\n"
                + "}\n"
                + "If there are no meaningful warnings, return positive or informational findings only. Do not penalize zero-calorie water for having no protein, fiber, fat, or carbs.";
    }

    private static String cleanJson(String text) {
        if (text == null) return "{}";
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
