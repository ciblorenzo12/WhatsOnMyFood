package com.ciblorenzo.whatsonmyfood;

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

import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultAdapter;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultDeduplicator;
import com.ciblorenzo.whatsonmyfood.analysis.AiSummaryFormatter;
import com.ciblorenzo.whatsonmyfood.analysis.HealthVerdict;
import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;
import com.ciblorenzo.whatsonmyfood.analysis.BitwiseAnalysisService;
import com.ciblorenzo.whatsonmyfood.analysis.ProductAnalysisReport;
import com.ciblorenzo.whatsonmyfood.analysis.rules.RuleEngine;
import com.ciblorenzo.whatsonmyfood.retail.RetailerCommerceViewBinder;
import com.ciblorenzo.whatsonmyfood.retail.RetailerRepository;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IngredientAnalysisActivity extends BaseActivity {

    public static final String EXTRA_INGREDIENTS_TEXT = "extra_ingredients_text";
    public static final String EXTRA_IMAGE_BYTES = "extra_image_bytes";
    public static final String EXTRA_BARCODE = "extra_barcode";
    public static final String EXTRA_SUPPLEMENTAL_ATTEMPT = "extra_supplemental_attempt";
    private static final String AI_CACHE_PREFIX = "BITWISE_AI_CACHE_V2:";

    private ProductWithDetails detectedProduct;
    private Button savePantryButton;
    private ProductRepository productRepository;
    private RetailerRepository retailerRepository;
    private RetailerCommerceViewBinder retailerCommerceViewBinder;
    private com.google.firebase.auth.FirebaseUser currentUser;
    private Bitmap capturedBitmap;
    private View loadingOverlay;
    private String analysisInputText;
    private String rawOcrText;
    private String sourceBarcode;
    private BitwiseAnalysisService bitwiseAnalysisService;
    private int supplementalAttempt;
    private boolean supplementalScanRequested;

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
        retailerRepository = new RetailerRepository(getApplication());
        retailerCommerceViewBinder = new RetailerCommerceViewBinder(
                this,
                findViewById(android.R.id.content),
                retailerRepository,
                new RetailerCommerceViewBinder.Host() {
                    @Override
                    public void runOnUiThread(Runnable runnable) {
                        IngredientAnalysisActivity.this.runOnUiThread(runnable);
                    }

                    @Override
                    public boolean isActive() {
                        return !isFinishing() && !isDestroyed();
                    }
                }
        );
        currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        doneButton.setOnClickListener(v -> finish());
        savePantryButton.setOnClickListener(v -> saveToPantry());

        healthScoreView.setText(R.string.analyzing);
        rawIngredientsView.setText(R.string.identifying_ingredients);

        String initialText = getIntent().getStringExtra(EXTRA_INGREDIENTS_TEXT);
        rawOcrText = initialText != null ? initialText : "";

        byte[] imageBytes = getIntent().getByteArrayExtra(EXTRA_IMAGE_BYTES);
        if (imageBytes != null) {
            capturedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ImageView productImageView = findViewById(R.id.product_image_view);
            if (productImageView != null) {
                productImageView.setImageBitmap(capturedBitmap);
            }
        }

        List<String> rules = new RuleEngine().getRuleDescriptions();
        sourceBarcode = BarcodeScanGate.normalizeAndValidate(getIntent().getStringExtra(EXTRA_BARCODE));
        supplementalAttempt = Math.max(0, getIntent().getIntExtra(EXTRA_SUPPLEMENTAL_ATTEMPT, 0));
        IngredientLabelValidator.Result labelValidation = IngredientLabelValidator.validate(initialText);
        if (!labelValidation.readable) {
            showUnreadableLabelDialog();
            return;
        }
        rawOcrText = labelValidation.cleanedText;
        String detectedIngredientLabel = IngredientTextParser.trimToLikelyIngredientList(rawOcrText);
        String analysisPrompt = "response_language: " + LanguageManager.getLanguageName(this) + "\n"
                + "scan_mode: ingredients\n"
                + "image_attached: " + (capturedBitmap != null ? "true" : "false") + "\n"
                + "available_barcode: " + (sourceBarcode != null ? sourceBarcode : "") + "\n"
                + "task: Parse the scanned ingredient label for scoring. Use visible package text and the ingredient pattern to identify the product, but do not replace or invent label ingredients.\n"
                + "detected_ingredient_label:\n"
                + detectedIngredientLabel + "\n"
                + "product_ocr_text:\n"
                + rawOcrText;
        analyzeWithAI(analysisPrompt, rules, capturedBitmap, healthScoreView, rawIngredientsView, analysisRecyclerView, progressBar);
    }

    private void analyzeWithAI(String prompt, List<String> rules, Bitmap bitmap, TextView healthScoreView, TextView rawIngredientsView, RecyclerView analysisRecyclerView, ProgressBar progressBar) {
        analysisInputText = prompt;
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        if (bitwiseAnalysisService != null) bitwiseAnalysisService.cancelActiveCall();
        bitwiseAnalysisService = new BitwiseAnalysisService();
        bitwiseAnalysisService.analyzeWithRules(prompt, rules, bitmap, new BitwiseAnalysisService.AnalysisCallback() {
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
                    ProductIdentity identity = inferProductIdentityFromOcr(rawOcrText);
                    if (identity.productName == null) {
                        requestSupplementalOcr(false, "", "");
                        return;
                    }
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
            String summary = com.ciblorenzo.whatsonmyfood.analysis.AiSummaryFormatter.format(obj.optString("summary", ""));
            ProductIdentity ocrIdentity = inferProductIdentityFromOcr(rawOcrText);

            boolean productNameMissing = isUnknownProductName(productName) || looksLikePromptInstruction(productName);
            if (productNameMissing) {
                productName = ocrIdentity.productName != null ? ocrIdentity.productName : "Scanned Product";
            }
            productNameMissing = isUnknownProductName(productName) || looksLikePromptInstruction(productName);
            if (brand.isEmpty() || brand.equalsIgnoreCase("Brand") || brand.equalsIgnoreCase("Brand Unknown")) {
                brand = ocrIdentity.brand != null ? ocrIdentity.brand : "Brand Unknown";
            }
            String productKey = sourceBarcode != null ? sourceBarcode : "ai-" + System.currentTimeMillis();

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
            List<Ingredient> ingredientList = readIngredientsForScoring(obj, productKey);
            boolean ingredientsMissing = ingredientList.isEmpty();
            if (ingredientsMissing || productNameMissing) {
                requestSupplementalOcr(ingredientsMissing, productName, brand);
                return;
            }

            detectedProduct = new ProductWithDetails();
            detectedProduct.product = new Product(productKey, productName, brand, null, null, null, null, null, null, null, null, null, buildAiInsightCache(summary, sourcesArr), null);
            detectedProduct.ingredients = ingredientList;
            if (retailerCommerceViewBinder != null) {
                retailerCommerceViewBinder.bind(detectedProduct);
            }

            // 3. Nutrition & Findings
            List<AnalysisResult> aiResults = new ArrayList<>();
            JSONArray findings = obj.optJSONArray("findings");
            if (findings != null) {
                for (int i = 0; i < findings.length(); i++) {
                    JSONObject f = findings.optJSONObject(i);
                    if (f != null) {
                        AnalysisResult res = new AnalysisResult(
                                f.optString("rule"),
                                parseAiWarningLevel(f.optString("impact")),
                                0,
                                f.optString("triggering_ingredient"),
                                f.optString("explanation")
                        );
                        res.setSourceUrl(f.optString("source_url", ""));
                        res.setVisualQuote(f.optString("visual_quote", ""));
                        aiResults.add(res);
                    }
                }
            }

            ProductAnalysisReport ruleReport = new RuleEngine().analyze(detectedProduct);
            int score = ruleReport.getOverallScore();
            detectedProduct.product.healthScore = score;

            List<AnalysisResult> displayResults = new ArrayList<>(ruleReport.getResults());
            displayResults.addAll(aiResults);
            displayResults = AnalysisResultDeduplicator.deduplicate(displayResults);
            recyclerView.setAdapter(new AnalysisResultAdapter(displayResults));

            ProductAnalysisReport displayReport = new ProductAnalysisReport(score, displayResults);
            HealthVerdict verdict = HealthVerdict.fromAiVerdict(
                    obj.optString("verdict", ""),
                    obj.optString("verdict_reason", ""),
                    displayResults,
                    ingredientList.size()
            );
            scoreView.setText(verdict.getLabel());
            scoreView.setTextColor(getVerdictColor(verdict));
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
            String cleanedText = json.replaceAll("\"[a-z_]+\":", "").replaceAll("[{}\\[\\]\"]", "").trim();
            rawIngredientsView.setText(cleanedText);
        }
    }

    private List<Ingredient> readIngredientsForScoring(JSONObject obj, String barcode) {
        List<Ingredient> ingredients = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<String> modelIngredients = new ArrayList<>();
        JSONArray ingredientArray = obj.optJSONArray("ingredients");
        if (ingredientArray != null) {
            for (int i = 0; i < ingredientArray.length(); i++) {
                modelIngredients.add(ingredientArray.optString(i, ""));
            }
        }
        for (String text : IngredientScoringInput.select(rawOcrText, modelIngredients)) {
            addIngredientIfUseful(ingredients, seen, barcode, text);
        }
        return ingredients;
    }

    private void addIngredientIfUseful(List<Ingredient> ingredients, Set<String> seen, String barcode, String text) {
        String cleaned = normalizeScannedIngredient(IngredientTextParser.cleanIngredientText(text));
        if (cleaned.isEmpty() || ingredients.size() >= 80) return;
        String key = cleaned.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
        if (seen.add(key)) {
            ingredients.add(new Ingredient(barcode, cleaned, ingredients.size()));
        }
    }

    private String normalizeScannedIngredient(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) return "";

        String lower = cleaned.toLowerCase(Locale.US);
        if ((lower.contains("organic") || lower.contains("organc"))
                && lower.contains("coconut")
                && (lower.contains("water") || lower.matches(".*\\bwa(t(e(r)?)?)?\\b.*"))) {
            return "Organic coconut water";
        }
        if (lower.contains("coconut")
                && (lower.contains("water") || lower.matches(".*\\bwa(t(e(r)?)?)?\\b.*"))) {
            return "Coconut water";
        }

        if (lower.matches(".*\\b(wa|wat|wate)\\b.*")) {
            return "";
        }
        return cleaned;
    }

    private AnalysisResult.WarningLevel parseAiWarningLevel(String impact) {
        if (impact == null) return AnalysisResult.WarningLevel.INFO;
        String normalized = impact.trim().toLowerCase(Locale.US);
        if (normalized.equals("positive") || normalized.equals("good") || normalized.equals("benefit")) {
            return AnalysisResult.WarningLevel.POSITIVE;
        }
        if (normalized.equals("negative") || normalized.equals("severe") || normalized.equals("bad")) {
            return AnalysisResult.WarningLevel.SEVERE;
        }
        if (normalized.equals("warning") || normalized.equals("caution") || normalized.equals("moderate")) {
            return AnalysisResult.WarningLevel.WARNING;
        }
        return AnalysisResult.WarningLevel.INFO;
    }

    private boolean looksLikePromptInstruction(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.US);
        return normalized.contains("front-label ocr")
                || normalized.contains("extract only")
                || normalized.contains("ingredient list from")
                || normalized.startsWith("task:")
                || normalized.startsWith("identify the visible product");
    }

    private boolean isUnknownProductName(String value) {
        if (value == null) return true;
        String normalized = value.trim().toLowerCase(Locale.US);
        return normalized.isEmpty()
                || normalized.equals("name")
                || normalized.equals("unknown")
                || normalized.equals("unknown product")
                || normalized.equals("scanned")
                || normalized.equals("scanned product");
    }

    private ProductIdentity inferProductIdentityFromOcr(String text) {
        if (text == null || text.trim().isEmpty()) return ProductIdentity.empty();

        List<String> candidates = new ArrayList<>();
        String identityText = textBeforeIngredientMarker(IngredientTextParser.stripPromptMetadata(text));
        String[] lines = identityText.split("\\r?\\n");
        for (String line : lines) {
            String cleaned = cleanProductIdentityLine(line);
            if (!cleaned.isEmpty()) {
                candidates.add(cleaned);
            }
            if (candidates.size() >= 8) break;
        }

        if (candidates.isEmpty()) return ProductIdentity.empty();

        String joined = String.join(" ", candidates);
        String lowerJoined = joined.toLowerCase(Locale.US);
        if (looksLikeVitaCoco(lowerJoined)) {
            String name = compactKnownProductName(joined, "Vita Coco");
            return new ProductIdentity(name, "Vita Coco");
        }

        String brand = candidates.get(0);
        StringBuilder name = new StringBuilder();
        for (String candidate : candidates) {
            if (name.length() > 0) name.append(' ');
            name.append(candidate);
            if (name.length() > 58) break;
        }
        return new ProductIdentity(toDisplayCase(name.toString()), toDisplayCase(brand));
    }

    private String textBeforeIngredientMarker(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.US);
        String[] markers = {"ingredients", "ingredient list", "ingrédients", "ingredientes", "contains:", "contient:", "contiene:"};
        int stop = -1;
        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index >= 0 && (stop == -1 || index < stop)) stop = index;
        }
        return stop >= 0 ? text.substring(0, stop) : text;
    }

    private void requestSupplementalOcr(boolean ingredientsMissing, String productName, String brand) {
        if (supplementalScanRequested || isFinishing()) return;
        supplementalScanRequested = true;

        int titleRes = ingredientsMissing
                ? R.string.missing_ingredients_scan_title
                : R.string.missing_product_name_scan_title;
        int messageRes = ingredientsMissing
                ? R.string.missing_ingredients_scan_message
                : R.string.missing_product_name_scan_message;

        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setCancelable(false)
                .setPositiveButton(R.string.continue_to_camera, (dialog, which) -> {
                    Intent scannerIntent = new Intent(this, ScanBarcodeActivity.class);
                    scannerIntent.putExtra(
                            ScanBarcodeActivity.EXTRA_SUPPLEMENTAL_TARGET,
                            ingredientsMissing
                                    ? ScanBarcodeActivity.TARGET_INGREDIENTS
                                    : ScanBarcodeActivity.TARGET_PRODUCT_NAME
                    );
                    scannerIntent.putExtra(
                            ScanBarcodeActivity.EXTRA_EXISTING_INGREDIENT_TEXT,
                            ingredientsMissing ? "" : rawOcrText
                    );
                    scannerIntent.putExtra(
                            ScanBarcodeActivity.EXTRA_EXISTING_PRODUCT_TEXT,
                            ingredientsMissing ? buildProductIdentityContext(productName, brand) : ""
                    );
                    if (sourceBarcode != null) {
                        scannerIntent.putExtra(ScanBarcodeActivity.EXTRA_SOURCE_BARCODE, sourceBarcode);
                    }
                    scannerIntent.putExtra(
                            ScanBarcodeActivity.EXTRA_SUPPLEMENTAL_ATTEMPT,
                            supplementalAttempt + 1
                    );
                    startActivity(scannerIntent);
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private String buildProductIdentityContext(String productName, String brand) {
        StringBuilder context = new StringBuilder();
        if (!isUnknownProductName(productName) && !looksLikePromptInstruction(productName)) {
            context.append(productName.trim());
        }
        if (brand != null
                && !brand.trim().isEmpty()
                && !brand.equalsIgnoreCase("Brand")
                && !brand.equalsIgnoreCase("Brand Unknown")) {
            if (context.length() > 0) context.append('\n');
            context.append(brand.trim());
        }
        if (context.length() == 0) {
            context.append(textBeforeIngredientMarker(rawOcrText).trim());
        }
        return context.toString().trim();
    }

    private void showUnreadableLabelDialog() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
        new AlertDialog.Builder(this)
                .setTitle(R.string.ingredients_not_read_title)
                .setMessage(R.string.ingredients_not_read_message)
                .setCancelable(false)
                .setPositiveButton(R.string.try_again, (dialog, which) -> finish())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private boolean looksLikeVitaCoco(String lowerText) {
        if (lowerText == null) return false;
        return (lowerText.contains("vita coco") || lowerText.contains("vta coco") || lowerText.contains("vita c0co") || lowerText.contains("vta c0co"))
                && (lowerText.contains("coconut") || lowerText.contains("cocon"));
    }

    private String compactKnownProductName(String joined, String brand) {
        String normalized = joined
                .replaceAll("(?i)\\bvta\\b", "Vita")
                .replaceAll("(?i)\\bc0co\\b", "Coco")
                .replaceAll("(?i)\\btwais\\b", "Water")
                .replaceAll("(?i)\\bcertified\\s+b\\s+corp\\b", " ")
                .replaceAll("(?i)\\bnot\\s+from\\s+concentrate\\b", " ")
                .replaceAll("(?i)\\bpure\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();

        List<String> parts = new ArrayList<>();
        parts.add(brand);
        if (normalized.toLowerCase(Locale.US).contains("farmers")) parts.add("Farmers");
        if (normalized.toLowerCase(Locale.US).contains("organic")) parts.add("Organic");
        if (normalized.toLowerCase(Locale.US).contains("coconut water")) parts.add("Coconut Water");
        if (parts.size() > 1) return String.join(" ", parts);
        return toDisplayCase(normalized);
    }

    private String cleanProductIdentityLine(String line) {
        if (line == null) return "";
        String cleaned = line.replaceAll("[^A-Za-z0-9&'\\- ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() < 3 || cleaned.length() > 42) return "";

        String lower = cleaned.toLowerCase(Locale.US);
        if (lower.startsWith("ingredients")
                || lower.startsWith("nutrition")
                || lower.startsWith("serving")
                || lower.startsWith("calories")
                || lower.startsWith("total fat")
                || lower.startsWith("contains")
                || lower.startsWith("barcode")
                || lower.contains("fl oz")
                || lower.matches(".*\\b\\d+\\s*(g|mg|ml|oz|l)\\b.*")) {
            return "";
        }
        return cleaned;
    }

    private String toDisplayCase(String value) {
        if (value == null) return "";
        String[] words = value.toLowerCase(Locale.US).replaceAll("\\s+", " ").trim().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            if (word.length() <= 2 && !word.equals("of")) {
                result.append(word.toUpperCase(Locale.US));
            } else {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return result.toString();
    }

    private static final class ProductIdentity {
        final String productName;
        final String brand;

        ProductIdentity(String productName, String brand) {
            this.productName = productName != null && !productName.trim().isEmpty() ? productName : null;
            this.brand = brand != null && !brand.trim().isEmpty() ? brand : null;
        }

        static ProductIdentity empty() {
            return new ProductIdentity(null, null);
        }
    }

    private int getVerdictColor(HealthVerdict verdict) {
        if (verdict == null) return ContextCompat.getColor(this, R.color.score_unknown);
        switch (verdict.getStatus()) {
            case HEALTHY:
                return ContextCompat.getColor(this, R.color.nutriscore_a);
            case NOT_HEALTHY:
                return ContextCompat.getColor(this, R.color.nutriscore_e);
            default:
                return ContextCompat.getColor(this, R.color.score_unknown);
        }
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
            int end = builder.length();

            for (AnalysisResult res : report.getResults()) {
                if (res.getTriggeringIngredient() == null || res.getTriggeringIngredient().trim().isEmpty()) {
                    continue;
                }
                if (res.getLevel() == AnalysisResult.WarningLevel.INFO) {
                    continue;
                }
                applyIngredientHighlight(builder, ingredient.text, start, end, res);
            }
            if (i < productDetails.ingredients.size() - 1) builder.append(", ");
        }
        ingredientsTextView.setText(builder);
    }

    private void applyIngredientHighlight(SpannableStringBuilder builder, String ingredientText, int ingredientStart, int ingredientEnd, AnalysisResult result) {
        String[] triggers = result.getTriggeringIngredient().split("[,;]");
        for (String trigger : triggers) {
            String cleanedTrigger = IngredientTextParser.cleanIngredientText(trigger);
            if (cleanedTrigger.isEmpty()) continue;

            Pattern pattern = Pattern.compile(Pattern.quote(cleanedTrigger), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(ingredientText);
            boolean matched = false;
            while (matcher.find()) {
                matched = true;
                int highlightStart = ingredientStart + matcher.start();
                int highlightEnd = ingredientStart + matcher.end();
                setIngredientHighlight(builder, highlightStart, highlightEnd, result.getLevel());
            }

            String normalizedIngredient = ingredientText.toLowerCase(Locale.US).trim();
            String normalizedTrigger = cleanedTrigger.toLowerCase(Locale.US).trim();
            if (!matched && (normalizedTrigger.contains(normalizedIngredient) || normalizedIngredient.contains(normalizedTrigger))) {
                setIngredientHighlight(builder, ingredientStart, ingredientEnd, result.getLevel());
            }
        }
    }

    private void setIngredientHighlight(SpannableStringBuilder builder, int start, int end, AnalysisResult.WarningLevel level) {
        int color;
        if (level == AnalysisResult.WarningLevel.POSITIVE) {
            color = 0x3322C55E;
        } else if (level == AnalysisResult.WarningLevel.SEVERE) {
            color = 0x33EF4444;
        } else {
            color = 0x33F59E0B;
        }
        builder.setSpan(new android.text.style.BackgroundColorSpan(color), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    @Override
    protected void onDestroy() {
        if (bitwiseAnalysisService != null) {
            bitwiseAnalysisService.cancelActiveCall();
        }
        super.onDestroy();
        if (productRepository != null) {
            productRepository.close();
        }
        if (retailerRepository != null) {
            retailerRepository.close();
        }
    }
}
