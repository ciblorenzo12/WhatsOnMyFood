package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.analysis.AnalysisResult;
import com.example.myapplication.analysis.AnalysisResultAdapter;
import com.example.myapplication.analysis.AiSummaryFormatter;
import com.example.myapplication.analysis.BitwiseAiCore;
import com.example.myapplication.analysis.OpenAIAnalysisService;
import com.example.myapplication.analysis.ProductAnalysisReport;
import com.example.myapplication.analysis.rules.RuleEngine;
import com.example.myapplication.api.SecureAiService;
import com.example.myapplication.utils.GlassMotion;
import com.example.myapplication.utils.LinkHandler;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailsActivity extends BaseActivity {

    public static final String EXTRA_BARCODE = "com.example.myapplication.BARCODE";
    private static final String AI_CACHE_PREFIX = "BITWISE_AI_CACHE_V2:";

    private ProductRepository productRepository;
    private ExecutorService executorService;
    private AppDatabase db;
    private RuleEngine ruleEngine;
    private FirebaseUser currentUser;

    private ImageView productImageView;
    private TextView productNameTextView, productBrandTextView, ingredientsTextView, healthScoreTextView;
    private TextView nutriscoreTextView, novaTextView, ecoscoreTextView, categoriesTextView, packagingTextView, labelsTextView, servingSizeTextView;
    private Button removeFromPantryButton;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private TableLayout nutritionFactsTable;
    private RecyclerView analysisRecyclerView;
    private View aiSummaryLayout;
    private View aiSummaryContainer;
    private View detailsLayout;
    private AiGlowView aiCardGlow;
    private TextView aiSummaryTextView;
    private TextView aiSourcesTextView;
    private View aiSourcesDivider;
    private View aiSourcesLabel;
    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        Toolbar toolbar = findViewById(R.id.product_details_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        productRepository = new ProductRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        db = AppDatabase.getDatabase(this);
        ruleEngine = new RuleEngine();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Not signed in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productImageView = findViewById(R.id.product_image_view);
        productNameTextView = findViewById(R.id.product_name_text_view);
        productBrandTextView = findViewById(R.id.product_brand_text_view);
        ingredientsTextView = findViewById(R.id.ingredients_text_view);
        nutriscoreTextView = findViewById(R.id.nutriscore_text_view);
        novaTextView = findViewById(R.id.nova_text_view);
        ecoscoreTextView = findViewById(R.id.ecoscore_text_view);
        categoriesTextView = findViewById(R.id.categories_text_view);
        packagingTextView = findViewById(R.id.packaging_text_view);
        labelsTextView = findViewById(R.id.labels_text_view);
        servingSizeTextView = findViewById(R.id.serving_size_text_view);
        healthScoreTextView = findViewById(R.id.health_score_text_view);
        removeFromPantryButton = findViewById(R.id.remove_from_pantry_button);
        GlassMotion.attachPress(removeFromPantryButton);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        nutritionFactsTable = findViewById(R.id.nutrition_facts_table);
        analysisRecyclerView = findViewById(R.id.analysis_recycler_view);
        detailsLayout = findViewById(R.id.details_layout);
        aiSummaryLayout = findViewById(R.id.ai_summary_layout);
        aiSummaryContainer = findViewById(R.id.ai_summary_container);
        aiCardGlow = findViewById(R.id.ai_card_glow);
        aiSummaryTextView = findViewById(R.id.ai_summary_text_view);
        aiSourcesTextView = findViewById(R.id.ai_sources_text_view);
        aiSourcesDivider = findViewById(R.id.ai_sources_divider);
        aiSourcesLabel = findViewById(R.id.ai_sources_label);
        loadingOverlay = findViewById(R.id.loading_overlay);
        analysisRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        nutriscoreTextView.setOnClickListener(v -> showScoreExplanation("Nutri-Score", "A nutritional rating system."));
        novaTextView.setOnClickListener(v -> showScoreExplanation("NOVA Group", "A food processing classification."));
        ecoscoreTextView.setOnClickListener(v -> showScoreExplanation("Eco-Score", "An environmental impact rating."));

        String barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        if (barcode != null) {
            loadProductDetails(barcode);
            checkIfProductInPantry(barcode);

            removeFromPantryButton.setOnClickListener(v -> {
                executorService.execute(() -> {
                    db.productDao().deletePantryProduct(barcode, currentUser.getUid());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Removed from Pantry", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK, new Intent().putExtra(PantryActivity.RESULT_DATA_CHANGED, true));
                        finish();
                    });
                });
            });
        } else {
            showErrorState("No barcode provided.", null);
        }
    }

    private void loadProductDetails(String barcode) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        productRepository.getProductByBarcode(barcode, new ProductRepository.RepositoryCallback<ProductRepository.ProductResult>() {
            @Override
            public void onComplete(ProductRepository.ProductResult result) {
                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (result != null && result.productWithDetails != null) {
                        displayProductDetails(result.productWithDetails);
                    } else {
                        showErrorState("Product not found for barcode: " + barcode, barcode);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    showErrorState("Error fetching product: " + e.getMessage(), null);
                });
            }
        });
    }

    private void checkIfProductInPantry(String barcode) {
        executorService.execute(() -> {
            Pantry pantryItem = db.productDao().findPantryItemByBarcode(barcode, currentUser.getUid());
            runOnUiThread(() -> removeFromPantryButton.setVisibility(pantryItem != null ? View.VISIBLE : View.GONE));
        });
    }

    private ProductAnalysisReport currentReport;

    private void displayProductDetails(ProductWithDetails productDetails) {
        collapsingToolbarLayout.setTitle(" ");
        if (productDetails.product.imageUrl != null && !productDetails.product.imageUrl.isEmpty()) {
            Picasso.get().load(productDetails.product.imageUrl).into(productImageView);
        }

        productNameTextView.setText(productDetails.product.productName != null ? productDetails.product.productName : "N/A");
        productBrandTextView.setText(productDetails.product.brands != null ? productDetails.product.brands : "");

        currentReport = ruleEngine.analyze(productDetails);
        if (currentReport != null) {
            applyRuleBasedScore(productDetails, currentReport);
            analysisRecyclerView.setAdapter(new AnalysisResultAdapter(currentReport.getResults()));
            displayHighlightedIngredients(productDetails, currentReport);
        }

        setScoreTextView(nutriscoreTextView, productDetails.product.nutriscoreGrade, "Nutri-Score");
        setScoreTextView(novaTextView, productDetails.product.novaGroup, "NOVA Group");
        setScoreTextView(ecoscoreTextView, productDetails.product.ecoscoreGrade, "Eco-Score");

        categoriesTextView.setText(productDetails.product.categories != null ? productDetails.product.categories : "");
        packagingTextView.setText(productDetails.product.packaging != null ? productDetails.product.packaging : "");
        labelsTextView.setText(productDetails.product.labels != null ? productDetails.product.labels : "");
        servingSizeTextView.setText(productDetails.product.servingSize != null ? productDetails.product.servingSize : "");

        displayNutriments(productDetails.nutriments);
        GlassMotion.enter(detailsLayout, 0L);

        performAiReasoning(productDetails);
    }

    private void performAiReasoning(ProductWithDetails product) {
        if (aiSummaryContainer == null) return;
        if (displayCachedAiInsight(product)) return;

        aiSummaryContainer.setVisibility(View.VISIBLE);
        GlassMotion.enter(aiSummaryContainer, 120L);
        aiSummaryTextView.setText(R.string.bitwise_reasoning);
        AiGlowManager.startGlow(this);

        StringBuilder productData = new StringBuilder();
        productData.append("response_language: ").append(LanguageManager.getLanguageName(this)).append("\n");
        productData.append("Name: ").append(product.product.productName).append("\n");
        productData.append("Brand: ").append(product.product.brands).append("\n");
        productData.append("baseline_health_score: ").append(currentReport != null ? currentReport.getOverallScore() : 100).append("\n");
        productData.append("Ingredients: ").append(formatIngredientsForAi(product)).append("\n");
        if (product.nutriments != null) {
            productData.append("\nNutrition Facts (per 100g): ").append(product.nutriments.toString());
        }

        BitwiseAiCore.startAnalysis(this, productData.toString(), null, new BitwiseAiCore.AiCallback() {
            @Override
            public void onResult(String result) {
                runOnUiThread(() -> {
                    try {
                        org.json.JSONObject obj = new org.json.JSONObject(result);

                        String aiName = obj.optString("product_name", "").trim();
                        String aiBrand = obj.optString("brand", "").trim();
                        String summary = com.example.myapplication.analysis.AiSummaryFormatter.format(obj.optString("summary", ""));

                        if (!aiName.isEmpty()) {
                            productNameTextView.setText(aiName);
                        }

                        if (!aiBrand.isEmpty()) {
                            productBrandTextView.setText(aiBrand);
                        }

                        // Sources
                        org.json.JSONArray sourcesArr = obj.optJSONArray("sources");
                        android.text.SpannableStringBuilder sourcesBuilder = new android.text.SpannableStringBuilder();
                        if (sourcesArr != null) {
                            for (int i = 0; i < sourcesArr.length(); i++) {
                                org.json.JSONObject sourceObj = sourcesArr.optJSONObject(i);
                                if (sourceObj != null) {
                                    String sName = sourceObj.optString("name", "Source");
                                    String url = sourceObj.optString("url", "");
                                    int start = sourcesBuilder.length();
                                    sourcesBuilder.append("\u2022 ").append(sName).append("\n");
                                    int end = sourcesBuilder.length() - 1;
                                    if (!url.isEmpty()) {
                                        String visualQuote = sourceObj.optString("visual_quote", "");
                                        sourcesBuilder.setSpan(new android.text.style.ClickableSpan() {
                                            @Override
                                            public void onClick(@androidx.annotation.NonNull View widget) {
                                                LinkHandler.openLink(ProductDetailsActivity.this, url, sName, visualQuote);
                                            }
                                        }, start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                }
                            }
                        }

                        if (!summary.isEmpty()) {
                            String cachedInsight = buildAiInsightCache(summary, sourcesArr);
                            productRepository.updateProductAiInsight(product.product.barcode, cachedInsight);
                            product.product.aiInsight = cachedInsight;
                            setResult(RESULT_OK, new Intent().putExtra(PantryActivity.RESULT_DATA_CHANGED, true));
                            animateText(aiSummaryTextView, summary);
                            displaySources(sourcesArr);
                        }

                        // Findings
                        org.json.JSONArray aiFindings = obj.optJSONArray("findings");
                        if (aiFindings != null && aiFindings.length() > 0) {
                            List<AnalysisResult> aiResults = new ArrayList<>();
                            for (int i = 0; i < aiFindings.length(); i++) {
                                org.json.JSONObject f = aiFindings.optJSONObject(i);
                                if (f != null) {
                                    AnalysisResult res = new AnalysisResult(f.optString("rule"),
                                        f.optString("impact").equalsIgnoreCase("negative") ? AnalysisResult.WarningLevel.SEVERE : AnalysisResult.WarningLevel.INFO,
                                        0, f.optString("triggering_ingredient"), f.optString("explanation"));
                                    res.setSourceUrl(f.optString("source_url", ""));
                                    res.setVisualQuote(f.optString("visual_quote", ""));
                                    aiResults.add(res);
                                }
                            }
                            if (currentReport != null && currentReport.getResults() != null) {
                                List<AnalysisResult> combinedResults = new ArrayList<>(currentReport.getResults());
                                combinedResults.addAll(aiResults);
                                analysisRecyclerView.setAdapter(new AnalysisResultAdapter(combinedResults));
                            } else {
                                analysisRecyclerView.setAdapter(new AnalysisResultAdapter(aiResults));
                            }
                        }
                    } catch (Exception e) {
                        aiSummaryTextView.setText(result);
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    aiSummaryTextView.setText("Bitwise AI Error: " + t.getMessage());
                });
            }
        });
    }

    private boolean displayCachedAiInsight(ProductWithDetails product) {
        if (product == null
                || product.product == null
                || product.product.aiInsight == null
                || product.product.aiInsight.trim().isEmpty()) {
            return false;
        }

        CachedAiInsight cachedInsight = parseCachedAiInsight(product.product.aiInsight);
        if (isProbablyIncomplete(cachedInsight.summary)) {
            return false;
        }

        aiSummaryContainer.clearAnimation();
        aiSummaryContainer.setVisibility(View.VISIBLE);
        aiSummaryContainer.setAlpha(1f);
        aiSummaryContainer.setTranslationY(0f);
        GlassMotion.enter(aiSummaryContainer, 80L);
        aiSummaryTextView.setText(android.text.Html.fromHtml(cachedInsight.summary, android.text.Html.FROM_HTML_MODE_COMPACT));
        displaySources(cachedInsight.sources);

        if (currentReport != null) {
            applyRuleBasedScore(product, currentReport);
        }

        AiGlowManager.stopGlow(this);
        return true;
    }

    private void applyRuleBasedScore(ProductWithDetails product, ProductAnalysisReport report) {
        if (product == null || product.product == null || report == null) return;

        int ruleScore = report.getOverallScore();
        product.product.healthScore = ruleScore;
        productRepository.updateProductHealthScore(product.product.barcode, ruleScore);
        setResult(RESULT_OK, new Intent().putExtra(PantryActivity.RESULT_DATA_CHANGED, true));

        healthScoreTextView.setText(getString(R.string.health_score_rule_based, ruleScore));
        healthScoreTextView.setTextColor(ContextCompat.getColor(this, R.color.ai_accent));
    }

    private String buildAiInsightCache(String summary, org.json.JSONArray sources) {
        try {
            org.json.JSONObject cache = new org.json.JSONObject();
            cache.put("summary", summary != null ? summary : "");
            cache.put("sources", sources != null ? sources : new org.json.JSONArray());
            return AI_CACHE_PREFIX + cache.toString();
        } catch (Exception e) {
            return summary != null ? summary : "";
        }
    }

    private CachedAiInsight parseCachedAiInsight(String storedInsight) {
        if (storedInsight != null && storedInsight.startsWith(AI_CACHE_PREFIX)) {
            try {
                org.json.JSONObject cache = new org.json.JSONObject(storedInsight.substring(AI_CACHE_PREFIX.length()));
                return new CachedAiInsight(cache.optString("summary", ""), cache.optJSONArray("sources"));
            } catch (Exception ignored) {
            }
        }
        return new CachedAiInsight(storedInsight != null ? storedInsight : "", null);
    }

    private boolean isProbablyIncomplete(String html) {
        if (html == null || html.trim().isEmpty()) {
            return true;
        }
        String text = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim();
        return !(text.endsWith(".") || text.endsWith("!") || text.endsWith("?"));
    }

    private void displaySources(org.json.JSONArray sources) {
        if (aiSourcesTextView == null) return;

        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (sources != null) {
            for (int i = 0; i < sources.length(); i++) {
                org.json.JSONObject sourceObj = sources.optJSONObject(i);
                if (sourceObj == null) continue;

                String name = sourceObj.optString("name", "Source").trim();
                String url = sourceObj.optString("url", "").trim();
                String visualQuote = sourceObj.optString("visual_quote", "");
                if (url.isEmpty()) continue;

                int start = builder.length();
                builder.append("\u2022 ").append(name.isEmpty() ? "Source" : name).append("\n");
                int end = builder.length() - 1;
                builder.setSpan(new android.text.style.ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        LinkHandler.openLink(ProductDetailsActivity.this, url, name, visualQuote);
                    }
                }, start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimary)), start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        boolean hasSources = builder.length() > 0;
        aiSourcesTextView.setText(builder);
        aiSourcesTextView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        aiSourcesTextView.setVisibility(hasSources ? View.VISIBLE : View.GONE);
        if (aiSourcesDivider != null) aiSourcesDivider.setVisibility(hasSources ? View.VISIBLE : View.GONE);
        if (aiSourcesLabel != null) aiSourcesLabel.setVisibility(hasSources ? View.VISIBLE : View.GONE);
    }

    private static class CachedAiInsight {
        final String summary;
        final org.json.JSONArray sources;

        CachedAiInsight(String summary, org.json.JSONArray sources) {
            this.summary = summary;
            this.sources = sources;
        }
    }

    private String formatIngredientsForAi(ProductWithDetails product) {
        if (product == null || product.ingredients == null || product.ingredients.isEmpty()) {
            return "Not listed";
        }

        StringBuilder builder = new StringBuilder();
        for (Ingredient ingredient : product.ingredients) {
            if (ingredient != null && ingredient.text != null && !ingredient.text.trim().isEmpty()) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(ingredient.text.trim());
            }
        }
        return builder.length() == 0 ? "Not listed" : builder.toString();
    }

    private void animateText(TextView textView, String text) {
        final int[] index = {0};
        final long delay = 16;
        final int charsPerTick = 14;
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
                    AiGlowManager.stopGlow(ProductDetailsActivity.this);
                    makeLinksClickable(textView);
                }
            }
        }, delay);
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
                .replace("Ingredient Insights", "<br><br><b>🧪 Ingredient Insights</b><br>")
                .replace("Nutrition Summary", "<br><br><b>🥗 Nutrition Summary</b><br>")
                .replace("Potential Benefits", "<br><br><b>✅ Potential Benefits</b><br>")
                .replace("Things To Consider", "<br><br><b>⚠️ Things To Consider</b><br>")
                .replace("Final Summary", "<br><br><b>🧠 Final Summary</b><br>");
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
                @Override public void onClick(@NonNull View widget) { LinkHandler.openLink(ProductDetailsActivity.this, url, "Scientific Source", url); }
            }, matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(spannableBuilder);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void displayHighlightedIngredients(ProductWithDetails productDetails, ProductAnalysisReport report) {
        if (productDetails.ingredients == null || productDetails.ingredients.isEmpty()) {
            ingredientsTextView.setText("No ingredients listed.");
            return;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (Ingredient ingredient : productDetails.ingredients) {
            if (ingredient.text != null) {
                SpannableString spannable = new SpannableString(ingredient.text);
                for(AnalysisResult res : report.getResults()){
                    if(res.getTriggeringIngredient() != null && ingredient.text.toLowerCase().contains(res.getTriggeringIngredient().toLowerCase())){
                        int color = res.getLevel() == AnalysisResult.WarningLevel.POSITIVE ? 0x3300FF00 : 0x33FF0000;
                        spannable.setSpan(new BackgroundColorSpan(color), 0, spannable.length(), 0);
                        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(), 0);
                        break;
                    }
                }
                builder.append(spannable).append("\n");
            }
        }
        ingredientsTextView.setText(builder);
    }

    private void displayNutriments(Nutriments nutriments) {
        nutritionFactsTable.removeAllViews();
        if (nutriments != null) {
            nutritionFactsTable.setVisibility(View.VISIBLE);
            addNutritionRow("Energy", nutriments.energy, "kcal");
            addNutritionRow("Fat", nutriments.fat, "g");
            addNutritionRow("Saturated Fat", nutriments.saturatedFat, "g");
            addNutritionRow("Carbohydrates", nutriments.carbohydrates, "g");
            addNutritionRow("Sugars", nutriments.sugars, "g");
            addNutritionRow("Fiber", nutriments.fiber, "g");
            addNutritionRow("Proteins", nutriments.proteins, "g");
            addNutritionRow("Salt", nutriments.salt, "g");
            addNutritionRow("Sodium", nutriments.sodium != null ? nutriments.sodium * 1000 : null, "mg");
        } else {
            nutritionFactsTable.setVisibility(View.GONE);
        }
    }

    private void addNutritionRow(String name, Double value, String unit) {
        if (value == null || value == 0.0) return;
        TableRow row = new TableRow(this);
        TextView nameView = new TextView(this);
        TextView valueView = new TextView(this);
        nameView.setText(name);
        valueView.setText(String.format("%.2f %s", value, unit));
        nameView.setPadding(8, 4, 8, 4);
        valueView.setPadding(8, 4, 8, 4);
        valueView.setGravity(Gravity.END);
        row.addView(nameView);
        row.addView(valueView);
        nutritionFactsTable.addView(row);
    }

    private void setScoreTextView(TextView textView, String score, String prefix) {
        if (score == null || score.isEmpty()) {
            textView.setVisibility(View.GONE);
            return;
        }
        textView.setVisibility(View.VISIBLE);
        textView.setText(String.format("%s: %s", prefix, score.toUpperCase()));
        Drawable background = ContextCompat.getDrawable(this, R.drawable.score_background);
        if(background != null) {
            DrawableCompat.setTint(background, getScoreColor(score));
            textView.setBackground(background);
        }
    }

    private int getScoreColor(String score) {
        if (score == null) return ContextCompat.getColor(this, R.color.score_unknown);
        switch (score.toLowerCase()) {
            case "a": case "1": return ContextCompat.getColor(this, R.color.nutriscore_a);
            case "b": return ContextCompat.getColor(this, R.color.nutriscore_b);
            case "c": case "2": return ContextCompat.getColor(this, R.color.nutriscore_c);
            case "d": case "3": return ContextCompat.getColor(this, R.color.nutriscore_d);
            case "e": case "4": return ContextCompat.getColor(this, R.color.nutriscore_e);
            default: return ContextCompat.getColor(this, R.color.score_unknown);
        }
    }

    private void showErrorState(String message, final String barcode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (barcode != null && message != null && message.toLowerCase().contains("not found")) {
            builder.setTitle("Product Not Found");
            builder.setMessage("This product is not in our database yet. Would you like to add it?");
            builder.setPositiveButton("Add Product", (dialog, which) -> {
                Intent intent = new Intent(ProductDetailsActivity.this, AddProductActivity.class);
                intent.putExtra(AddProductActivity.EXTRA_BARCODE, barcode);
                startActivity(intent);
                finish();
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> finish());
        } else {
            builder.setTitle("Error");
            builder.setMessage(message);
            builder.setPositiveButton("OK", (dialog, which) -> finish());
        }
        builder.setOnCancelListener(dialog -> finish());
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showScoreExplanation(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }
}
