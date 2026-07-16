package com.ciblorenzo.whatsonmyfood;

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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultAdapter;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultDeduplicator;
import com.ciblorenzo.whatsonmyfood.analysis.AiSummaryFormatter;
import com.ciblorenzo.whatsonmyfood.analysis.AiIngredientRecovery;
import com.ciblorenzo.whatsonmyfood.analysis.BitwiseAiCore;
import com.ciblorenzo.whatsonmyfood.analysis.HealthVerdict;
import com.ciblorenzo.whatsonmyfood.analysis.HealthVerdictExplanationBuilder;
import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;
import com.ciblorenzo.whatsonmyfood.analysis.ProductAnalysisReport;
import com.ciblorenzo.whatsonmyfood.analysis.rules.RuleEngine;
import com.ciblorenzo.whatsonmyfood.retail.RetailerCommerceViewBinder;
import com.ciblorenzo.whatsonmyfood.retail.RetailerRepository;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailsActivity extends BaseActivity {

    public static final String EXTRA_BARCODE = "com.ciblorenzo.whatsonmyfood.BARCODE";
    public static final String EXTRA_AI_ENABLED = "com.ciblorenzo.whatsonmyfood.AI_ENABLED";
    private static final String AI_CACHE_PREFIX = "BITWISE_AI_CACHE_V8:";
    private static final String AI_CACHE_LEGACY_PREFIX = "BITWISE_AI_CACHE_";

    private ProductRepository productRepository;
    private RetailerRepository retailerRepository;
    private ExecutorService executorService;
    private AppDatabase db;
    private RuleEngine ruleEngine;
    private FirebaseUser currentUser;

    private ImageView productImageView;
    private TextView productNameTextView, productBrandTextView, ingredientsTextView, healthScoreTextView;
    private TextView sourceStatusTextView;
    private TextView nutriscoreTextView, novaTextView, ecoscoreTextView, categoriesTextView, packagingTextView, labelsTextView, servingSizeTextView;
    private View labelsLabel;
    private HorizontalScrollView certificateBadgesScrollView;
    private LinearLayout certificateBadgesContainer;
    private Button removeFromPantryButton;
    private Button updateProductButton;
    private Button contributeIngredientsButton;
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
    private BitwiseEntitlementManager bitwiseEntitlementManager;
    private HealthVerdict latestVerdict;
    private View loadingOverlay;
    private RetailerCommerceViewBinder retailerCommerceViewBinder;
    private boolean aiEnabled = true;
    private ProductWithDetails currentProductDetails;
    private final List<String> suggestedIngredients = new ArrayList<>();
    private final Set<ProductRepository.SourceStatus> displayedSourceStatuses = new LinkedHashSet<>();
    private String translationCheckedBarcode;

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
        retailerRepository = new RetailerRepository(getApplication());
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
        sourceStatusTextView = findViewById(R.id.source_status_text_view);
        ingredientsTextView = findViewById(R.id.ingredients_text_view);
        nutriscoreTextView = findViewById(R.id.nutriscore_text_view);
        novaTextView = findViewById(R.id.nova_text_view);
        ecoscoreTextView = findViewById(R.id.ecoscore_text_view);
        categoriesTextView = findViewById(R.id.categories_text_view);
        packagingTextView = findViewById(R.id.packaging_text_view);
        labelsLabel = findViewById(R.id.labels_label);
        labelsTextView = findViewById(R.id.labels_text_view);
        certificateBadgesScrollView = findViewById(R.id.certificate_badges_scroll_view);
        certificateBadgesContainer = findViewById(R.id.certificate_badges_container);
        servingSizeTextView = findViewById(R.id.serving_size_text_view);
        healthScoreTextView = findViewById(R.id.health_score_text_view);
        updateProductButton = findViewById(R.id.update_product_button);
        contributeIngredientsButton = findViewById(R.id.contribute_ingredients_button);
        removeFromPantryButton = findViewById(R.id.remove_from_pantry_button);
        GlassMotion.attachPress(updateProductButton);
        GlassMotion.attachPress(contributeIngredientsButton);
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
        bitwiseEntitlementManager = new BitwiseEntitlementManager(this);
        bitwiseEntitlementManager.start();
        loadingOverlay = findViewById(R.id.loading_overlay);
        retailerCommerceViewBinder = new RetailerCommerceViewBinder(
                this,
                findViewById(android.R.id.content),
                retailerRepository,
                new RetailerCommerceViewBinder.Host() {
                    @Override
                    public void runOnUiThread(Runnable runnable) {
                        ProductDetailsActivity.this.runOnUiThread(runnable);
                    }

                    @Override
                    public boolean isActive() {
                        return !isFinishing() && !isDestroyed();
                    }
                }
        );
        analysisRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        nutriscoreTextView.setOnClickListener(v -> showScoreExplanation("Nutri-Score", "A nutritional rating system."));
        novaTextView.setOnClickListener(v -> showScoreExplanation("NOVA Group", "A food processing classification."));
        ecoscoreTextView.setOnClickListener(v -> showScoreExplanation("Eco-Score", "An environmental impact rating."));

        aiEnabled = getIntent().getBooleanExtra(EXTRA_AI_ENABLED, true);
        String barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        if (barcode != null) {
            loadProductDetails(barcode);
            checkIfProductInPantry(barcode);

            updateProductButton.setOnClickListener(v -> updateProductFromSources(barcode));
            contributeIngredientsButton.setOnClickListener(v -> showIngredientContribution(barcode));

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
        showCachedProductWhileRefreshing(barcode);
        productRepository.getProductByBarcode(barcode, new ProductRepository.RepositoryCallback<ProductRepository.ProductResult>() {
            @Override
            public void onComplete(ProductRepository.ProductResult result) {
                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (result != null && result.productWithDetails != null) {
                        displayProductDetails(result.productWithDetails);
                        displaySourceStatuses(result.sourceStatuses);
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

    private void showCachedProductWhileRefreshing(String barcode) {
        executorService.execute(() -> {
            ProductWithDetails cachedProduct = db.productDao().getProductWithDetails(barcode);
            if (cachedProduct == null
                    || cachedProduct.product == null
                    || isLikelyWarningOnlyIngredientCache(cachedProduct)) {
                return;
            }

            runOnUiThread(() -> {
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                displayProductDetails(cachedProduct, false);
            });
        });
    }

    private boolean isLikelyWarningOnlyIngredientCache(ProductWithDetails product) {
        if (product == null || product.ingredients == null || product.ingredients.size() != 1) {
            return false;
        }
        Ingredient ingredient = product.ingredients.get(0);
        if (ingredient == null || ingredient.text == null) {
            return true;
        }
        String normalized = ingredient.text.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
        return normalized.equals("phenylalanine")
                || normalized.equals("no calories")
                || normalized.equals("no sugar")
                || normalized.equals("zero calories")
                || normalized.equals("zero sugar");
    }

    private void updateProductFromSources(String barcode) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        if (updateProductButton != null) updateProductButton.setEnabled(false);

        productRepository.refreshProductByBarcode(barcode, new ProductRepository.RepositoryCallback<ProductRepository.ProductResult>() {
            @Override
            public void onComplete(ProductRepository.ProductResult result) {
                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (updateProductButton != null) updateProductButton.setEnabled(true);
                    if (result != null && result.productWithDetails != null) {
                        displayProductDetails(result.productWithDetails);
                        displaySourceStatuses(result.sourceStatuses);
                        Toast.makeText(ProductDetailsActivity.this, R.string.product_updated, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK, new Intent().putExtra(PantryActivity.RESULT_DATA_CHANGED, true));
                    } else {
                        Toast.makeText(ProductDetailsActivity.this, R.string.product_update_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (updateProductButton != null) updateProductButton.setEnabled(true);
                    Toast.makeText(ProductDetailsActivity.this, getString(R.string.product_update_failed_with_reason, e.getMessage()), Toast.LENGTH_LONG).show();
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
        displayProductDetails(productDetails, true);
    }

    private void displayProductDetails(ProductWithDetails productDetails, boolean allowAiInsight) {
        currentProductDetails = productDetails;
        collapsingToolbarLayout.setTitle(" ");
        if (productDetails.product.imageUrl != null && !productDetails.product.imageUrl.isEmpty()) {
            Picasso.get().load(productDetails.product.imageUrl).into(productImageView);
        }

        productNameTextView.setText(displayProductName(productDetails.product.productName));
        productBrandTextView.setText(productDetails.product.brands != null ? productDetails.product.brands : "");
        if (retailerCommerceViewBinder != null) {
            retailerCommerceViewBinder.bind(productDetails);
        }

        currentReport = ruleEngine.analyze(productDetails);
        boolean ingredientsMissing = !hasListedIngredients(productDetails);
        contributeIngredientsButton.setVisibility(ingredientsMissing ? View.VISIBLE : View.GONE);
        if (!ingredientsMissing) suggestedIngredients.clear();
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
        ProductCertificateBadgeRenderer.bind(
                this,
                labelsLabel,
                labelsTextView,
                certificateBadgesScrollView,
                certificateBadgesContainer,
                productDetails.product.labels
        );
        servingSizeTextView.setText(productDetails.product.servingSize != null ? productDetails.product.servingSize : "");

        displayNutriments(productDetails.nutriments);
        GlassMotion.enter(detailsLayout, 0L);

        if (allowAiInsight && hasListedIngredients(productDetails)
                && translateListedIngredientsIfNeeded(productDetails)) {
            return;
        }
        if (allowAiInsight && aiEnabled) {
            performAiReasoning(productDetails);
        } else if (aiSummaryContainer != null) {
            aiSummaryContainer.setVisibility(View.GONE);
            AiGlowManager.stopGlow(this);
        }
    }

    private void performAiReasoning(ProductWithDetails product) {
        if (aiSummaryContainer == null) return;
        if (hasListedIngredients(product) && displayCachedAiInsight(product)) return;
        if (!bitwiseEntitlementManager.canUseBitwise()) {
            showBitwiseUpgradePrompt();
            return;
        }

        aiSummaryContainer.setVisibility(View.VISIBLE);
        aiSummaryContainer.setOnClickListener(null);
        GlassMotion.enter(aiSummaryContainer, 120L);
        aiSummaryTextView.setText(R.string.bitwise_reasoning);
        AiGlowManager.startGlow(this);
        bitwiseEntitlementManager.recordBitwiseUse();

        StringBuilder productData = new StringBuilder();
        productData.append("response_language: ").append(LanguageManager.getLanguageName(this)).append("\n");
        productData.append("Barcode: ").append(product.product.barcode).append("\n");
        productData.append("Name: ").append(isMeaningfulProductName(product.product.productName) ? product.product.productName : "").append("\n");
        productData.append("Brand: ").append(product.product.brands).append("\n");
        productData.append("Categories: ").append(product.product.categories).append("\n");
        productData.append("Quantity: ").append(product.product.quantity).append("\n");
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
                        String summary = com.ciblorenzo.whatsonmyfood.analysis.AiSummaryFormatter.format(obj.optString("summary", ""));
                        String aiVerdict = obj.optString("verdict", "");
                        String aiVerdictReason = obj.optString("verdict_reason", "");
                        AiIngredientRecovery.Recovery recoveredIngredients = AiIngredientRecovery.parse(result);

                        if (isMeaningfulProductName(aiName)) {
                            productNameTextView.setText(aiName);
                        }

                        if (!aiBrand.isEmpty()) {
                            productBrandTextView.setText(aiBrand);
                        }
                        if (AiIngredientRecovery.shouldDisplay(product, recoveredIngredients)) {
                            suggestedIngredients.clear();
                            suggestedIngredients.addAll(recoveredIngredients.ingredients);
                            contributeIngredientsButton.setVisibility(View.VISIBLE);
                            displayRecoveredIngredientsInEnglish(recoveredIngredients);
                            addSourceStatus(ProductRepository.SourceStatus.INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE);
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
                            HealthVerdict summaryVerdict = HealthVerdict.fromAiVerdict(
                                    aiVerdict,
                                    aiVerdictReason,
                                    currentReport != null ? currentReport.getResults() : null,
                                    getIngredientCount(product)
                            );
                            if (summaryVerdict.getStatus() == HealthVerdict.Status.NOT_HEALTHY
                                    && currentReport != null
                                    && HealthVerdictExplanationBuilder.isContradictingNotHealthy(summary)) {
                                String localExplanation = HealthVerdictExplanationBuilder.buildNotHealthyExplanation(product, currentReport.getResults());
                                if (!localExplanation.isEmpty()) {
                                    summary = localExplanation;
                                    sourcesArr = HealthVerdictExplanationBuilder.buildSources(currentReport.getResults());
                                }
                            }
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
                                        parseAiWarningLevel(f.optString("impact")),
                                        0, f.optString("triggering_ingredient"), f.optString("explanation"));
                                    res.setSourceUrl(f.optString("source_url", ""));
                                    res.setVisualQuote(f.optString("visual_quote", ""));
                                    aiResults.add(res);
                                }
                            }
                            if (currentReport != null && currentReport.getResults() != null) {
                                List<AnalysisResult> combinedResults = new ArrayList<>(currentReport.getResults());
                                combinedResults.addAll(aiResults);
                                combinedResults = AnalysisResultDeduplicator.deduplicate(combinedResults);
                                analysisRecyclerView.setAdapter(new AnalysisResultAdapter(combinedResults));
                                applyHealthVerdict(product, currentReport, combinedResults, aiVerdict, aiVerdictReason);
                            } else {
                                List<AnalysisResult> dedupedAiResults = AnalysisResultDeduplicator.deduplicate(aiResults);
                                analysisRecyclerView.setAdapter(new AnalysisResultAdapter(dedupedAiResults));
                                applyHealthVerdict(product, null, dedupedAiResults, aiVerdict, aiVerdictReason);
                            }
                        } else {
                            applyHealthVerdict(product, currentReport, currentReport != null ? currentReport.getResults() : null, aiVerdict, aiVerdictReason);
                        }
                    } catch (Exception e) {
                        aiSummaryTextView.setText("Bitwise could not format this explanation. Please try again.");
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    aiSummaryTextView.setText(t.getMessage());
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
        if (currentReport != null) {
            applyRuleBasedScore(product, currentReport);
        }

        String summary = cachedInsight.summary;
        org.json.JSONArray sources = cachedInsight.sources;
        if (latestVerdict != null
                && latestVerdict.getStatus() == HealthVerdict.Status.NOT_HEALTHY
                && currentReport != null
                && HealthVerdictExplanationBuilder.isContradictingNotHealthy(summary)) {
            String localExplanation = HealthVerdictExplanationBuilder.buildNotHealthyExplanation(product, currentReport.getResults());
            if (!localExplanation.isEmpty()) {
                summary = localExplanation;
                sources = HealthVerdictExplanationBuilder.buildSources(currentReport.getResults());
            }
        }

        aiSummaryTextView.setText(android.text.Html.fromHtml(summary, android.text.Html.FROM_HTML_MODE_COMPACT));
        displaySources(sources);

        AiGlowManager.stopGlow(this);
        return true;
    }

    private void applyRuleBasedScore(ProductWithDetails product, ProductAnalysisReport report) {
        applyHealthVerdict(product, report, report.getResults(), null, null);
    }

    private void applyHealthVerdict(ProductWithDetails product, ProductAnalysisReport report, List<AnalysisResult> results, String aiVerdict, String aiVerdictReason) {
        if (product == null || product.product == null) return;

        if (report != null) {
            int ruleScore = report.getOverallScore();
            product.product.healthScore = ruleScore;
            productRepository.updateProductHealthScore(product.product.barcode, ruleScore);
            setResult(RESULT_OK, new Intent().putExtra(PantryActivity.RESULT_DATA_CHANGED, true));
        }

        latestVerdict = HealthVerdict.fromAiVerdict(aiVerdict, aiVerdictReason, results, getIngredientCount(product));
        healthScoreTextView.setText(latestVerdict.getLabel());
        healthScoreTextView.setTextColor(getVerdictColor(latestVerdict));
    }

    private int getIngredientCount(ProductWithDetails product) {
        return product != null && product.ingredients != null ? product.ingredients.size() : 0;
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
        if (storedInsight != null && storedInsight.startsWith(AI_CACHE_LEGACY_PREFIX)) {
            return new CachedAiInsight("", null);
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
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (Ingredient ingredient : product.ingredients) {
            if (ingredient != null && ingredient.text != null && !ingredient.text.trim().isEmpty()) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(ingredient.text.trim());
            }
        }
        return builder.toString();
    }

    private boolean hasListedIngredients(ProductWithDetails product) {
        if (product == null || product.ingredients == null) return false;
        for (Ingredient ingredient : product.ingredients) {
            if (ingredient != null && ingredient.text != null && !ingredient.text.trim().isEmpty()) return true;
        }
        return false;
    }

    private void showIngredientContribution(String barcode) {
        OpenFoodFactsContributionDialog.show(
                this,
                barcode,
                suggestedIngredients,
                verifiedIngredients -> saveConfirmedIngredients(barcode, verifiedIngredients)
        );
    }

    private boolean translateListedIngredientsIfNeeded(ProductWithDetails product) {
        String barcode = product != null && product.product != null ? product.product.barcode : null;
        if (barcode == null || barcode.equals(translationCheckedBarcode)) return false;
        translationCheckedBarcode = barcode;
        String original = formatIngredientsForAi(product);
        if (original.isEmpty()) return false;
        ingredientsTextView.append("\n\n" + getString(R.string.ingredients_checking_language));
        MlKitIngredientTranslator.translateToEnglish(original, new MlKitIngredientTranslator.Callback() {
            @Override
            public void onSuccess(MlKitIngredientTranslator.TranslationResult result) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (result.translated && !result.englishText.isEmpty()) {
                        product.ingredients = buildIngredientList(barcode, result.englishText);
                        currentProductDetails = product;
                        executorService.execute(() -> db.productDao().insertProductWithDetails(product));
                    }
                    displayProductDetails(product, true);
                });
            }

            @Override
            public void onError(@NonNull Exception error) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) displayProductDetails(product, true);
                });
            }
        });
        return true;
    }

    private void displayRecoveredIngredientsInEnglish(AiIngredientRecovery.Recovery recovery) {
        String original = OpenFoodFactsContributionValidator.joinSuggestedIngredients(recovery.ingredients);
        ingredientsTextView.setText(recovery.toDisplayText() + "\n\n" + getString(R.string.ingredients_checking_language));
        MlKitIngredientTranslator.translateToEnglish(original, new MlKitIngredientTranslator.Callback() {
            @Override
            public void onSuccess(MlKitIngredientTranslator.TranslationResult result) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || !result.translated) return;
                    ingredientsTextView.setText(getString(
                            R.string.ingredients_translation_display,
                            result.sourceLanguage,
                            result.englishText,
                            result.originalText
                    ));
                });
            }

            @Override
            public void onError(@NonNull Exception error) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) ingredientsTextView.setText(recovery.toDisplayText());
                });
            }
        });
    }

    private void displaySourceStatuses(List<ProductRepository.SourceStatus> statuses) {
        displayedSourceStatuses.clear();
        if (statuses != null) {
            displayedSourceStatuses.addAll(statuses);
        }
        renderSourceStatuses();
    }

    private void addSourceStatus(ProductRepository.SourceStatus status) {
        if (status == null) return;
        displayedSourceStatuses.add(status);
        renderSourceStatuses();
    }

    private void renderSourceStatuses() {
        if (sourceStatusTextView == null) return;
        String message = SourceStatusMessageFormatter.format(
                this,
                new ArrayList<>(displayedSourceStatuses)
        );
        sourceStatusTextView.setText(message);
        sourceStatusTextView.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private List<Ingredient> buildIngredientList(String barcode, String ingredientText) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (String text : IngredientTextParser.parseIngredientCandidates(ingredientText)) {
            ingredients.add(new Ingredient(barcode, text, ingredients.size()));
        }
        return ingredients;
    }

    private void saveConfirmedIngredients(String barcode, String verifiedIngredients) {
        if (currentProductDetails == null || currentProductDetails.product == null) return;
        currentProductDetails.ingredients = buildIngredientList(barcode, verifiedIngredients);
        suggestedIngredients.clear();
        contributeIngredientsButton.setVisibility(View.GONE);
        executorService.execute(() -> {
            db.productDao().insertProductWithDetails(currentProductDetails);
            runOnUiThread(() -> displayProductDetails(currentProductDetails, false));
        });
    }

    private String displayProductName(String value) {
        return isMeaningfulProductName(value) ? value.trim() : "Product name unavailable";
    }

    private boolean isMeaningfulProductName(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String normalized = value.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
        return !normalized.equals("scanned product")
                && !normalized.equals("scanned")
                && !normalized.equals("product")
                && !normalized.equals("name")
                && !normalized.equals("unknown")
                && !normalized.equals("unknown product")
                && !normalized.equals("n a");
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
                AnalysisResult highlightResult = findIngredientHighlightResult(ingredient.text, report.getResults());
                if (highlightResult != null) {
                    int color = highlightResult.getLevel() == AnalysisResult.WarningLevel.POSITIVE ? 0x3300FF00 : 0x33FF0000;
                    spannable.setSpan(new BackgroundColorSpan(color), 0, spannable.length(), 0);
                    spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(), 0);
                }
                builder.append(spannable).append("\n");
            }
        }
        ingredientsTextView.setText(builder);
    }

    private AnalysisResult findIngredientHighlightResult(String ingredientText, List<AnalysisResult> results) {
        AnalysisResult best = null;
        if (ingredientText == null || results == null) return null;
        for (AnalysisResult result : results) {
            if (result == null || !matchesTrigger(ingredientText, result.getTriggeringIngredient())) {
                continue;
            }
            if (best == null || ingredientHighlightPriority(ingredientText, result) > ingredientHighlightPriority(ingredientText, best)) {
                best = result;
            }
        }
        return best;
    }

    private boolean matchesTrigger(String ingredientText, String trigger) {
        if (trigger == null || trigger.trim().isEmpty()) return false;
        String normalizedIngredient = ingredientText.toLowerCase(Locale.US);
        if (isNutritionClaimNotIngredient(normalizedIngredient)) return false;
        for (String part : trigger.split("[,;]")) {
            String normalizedTrigger = part.trim().toLowerCase(Locale.US);
            if (!normalizedTrigger.isEmpty() && ingredientTriggerMatches(normalizedIngredient, normalizedTrigger)) {
                return true;
            }
        }
        return false;
    }

    private void showBitwiseUpgradePrompt() {
        AiGlowManager.stopGlow(this);
        aiSummaryContainer.clearAnimation();
        aiSummaryContainer.setVisibility(View.VISIBLE);
        GlassMotion.enter(aiSummaryContainer, 120L);
        aiSummaryTextView.setText(getString(R.string.bitwise_free_limit_reached));
        if (aiSourcesDivider != null) aiSourcesDivider.setVisibility(View.GONE);
        if (aiSourcesLabel != null) aiSourcesLabel.setVisibility(View.GONE);
        if (aiSourcesTextView != null) aiSourcesTextView.setVisibility(View.GONE);
        aiSummaryContainer.setOnClickListener(v -> startActivity(new Intent(this, SubscriptionActivity.class)));
        Toast.makeText(this, "Tap the Bitwise card to see Plus options.", Toast.LENGTH_LONG).show();
    }

    private boolean ingredientTriggerMatches(String normalizedIngredient, String normalizedTrigger) {
        return normalizedIngredient.equals(normalizedTrigger)
                || normalizedIngredient.matches(".*\\b" + java.util.regex.Pattern.quote(normalizedTrigger) + "\\b.*")
                || normalizedTrigger.matches(".*\\b" + java.util.regex.Pattern.quote(normalizedIngredient) + "\\b.*");
    }

    private boolean isNutritionClaimNotIngredient(String normalizedIngredient) {
        return normalizedIngredient.matches("^(no|zero)\\s+(added\\s+)?(sugar|calories)\\b.*")
                || normalizedIngredient.matches("^sugar\\s*free\\b.*")
                || normalizedIngredient.matches("^\\d+\\s*(g|mg)?\\s*(added\\s+)?sugar\\b.*");
    }

    private int ingredientHighlightPriority(String ingredientText, AnalysisResult result) {
        String normalizedIngredient = ingredientText.toLowerCase(Locale.US);
        boolean organicOverride = normalizedIngredient.contains("organic") && !containsRefinedOilRisk(normalizedIngredient);
        if (organicOverride && result.getLevel() == AnalysisResult.WarningLevel.POSITIVE) {
            return 100;
        }
        if (result.getLevel() == AnalysisResult.WarningLevel.SEVERE) return 80;
        if (result.getLevel() == AnalysisResult.WarningLevel.WARNING) return 70;
        if (result.getLevel() == AnalysisResult.WarningLevel.POSITIVE) return 60;
        return 20;
    }

    private boolean containsRefinedOilRisk(String normalizedIngredient) {
        return normalizedIngredient.contains("vegetable oil")
                || normalizedIngredient.contains("sunflower oil")
                || normalizedIngredient.contains("canola oil")
                || normalizedIngredient.contains("soybean oil")
                || normalizedIngredient.contains("corn oil")
                || normalizedIngredient.contains("palm oil")
                || normalizedIngredient.contains("palm kernel oil")
                || normalizedIngredient.contains("shortening")
                || normalizedIngredient.contains("partially hydrogenated")
                || normalizedIngredient.contains("hydrogenated oil");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bitwiseEntitlementManager != null) {
            bitwiseEntitlementManager.end();
        }
        if (productRepository != null) {
            productRepository.close();
        }
        if (retailerRepository != null) {
            retailerRepository.close();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void showScoreExplanation(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }
}
