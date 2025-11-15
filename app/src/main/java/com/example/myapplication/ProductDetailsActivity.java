package com.example.myapplication;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.squareup.picasso.Picasso;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "com.example.myapplication.BARCODE";
    public static final String RESULT_DATA_CHANGED = "com.example.myapplication.DATA_CHANGED";

    private ProductRepository productRepository;
    private ExecutorService executorService;
    private AppDatabase db;

    private ImageView productImageView;
    private TextView productNameTextView, productBrandTextView, packagingTextView, labelsTextView, ingredientsTextView;
    private TextView nutriscoreTextView, novaTextView, ecoscoreTextView, categoriesTextView, servingSizeTextView;
    private Button removeFromPantryButton;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private LinearLayout detailsLayout;
    private TableLayout nutritionFactsTable;

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

        productImageView = findViewById(R.id.product_image_view);
        productNameTextView = findViewById(R.id.product_name_text_view);
        productBrandTextView = findViewById(R.id.product_brand_text_view);
        packagingTextView = findViewById(R.id.packaging_text_view);
        labelsTextView = findViewById(R.id.labels_text_view);
        ingredientsTextView = findViewById(R.id.ingredients_text_view);
        nutriscoreTextView = findViewById(R.id.nutriscore_text_view);
        novaTextView = findViewById(R.id.nova_text_view);
        ecoscoreTextView = findViewById(R.id.ecoscore_text_view);
        categoriesTextView = findViewById(R.id.categories_text_view);
        servingSizeTextView = findViewById(R.id.serving_size_text_view);
        removeFromPantryButton = findViewById(R.id.remove_from_pantry_button);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        detailsLayout = findViewById(R.id.details_layout);
        nutritionFactsTable = findViewById(R.id.nutrition_facts_table);

        nutriscoreTextView.setOnClickListener(v -> showNutriscoreExplanation());
        novaTextView.setOnClickListener(v -> showNovaExplanation());
        ecoscoreTextView.setOnClickListener(v -> showEcoScoreExplanation());

        String barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        if (barcode != null) {
            loadProductDetails(barcode);
            checkIfProductInPantry(barcode);

            removeFromPantryButton.setOnClickListener(v -> {
                executorService.execute(() -> {
                    db.productDao().deletePantryProduct(barcode);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Removed from Pantry", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(RESULT_DATA_CHANGED, true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                });
            });
        } else {
            showErrorState("No barcode provided.");
        }
    }

    @Override
    protected void onDestroy() {
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
                runOnUiThread(() -> {
                    if (result != null && result.productWithDetails != null) {
                        displayProductDetails(result.productWithDetails);
                        switch (result.status) {
                            case STALE:
                                Toast.makeText(ProductDetailsActivity.this, "Showing stale data. Please connect to the internet for the latest.", Toast.LENGTH_LONG).show();
                                break;
                            case OFFLINE:
                                Toast.makeText(ProductDetailsActivity.this, "You are offline. Showing cached data.", Toast.LENGTH_LONG).show();
                                break;
                        }
                    } else {
                        showErrorState("Product not found for barcode: " + barcode);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> showErrorState("Error: " + e.getMessage()));
            }
        });
    }

    private void checkIfProductInPantry(String barcode) {
        executorService.execute(() -> {
            Pantry pantryItem = db.productDao().findPantryItemByBarcode(barcode);
            runOnUiThread(() -> {
                removeFromPantryButton.setVisibility(pantryItem != null ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void displayProductDetails(ProductWithDetails productDetails) {
        collapsingToolbarLayout.setTitle(" ");

        if (productDetails.product.imageUrl != null && !productDetails.product.imageUrl.isEmpty()) {
            Picasso.get().load(productDetails.product.imageUrl).into(productImageView);
        }

        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        detailsLayout.startAnimation(animation);

        productNameTextView.setText(productDetails.product.productName != null ? productDetails.product.productName : "N/A");
        productBrandTextView.setText(productDetails.product.brands != null ? productDetails.product.brands : "");
        packagingTextView.setText(productDetails.product.packaging != null ? productDetails.product.packaging : "");
        labelsTextView.setText(productDetails.product.labels != null ? productDetails.product.labels : "");
        categoriesTextView.setText(productDetails.product.categories != null ? productDetails.product.categories : "");
        servingSizeTextView.setText(productDetails.product.servingSize != null ? productDetails.product.servingSize : "");

        setScoreTextView(nutriscoreTextView, productDetails.product.nutriscoreGrade, "Nutri-Score");
        setScoreTextView(novaTextView, productDetails.product.novaGroup, "NOVA Group");
        setScoreTextView(ecoscoreTextView, productDetails.product.ecoscoreGrade, "Eco-Score");

        displayNutriments(productDetails.nutriments);

        if (productDetails.ingredients != null && !productDetails.ingredients.isEmpty()) {
            StringBuilder ingredientsString = new StringBuilder();
            for (Ingredient ingredient : productDetails.ingredients) {
                if (ingredient.text != null) {
                    ingredientsString.append("\u2022 ").append(ingredient.text).append("\n");
                }
            }
            ingredientsTextView.setText(ingredientsString.toString().trim());
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
        TableRow row = new TableRow(this);
        TextView nameView = new TextView(this);
        TextView valueView = new TextView(this);

        nameView.setText(name);
        valueView.setText(String.format("%.2f %s", value, unit));

        row.addView(nameView);
        row.addView(valueView);
        nutritionFactsTable.addView(row);
    }

    private void setScoreTextView(TextView textView, String score, String prefix) {
        Drawable backgroundDrawable = ContextCompat.getDrawable(this, R.drawable.score_background);
        if (backgroundDrawable == null) return;

        Drawable newDrawable = backgroundDrawable.getConstantState().newDrawable().mutate();

        int tintColor;
        if (score != null) {
            textView.setText(prefix + ": " + score.toUpperCase());
            tintColor = getScoreColor(score);
        } else {
            textView.setText(prefix + ": N/A");
            tintColor = ContextCompat.getColor(this, R.color.score_unknown);
        }

        DrawableCompat.setTint(newDrawable, tintColor);
        textView.setBackground(newDrawable);
    }

    private int getScoreColor(String score) {
        switch (score.toLowerCase()) {
            case "a": return ContextCompat.getColor(this, R.color.nutriscore_a);
            case "b": return ContextCompat.getColor(this, R.color.nutriscore_b);
            case "c": return ContextCompat.getColor(this, R.color.nutriscore_c);
            case "d": return ContextCompat.getColor(this, R.color.nutriscore_d);
            case "e": return ContextCompat.getColor(this, R.color.nutriscore_e);
            case "1": return ContextCompat.getColor(this, R.color.nova_1);
            case "2": return ContextCompat.getColor(this, R.color.nova_2);
            case "3": return ContextCompat.getColor(this, R.color.nova_3);
            case "4": return ContextCompat.getColor(this, R.color.nova_4);
            default: return ContextCompat.getColor(this, R.color.score_unknown);
        }
    }

    private void showErrorState(String message) {
        collapsingToolbarLayout.setTitle("Error");
        productNameTextView.setText(message);
        productBrandTextView.setText("");
        packagingTextView.setText("");
        labelsTextView.setText("");
        ingredientsTextView.setText("");
        categoriesTextView.setText("");
        servingSizeTextView.setText("");
        nutritionFactsTable.setVisibility(View.GONE);
        removeFromPantryButton.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNutriscoreExplanation() {
        new AlertDialog.Builder(this)
                .setTitle("What is Nutri-Score?")
                .setMessage("Nutri-Score is a nutritional rating system that converts the nutritional value of products into a simple A-E letter grade. \n\nA (Green) = Healthiest Choice\nE (Red) = Less Healthy Choice\n\nThis system helps you easily compare the nutritional quality of similar products.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showNovaExplanation() {
        new AlertDialog.Builder(this)
                .setTitle("What is NOVA Group?")
                .setMessage("The NOVA classification is a system that categorizes foods according to the extent and purpose of industrial processing, rather than in terms of nutrients.\n\n1 - Unprocessed or minimally processed foods\n2 - Processed culinary ingredients\n3 - Processed foods\n4 - Ultra-processed food and drink products")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showEcoScoreExplanation() {
        new AlertDialog.Builder(this)
                .setTitle("What is Eco-Score?")
                .setMessage("The Eco-Score is a letter grade from A to E that summarizes the environmental impact of food products. It is based on a life cycle assessment of the product, from farm to fork.")
                .setPositiveButton("OK", null)
                .show();
    }
}
