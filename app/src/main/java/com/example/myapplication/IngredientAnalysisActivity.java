package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.analysis.AnalysisResult;
import com.example.myapplication.analysis.AnalysisResultAdapter;
import com.example.myapplication.analysis.OpenAIAnalysisService;
import com.example.myapplication.analysis.ProductAnalysisReport;
import com.example.myapplication.analysis.rules.RuleEngine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IngredientAnalysisActivity extends AppCompatActivity {

    public static final String EXTRA_INGREDIENTS_TEXT = "extra_ingredients_text";
    public static final String EXTRA_IMAGE_BYTES = "extra_image_bytes";
    public static final String EXTRA_BARCODE = "extra_barcode";
    
    private ProductWithDetails detectedProduct;
    private Button savePantryButton;
    private ProductRepository productRepository;
    private com.google.firebase.auth.FirebaseUser currentUser;
    private Bitmap capturedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_analysis);

        Toolbar toolbar = findViewById(R.id.analysis_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String ingredientsText = getIntent().getStringExtra(EXTRA_INGREDIENTS_TEXT);
        if (ingredientsText == null) {
            finish();
            return;
        }

        TextView healthScoreView = findViewById(R.id.health_score_text_view);
        TextView rawIngredientsView = findViewById(R.id.raw_ingredients_text_view);
        RecyclerView analysisRecyclerView = findViewById(R.id.analysis_recycler_view);
        analysisRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        Button doneButton = findViewById(R.id.done_button);
        savePantryButton = findViewById(R.id.save_pantry_button);
        ProgressBar progressBar = findViewById(R.id.analysis_progress_bar);

        productRepository = new ProductRepository(getApplication());
        currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        doneButton.setOnClickListener(v -> finish());
        savePantryButton.setOnClickListener(v -> saveToPantry());

        healthScoreView.setText("Analyzing...");
        rawIngredientsView.setText("Identifying ingredients...");

        analyzeIngredientsLocal(ingredientsText, healthScoreView, rawIngredientsView, analysisRecyclerView);
        
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        AiGlowManager.startGlow(this);

        List<String> rules = new RuleEngine().getRuleDescriptions();
        String barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        
        byte[] imageBytes = getIntent().getByteArrayExtra(EXTRA_IMAGE_BYTES);
        if (imageBytes != null) {
            capturedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ImageView productImageView = findViewById(R.id.product_image_view);
            if (productImageView != null) {
                productImageView.setImageBitmap(capturedBitmap);
                productImageView.setAlpha(1.0f); // Solid image
            }
        }

        if (barcode != null && !barcode.isEmpty()) {
            new Thread(() -> {
                try {
                    OpenFoodFactsApiClient apiClient = new OpenFoodFactsApiClient(getCacheDir());
                    ProductResponse response = apiClient.getProduct(barcode);
                    if (response != null && response.status == 1 && response.product != null) {
                        String apiIngredients = response.product.ingredientsText;
                        runOnUiThread(() -> {
                            // Update UI with API data if available
                            if (apiIngredients != null && !apiIngredients.isEmpty()) {
                                rawIngredientsView.setText(apiIngredients);
                            }
                        });
                        // Pass API data to AI for better reasoning
                        String enrichedPrompt = "API Data for barcode " + barcode + ": " + 
                            (response.product.productName != null ? response.product.productName : "") + " " +
                            (apiIngredients != null ? apiIngredients : "") + "\n\n" + ingredientsText;
                        analyzeWithAI(enrichedPrompt, rules, capturedBitmap, healthScoreView, rawIngredientsView, analysisRecyclerView, progressBar);
                    } else {
                        analyzeWithAI(ingredientsText, rules, capturedBitmap, healthScoreView, rawIngredientsView, analysisRecyclerView, progressBar);
                    }
                } catch (Exception e) {
                    analyzeWithAI(ingredientsText, rules, capturedBitmap, healthScoreView, rawIngredientsView, analysisRecyclerView, progressBar);
                }
            }).start();
        } else {
            analyzeWithAI(ingredientsText, rules, capturedBitmap, healthScoreView, rawIngredientsView, analysisRecyclerView, progressBar);
        }
    }

    private void analyzeWithAI(String prompt, List<String> rules, Bitmap bitmap, TextView healthScoreView, TextView rawIngredientsView, RecyclerView analysisRecyclerView, ProgressBar progressBar) {
        new OpenAIAnalysisService().analyzeWithRules(prompt, rules, bitmap, new OpenAIAnalysisService.AnalysisCallback() {
            @Override
            public void onResult(String jsonResult) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    applyGeminiResults(jsonResult, healthScoreView, rawIngredientsView, analysisRecyclerView);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    AiGlowManager.stopGlow(IngredientAnalysisActivity.this);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    
                    // Fallback: show raw ingredients and allow saving even if AI fails
                    if (detectedProduct != null) {
                        rawIngredientsView.setTextColor(ContextCompat.getColor(IngredientAnalysisActivity.this, R.color.text_secondary));
                        displayHighlightedIngredients(detectedProduct, new ProductAnalysisReport(0, new ArrayList<>()), rawIngredientsView);
                        if (savePantryButton != null) savePantryButton.setVisibility(View.VISIBLE);
                        healthScoreView.setText("N/A");
                    }
                    
                    String errorMsg = "AI Analysis failed. Using local results.";
                    Toast.makeText(IngredientAnalysisActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void applyGeminiResults(String json, TextView scoreView, TextView rawIngredientsView, RecyclerView recyclerView) {
        try {
            JSONObject obj = new JSONObject(json);
            
            // AI-detected Product Info
            String productName = obj.optString("product_name", "");
            if (productName.isEmpty()) {
                if (obj.optInt("score", 0) == 0 && !obj.optString("summary", "").isEmpty()) {
                    productName = "Not a Food Item";
                } else {
                    productName = "Scanned Product";
                }
            }
            
            String brand = obj.optString("brand", "");
            if (brand.isEmpty()) brand = "Generic Brand";

            TextView nameView = findViewById(R.id.product_name_text_view);
            TextView brandView = findViewById(R.id.product_brand_text_view);
            if (nameView != null) nameView.setText(productName);
            if (brandView != null) brandView.setText(brand);

            int score = obj.optInt("score", 0);
            String summary = obj.optString("summary", "");
            
            // Generate a unique ID for non-barcode scans
            String fakeBarcode = "ai-" + System.currentTimeMillis();

            // Nutrition Data
            JSONObject nutrition = obj.optJSONObject("nutrition");
            if (nutrition != null) {
                String nutritionSummary = String.format("\n\nEstimated Nutrition:\nEnergy: %s | Fat: %s | Sugars: %s | Protein: %s",
                        nutrition.optString("energy", "N/A"),
                        nutrition.optString("fat", "N/A"),
                        nutrition.optString("sugars", "N/A"),
                        nutrition.optString("protein", "N/A"));
                summary += nutritionSummary;
            }

            // Extract AI triggering ingredients
            JSONArray findings = obj.optJSONArray("findings");
            List<String> aiTriggers = new ArrayList<>();
            if (findings != null) {
                for (int i = 0; i < findings.length(); i++) {
                    JSONObject finding = findings.getJSONObject(i);
                    String trigger = finding.optString("triggering_ingredient", "");
                    if (!trigger.isEmpty()) aiTriggers.add(trigger);
                }
            }

            JSONArray ings = obj.optJSONArray("ingredients");
            List<Ingredient> ingredientList = new ArrayList<>();
            if (ings != null) {
                for (int i = 0; i < ings.length(); i++) {
                    ingredientList.add(new Ingredient(fakeBarcode, ings.getString(i), i));
                }
            } else {
                ingredientList = parseIngredients(fakeBarcode, getIntent().getStringExtra(EXTRA_INGREDIENTS_TEXT));
            }

            scoreView.setText(String.format(Locale.getDefault(), "%d/100", score));
            scoreView.setTextColor(ContextCompat.getColor(this, R.color.ai_accent));

            detectedProduct = new ProductWithDetails();
            detectedProduct.product = new Product(fakeBarcode, productName, brand, null, null, null, null, null, null, null, null, null, summary, score);
            detectedProduct.ingredients = ingredientList;

            // Show save button if we have ingredients
            if (!ingredientList.isEmpty() && savePantryButton != null) {
                savePantryButton.setVisibility(View.VISIBLE);
            }

            rawIngredientsView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            // RE-EVALUATE: Run local rules on the AI-extracted ingredients to ensure highlighting
            RuleEngine engine = new RuleEngine();
            ProductAnalysisReport localReport = engine.analyze(detectedProduct);
            
            // Merge AI findings with local rule results for comprehensive highlighting
            List<AnalysisResult> finalResults = new ArrayList<>(localReport.getResults());
            if (findings != null) {
                for (int i = 0; i < findings.length(); i++) {
                    JSONObject f = findings.getJSONObject(i);
                    finalResults.add(new AnalysisResult(f.optString("rule"), 
                        f.optString("impact").equals("negative") ? AnalysisResult.WarningLevel.SEVERE : AnalysisResult.WarningLevel.INFO,
                        0, f.optString("triggering_ingredient"), f.optString("explanation")));
                }
            }

            ProductAnalysisReport report = new ProductAnalysisReport(score, finalResults);
            report.setAiTriggeringIngredients(aiTriggers);

            displayHighlightedIngredients(detectedProduct, report, rawIngredientsView);
            recyclerView.setAdapter(new AnalysisResultAdapter(finalResults));
            
            if (!summary.isEmpty()) {
                View summaryLayout = findViewById(R.id.ai_summary_layout);
                TextView summaryTextView = findViewById(R.id.ai_summary_text_view);
                if (summaryLayout != null && summaryTextView != null) {
                    summaryLayout.setVisibility(View.VISIBLE);
                    animateText(summaryTextView, summary);
                } else {
                    AiGlowManager.stopGlow(this);
                }
            } else {
                AiGlowManager.stopGlow(this);
            }

            TextView novaView = findViewById(R.id.nova_text_view);
            if (json.toLowerCase().contains("ultra-processed") || json.contains("NOVA 4")) {
                if (novaView != null) novaView.setText("4");
            } else {
                if (novaView != null) novaView.setText("1");
            }
        } catch (Exception e) {
            AiGlowManager.stopGlow(this);
            e.printStackTrace();
        }
    }

    private void animateText(TextView textView, String text) {
        final int[] index = {0};
        final long delay = 8; 
        Handler handler = new Handler(Looper.getMainLooper());
        textView.setText("");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index[0] < text.length()) {
                    textView.append(String.valueOf(text.charAt(index[0]++)));
                    handler.postDelayed(this, delay);
                } else {
                    AiGlowManager.stopGlow(IngredientAnalysisActivity.this);
                }
            }
        }, delay);
    }

    private void analyzeIngredientsLocal(String text, TextView scoreView, TextView rawIngredientsView, RecyclerView recyclerView) {
        String tempBarcode = "ai-scan-" + System.currentTimeMillis();
        List<Ingredient> ingredients = parseIngredients(tempBarcode, text);
        
        detectedProduct = new ProductWithDetails();
        detectedProduct.product = new Product(tempBarcode, "Scanned Product", "Brand Unknown", null, null, null, null, null, null, null, null, null, null, 0);
        detectedProduct.ingredients = ingredients;
        
        scoreView.setText("Analyzing...");
        scoreView.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        
        rawIngredientsView.setText("Analyzing ingredients list...");
        rawIngredientsView.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
    }

    private int getScoreColor(String score) {
        if (score == null) return 0xFF9CA3AF;
        switch (score.toLowerCase()) {
            case "a": case "1": return 0xFF059669;
            case "b": case "2": return 0xFF10B981;
            case "c": case "3": return 0xFFF59E0B;
            case "d": case "4": return 0xFFF97316;
            case "e": return 0xFFEF4444;
            default: return 0xFF9CA3AF;
        }
    }

    private void displayHighlightedIngredients(ProductWithDetails productDetails, ProductAnalysisReport report, TextView ingredientsTextView) {
        if (productDetails.ingredients == null || productDetails.ingredients.isEmpty()) {
            ingredientsTextView.setText("No ingredients identified.");
            return;
        }

        android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder();
        List<String> aiTriggers = report.getAiTriggeringIngredients();

        for (int i = 0; i < productDetails.ingredients.size(); i++) {
            Ingredient ingredient = productDetails.ingredients.get(i);
            int start = builder.length();
            builder.append(ingredient.text);
            
            boolean highlighted = false;

            // Manual Rule Highlighting (Red)
            for (com.example.myapplication.analysis.AnalysisResult result : report.getResults()) {
                if (result.getTriggeringIngredient() != null) {
                    String trigger = result.getTriggeringIngredient().toLowerCase();
                    String ingText = ingredient.text.toLowerCase();
                    
                    // Use exact match or boundary check for short words like "water"
                    boolean isMatch;
                    if (trigger.length() <= 5) {
                        isMatch = ingText.equals(trigger) || ingText.matches(".*\\b" + trigger + "\\b.*");
                    } else {
                        isMatch = ingText.contains(trigger);
                    }

                    if (isMatch) {
                        int color;
                        if (result.getLevel() == com.example.myapplication.analysis.AnalysisResult.WarningLevel.POSITIVE) {
                            color = 0x3300FF00; // Green tint
                        } else {
                            color = 0x33FF0000; // Red tint
                        }
                        builder.setSpan(new android.text.style.BackgroundColorSpan(color), start, builder.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, builder.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        highlighted = true;
                        break;
                    }
                }
            }

            // AI Trigger Highlighting (Purple)
            if (!highlighted && aiTriggers != null) {
                for (String trigger : aiTriggers) {
                    String lowerTrigger = trigger.toLowerCase();
                    String ingText = ingredient.text.toLowerCase();
                    
                    if (ingText.contains(lowerTrigger) && !lowerTrigger.equals("water")) {
                        int color = 0x337C4DFF; // AI Purple tint
                        builder.setSpan(new android.text.style.BackgroundColorSpan(color), start, builder.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, builder.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    }
                }
            }
            
            if (i < productDetails.ingredients.size() - 1) {
                builder.append(", ");
            }
        }
        ingredientsTextView.setText(builder);
    }

    private List<Ingredient> parseIngredients(String barcode, String text) {
        List<Ingredient> ingredients = new ArrayList<>();
        if (text == null || text.isEmpty()) return ingredients;

        String[] parts = text.split("[,;\\n]");
        int rank = 0;
        for (String part : parts) {
            String trimmed = part.trim().replaceAll("[*_]", "");
            if (!trimmed.isEmpty() && trimmed.length() > 2) {
                ingredients.add(new Ingredient(barcode, trimmed, rank++));
            }
        }
        return ingredients;
    }

    @Override
    public boolean onSupportNavigateUp() {
        AiGlowManager.stopGlow(this);
        finish();
        return true;
    }

    private void saveToPantry() {
        if (detectedProduct == null || currentUser == null) return;

        savePantryButton.setEnabled(false);
        savePantryButton.setText("Saving...");

        // Run on background thread
        new Thread(() -> {
            try {
                // Save image locally if available
                if (capturedBitmap != null) {
                    String fileName = "product_" + detectedProduct.product.barcode + ".jpg";
                    java.io.File file = new java.io.File(getFilesDir(), fileName);
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                        capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        detectedProduct.product.imageUrl = "file://" + file.getAbsolutePath();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }

                AppDatabase.getDatabase(this).productDao().insertProductWithDetails(detectedProduct);
                AppDatabase.getDatabase(this).productDao().insertPantry(new Pantry(detectedProduct.product.barcode, currentUser.getUid()));
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Added to Pantry!", Toast.LENGTH_SHORT).show();
                    savePantryButton.setText("Saved ✓");
                    // Optionally refresh the pantry or return
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving product", Toast.LENGTH_SHORT).show();
                    savePantryButton.setEnabled(true);
                    savePantryButton.setText("Add to Pantry");
                });
            }
        }).start();
    }
}
