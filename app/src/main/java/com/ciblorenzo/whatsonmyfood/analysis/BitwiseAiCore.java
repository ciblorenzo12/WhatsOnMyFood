package com.ciblorenzo.whatsonmyfood.analysis;

import android.app.Activity;
import android.graphics.Bitmap;

import com.ciblorenzo.whatsonmyfood.AiGlowManager;
import com.ciblorenzo.whatsonmyfood.api.SecureAiService;

import okhttp3.Call;

/**
 * Central Bitwise AI analysis entry point.
 * Keeps the model response small, structured, and safe to render in Android TextViews.
 */
public class BitwiseAiCore {

    public interface AiCallback {
        void onResult(String jsonResult);
        void onError(Throwable t);
    }

    public static Call startAnalysis(Activity activity, String productData, Bitmap bitmap, AiCallback callback) {
        if (activity != null) AiGlowManager.startGlow(activity);

        return SecureAiService.analyzeProduct(buildProductPrompt(productData), bitmap, new SecureAiService.AiCallback() {
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
                + "First correct likely spelling mistakes in the search term and return the canonical ingredient name. "
                + "For example, 'ethritol' should be understood as 'erythritol'.\n"
                + "IMPORTANT: Only return all fields as null or empty when the input is clearly not a food ingredient, additive, or chemical after considering likely spelling mistakes.\n"
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
        PromptContext context = parsePromptContext(productData);
        return "You are Bitwise AI, a careful ingredient-label assistant for a consumer Android app.\n"
                + "You can analyze packaged foods, drinks, supplements, oral-care products, and personal-care products.\n"
                + "Use the OCR text and image carefully. Do not invent label claims or scientific facts.\n"
                + "Use the voice of a trusted nutrition educator: warm, direct, and easy to understand. Do not claim to be a doctor, dietitian, or clinician, and do not diagnose or give personalized medical advice.\n"
                + "Explain what the label suggests without fear or judgment. One ingredient or product does not determine a person's health; give balanced context and a practical next step.\n"
                + "If an allergy, pregnancy, medical condition, or medication question is relevant, encourage the shopper to consult a qualified healthcare professional rather than guessing.\n"
                + "Response language: " + context.responseLanguage + "\n"
                + "IMPORTANT: The OCR text and attached image may contain the front of the product, a barcode, nutrition facts, and the back ingredient list.\n"
                + "Use the image and OCR together: identify product_name, brand, and product_type from visible front-label/package text when possible.\n"
                + "If DETECTED INGREDIENT LABEL is non-empty, it is the source of truth for the ingredients array. Parse and correct that label text before using any product-name inference.\n"
                + "Treat placeholders such as Not listed, Unknown, None, N/A, and Not available as an empty ingredient list.\n"
                + "Correct obvious OCR truncations in ingredient names when the meaning is clear, such as 'organic coconut wa' to 'organic coconut water'. Do not return partial ingredient fragments.\n"
                + "If DETECTED INGREDIENT LABEL is empty, first try ingredient-panel text, INCI text, supplement facts, or a clearly labeled ingredients section from OCR TEXT or the image.\n"
                + "Front-label text is for product identity only; put ONLY true label ingredients in the ingredients array.\n"
                + "Never include metadata, language fields, brand names, product names, net weight, marketing claims, UI text, directions, warnings, or store-card text as ingredients.\n"
                + "If the product_name and brand are known but the ingredient panel is missing, use the exact product identity, barcode, category, quantity, and nutrition facts to infer the likely ingredients for that exact variant.\n"
                + "For inferred ingredients, include only ingredients reasonably associated with that exact product variant. Do not invent percentages or label claims. Clearly state in the summary and verdict_reason that the list is inferred and should be verified on the package.\n"
                + "Return each ingredient as its own item in the ingredients array. Never return the entire comma-separated ingredient list as one array item. Product-identity inference cannot have high confidence because no package ingredient panel was verified.\n"
                + "For single-ingredient products such as plain coconut water, bottled water, plain milk, plain oats, or plain rice, infer the simple ingredient from the product name when the identity is clear.\n"
                + "Only use REVIEW when neither a readable ingredient panel nor a confident product identity is available.\n"
                + "For foods, decide whether this is HEALTHY or NOT_HEALTHY for everyday use by looking for disqualifying ingredients and nutrition signals, not by assigning a score.\n"
                + "For personal-care and oral-care products, decide whether it is APPROVED or NOT_APPROVED based on ingredient concerns and intended use; still explain good and bad ingredients clearly.\n"
                + "Treat ordinary base ingredients such as water, rose water, glycerin, aloe, citric acid, salt used in small amounts, and mineral salts as neutral or positive unless the label shows a specific concern.\n"
                + "Reserve negative findings for clear issues: artificial colors, high added sugar, trans-fat sources, high-fructose corn syrup, multiple ultra-processed additives, high sodium/sugar, concerning preservatives, fragrance allergens, or ingredients with evidence-based cautions.\n"
                + "When giving a NOT_HEALTHY or NOT_APPROVED verdict, say which concerning ingredients or nutrition signals are present and why they matter. Do not justify the verdict by listing ingredients that are absent.\n\n"
                + "DETECTED INGREDIENT LABEL:\n"
                + context.detectedIngredientLabel + "\n\n"
                + "OCR TEXT:\n"
                + context.productText + "\n\n"
                + "Return valid JSON only. No Markdown fences, no headings outside JSON, no commentary.\n"
                + "The summary must be 100-160 words, specific, calm, and written like a nutrition-savvy human talking to a shopper. Use only <b> and <br> tags.\n"
                + "Organize the summary with exactly three helpful sections: <b>Why this rating</b>, <b>Portion guidance</b>, and <b>Fact check</b>. Keep the writing natural rather than sounding like a form.\n"
                + "In Why this rating, name the actual ingredients and nutrition values that support HEALTHY or NOT_HEALTHY. Explain what they mean instead of referring vaguely to app rules or database classifications.\n"
                + "In Portion guidance, use the serving size printed on the label when available and give a practical frequency or pairing suggestion. Never invent grams, cups, pieces, or medical limits. If serving data is missing, say to start with the package serving and adjust for the rest of the meal.\n"
                + "In Fact check, briefly state which important claim was checked against the grounded sources and mention any uncertainty. The clickable sources will appear directly below the summary.\n"
                + "Do not write phrases like I do not see, does not contain, no artificial colors, no partially hydrogenated oils, or no high-intensity sweeteners. Focus on the ingredients and nutrition facts that are actually present.\n"
                + "Do not sound like a generic AI template. Avoid phrases like readable label text, concrete basis, does not prove, high-concern ingredients, clean-looking ingredient list, backup result, fallback service, no major watch items, or highly nutritious.\n"
                + "Name the product type when possible, explain the meaningful ingredients in everyday scientific language, and make the advice practical rather than trying to force a positive recommendation.\n"
                + "Important oil distinction: sunflower, canola, soybean, corn, and other liquid vegetable oils are not trans fats and must not make a product NOT_HEALTHY on their own. Mention them only when useful in the overall label context. Palm and palm-kernel oils can be discussed alongside the actual saturated-fat value. Never give a negative verdict from an oil name alone.\n"
                + "If the product is simple, say so directly. Do not make a simple product sound suspicious just because it is not nutritionally complete.\n"
                + "Finish every sentence. Each of the three summary sections must be complete, practical, and end with punctuation.\n"
                + "Return no more than 5 findings. Each finding must be specific, evidence-based, and tied to an actual ingredient or nutrient signal.\n"
                + "The backend will attach the exact verified web sources after the analysis. Use only that evidence for scientific claims and source_url values. Never invent or substitute a citation.\n"
                + "Use this exact JSON shape:\n"
                + "{\n"
                + "  \"product_name\": \"\",\n"
                + "  \"brand\": \"\",\n"
                + "  \"product_type\": \"food | beverage | supplement | oral_care | personal_care | unknown\",\n"
                + "  \"verdict\": \"HEALTHY | NOT_HEALTHY | APPROVED | NOT_APPROVED | REVIEW\",\n"
                + "  \"verdict_reason\": \"One short reason for the verdict.\",\n"
                + "  \"ingredients\": [\"ingredient one\", \"ingredient two\"],\n"
                + "  \"ingredients_source\": \"label | product_identity | unknown\",\n"
                + "  \"ingredient_confidence\": \"high | medium | low\",\n"
                + "  \"summary\": \"<b>Why this rating</b><br>A label-specific explanation.<br><br><b>Portion guidance</b><br>A practical amount or serving-size-based suggestion.<br><br><b>Fact check</b><br>What Gemini verified and any uncertainty.\",\n"
                + "  \"findings\": [\n"
                + "    {\"rule\": \"Short label\", \"impact\": \"positive | neutral | warning | negative\", \"triggering_ingredient\": \"\", \"explanation\": \"Brief, specific reason with scientific context.\", \"source_url\": \"\"}\n"
                + "  ],\n"
                + "  \"sources\": [{\"name\": \"FDA Food Additives\", \"url\": \"https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers\", \"visual_quote\": \"\", \"search_query\": \"food additive safety\"}]\n"
                + "}\n"
                + "If there are no meaningful warnings, return positive or neutral findings only. Do not penalize water-like products for having no protein, fiber, fat, or carbs.";
    }

    private static PromptContext parsePromptContext(String productData) {
        String responseLanguage = "English";
        String productText = productData != null ? productData : "";
        StringBuilder detectedIngredientLabel = new StringBuilder();

        String[] lines = productText.split("\\r?\\n");
        StringBuilder cleaned = new StringBuilder();
        boolean skippingMarker = false;
        boolean readingIngredientLabel = false;
        for (String line : lines) {
            String lower = line.trim().toLowerCase();
            if (lower.startsWith("response_language:")) {
                String value = line.substring(line.indexOf(':') + 1).trim();
                if (!value.isEmpty()) responseLanguage = value;
                continue;
            }
            if (lower.startsWith("scan_mode:")
                    || lower.startsWith("image_attached:")
                    || lower.startsWith("available_barcode:")
                    || lower.startsWith("task:")) {
                readingIngredientLabel = false;
                continue;
            }
            if (lower.equals("detected_ingredient_label:")) {
                readingIngredientLabel = true;
                skippingMarker = false;
                continue;
            }
            if (lower.equals("product_ocr_text:") || lower.equals("ocr_text:")) {
                readingIngredientLabel = false;
                skippingMarker = true;
                continue;
            }
            if (!skippingMarker && (lower.startsWith("baseline_health_score:") || lower.startsWith("product data:"))) {
                continue;
            }
            if (readingIngredientLabel) {
                detectedIngredientLabel.append(line).append('\n');
                continue;
            }
            cleaned.append(line).append('\n');
        }

        return new PromptContext(responseLanguage, cleaned.toString().trim(), detectedIngredientLabel.toString().trim());
    }

    private static final class PromptContext {
        final String responseLanguage;
        final String productText;
        final String detectedIngredientLabel;

        PromptContext(String responseLanguage, String productText, String detectedIngredientLabel) {
            this.responseLanguage = responseLanguage;
            this.productText = productText;
            this.detectedIngredientLabel = detectedIngredientLabel;
        }
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
