package com.ciblorenzo.whatsonmyfood;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultAdapter;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultDeduplicator;
import com.ciblorenzo.whatsonmyfood.analysis.AiSummaryFormatter;
import com.ciblorenzo.whatsonmyfood.analysis.HealthVerdict;
import com.ciblorenzo.whatsonmyfood.analysis.HealthVerdictExplanationBuilder;
import com.ciblorenzo.whatsonmyfood.analysis.OpenAIAnalysisService;
import com.ciblorenzo.whatsonmyfood.analysis.ProductAnalysisReport;
import com.ciblorenzo.whatsonmyfood.analysis.rules.RuleEngine;
import com.ciblorenzo.whatsonmyfood.retail.RetailerCommerceViewBinder;
import com.ciblorenzo.whatsonmyfood.retail.RetailerRepository;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailsFragment extends BottomSheetDialogFragment {

    private static final String ARG_BARCODE = "barcode";
    private static final String ARG_AI_ENABLED = "ai_enabled";
    private static final String AI_CACHE_PREFIX = "BITWISE_AI_CACHE_V7:";
    private static final String AI_CACHE_LEGACY_PREFIX = "BITWISE_AI_CACHE_";

    private ProductRepository productRepository;
    private RetailerRepository retailerRepository;
    private ExecutorService executorService;
    private AppDatabase db;
    private RuleEngine ruleEngine;
    private FirebaseUser currentUser;

    private ImageView productImageView;
    private TextView productNameTextView, productBrandTextView, packagingTextView, labelsTextView, ingredientsTextView;
    private TextView nutriscoreTextView, novaTextView, ecoscoreTextView, categoriesTextView, servingSizeTextView, healthScoreTextView;
    private View labelsLabel;
    private HorizontalScrollView certificateBadgesScrollView;
    private LinearLayout certificateBadgesContainer;
    private TextView aiSummaryTextView;
    private View aiSummaryLayout;
    private View aiSummaryContainer;
    private View detailsLayout;
    private AiGlowView aiCardGlow;
    private android.widget.Button updateProductButton;
    private android.widget.Button removeFromPantryButton;
    private TableLayout nutritionFactsTable;
    private RecyclerView analysisRecyclerView;
    private View loadingOverlay;
    private RetailerCommerceViewBinder retailerCommerceViewBinder;

    private TextView aiSourcesTextView;
    private View aiSourcesDivider;
    private View aiSourcesLabel;
    private HealthVerdict latestVerdict;

    public static ProductDetailsFragment newInstance(String barcode) {
        return newInstance(barcode, true);
    }

    public static ProductDetailsFragment newInstance(String barcode, boolean aiEnabled) {
        ProductDetailsFragment fragment = new ProductDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BARCODE, barcode);
        args.putBoolean(ARG_AI_ENABLED, aiEnabled);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        productRepository = new ProductRepository(requireActivity().getApplication());
        retailerRepository = new RetailerRepository(requireActivity().getApplication());
        executorService = Executors.newSingleThreadExecutor();
        db = AppDatabase.getDatabase(requireContext());
        ruleEngine = new RuleEngine();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_details, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setFitToContents(false);
                behavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels / 2);
                behavior.setHideable(true);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        productImageView = view.findViewById(R.id.product_image_view);
        productNameTextView = view.findViewById(R.id.product_name_text_view);
        productBrandTextView = view.findViewById(R.id.product_brand_text_view);
        packagingTextView = view.findViewById(R.id.packaging_text_view);
        labelsLabel = view.findViewById(R.id.labels_label);
        labelsTextView = view.findViewById(R.id.labels_text_view);
        certificateBadgesScrollView = view.findViewById(R.id.certificate_badges_scroll_view);
        certificateBadgesContainer = view.findViewById(R.id.certificate_badges_container);
        ingredientsTextView = view.findViewById(R.id.ingredients_text_view);
        nutriscoreTextView = view.findViewById(R.id.nutriscore_text_view);
        novaTextView = view.findViewById(R.id.nova_text_view);
        ecoscoreTextView = view.findViewById(R.id.ecoscore_text_view);
        categoriesTextView = view.findViewById(R.id.categories_text_view);
        servingSizeTextView = view.findViewById(R.id.serving_size_text_view);
        healthScoreTextView = view.findViewById(R.id.health_score_text_view);
        aiSummaryTextView = view.findViewById(R.id.ai_summary_text_view);
        detailsLayout = view.findViewById(R.id.details_layout);
        aiSummaryLayout = view.findViewById(R.id.ai_summary_layout);
        aiSummaryContainer = view.findViewById(R.id.ai_summary_container);
        aiCardGlow = view.findViewById(R.id.ai_card_glow);
        aiSourcesTextView = view.findViewById(R.id.ai_sources_text_view);
        aiSourcesDivider = view.findViewById(R.id.ai_sources_divider);
        aiSourcesLabel = view.findViewById(R.id.ai_sources_label);
        updateProductButton = view.findViewById(R.id.update_product_button);
        removeFromPantryButton = view.findViewById(R.id.remove_from_pantry_button);
        GlassMotion.attachPress(updateProductButton);
        GlassMotion.attachPress(removeFromPantryButton);
        nutritionFactsTable = view.findViewById(R.id.nutrition_facts_table);
        analysisRecyclerView = view.findViewById(R.id.analysis_recycler_view);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        retailerCommerceViewBinder = new RetailerCommerceViewBinder(
                requireContext(),
                view,
                retailerRepository,
                new RetailerCommerceViewBinder.Host() {
                    @Override
                    public void runOnUiThread(Runnable runnable) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(runnable);
                        }
                    }

                    @Override
                    public boolean isActive() {
                        return isAdded() && getActivity() != null;
                    }
                }
        );
        analysisRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        nutriscoreTextView.setOnClickListener(v -> showScoreExplanation("Nutri-Score", "A nutritional rating system."));
        novaTextView.setOnClickListener(v -> showScoreExplanation("NOVA Group", "A food processing classification."));
        ecoscoreTextView.setOnClickListener(v -> showScoreExplanation("Eco-Score", "An environmental impact rating."));

        if (currentUser == null) {
            showErrorAndDismiss("Authentication error.");
            return;
        }

        String barcode = getArguments() != null ? getArguments().getString(ARG_BARCODE) : null;
        if (barcode != null) {
            loadProductDetails(barcode);
            updateProductButton.setOnClickListener(v -> updateProductFromSources(barcode));
            removeFromPantryButton.setOnClickListener(v -> {
                executorService.execute(() -> {
                    db.productDao().deletePantryProduct(barcode, currentUser.getUid());
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Removed from Pantry", Toast.LENGTH_SHORT).show();
                            checkIfProductInPantry(barcode);
                        });
                    }
                });
            });
        } else {
            showErrorAndDismiss("No barcode provided.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    private void loadProductDetails(String barcode) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        showCachedProductWhileRefreshing(barcode);
        productRepository.getProductByBarcode(barcode, new ProductRepository.RepositoryCallback<ProductRepository.ProductResult>() {
            @Override
            public void onComplete(ProductRepository.ProductResult result) {
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (result != null && result.productWithDetails != null) {
                        displayProductDetails(result.productWithDetails);
                        executorService.execute(() -> {
                            db.productDao().insertPantry(new Pantry(barcode, currentUser.getUid()));
                            checkIfProductInPantry(barcode);
                        });
                    } else {
                        showAddProductDialog(barcode);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    showErrorAndDismiss("Error: " + e.getMessage());
                });
            }
        });
    }

    private void showCachedProductWhileRefreshing(String barcode) {
        executorService.execute(() -> {
            ProductWithDetails cachedProduct = db.productDao().getProductWithDetails(barcode);
            if (cachedProduct == null
                    || cachedProduct.product == null
                    || isLikelyWarningOnlyIngredientCache(cachedProduct)
                    || getActivity() == null
                    || !isAdded()) {
                return;
            }

            getActivity().runOnUiThread(() -> {
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
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (updateProductButton != null) updateProductButton.setEnabled(true);
                    if (result != null && result.productWithDetails != null) {
                        displayProductDetails(result.productWithDetails);
                        Toast.makeText(getContext(), R.string.product_updated, Toast.LENGTH_SHORT).show();
                        executorService.execute(() -> {
                            db.productDao().insertPantry(new Pantry(barcode, currentUser.getUid()));
                            checkIfProductInPantry(barcode);
                        });
                    } else {
                        Toast.makeText(getContext(), R.string.product_update_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (updateProductButton != null) updateProductButton.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.product_update_failed_with_reason, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void checkIfProductInPantry(String barcode) {
        executorService.execute(() -> {
            Pantry pantryItem = db.productDao().findPantryItemByBarcode(barcode, currentUser.getUid());
            if(getActivity() != null) {
                getActivity().runOnUiThread(() -> removeFromPantryButton.setVisibility(pantryItem != null ? View.VISIBLE : View.GONE));
            }
        });
    }

    private ProductAnalysisReport currentReport;

    private void displayProductDetails(ProductWithDetails productDetails) {
        displayProductDetails(productDetails, true);
    }

    private void displayProductDetails(ProductWithDetails productDetails, boolean allowAiInsight) {
        if (productDetails.product.imageUrl != null && !productDetails.product.imageUrl.isEmpty()) {
            Picasso.get().load(productDetails.product.imageUrl).into(productImageView);
        }

        productNameTextView.setText(displayProductName(productDetails.product.productName));
        productBrandTextView.setText(productDetails.product.brands != null ? productDetails.product.brands : "");
        if (retailerCommerceViewBinder != null) {
            retailerCommerceViewBinder.bind(productDetails);
        }
        packagingTextView.setText(productDetails.product.packaging != null ? productDetails.product.packaging : "");
        ProductCertificateBadgeRenderer.bind(
                requireContext(),
                labelsLabel,
                labelsTextView,
                certificateBadgesScrollView,
                certificateBadgesContainer,
                productDetails.product.labels
        );
        categoriesTextView.setText(productDetails.product.categories != null ? productDetails.product.categories : "");
        servingSizeTextView.setText(productDetails.product.servingSize != null ? productDetails.product.servingSize : "");

        currentReport = ruleEngine.analyze(productDetails);
        if (currentReport != null) {
            applyRuleBasedScore(productDetails, currentReport);
            analysisRecyclerView.setAdapter(new AnalysisResultAdapter(currentReport.getResults()));
            displayHighlightedIngredients(productDetails, currentReport);
        } else {
            healthScoreTextView.setText("Needs Review");
            healthScoreTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.score_unknown));
            analysisRecyclerView.setAdapter(null);
            ingredientsTextView.setText("Could not analyze ingredients.");
        }

        setScoreTextView(nutriscoreTextView, productDetails.product.nutriscoreGrade, "Nutri-Score");
        setScoreTextView(novaTextView, productDetails.product.novaGroup, "NOVA Group");
        setScoreTextView(ecoscoreTextView, productDetails.product.ecoscoreGrade, "Eco-Score");

        displayNutriments(productDetails.nutriments);
        GlassMotion.enter(detailsLayout, 0L);

        if (allowAiInsight && isAiEnabled()) {
            fetchAiInsight(productDetails);
        } else if (!allowAiInsight || !isAiEnabled()) {
            hideAiInsight();
        }
    }

    private boolean isAiEnabled() {
        return getArguments() == null || getArguments().getBoolean(ARG_AI_ENABLED, true);
    }

    private void hideAiInsight() {
        if (aiSummaryContainer != null) {
            aiSummaryContainer.setVisibility(View.GONE);
        }
        if (getActivity() != null) {
            AiGlowManager.stopGlow(getActivity());
        }
    }

    private void fetchAiInsight(ProductWithDetails productDetails) {
        if (aiSummaryContainer == null) return;
        if (displayCachedAiInsight(productDetails)) return;

        aiSummaryContainer.setVisibility(View.VISIBLE);
        GlassMotion.enter(aiSummaryContainer, 120L);
        aiSummaryTextView.setText(R.string.bitwise_reasoning);
        AiGlowManager.startGlow(getActivity());

        StringBuilder productData = new StringBuilder();
        productData.append("response_language: ").append(LanguageManager.getLanguageName(requireContext())).append("\n");
        productData.append("Name: ").append(isMeaningfulProductName(productDetails.product.productName) ? productDetails.product.productName : "").append("\n");
        productData.append("Brand: ").append(productDetails.product.brands).append("\n");
        productData.append("Ingredients: ").append(formatIngredientsForAi(productDetails)).append("\n");
        if (productDetails.nutriments != null) {
            productData.append("\nNutrition: ").append(productDetails.nutriments.toString());
        }

        new OpenAIAnalysisService().analyzeWithRules(productData.toString(), null, new OpenAIAnalysisService.AnalysisCallback() {
            @Override
            public void onResult(String result) {
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    try {
                        org.json.JSONObject obj = new org.json.JSONObject(result);

                        String aiName = obj.optString("product_name", "");
                        String aiBrand = obj.optString("brand", "");
                        String summary = AiSummaryFormatter.format(obj.optString("summary", ""));
                        String aiVerdict = obj.optString("verdict", "");
                        String aiVerdictReason = obj.optString("verdict_reason", "");

                        if (isMeaningfulProductName(aiName)) productNameTextView.setText(aiName);
                        if (!aiBrand.isEmpty() && !aiBrand.equalsIgnoreCase("Brand")) productBrandTextView.setText(aiBrand);

                        // 2. Nutrition table
                        org.json.JSONObject aiNutrition = obj.optJSONObject("nutrition");
                        if (aiNutrition != null && productDetails.nutriments != null) {
                            if (aiNutrition.has("energy")) productDetails.nutriments.energy = parseDouble(aiNutrition.optString("energy"));
                            if (aiNutrition.has("fat")) productDetails.nutriments.fat = parseDouble(aiNutrition.optString("fat"));
                            if (aiNutrition.has("sugars")) productDetails.nutriments.sugars = parseDouble(aiNutrition.optString("sugars"));
                            if (aiNutrition.has("protein")) productDetails.nutriments.proteins = parseDouble(aiNutrition.optString("protein"));
                            displayNutriments(productDetails.nutriments);
                        }

                        // 3. Findings
                        org.json.JSONArray aiFindings = obj.optJSONArray("findings");
                        if (aiFindings != null && aiFindings.length() > 0) {
                            List<AnalysisResult> aiResults = new ArrayList<>();
                            for (int i = 0; i < aiFindings.length(); i++) {
                                org.json.JSONObject f = aiFindings.optJSONObject(i);
                                if (f != null) {
                                    aiResults.add(new AnalysisResult(f.optString("rule"),
                                        parseAiWarningLevel(f.optString("impact")),
                                        0, f.optString("triggering_ingredient"), f.optString("explanation")));
                                }
                            }
                            if (currentReport != null && currentReport.getResults() != null) {
                                List<AnalysisResult> combinedResults = new ArrayList<>(currentReport.getResults());
                                combinedResults.addAll(aiResults);
                                combinedResults = AnalysisResultDeduplicator.deduplicate(combinedResults);
                                analysisRecyclerView.setAdapter(new AnalysisResultAdapter(combinedResults));
                                applyHealthVerdict(productDetails, currentReport, combinedResults, aiVerdict, aiVerdictReason);
                            } else {
                                List<AnalysisResult> dedupedAiResults = AnalysisResultDeduplicator.deduplicate(aiResults);
                                analysisRecyclerView.setAdapter(new AnalysisResultAdapter(dedupedAiResults));
                                applyHealthVerdict(productDetails, null, dedupedAiResults, aiVerdict, aiVerdictReason);
                            }
                        } else {
                            applyHealthVerdict(productDetails, currentReport, currentReport != null ? currentReport.getResults() : null, aiVerdict, aiVerdictReason);
                        }
                        if (currentReport != null) {
                            displayHighlightedIngredients(productDetails, currentReport);
                        }

                        if (!summary.isEmpty()) {
                            org.json.JSONArray sourcesArr = obj.optJSONArray("sources");
                            android.text.SpannableStringBuilder sourcesBuilder = new android.text.SpannableStringBuilder();
                            if (sourcesArr != null) {
                                for (int i = 0; i < sourcesArr.length(); i++) {
                                    org.json.JSONObject sourceObj = sourcesArr.optJSONObject(i);
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
                                                    if (getActivity() != null) {
                                                        LinkHandler.openLink(getActivity(), url, sName, visualQuote);
                                                    }
                                                }
                                            }, start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                            sourcesBuilder.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.colorPrimary)), start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        }
                                    }
                                }
                            }
                            if (getActivity() != null && isAdded()) {
                                getActivity().runOnUiThread(() -> {
                                    updateAiUI(productDetails, summary, sourcesArr);
                                });
                            }
                        } else {
                            updateAiUI(productDetails, result, null);
                        }

                    } catch (Exception e) {
                        String summaryFallback = result.replaceAll("\"[a-z_]+\":", "").replaceAll("[{}\\[\\]\"]", "").trim();
                        updateAiUI(productDetails, summaryFallback, null);
                    }
                });
            }

            private Double parseDouble(String val) {
                try { return Double.parseDouble(val.replaceAll("[^0-9.]", "")); } catch (Exception e) { return 0.0; }
            }

            @Override
            public void onError(Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        AiGlowManager.stopGlow(getActivity());
                        aiSummaryTextView.setText("Bitwise AI is on break. Error: " + t.getMessage());
                    });
                }
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
                && currentReport != null) {
            String localExplanation = HealthVerdictExplanationBuilder.buildNotHealthyExplanation(product, currentReport.getResults());
            if (!localExplanation.isEmpty()) {
                summary = localExplanation;
                sources = HealthVerdictExplanationBuilder.buildSources(currentReport.getResults());
            }
        }

        aiSummaryTextView.setText(android.text.Html.fromHtml(summary, android.text.Html.FROM_HTML_MODE_COMPACT));
        displaySources(sources);

        if (getActivity() != null) {
            AiGlowManager.stopGlow(getActivity());
        }
        return true;
    }

    private void applyRuleBasedScore(ProductWithDetails product, ProductAnalysisReport report) {
        applyHealthVerdict(product, report, report.getResults(), null, null);
    }

    private void applyHealthVerdict(ProductWithDetails product, ProductAnalysisReport report, List<AnalysisResult> results, String aiVerdict, String aiVerdictReason) {
        if (product == null || product.product == null || getContext() == null) return;

        if (report != null) {
            int ruleScore = report.getOverallScore();
            product.product.healthScore = ruleScore;
            productRepository.updateProductHealthScore(product.product.barcode, ruleScore);
        }
        latestVerdict = HealthVerdict.fromAiVerdict(aiVerdict, aiVerdictReason, results, getIngredientCount(product));
        healthScoreTextView.setText(latestVerdict.getLabel());
        healthScoreTextView.setTextColor(getVerdictColor(latestVerdict));
    }

    private int getIngredientCount(ProductWithDetails product) {
        return product != null && product.ingredients != null ? product.ingredients.size() : 0;
    }

    private int getVerdictColor(HealthVerdict verdict) {
        if (getContext() == null) return 0xFF9CA3AF;
        if (verdict == null) return ContextCompat.getColor(requireContext(), R.color.score_unknown);
        switch (verdict.getStatus()) {
            case HEALTHY:
                return ContextCompat.getColor(requireContext(), R.color.nutriscore_a);
            case NOT_HEALTHY:
                return ContextCompat.getColor(requireContext(), R.color.nutriscore_e);
            default:
                return ContextCompat.getColor(requireContext(), R.color.score_unknown);
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
        if (aiSourcesTextView == null || getContext() == null) return;

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
                        if (getActivity() != null) {
                            LinkHandler.openLink(getActivity(), url, name, visualQuote);
                        }
                    }
                }, start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.colorPrimary)), start + 2, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    private void updateAiUI(ProductWithDetails product, String text, org.json.JSONArray sources) {
        if (getActivity() == null || !isAdded()) return;
        if (currentReport != null) {
            applyRuleBasedScore(product, currentReport);
        }
        if (latestVerdict != null
                && latestVerdict.getStatus() == HealthVerdict.Status.NOT_HEALTHY
                && currentReport != null) {
            String localExplanation = HealthVerdictExplanationBuilder.buildNotHealthyExplanation(product, currentReport.getResults());
            if (!localExplanation.isEmpty()) {
                text = localExplanation;
                sources = HealthVerdictExplanationBuilder.buildSources(currentReport.getResults());
            }
        }
        String cachedInsight = buildAiInsightCache(text, sources);
        productRepository.updateProductAiInsight(product.product.barcode, cachedInsight);
        product.product.aiInsight = cachedInsight;

        aiSummaryContainer.setVisibility(View.VISIBLE);
        GlassMotion.enter(aiSummaryContainer, 80L);
        AiGlowManager.stopGlow(getActivity());
        animateText(aiSummaryTextView, text);
        displaySources(sources);

    }

    private void animateText(TextView textView, String text) {
        final int[] index = {0};
        final int charsPerTick = 14;
        final long delay = 16L;
        final Handler handler = new Handler();

        final android.text.Spanned htmlText = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_COMPACT);
        final String rawText = htmlText.toString();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index[0] <= rawText.length()) {
                    textView.setText(htmlText.subSequence(0, index[0]));
                    index[0] = Math.min(rawText.length() + 1, index[0] + charsPerTick);
                    handler.postDelayed(this, delay);
                } else {
                    textView.setText(htmlText);
                    if (getActivity() != null) {
                        AiGlowManager.stopGlow(getActivity());
                        makeLinksClickable(textView);
                    }
                }
            }
        }, 15);
    }

    private void makeLinksClickable(TextView textView) {
        CharSequence currentText = textView.getText();
        String text = currentText.toString();
        SpannableStringBuilder spannableBuilder = new SpannableStringBuilder(currentText);

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "((http|https)://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(/\\S*)?)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            final String url = matcher.group();
            spannableBuilder.setSpan(new android.text.style.ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { LinkHandler.openLink(getActivity(), url, "Scientific Source", url); }
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
        List<String> aiTriggers = report.getAiTriggeringIngredients();

        for (Ingredient ingredient : productDetails.ingredients) {
            if (ingredient.text != null) {
                SpannableString spannable = new SpannableString(ingredient.text);
                boolean highlighted = false;

                AnalysisResult highlightResult = findIngredientHighlightResult(ingredient.text, report.getResults());
                if (highlightResult != null) {
                    if (highlightResult.getLevel() == AnalysisResult.WarningLevel.POSITIVE) {
                        spannable.setSpan(new BackgroundColorSpan(0x3300FF00), 0, spannable.length(), 0); // Green for positive
                    } else {
                        spannable.setSpan(new BackgroundColorSpan(0x33FF0000), 0, spannable.length(), 0); // Red for others
                    }
                    spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(), 0);
                    highlighted = true;
                }

                if (!highlighted && aiTriggers != null) {
                    for (String trigger : aiTriggers) {
                        if (matchesTrigger(ingredient.text, trigger)) {
                            spannable.setSpan(new BackgroundColorSpan(0x337C4DFF), 0, spannable.length(), 0);
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
        TableRow row = new TableRow(getContext());
        TextView nameView = new TextView(getContext());
        TextView valueView = new TextView(getContext());
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
        Drawable background = ContextCompat.getDrawable(requireContext(), R.drawable.score_background);
        if(background != null) {
            DrawableCompat.setTint(background, getScoreColor(score));
            textView.setBackground(background);
        }
    }

    private int getScoreColor(String score) {
        if (score == null) return ContextCompat.getColor(requireContext(), R.color.score_unknown);
        switch (score.toLowerCase()) {
            case "a":
            case "1":
                return ContextCompat.getColor(requireContext(), R.color.nutriscore_a);
            case "b":
                return ContextCompat.getColor(requireContext(), R.color.nutriscore_b);
            case "c":
            case "2":
                return ContextCompat.getColor(requireContext(), R.color.nutriscore_c);
            case "d":
            case "3":
                return ContextCompat.getColor(requireContext(), R.color.nutriscore_d);
            case "e":
            case "4":
                return ContextCompat.getColor(requireContext(), R.color.nutriscore_e);
            default:
                return ContextCompat.getColor(requireContext(), R.color.score_unknown);
        }
    }

    private void showErrorAndDismiss(String message) {
        if(getContext() == null) return;
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        dismiss();
    }

    private void showAddProductDialog(String barcode) {
        if(getContext() == null) return;
        new AlertDialog.Builder(requireContext())
            .setTitle("Product Not Found")
            .setMessage("This product is not in our database yet. Would you like to add it?")
            .setPositiveButton("Add Product", (dialog, which) -> {
                Intent intent = new Intent(getActivity(), AddProductActivity.class);
                intent.putExtra(AddProductActivity.EXTRA_BARCODE, barcode);
                startActivity(intent);
                dismiss();
            })
            .setNegativeButton("Cancel", (dialog, which) -> dismiss())
            .setOnCancelListener(dialog -> dismiss())
            .show();
    }

    private void showScoreExplanation(String title, String message) {
        new AlertDialog.Builder(requireContext()).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }
}
