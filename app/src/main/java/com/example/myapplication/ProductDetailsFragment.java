package com.example.myapplication;

import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.analysis.ProductAnalysisReport;
import com.example.myapplication.analysis.AnalysisResult;
import com.example.myapplication.analysis.AnalysisResultAdapter;
import com.example.myapplication.analysis.rules.RuleEngine;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailsFragment extends BottomSheetDialogFragment {

    private static final String ARG_BARCODE = "barcode";

    private ProductRepository productRepository;
    private ExecutorService executorService;
    private AppDatabase db;
    private RuleEngine ruleEngine;

    private ImageView productImageView;
    private TextView productNameTextView, productBrandTextView, packagingTextView, labelsTextView, ingredientsTextView;
    private TextView nutriscoreTextView, novaTextView, ecoscoreTextView, categoriesTextView, servingSizeTextView, healthScoreTextView;
    private Button removeFromPantryButton;
    private TableLayout nutritionFactsTable;
    private ConstraintLayout scoresLayout;
    private RecyclerView analysisRecyclerView;

    public static ProductDetailsFragment newInstance(String barcode) {
        ProductDetailsFragment fragment = new ProductDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BARCODE, barcode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        productRepository = new ProductRepository(requireActivity().getApplication());
        executorService = Executors.newSingleThreadExecutor();
        db = AppDatabase.getDatabase(requireContext());
        ruleEngine = new RuleEngine();
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

                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismiss();
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        float baseMargin = 16 * getResources().getDisplayMetrics().density;
                        float newMargin = Math.max(0f, baseMargin * (1 - slideOffset));

                        if (bottomSheet.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
                            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
                            params.leftMargin = (int) newMargin;
                            params.rightMargin = (int) newMargin;
                            bottomSheet.setLayoutParams(params);
                        }
                    }
                });
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
        labelsTextView = view.findViewById(R.id.labels_text_view);
        ingredientsTextView = view.findViewById(R.id.ingredients_text_view);
        nutriscoreTextView = view.findViewById(R.id.nutriscore_text_view);
        novaTextView = view.findViewById(R.id.nova_text_view);
        ecoscoreTextView = view.findViewById(R.id.ecoscore_text_view);
        categoriesTextView = view.findViewById(R.id.categories_text_view);
        servingSizeTextView = view.findViewById(R.id.serving_size_text_view);
        healthScoreTextView = view.findViewById(R.id.health_score_text_view);
        removeFromPantryButton = view.findViewById(R.id.remove_from_pantry_button);
        nutritionFactsTable = view.findViewById(R.id.nutrition_facts_table);
        scoresLayout = view.findViewById(R.id.scores_layout);
        analysisRecyclerView = view.findViewById(R.id.analysis_recycler_view);
        analysisRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        nutriscoreTextView.setOnClickListener(v -> showNutriscoreExplanation());
        novaTextView.setOnClickListener(v -> showNovaExplanation());
        ecoscoreTextView.setOnClickListener(v -> showEcoScoreExplanation());

        String barcode = getArguments() != null ? getArguments().getString(ARG_BARCODE) : null;
        if (barcode != null) {
            loadProductDetails(barcode);

            removeFromPantryButton.setOnClickListener(v -> {
                executorService.execute(() -> {
                    db.productDao().deletePantryProduct(barcode);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Removed from Pantry", Toast.LENGTH_SHORT).show();
                        checkIfProductInPantry(barcode);
                    });
                });
            });
        } else {
            Toast.makeText(getContext(), "No barcode provided.", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (productRepository != null) {
            productRepository.close();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void loadProductDetails(String barcode) {
        productRepository.getProductByBarcode(barcode, new ProductRepository.RepositoryCallback<ProductRepository.ProductResult>() {
            @Override
            public void onComplete(ProductRepository.ProductResult result) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (result != null && result.productWithDetails != null) {
                        displayProductDetails(result.productWithDetails);

                        if (result.apiSourceName != null && !result.apiSourceName.isEmpty()) {
                            Toast.makeText(getContext(), "Data from: " + result.apiSourceName, Toast.LENGTH_LONG).show();
                        }

                        executorService.execute(() -> {
                            db.productDao().insertPantry(new Pantry(barcode));
                            checkIfProductInPantry(barcode);
                        });

                        switch (result.status) {
                            case STALE:
                                Toast.makeText(getContext(), "Showing stale data. Refresh for the latest.", Toast.LENGTH_LONG).show();
                                break;
                            case OFFLINE:
                                Toast.makeText(getContext(), "Offline. Showing cached data.", Toast.LENGTH_LONG).show();
                                break;
                        }
                    } else {
                        Toast.makeText(getContext(), "Product not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            }
        });
    }

    private void checkIfProductInPantry(String barcode) {
        executorService.execute(() -> {
            Pantry pantryItem = db.productDao().findPantryItemByBarcode(barcode);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                removeFromPantryButton.setVisibility(pantryItem != null ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void displayProductDetails(ProductWithDetails productDetails) {
        if (productDetails.product.imageUrl != null && !productDetails.product.imageUrl.isEmpty()) {
            Picasso.get().load(productDetails.product.imageUrl).into(productImageView);
        }

        productNameTextView.setText(productDetails.product.productName != null ? productDetails.product.productName : "N/A");
        productBrandTextView.setText(productDetails.product.brands != null ? productDetails.product.brands : "");
        packagingTextView.setText(productDetails.product.packaging != null ? productDetails.product.packaging : "");
        labelsTextView.setText(productDetails.product.labels != null ? productDetails.product.labels : "");
        categoriesTextView.setText(productDetails.product.categories != null ? productDetails.product.categories : "");
        servingSizeTextView.setText(productDetails.product.servingSize != null ? productDetails.product.servingSize : "");

        ProductAnalysisReport report = ruleEngine.analyze(productDetails);
        healthScoreTextView.setText(String.format("Health Score: %d/100", report.getOverallScore()));
        analysisRecyclerView.setAdapter(new AnalysisResultAdapter(report.getResults()));

        setScoreTextView(nutriscoreTextView, productDetails.product.nutriscoreGrade, "Nutri-Score");
        setScoreTextView(novaTextView, productDetails.product.novaGroup, "NOVA Group");
        setScoreTextView(ecoscoreTextView, productDetails.product.ecoscoreGrade, "Eco-Score");

        displayNutriments(productDetails.nutriments);
        displayHighlightedIngredients(productDetails, report);
    }

    private void displayHighlightedIngredients(ProductWithDetails productDetails, ProductAnalysisReport report) {
        List<String> badTerms = new ArrayList<>();
        List<String> goodTerms = new ArrayList<>();
        for (AnalysisResult result : report.getResults()) {
            if (result.getTriggeringIngredient() != null) {
                if (result.getLevel() == AnalysisResult.WarningLevel.INFO) {
                    goodTerms.add(result.getTriggeringIngredient().toLowerCase());
                } else {
                    badTerms.add(result.getTriggeringIngredient().toLowerCase());
                }
            }
        }

        if (productDetails.ingredients != null && !productDetails.ingredients.isEmpty()) {
            SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
            for (int i = 0; i < productDetails.ingredients.size(); i++) {
                Ingredient ingredient = productDetails.ingredients.get(i);
                if (ingredient.text != null) {
                    String ingredientLine = ingredient.text;
                    SpannableString spannableString = new SpannableString(ingredientLine);

                    boolean isBad = false;
                    for (String badTerm : badTerms) {
                        if (ingredientLine.toLowerCase().contains(badTerm)) {
                            isBad = true;
                            break;
                        }
                    }

                    if (isBad) {
                        spannableString.setSpan(new BackgroundColorSpan(0x33FF0000), 0, ingredientLine.length(), 0);
                        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, ingredientLine.length(), 0);
                        spannableString.setSpan(new RelativeSizeSpan(1.2f), 0, ingredientLine.length(), 0);
                    } else {
                        boolean isGood = false;
                        for (String goodTerm : goodTerms) {
                            if (ingredientLine.toLowerCase().contains(goodTerm)) {
                                isGood = true;
                                break;
                            }
                        }
                        if (isGood || ingredientLine.toLowerCase().contains("organic") || ingredientLine.toLowerCase().contains("whole grain")) {
                            spannableString.setSpan(new BackgroundColorSpan(0x3300FF00), 0, ingredientLine.length(), 0);
                        }
                    }
                    
                    spannableBuilder.append(spannableString);
                    if (i < productDetails.ingredients.size() - 1) {
                        spannableBuilder.append("\n");
                    }
                }
            }
            ingredientsTextView.setText(spannableBuilder);
        } else {
            ingredientsTextView.setText("No ingredients listed.");
        }
    }

    private void displayNutriments(Nutriments nutriments) {
        nutritionFactsTable.removeAllViews();
        if (nutriments != null) {
            nutritionFactsTable.setVisibility(View.VISIBLE);
            
            addNutritionHeaderRow("Energy & Macronutrients");
            if (nutriments.energy != null && nutriments.energy > 0) addNutritionRow("Energy", nutriments.energy, "kcal");
            if (nutriments.energyKj != null && nutriments.energyKj > 0) addNutritionRow("Energy (kJ)", nutriments.energyKj, "kJ");
            if (nutriments.proteins != null && nutriments.proteins > 0) addNutritionRow("Proteins", nutriments.proteins, "g");
            if (nutriments.carbohydrates != null && nutriments.carbohydrates > 0) addNutritionRow("Carbohydrates", nutriments.carbohydrates, "g");
            if (nutriments.fat != null && nutriments.fat > 0) addNutritionRow("Fat", nutriments.fat, "g");
            if (nutriments.fiber != null && nutriments.fiber > 0) addNutritionRow("Fiber", nutriments.fiber, "g");

            addNutritionHeaderRow("Sugars");
            if (nutriments.sugars != null && nutriments.sugars > 0) addNutritionRow("Total Sugars", nutriments.sugars, "g");
            if (nutriments.addedSugars != null && nutriments.addedSugars > 0) addNutritionRow("Added Sugars", nutriments.addedSugars, "g");
            if (nutriments.sucrose != null && nutriments.sucrose > 0) addNutritionRow("Sucrose", nutriments.sucrose, "g");
            if (nutriments.glucose != null && nutriments.glucose > 0) addNutritionRow("Glucose", nutriments.glucose, "g");
            if (nutriments.fructose != null && nutriments.fructose > 0) addNutritionRow("Fructose", nutriments.fructose, "g");
            if (nutriments.lactose != null && nutriments.lactose > 0) addNutritionRow("Lactose", nutriments.lactose, "g");
            if (nutriments.maltose != null && nutriments.maltose > 0) addNutritionRow("Maltose", nutriments.maltose, "g");
            if (nutriments.maltodextrins != null && nutriments.maltodextrins > 0) addNutritionRow("Maltodextrins", nutriments.maltodextrins, "g");
            if (nutriments.starch != null && nutriments.starch > 0) addNutritionRow("Starch", nutriments.starch, "g");
            if (nutriments.polyols != null && nutriments.polyols > 0) addNutritionRow("Polyols", nutriments.polyols, "g");

            addNutritionHeaderRow("Fats & Fatty Acids");
            if (nutriments.saturatedFat != null && nutriments.saturatedFat > 0) addNutritionRow("Saturated Fat", nutriments.saturatedFat, "g");
            if (nutriments.monounsaturatedFat != null && nutriments.monounsaturatedFat > 0) addNutritionRow("Monounsaturated Fat", nutriments.monounsaturatedFat, "g");
            if (nutriments.polyunsaturatedFat != null && nutriments.polyunsaturatedFat > 0) addNutritionRow("Polyunsaturated Fat", nutriments.polyunsaturatedFat, "g");
            if (nutriments.transFat != null && nutriments.transFat > 0) addNutritionRow("Trans Fat", nutriments.transFat, "g");
            if (nutriments.cholesterol != null && nutriments.cholesterol > 0) addNutritionRow("Cholesterol", nutriments.cholesterol, "mg");
            if (nutriments.omega3Fat != null && nutriments.omega3Fat > 0) addNutritionRow("Omega-3 Fat", nutriments.omega3Fat, "g");
            if (nutriments.alphaLinolenicAcid != null && nutriments.alphaLinolenicAcid > 0) addNutritionRow("Alpha-Linolenic Acid", nutriments.alphaLinolenicAcid, "g");
            if (nutriments.eicosapentaenoicAcid != null && nutriments.eicosapentaenoicAcid > 0) addNutritionRow("Eicosapentaenoic Acid", nutriments.eicosapentaenoicAcid, "g");
            if (nutriments.docosahexaenoicAcid != null && nutriments.docosahexaenoicAcid > 0) addNutritionRow("Docosahexaenoic Acid (DHA)", nutriments.docosahexaenoicAcid, "g");
            if (nutriments.omega6Fat != null && nutriments.omega6Fat > 0) addNutritionRow("Omega-6 Fat", nutriments.omega6Fat, "g");
            if (nutriments.linoleicAcid != null && nutriments.linoleicAcid > 0) addNutritionRow("Linoleic Acid", nutriments.linoleicAcid, "g");
            if (nutriments.arachidonicAcid != null && nutriments.arachidonicAcid > 0) addNutritionRow("Arachidonic Acid", nutriments.arachidonicAcid, "g");
            if (nutriments.gammaLinolenicAcid != null && nutriments.gammaLinolenicAcid > 0) addNutritionRow("Gamma-Linolenic Acid", nutriments.gammaLinolenicAcid, "g");
            if (nutriments.dihomoGammaLinolenicAcid != null && nutriments.dihomoGammaLinolenicAcid > 0) addNutritionRow("Dihomo-Gamma-Linolenic Acid", nutriments.dihomoGammaLinolenicAcid, "g");
            if (nutriments.omega9Fat != null && nutriments.omega9Fat > 0) addNutritionRow("Omega-9 Fat", nutriments.omega9Fat, "g");
            if (nutriments.oleicAcid != null && nutriments.oleicAcid > 0) addNutritionRow("Oleic Acid", nutriments.oleicAcid, "g");

            addNutritionHeaderRow("Vitamins");
            if (nutriments.vitaminA != null && nutriments.vitaminA > 0) addNutritionRow("Vitamin A", nutriments.vitaminA, "IU");
            if (nutriments.vitaminD != null && nutriments.vitaminD > 0) addNutritionRow("Vitamin D", nutriments.vitaminD, "IU");
            if (nutriments.vitaminE != null && nutriments.vitaminE > 0) addNutritionRow("Vitamin E", nutriments.vitaminE, "mg");
            if (nutriments.vitaminK != null && nutriments.vitaminK > 0) addNutritionRow("Vitamin K", nutriments.vitaminK, "µg");
            if (nutriments.vitaminC != null && nutriments.vitaminC > 0) addNutritionRow("Vitamin C", nutriments.vitaminC, "mg");
            if (nutriments.vitaminB1 != null && nutriments.vitaminB1 > 0) addNutritionRow("Vitamin B1 (Thiamin)", nutriments.vitaminB1, "mg");
            if (nutriments.vitaminB2 != null && nutriments.vitaminB2 > 0) addNutritionRow("Vitamin B2 (Riboflavin)", nutriments.vitaminB2, "mg");
            if (nutriments.vitaminPP != null && nutriments.vitaminPP > 0) addNutritionRow("Vitamin B3 (Niacin)", nutriments.vitaminPP, "mg");
            if (nutriments.vitaminB6 != null && nutriments.vitaminB6 > 0) addNutritionRow("Vitamin B6", nutriments.vitaminB6, "mg");
            if (nutriments.vitaminB9 != null && nutriments.vitaminB9 > 0) addNutritionRow("Vitamin B9 (Folate)", nutriments.vitaminB9, "µg");
            if (nutriments.vitaminB12 != null && nutriments.vitaminB12 > 0) addNutritionRow("Vitamin B12", nutriments.vitaminB12, "µg");
            if (nutriments.biotin != null && nutriments.biotin > 0) addNutritionRow("Biotin (B7)", nutriments.biotin, "µg");
            if (nutriments.pantothenicAcid != null && nutriments.pantothenicAcid > 0) addNutritionRow("Pantothenic Acid (B5)", nutriments.pantothenicAcid, "mg");

            addNutritionHeaderRow("Minerals & Trace Elements");
            if (nutriments.silica != null && nutriments.silica > 0) addNutritionRow("Silica", nutriments.silica, "mg");
            if (nutriments.bicarbonate != null && nutriments.bicarbonate > 0) addNutritionRow("Bicarbonate", nutriments.bicarbonate, "mg");
            if (nutriments.potassium != null && nutriments.potassium > 0) addNutritionRow("Potassium", nutriments.potassium, "mg");
            if (nutriments.chloride != null && nutriments.chloride > 0) addNutritionRow("Chloride", nutriments.chloride, "mg");
            if (nutriments.calcium != null && nutriments.calcium > 0) addNutritionRow("Calcium", nutriments.calcium, "mg");
            if (nutriments.phosphorus != null && nutriments.phosphorus > 0) addNutritionRow("Phosphorus", nutriments.phosphorus, "mg");
            if (nutriments.iron != null && nutriments.iron > 0) addNutritionRow("Iron", nutriments.iron, "mg");
            if (nutriments.magnesium != null && nutriments.magnesium > 0) addNutritionRow("Magnesium", nutriments.magnesium, "mg");
            if (nutriments.zinc != null && nutriments.zinc > 0) addNutritionRow("Zinc", nutriments.zinc, "mg");
            if (nutriments.copper != null && nutriments.copper > 0) addNutritionRow("Copper", nutriments.copper, "mg");
            if (nutriments.manganese != null && nutriments.manganese > 0) addNutritionRow("Manganese", nutriments.manganese, "mg");
            if (nutriments.fluoride != null && nutriments.fluoride > 0) addNutritionRow("Fluoride", nutriments.fluoride, "µg");
            if (nutriments.selenium != null && nutriments.selenium > 0) addNutritionRow("Selenium", nutriments.selenium, "µg");
            if (nutriments.chromium != null && nutriments.chromium > 0) addNutritionRow("Chromium", nutriments.chromium, "µg");
            if (nutriments.molybdenum != null && nutriments.molybdenum > 0) addNutritionRow("Molybdenum", nutriments.molybdenum, "µg");
            if (nutriments.iodine != null && nutriments.iodine > 0) addNutritionRow("Iodine", nutriments.iodine, "µg");

            addNutritionHeaderRow("Other");
            if (nutriments.sodium != null && nutriments.sodium > 0) addNutritionRow("Sodium", nutriments.sodium, "mg");
            if (nutriments.alcohol != null && nutriments.alcohol > 0) addNutritionRow("Alcohol", nutriments.alcohol, "% vol");
            if (nutriments.caffeine != null && nutriments.caffeine > 0) addNutritionRow("Caffeine", nutriments.caffeine, "mg");
            if (nutriments.taurine != null && nutriments.taurine > 0) addNutritionRow("Taurine", nutriments.taurine, "mg");
            if (nutriments.carbonFootprint != null && nutriments.carbonFootprint > 0) addNutritionRow("Carbon Footprint", nutriments.carbonFootprint, "g CO2e/100g");

        } else {
            nutritionFactsTable.setVisibility(View.GONE);
        }
    }

    private void addNutritionHeaderRow(String title) {
        TableRow row = new TableRow(getContext());
        TextView header = new TextView(getContext());
        header.setText(title);
        header.setTextSize(18);
        header.setTypeface(null, Typeface.BOLD);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span = 2;
        params.topMargin = 24;
        params.bottomMargin = 8;
        header.setLayoutParams(params);
        row.addView(header);
        nutritionFactsTable.addView(row);
    }


    private void addNutritionRow(String name, double value, String unit) {
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
        Drawable backgroundDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.score_background);
        if (backgroundDrawable == null) return;

        Drawable newDrawable = backgroundDrawable.getConstantState().newDrawable().mutate();

        int tintColor;
        if (score != null) {
            textView.setText(prefix + ": " + score.toUpperCase());
            tintColor = getScoreColor(score);
        } else {
            textView.setText(prefix + ": N/A");
            tintColor = ContextCompat.getColor(requireContext(), R.color.score_unknown);
        }

        DrawableCompat.setTint(newDrawable, tintColor);
        textView.setBackground(newDrawable);
    }

    private int getScoreColor(String score) {
        if (score == null) return ContextCompat.getColor(requireContext(), R.color.score_unknown);

        try {
            int novaScore = (int) Math.round(Double.parseDouble(score));
            switch (novaScore) {
                case 1: return ContextCompat.getColor(requireContext(), R.color.nova_1);
                case 2: return ContextCompat.getColor(requireContext(), R.color.nova_2);
                case 3: return ContextCompat.getColor(requireContext(), R.color.nova_3);
                case 4: return ContextCompat.getColor(requireContext(), R.color.nova_4);
            }
        } catch (NumberFormatException e) {
            // Not a number, fall through to check for letter grades.
        }

        switch (score.toLowerCase()) {
            case "a": return ContextCompat.getColor(requireContext(), R.color.nutriscore_a);
            case "b": return ContextCompat.getColor(requireContext(), R.color.nutriscore_b);
            case "c": return ContextCompat.getColor(requireContext(), R.color.nutriscore_c);
            case "d": return ContextCompat.getColor(requireContext(), R.color.nutriscore_d);
            case "e": return ContextCompat.getColor(requireContext(), R.color.nutriscore_e);
            default: return ContextCompat.getColor(requireContext(), R.color.score_unknown);
        }
    }

    private void showNutriscoreExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("What is Nutri-Score?")
                .setMessage("Nutri-Score is a nutritional rating system that converts the nutritional value of products into a simple A-E letter grade. \n\nA (Green) = Healthiest Choice\nE (Red) = Less Healthy Choice\n\nThis system helps you easily compare the nutritional quality of similar products.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showNovaExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("What is NOVA Group?")
                .setMessage("The NOVA classification is a system that categorizes foods according to the extent and purpose of industrial processing, rather than in terms of nutrients.\n\n1 - Unprocessed or minimally processed foods\n2 - Processed culinary ingredients\n3 - Processed foods\n4 - Ultra-processed food and drink products")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showEcoScoreExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("What is Eco-Score?")
                .setMessage("The Eco-Score is a letter grade from A to E that summarizes the environmental impact of food products. It is based on a life cycle assessment of the product, from farm to fork.")
                .setPositiveButton("OK", null)
                .show();
    }
}
