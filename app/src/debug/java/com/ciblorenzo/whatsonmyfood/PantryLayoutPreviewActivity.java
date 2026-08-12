package com.ciblorenzo.whatsonmyfood;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Debug-only pantry screen for repeatable empty, populated, and navigation checks. */
public class PantryLayoutPreviewActivity extends AppCompatActivity {

    private String selectedBarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry);

        Toolbar toolbar = findViewById(R.id.pantry_toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.loading_overlay).setVisibility(View.GONE);

        RecyclerView recyclerView = findViewById(R.id.pantry_recycler_view);
        View emptyState = findViewById(R.id.pantry_empty_state);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Product> products = getIntent().getBooleanExtra("empty", false)
                ? Collections.emptyList()
                : sampleProducts();
        recyclerView.setAdapter(new PantryAdapter(
                products,
                product -> selectedBarcode = product.barcode,
                (product, score) -> product.userIngredientRiskScore = score
        ));
        PantryListStateViewBinder.bind(recyclerView, emptyState, products.size());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pantry_menu, menu);
        return true;
    }

    public String getSelectedBarcode() {
        return selectedBarcode;
    }

    private List<Product> sampleProducts() {
        return Arrays.asList(
                product("m4-05-apple", "Apple Oats", "Morning Foods", 91),
                product("m4-05-banana", "Banana Cereal", "Daily Market", 74),
                product("m4-05-zucchini", "Zucchini Crackers", "Garden Foods", null)
        );
    }

    private Product product(String barcode, String name, String brand, Integer healthScore) {
        return new Product(
                barcode, name, brand, "12 oz", "", "", "Box", "Pantry preview",
                "1 serving", "b", "2", "b", null, healthScore
        );
    }
}
