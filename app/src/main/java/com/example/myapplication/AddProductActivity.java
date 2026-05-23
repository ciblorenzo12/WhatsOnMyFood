package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

public class AddProductActivity extends BaseActivity {

    public static final String EXTRA_BARCODE = "com.example.myapplication.EXTRA_BARCODE";

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

        if (TextUtils.isEmpty(productName) || TextUtils.isEmpty(brand)) {
            Toast.makeText(this, "Product name and brand are required", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                OpenFoodFactsApiClient apiClient = new OpenFoodFactsApiClient(getCacheDir());
                boolean success = apiClient.addProduct(barcode, productName, brand, ingredients);
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(AddProductActivity.this, "Product submitted successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(AddProductActivity.this, "Failed to submit product.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(AddProductActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
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
