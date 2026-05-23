package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.analysis.AnalysisResult;
import com.example.myapplication.analysis.AnalysisResultAdapter;
import com.example.myapplication.analysis.AiSummaryFormatter;
import com.example.myapplication.analysis.OpenAIAnalysisService;
import com.example.myapplication.analysis.ProductAnalysisReport;
import com.example.myapplication.analysis.rules.RuleEngine;
import com.example.myapplication.utils.GlassMotion;
import com.example.myapplication.utils.LinkHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IngredientAnalysisActivity extends BaseActivity {

    public static final String EXTRA_INGREDIENTS_TEXT = "extra_ingredients_text";
    public static final String EXTRA_IMAGE_BYTES = "extra_image_bytes";
    public static final String EXTRA_BARCODE = "extra_barcode";
    private static final String AI_CACHE_PREFIX = "BITWISE_AI_CACHE_V2:";

    private ProductWithDetails detectedProduct;
    private Button savePantryButton;
    private ProductRepository productRepository;
    private com.google.firebase.auth.FirebaseUser currentUser;
    private Bitmap capturedBitmap;
    private View loadingOverlay;
    private String analysisInputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_analysis);

        Toolbar toolbar = findViewById(R.id.analysis_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView healthScoreView = findViewById(R.id.health_score_text_view);
        TextView rawIngredientsView = findViewById(R.id.raw_ingredients_text_view);
        RecyclerView analysisRecyclerView = findViewById(R.id.analysis_recycler_view);
        analysisRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button doneButton = findViewById(R.id.done_button);
        savePantryButton = findViewById(R.id.save_pantry_button);
        loadingOverlay = findViewById(R.id.loading_overlay);
        ProgressBar progressBar = findViewById(R.id.analysis_progress_bar);
        GlassMotion.enter(findViewById(R.id.product_summary_card), 0L);
        GlassMotion.enter(findViewById(R.id.quick_stats_row), 80L);
        GlassMotion.enter(findViewById(R.id.raw_ingredients_card), 140L);
        GlassMotion.attachPress(doneButton);
        GlassMotion.attachPress(savePantryButton);

        productRepository = new ProductRepository(getApplication());
        currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        doneButton.setOnClickListener(v -> finish());
        savePantryButton.setOnClickListener(v -> saveToPantry());

        healthScoreView.setText(R.string.analyzing);
        rawIngredientsView.setText(R.string.identifying_ingredients);

        String initialText = getIntent().getStringExtra(EXTRA_INGREDIENTS_TEXT);

        byte[] imageBytes = getIntent().getByteArrayExtra(EXTRA_IMAGE_BYTES);
        if (imageBytes != null) {
            capturedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ImageView productImageView = findViewById(R.id.product_image_view);
            if (productImageView != null) {
                productImageView.setImageBitmap(capturedBitmap);
            }
        }

        List<String> rules = new RuleEngine().getRuleDescriptions();
        String analysisPrompt = "response_language: " + LanguageManager.getLanguageName(this) + "\n"
                + (initialText != null ? initialText : "Product Image Analysis");
        analyzeWithAI(analysisPrompt, rules, capturedBitmap, healthScoreView, rawIngredientsView, analysisRecyclerView, progressBar);
    }

    private void analyzeWithAI(String prompt, List<String> rules, Bitmap bitmap, TextView healthScoreView, TextView rawIngredientsView, RecyclerView analysisRecyclerView, ProgressBar progressBar) {
        analysisInputText = prompt;
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        new OpenAIAnalysisService().analyzeWithRules(prompt, rules, bitmap, new OpenAIAnalysisService.AnalysisCallback() {
            @Override
            public void onResult(String jsonResult) {
                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    AiGlowManager.stopGlow(IngredientAnalysisActivity.this);
                    applyAiResults(jsonResult, healthScoreView, rawIngredientsView, analysisRecyclerView);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    AiGlowManager.stopGlow(IngredientAnalysisActivity.this);
                    healthScoreView.setText("N/A");
                    Toast.makeText(IngredientAnalysisActivity.this, "Bitwise AI is busy. Try again soon.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void applyAiResults(String json, TextView scoreView, TextView rawIngredientsView, RecyclerView recyclerView) {
        try {
            JSONObject obj = new JSONObject(json);

            String productName = obj.optString("product_name", "").trim();
            String brand = obj.optString("brand", "").trim();
            String summary = com.example.myapplication.analysis.AiSummaryFormatter.format(obj.optString("summary", ""));

            if (productName.isEmpty() || productName.equalsIgnoreCase("Name")) productName = "Scanned Product";
            if (brand.isEmpty() || brand.equalsIgnoreCase("Brand")) brand = "Brand Unknown";
            String fakeBarcode = "ai-" + System.currentTimeMillis();

            JSONArray sourcesArr = obj.optJSONArray("sources");
            android.text.SpannableStringBuilder sourcesBuilder = new android.text.SpannableStringBuilder();
            if (sourcesArr != null) {
                for (int i = 0; i < sourcesArr.length(); i++) {
                    JSONObject sourceObj = sourcesArr.optJSONObject(i);
                    if (sourceObj != null) {
                        String sName = sourceObj.optString("name", "Source");
                        String url = sourceObj.optString("url", "");
                        String query = sourceObj.optString("search_query", "");

                        int start = sourcesBuilder.length();
                        sourcesBuilder.append("\u2022 ").append(sName).append("\n");
                        int end = sourcesBuilder.length() - 1;

                        if (!url.isEmpty()) {
                            String visualQuote = sourceObj.optString("visual_quote", "");
                            sourcesBuilder.setSpan(new android.text.style.ClickableSpan() {
                                @Override
                                public void onClick(@androidx.annotation.NonNull View widget) {
                                    LinkHandler.openLink(IngredientAnalysisActivity.this, url, sName, visualQuote);
                                }
                            }, start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            sourcesBuilder.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimary)), start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }
            final CharSequence finalSources = sourcesBuilder;

            TextView nameView = findViewById(R.id.product_name_text_view);
            TextView brandView = findViewById(R.id.product_brand_text_view);
            if (nameView != null && !productName.equalsIgnoreCase("Name")) nameView.setText(productName);
            if (brandView != null && !brand.equalsIgnoreCase("Brand")) brandView.setText(brand);

            // 2. Ingredients
            List<Ingredient> ingredientList = readIngredientsForScoring(obj, fakeBarcode);

            detectedProduct = new ProductWithDetails();
            detectedProduct.product = new Product(fakeBarcode, productName, brand, null, null, null, null, null, null, null, null, null, buildAiInsightCache(summary, sourcesArr), null);
            detectedProduct.ingredients = ingredientList;

            // 3. Nutrition & Findings
            List<AnalysisResult> aiResults = new ArrayList<>();
            JSONArray findings = obj.optJSONArray("findings");
            if (findings != null) {
                for (int i = 0; i < findings.length(); i++) {
                    JSONObject f = findings.optJSONObject(i);
                    if (f != null) {
                        AnalysisResult res = new AnalysisResult(f.optString("rule"),
                            f.optString("impact").equalsIgnoreCase("negative") ? AnalysisResult.WarningLevel.SEVERE : AnalysisResult.WarningLevel.INFO,
                            0, f.optString("triggering_ingredient"), f.optString("explanation"));
                        res.setSourceUrl(f.optString("source_url", ""));
                        res.setVisualQuote(f.optString("visual_quote", ""));
                        aiResults.add(res);
                    }
                }
            }

            ProductAnalysisReport ruleReport = new RuleEngine().analyze(detectedProduct);
            int score = ruleReport.getOverallScore();
            detectedProduct.product.healthScore = score;

            scoreView.setText(String.format(Locale.getDefault(), "%d/100", score));
            scoreView.setTextColor(ContextCompat.getColor(this, R.color.ai_accent));

            List<AnalysisResult> displayResults = new ArrayList<>(ruleReport.getResults());
            displayResults.addAll(aiResults);
            recyclerView.setAdapter(new AnalysisResultAdapter(displayResults));

            ProductAnalysisReport displayReport = new ProductAnalysisReport(score, displayResults);
            displayHighlightedIngredients(detectedProduct, displayReport, rawIngredientsView);

            if (!summary.isEmpty()) {
                View summaryLayout = findViewById(R.id.ai_summary_layout);
                TextView summaryTextView = findViewById(R.id.ai_summary_text_view);
                TextView sourcesTextView = findViewById(R.id.ai_sources_text_view);
                View sourcesDivider = findViewById(R.id.ai_sources_divider);
                View sourcesLabel = findViewById(R.id.ai_sources_label);

                if (summaryLayout != null && summaryTextView != null) {
                    summaryLayout.setVisibility(View.VISIBLE);
                    GlassMotion.enter(summaryLayout, 80L);
                    animateText(summaryTextView, summary);

                    if (sourcesTextView != null && finalSources.length() > 0) {
                        sourcesTextView.setText(finalSources);
                        sourcesTextView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                        sourcesTextView.setVisibility(View.VISIBLE);
                        if (sourcesDivider != null) sourcesDivider.setVisibility(View.VISIBLE);
                        if (sourcesLabel != null) sourcesLabel.setVisibility(View.VISIBLE);
                    } else if (sourcesTextView != null) {
                        sourcesTextView.setVisibility(View.GONE);
                        if (sourcesDivider != null) sourcesDivider.setVisibility(View.GONE);
                        if (sourcesLabel != null) sourcesLabel.setVisibility(View.GONE);
                    }
                }
            }

            if (savePantryButton != null) {
                savePantryButton.setVisibility(View.VISIBLE);
                GlassMotion.enter(savePantryButton, 160L);
            }

        } catch (Exception e) {
            AiGlowManager.stopGlow(this);
            // Cleaner fallback if JSON fails
            String cleanedText = json.replaceAll("\"[a-z_]+\":", "").replaceAll("[{}\\[\\]\"]", "").trim();
            rawIngredientsView.setText(cleanedText);
        }
    }

    private List<Ingredient> readIngredientsForScoring(JSONObject obj, String barcode) {
        List<Ingredient> ingredients = new ArrayList<>();
        JSONArray ingredientArray = obj.optJSONArray("ingredients");
        if (ingredientArray != null) {
            for (int i = 0; i < ingredientArray.length(); i++) {
                String text = cleanIngredientText(ingredientArray.optString(i, ""));
                if (!text.isEmpty()) {
                    ingredients.add(new Ingredient(barcode, text, ingredients.size()));
                }
            }
        }

        if (ingredients.isEmpty() && analysisInputText != null) {
            String source = trimToLikelyIngredientList(analysisInputText);
            String[] parts = source.split("[,;\\n\\u2022]");
            for (String part : parts) {
                String text = cleanIngredientText(part);
                if (!text.isEmpty() && ingredients.size() < 80) {
                    ingredients.add(new Ingredient(barcode, text, ingredients.size()));
                }
            }
        }

        return ingredients;
    }

    private String trimToLikelyIngredientList(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.US);
        String[] markers = {"ingredients:", "ingredients", "contains:"};
        int start = -1;
        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index >= 0 && (start == -1 || index < start)) {
                start = index + marker.length();
            }
        }
        return start >= 0 ? text.substring(start) : text;
    }

    private String cleanIngredientText(String text) {
        if (text == null) return "";
        String cleaned = text
                .replaceAll("\\[[a-zA-Z:-]+\\]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() < 2) return "";

        String lower = cleaned.toLowerCase(Locale.US);
        if (lower.startsWith("nutrition facts")
                || lower.startsWith("serving size")
                || lower.startsWith("calories")
                || lower.startsWith("total fat")
                || lower.startsWith("barcode")) {
            return "";
        }
        return cleaned;
    }

    private void animateText(TextView textView, String text) {
        final int[] index = {0};
        final long delay = 16;
        final int charsPerTick = 10;
        Handler handler = new Handler(Looper.getMainLooper());

        final android.text.Spanned htmlText = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_COMPACT);
        final String rawText = htmlText.toString();

        textView.setText("");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index[0] < rawText.length()) {
                    index[0] = Math.min(rawText.length(), index[0] + charsPerTick);
                    textView.setText(htmlText.subSequence(0, index[0]));
                    handler.postDelayed(this, delay);
                } else {
                    textView.setText(htmlText);
                    AiGlowManager.stopGlow(IngredientAnalysisActivity.this);
                    makeLinksClickable(textView);
                }
            }
        }, delay);
    }

    private String buildAiInsightCache(String summary, JSONArray sources) {
        try {
            JSONObject cache = new JSONObject();
            cache.put("summary", summary != null ? summary : "");
            cache.put("sources", sources != null ? sources : new JSONArray());
            return AI_CACHE_PREFIX + cache.toString();
        } catch (Exception e) {
            return summary != null ? summary : "";
        }
    }

    private String formatAiSummary(String raw) {
        if (raw == null) return "";
        if (raw.length() >= 0) return AiSummaryFormatter.format(raw);

        return raw
                .replace("###", "<br><br><b>")
                .replace("**", "")
                .replaceAll("(?m)^-\\s+", "\u2022 ")
                .replace("\n\n", "<br><br>")
                .replace("\n", "<br>")
                .replace("Overview", "<b>📋 Product Overview</b><br>")
                .replace("Health Benefits", "<br><br><b>✅ Potential Benefits</b><br>")
                .replace("Recommendations", "<br><br><b>📍 Recommendations</b><br>")
                .replace("Conclusion", "<br><br><b>🧠 Final Summary</b><br>");
    }

    private void makeLinksClickable(TextView textView) {
        CharSequence currentText = textView.getText();
        String text = currentText.toString();
        SpannableStringBuilder spannableBuilder = new SpannableStringBuilder(currentText);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("((http|https)://[a-zA-Z0-9\\-.]+\\.[a-zA-Z]{2,3}(/\\S*)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            final String url = matcher.group();
            spannableBuilder.setSpan(new android.text.style.ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { LinkHandler.openLink(IngredientAnalysisActivity.this, url, "Scientific Source", url); }
            }, matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(spannableBuilder);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void displayHighlightedIngredients(ProductWithDetails productDetails, ProductAnalysisReport report, TextView ingredientsTextView) {
        if (productDetails.ingredients == null || productDetails.ingredients.isEmpty()) {
            ingredientsTextView.setText("No ingredients identified.");
            return;
        }

        android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder();
        for (int i = 0; i < productDetails.ingredients.size(); i++) {
            Ingredient ingredient = productDetails.ingredients.get(i);
            int start = builder.length();
            builder.append(ingredient.text);

            for (AnalysisResult res : report.getResults()) {
                if (res.getTriggeringIngredient() != null && ingredient.text.toLowerCase().contains(res.getTriggeringIngredient().toLowerCase())) {
                    int color = res.getLevel() == AnalysisResult.WarningLevel.POSITIVE ? 0x3300FF00 : 0x33FF0000;
                    builder.setSpan(new android.text.style.BackgroundColorSpan(color), start, builder.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, builder.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
            }
            if (i < productDetails.ingredients.size() - 1) builder.append(", ");
        }
        ingredientsTextView.setText(builder);
    }

    private void saveToPantry() {
        if (detectedProduct == null || currentUser == null) return;
        
        boolean hasIngredients = detectedProduct.ingredients != null && !detectedProduct.ingredients.isEmpty();
        
        if (detectedProduct.product == null || !detectedProduct.product.isValid() || !hasIngredients) {
            Toast.makeText(this, "Cannot save unknown or invalid product. No ingredients identified.", Toast.LENGTH_LONG).show();
            return;
        }
        savePantryButton.setEnabled(false);
        savePantryButton.setText("Saving...");
        new Thread(() -> {
            try {
                if (capturedBitmap != null) {
                    String fileName = "product_" + detectedProduct.product.barcode + ".jpg";
                    java.io.File file = new java.io.File(getFilesDir(), fileName);
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                        capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        detectedProduct.product.imageUrl = "file://" + file.getAbsolutePath();
                    } catch (java.io.IOException ignored) {}
                }
                AppDatabase.getDatabase(this).productDao().insertProductWithDetails(detectedProduct);
                AppDatabase.getDatabase(this).productDao().insertPantry(new Pantry(detectedProduct.product.barcode, currentUser.getUid()));
                runOnUiThread(() -> {
                    Toast.makeText(this, "Added to Pantry!", Toast.LENGTH_SHORT).show();
                    savePantryButton.setText("Saved \u2713");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    savePantryButton.setEnabled(true);
                    savePantryButton.setText("Add to Pantry");
                });
            }
        }).start();
    }
}
