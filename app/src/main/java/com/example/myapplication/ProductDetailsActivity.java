package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.analysis.AnalysisResult;
import com.example.myapplication.analysis.AnalysisResultAdapter;
import com.example.myapplication.analysis.OpenAIAnalysisService;
import com.example.myapplication.analysis.ProductAnalysisReport;
import com.example.myapplication.analysis.rules.RuleEngine;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "com.example.myapplication.BARCODE";

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
    private AiGlowView aiCardGlow;
    private TextView aiSummaryTextView;

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
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        nutritionFactsTable = findViewById(R.id.nutrition_facts_table);
        analysisRecyclerView = findViewById(R.id.analysis_recycler_view);
        aiSummaryLayout = findViewById(R.id.ai_summary_layout);
        aiSummaryContainer = findViewById(R.id.ai_summary_container);
        aiCardGlow = findViewById(R.id.ai_card_glow);
        aiSummaryTextView = findViewById(R.id.ai_summary_text_view);
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
        productRepository.getProductByBarcode(barcode, new ProductRepository.RepositoryCallback<ProductRepository.ProductResult>() {
            @Override
            public void onComplete(ProductRepository.ProductResult result) {
                runOnUiThread(() -> {
                    if (result != null && result.productWithDetails != null) {
                        displayProductDetails(result.productWithDetails);
                    } else {
                        showErrorState("Product not found for barcode: " + barcode, barcode);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> showErrorState("Error fetching product: " + e.getMessage(), null));
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
            int scoreToDisplay = currentReport.getOverallScore();
            String scoreSuffix = "";
            if (productDetails.product.healthScore != null) {
                scoreToDisplay = productDetails.product.healthScore;
                scoreSuffix = " (AI Verified)";
                healthScoreTextView.setTextColor(ContextCompat.getColor(this, R.color.ai_accent));
            }
            healthScoreTextView.setText(String.format("Health Score: %d/100%s", scoreToDisplay, scoreSuffix));
            analysisRecyclerView.setAdapter(new AnalysisResultAdapter(currentReport.getResults()));
            displayHighlightedIngredients(productDetails, currentReport);
        } else {
            healthScoreTextView.setText("Health Score: N/A");
            analysisRecyclerView.setAdapter(null);
            ingredientsTextView.setText("Could not analyze ingredients.");
        }

        setScoreTextView(nutriscoreTextView, productDetails.product.nutriscoreGrade, "Nutri-Score");
        setScoreTextView(novaTextView, productDetails.product.novaGroup, "NOVA Group");
        setScoreTextView(ecoscoreTextView, productDetails.product.ecoscoreGrade, "Eco-Score");

        categoriesTextView.setText(productDetails.product.categories != null ? productDetails.product.categories : "");
        packagingTextView.setText(productDetails.product.packaging != null ? productDetails.product.packaging : "");
        labelsTextView.setText(productDetails.product.labels != null ? productDetails.product.labels : "");
        servingSizeTextView.setText(productDetails.product.servingSize != null ? productDetails.product.servingSize : "");

        displayNutriments(productDetails.nutriments);

        // Start AI Reasoning in background
        performAiReasoning(productDetails);
    }

    private void performAiReasoning(ProductWithDetails product) {
        if (aiSummaryContainer == null) return;

        // If we already have AI insight and a stored health score, we can skip re-analyzing unless needed.
        if (product.product.aiInsight != null && !product.product.aiInsight.isEmpty() && product.product.healthScore != null) {
            aiSummaryContainer.setVisibility(View.VISIBLE);
            animateText(aiSummaryTextView, product.product.aiInsight);
            healthScoreTextView.setText(String.format("Health Score: %d/100 (AI Verified)", product.product.healthScore));
            return;
        }

        StringBuilder productData = new StringBuilder();
        productData.append("Name: ").append(product.product.productName).append("\n");
        productData.append("Brand: ").append(product.product.brands).append("\n");
        productData.append("Categories: ").append(product.product.categories).append("\n");
        if (product.ingredients != null) {
            productData.append("Ingredients: ");
            for (Ingredient ing : product.ingredients) {
                productData.append(ing.text).append(", ");
            }
        }
        if (product.nutriments != null) {
            productData.append("\nNutriments: ").append(product.nutriments.toString());
        }

        aiSummaryContainer.setVisibility(View.VISIBLE);
        aiSummaryTextView.setText("AI is reasoning with rules...");

        AiGlowManager.startGlow(this);

        List<String> rules = ruleEngine.getRuleDescriptions();

        new OpenAIAnalysisService().analyzeWithRules(productData.toString(), rules, null, new OpenAIAnalysisService.AnalysisCallback() {
            @Override
            public void onResult(String jsonResult) {
                try {
                    org.json.JSONObject obj = new org.json.JSONObject(jsonResult);
                    String summary = obj.optString("summary", "");
                    int score = obj.optInt("score", -1);
                    
                    // Extract AI findings for highlighting
                    org.json.JSONArray findings = obj.optJSONArray("findings");
                    List<String> aiTriggers = new ArrayList<>();
                    if (findings != null) {
                        for (int i = 0; i < findings.length(); i++) {
                            org.json.JSONObject finding = findings.getJSONObject(i);
                            String triggeringIngredient = finding.optString("triggering_ingredient", "");
                            if (!triggeringIngredient.isEmpty()) {
                                aiTriggers.add(triggeringIngredient);
                            }
                        }
                    }

                    if (currentReport != null && !aiTriggers.isEmpty()) {
                        currentReport.setAiTriggeringIngredients(aiTriggers);
                    }

                    if (!summary.isEmpty()) {
                        productRepository.updateProductAiInsight(product.product.barcode, summary);
                        runOnUiThread(() -> {
                            aiSummaryContainer.setVisibility(View.VISIBLE);
                            aiSummaryContainer.setAlpha(0f);
                            aiSummaryContainer.setTranslationY(20f);
                            aiSummaryContainer.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(500)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();
                            animateText(aiSummaryTextView, summary);
                            
                            // Re-highlight if we got new insights (if we had a way to get ingredients from AI)
                            if (currentReport != null) {
                                displayHighlightedIngredients(product, currentReport);
                            }
                        });
                    }

                    if (score != -1) {
                        productRepository.updateProductHealthScore(product.product.barcode, score);
                        runOnUiThread(() -> {
                            healthScoreTextView.setText(String.format("Health Score: %d/100 (AI Verified)", score));
                            healthScoreTextView.setTextColor(ContextCompat.getColor(ProductDetailsActivity.this, R.color.ai_accent));
                        });
                    }
                    
                    // Note: In a future update, we could also display the specific 'findings' in the analysisRecyclerView

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        AiGlowManager.stopGlow(ProductDetailsActivity.this);
                        aiSummaryLayout.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    AiGlowManager.stopGlow(ProductDetailsActivity.this);
                    aiSummaryContainer.setVisibility(View.GONE);
                });
            }
        });
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
                    AiGlowManager.stopGlow(ProductDetailsActivity.this);
                }
            }
        }, delay);
    }

    private void displayHighlightedIngredients(ProductWithDetails productDetails, ProductAnalysisReport report) {
        if (productDetails.ingredients == null || productDetails.ingredients.isEmpty()) {
            ingredientsTextView.setText("No ingredients listed.");
            return;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        List<String> aiTriggers = report.getAiTriggeringIngredients();

        for (Ingredient ingredient : productDetails.ingredients) {
            if (ingredient.text != null) {
                SpannableString spannable = new SpannableString(ingredient.text);
                boolean highlighted = false;

                // Check manual rule triggers
                for(AnalysisResult res : report.getResults()){
                    if(res.getTriggeringIngredient() != null && ingredient.text.toLowerCase().contains(res.getTriggeringIngredient().toLowerCase())){
                        if (res.getLevel() == AnalysisResult.WarningLevel.POSITIVE) {
                            spannable.setSpan(new BackgroundColorSpan(0x3300FF00), 0, spannable.length(), 0); // Green for positive
                        } else {
                            spannable.setSpan(new BackgroundColorSpan(0x33FF0000), 0, spannable.length(), 0); // Red for others
                        }
                        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(), 0);
                        highlighted = true;
                        break;
                    }
                }

                // Check AI triggers
                if (!highlighted && aiTriggers != null) {
                    for (String trigger : aiTriggers) {
                        if (ingredient.text.toLowerCase().contains(trigger.toLowerCase())) {
                            spannable.setSpan(new BackgroundColorSpan(0x337C4DFF), 0, spannable.length(), 0); // AI Purple tint
                            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(), 0);
                            break;
                        }
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
            case "a":
            case "1":
                return ContextCompat.getColor(this, R.color.nutriscore_a);
            case "b":
                return ContextCompat.getColor(this, R.color.nutriscore_b);
            case "c":
            case "2":
                return ContextCompat.getColor(this, R.color.nutriscore_c);
            case "d":
            case "3":
                return ContextCompat.getColor(this, R.color.nutriscore_d);
            case "e":
            case "4":
                return ContextCompat.getColor(this, R.color.nutriscore_e);
            default:
                return ContextCompat.getColor(this, R.color.score_unknown);
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
