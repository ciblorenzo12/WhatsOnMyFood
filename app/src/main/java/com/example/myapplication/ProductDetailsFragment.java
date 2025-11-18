package com.example.myapplication;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
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
import com.example.myapplication.analysis.rules.AnalysisResult;
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
                    if (getActivity() == null || !isAdded()) {
                        return;
                    }
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
                     if (getActivity() == null || !isAdded()) {
                        return;
                    }
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
                 if (getActivity() == null || !isAdded()) {
                        return;
                    }
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
        boolean hasBadIngredients = false;
        for (AnalysisResult result : report.getResults()) {
            if (result.getLevel() == AnalysisResult.WarningLevel.WARNING || result.getLevel() == AnalysisResult.WarningLevel.SEVERE) {
                hasBadIngredients = true;
                break;
            }
        }

        if (productDetails.ingredients != null && !productDetails.ingredients.isEmpty()) {
            SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
            for (int i = 0; i < productDetails.ingredients.size(); i++) {
                Ingredient ingredient = productDetails.ingredients.get(i);
                if (ingredient.text != null) {
                    spannableBuilder.append(ingredient.text);
                    if (i < productDetails.ingredients.size() - 1) {
                        spannableBuilder.append(", ");
                    }
                }
            }

            if (hasBadIngredients) {
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

                for (String goodTerm : goodTerms) {
                    int startIndex = 0;
                    while (startIndex >= 0) {
                        startIndex = spannableBuilder.toString().toLowerCase().indexOf(goodTerm, startIndex);
                        if (startIndex >= 0) {
                            spannableBuilder.setSpan(new BackgroundColorSpan(0x3300FF00), startIndex, startIndex + goodTerm.length(), 0);
                            startIndex += goodTerm.length();
                        }
                    }
                }

                for (String badTerm : badTerms) {
                    int startIndex = 0;
                    while (startIndex >= 0) {
                        startIndex = spannableBuilder.toString().toLowerCase().indexOf(badTerm, startIndex);
                        if (startIndex >= 0) {
                            spannableBuilder.setSpan(new BackgroundColorSpan(0x33FF0000), startIndex, startIndex + badTerm.length(), 0);
                            spannableBuilder.setSpan(new StyleSpan(Typeface.BOLD), startIndex, startIndex + badTerm.length(), 0);
                            spannableBuilder.setSpan(new RelativeSizeSpan(1.2f), startIndex, startIndex + badTerm.length(), 0);
                            startIndex += badTerm.length();
                        }
                    }
                }
            } else {
                // Apply prominent "good" styling to the entire string
                spannableBuilder.setSpan(new BackgroundColorSpan(0x3300FF00), 0, spannableBuilder.length(), 0);
                spannableBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0, spannableBuilder.length(), 0);
                spannableBuilder.setSpan(new RelativeSizeSpan(1.2f), 0, spannableBuilder.length(), 0);
            }
            ingredientsTextView.setText(spannableBuilder);
        } else {
            ingredientsTextView.setText("No ingredients listed.");
        }
    }

    private void displayNutriments(Nutriments nutriments) {
        if (nutriments != null) {
            nutritionFactsTable.setVisibility(View.VISIBLE);
            addNutritionRow("Energy", nutriments.energy, "kcal");
            addNutritionRow("Fat", nutriments.fat, "g");
            addNutritionRow("Saturated Fat", nutriments.saturatedFat, "g");
            addNutritionRow("Carbohydrate", nutriments.carbohydrate, "g");
            addNutritionRow("Sugar", nutriments.sugar, "g");
            addNutritionRow("Proteins", nutriments.proteins, "g");
            addNutritionRow("Salt", nutriments.salt, "g");
            addNutritionRow("Fiber", nutriments.fiber, "g");
        } else {
            nutritionFactsTable.setVisibility(View.GONE);
        }
    }

    private void addNutritionRow(String name, double value, String unit) {
        TableRow row = new TableRow(getContext());
        TextView nameView = new TextView(getContext());
        TextView valueView = new TextView(getContext());

        nameView.setText(name);
        valueView.setText(String.format("%.2f %s", value, unit));

        row.addView(nameView);
        row.addView(valueView);
        nutritionFactsTable.addView(row);
    }

    private void setScoreTextView(TextView textView, String score, String prefix) {
        if (getContext() == null) return;
        Drawable backgroundDrawable = ContextCompat.getDrawable(getContext(), R.drawable.score_background);
        if (backgroundDrawable == null) return;

        Drawable newDrawable = backgroundDrawable.getConstantState().newDrawable().mutate();

        int tintColor;
        if (score != null) {
            textView.setText(prefix + ": " + score.toUpperCase());
            tintColor = getScoreColor(score);
        } else {
            textView.setText(prefix + ": N/A");
            tintColor = ContextCompat.getColor(getContext(), R.color.score_unknown);
        }

        DrawableCompat.setTint(newDrawable, tintColor);
        textView.setBackground(newDrawable);
    }

    private int getScoreColor(String score) {
        if (getContext() == null) return Color.GRAY; 
        if (score == null) return ContextCompat.getColor(getContext(), R.color.score_unknown);
        
        try {
            int novaScore = (int) Math.round(Double.parseDouble(score));
            switch (novaScore) {
                case 1: return ContextCompat.getColor(getContext(), R.color.nova_1);
                case 2: return ContextCompat.getColor(getContext(), R.color.nova_2);
                case 3: return ContextCompat.getColor(getContext(), R.color.nova_3);
                case 4: return ContextCompat.getColor(getContext(), R.color.nova_4);
            }
        } catch (NumberFormatException e) {
            // Not a number, fall through to check for letter grades.
        }

        switch (score.toLowerCase()) {
            case "a": return ContextCompat.getColor(getContext(), R.color.nutriscore_a);
            case "b": return ContextCompat.getColor(getContext(), R.color.nutriscore_b);
            case "c": return ContextCompat.getColor(getContext(), R.color.nutriscore_c);
            case "d": return ContextCompat.getColor(getContext(), R.color.nutriscore_d);
            case "e": return ContextCompat.getColor(getContext(), R.color.nutriscore_e);
            default: return ContextCompat.getColor(getContext(), R.color.score_unknown);
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
