package com.ciblorenzo.whatsonmyfood;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddProductActivity extends BaseActivity {

    public static final String EXTRA_BARCODE = "com.ciblorenzo.whatsonmyfood.EXTRA_BARCODE";
    private static final String TAG = "AddProductActivity";

    private TextInputEditText productNameEditText, brandEditText, ingredientsEditText;
    private TextView barcodeTextView;
    private String barcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        Toolbar toolbar = findViewById(R.id.add_product_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        barcode = getIntent().getStringExtra(EXTRA_BARCODE);

        barcodeTextView = findViewById(R.id.barcode_text_view);
        productNameEditText = findViewById(R.id.product_name_edit_text);
        brandEditText = findViewById(R.id.brand_edit_text);
        ingredientsEditText = findViewById(R.id.ingredients_edit_text);
        Button submitButton = findViewById(R.id.submit_button);

        if (barcode != null) {
            barcodeTextView.setText(barcode);
        } else {
            Toast.makeText(this, "No barcode provided", Toast.LENGTH_SHORT).show();
            finish();
        }

        submitButton.setOnClickListener(v -> submitProduct());
    }

    private void submitProduct() {
        String productName = productNameEditText.getText().toString().trim();
        String brand = brandEditText.getText().toString().trim();
        String ingredients = ingredientsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(productName)) {
            Toast.makeText(this, R.string.product_name_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(brand)) {
            brand = getString(R.string.brand_unknown);
        }

        String finalBrand = brand;

        new Thread(() -> {
            try {
                ProductWithDetails manualProduct = buildManualProduct(productName, finalBrand, ingredients);
                ProductDao productDao = AppDatabase.getDatabase(getApplicationContext()).productDao();
                productDao.insertProductWithDetails(manualProduct);
                productDao.insertCacheMeta(new CacheMeta(barcode, System.currentTimeMillis()));
                runOnUiThread(() -> {
                    Toast.makeText(AddProductActivity.this, R.string.product_saved_locally, Toast.LENGTH_LONG).show();
                    Intent detailsIntent = new Intent(AddProductActivity.this, ProductDetailsActivity.class);
                    detailsIntent.putExtra(ProductDetailsActivity.EXTRA_BARCODE, barcode);
                    startActivity(detailsIntent);
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving manual product", e);
                runOnUiThread(() -> Toast.makeText(AddProductActivity.this, R.string.failed_to_save_product, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private ProductWithDetails buildManualProduct(String productName, String brand, String ingredientsText) {
        Product product = new Product(
                barcode,
                productName,
                brand,
                null,
                null,
                null,
                null,
                getString(R.string.manually_added),
                null,
                null,
                null,
                null
        );

        List<Ingredient> ingredients = new ArrayList<>();
        for (String ingredientText : IngredientTextParser.parseIngredientCandidates(ingredientsText)) {
            ingredients.add(new Ingredient(barcode, ingredientText, ingredients.size()));
        }

        ProductWithDetails productWithDetails = new ProductWithDetails();
        productWithDetails.product = product;
        productWithDetails.ingredients = ingredients;
        productWithDetails.nutriments = null;
        return productWithDetails;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
