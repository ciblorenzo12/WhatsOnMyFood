package com.example.myapplication.retail;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.BaseActivity;
import com.example.myapplication.ProductRepository;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarketplaceActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_JSON = "extra_product_json";

    private RetailerRepository repository;
    private MarketplaceAdapter adapter;
    private List<MarketplaceItem> allItems = new ArrayList<>();
    private View loadingOverlay;
    private ProductWithDetails productDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace);

        Toolbar toolbar = findViewById(R.id.marketplace_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Marketplace Comparison");
        }

        repository = new RetailerRepository(getApplication());
        loadingOverlay = findViewById(R.id.loading_overlay);
        
        RecyclerView recyclerView = findViewById(R.id.marketplace_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MarketplaceAdapter(this);
        recyclerView.setAdapter(adapter);

        Spinner sortSpinner = findViewById(R.id.sort_spinner);
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortItems(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String productJson = getIntent().getStringExtra(EXTRA_PRODUCT_JSON);
        if (productJson != null) {
            productDetails = new Gson().fromJson(productJson, ProductWithDetails.class);
            loadData();
        } else {
            Toast.makeText(this, "No product data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadData() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        
        repository.getAvailability(productDetails, new ProductRepository.RepositoryCallback<List<RetailerAvailability>>() {
            @Override
            public void onComplete(List<RetailerAvailability> availabilities) {
                repository.getAlternatives(productDetails, new ProductRepository.RepositoryCallback<List<RetailerAlternative>>() {
                    @Override
                    public void onComplete(List<RetailerAlternative> alternatives) {
                        runOnUiThread(() -> {
                            if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                            processResults(availabilities, alternatives);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                            processResults(availabilities, new ArrayList<>());
                        });
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(MarketplaceActivity.this, "Failed to load retail data", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void processResults(List<RetailerAvailability> availabilities, List<RetailerAlternative> alternatives) {
        allItems.clear();
        int originalScore = productDetails.product.healthScore != null ? productDetails.product.healthScore : 0;

        if (availabilities != null) {
            for (RetailerAvailability a : availabilities) {
                allItems.add(new MarketplaceItem(
                        productDetails.product.productName,
                        productDetails.product.brands,
                        a.retailerName,
                        a.price,
                        a.priceValue,
                        a.distance,
                        a.distanceValue,
                        originalScore,
                        a.productUrl,
                        productDetails.product.imageUrl,
                        MarketplaceItem.ItemType.ORIGINAL
                ));
            }
        }

        if (alternatives != null) {
            for (RetailerAlternative alt : alternatives) {
                allItems.add(new MarketplaceItem(
                        alt.productName,
                        alt.brand,
                        alt.retailerHint.split(" ")[2], // Simplified retailer extraction from hint
                        alt.priceValue > 0 ? "$" + alt.priceValue : "Varies",
                        alt.priceValue,
                        alt.distanceValue > 0 ? alt.distanceValue + " mi" : "Nearby",
                        alt.distanceValue,
                        alt.healthScore,
                        alt.productUrl,
                        alt.imageUrl,
                        MarketplaceItem.ItemType.ALTERNATIVE
                ));
            }
        }
        
        // Initial sort by health (Good or not)
        sortItems(2); 
    }

    private void sortItems(int position) {
        if (allItems.isEmpty()) return;

        switch (position) {
            case 0: // Cost
                Collections.sort(allItems, (o1, o2) -> Double.compare(o1.priceValue, o2.priceValue));
                break;
            case 1: // Distance
                Collections.sort(allItems, (o1, o2) -> Double.compare(o1.distanceValue, o2.distanceValue));
                break;
            case 2: // Good or not (Health Score - Higher is better)
                Collections.sort(allItems, (o1, o2) -> Integer.compare(o2.healthScore, o1.healthScore));
                break;
        }
        adapter.submitList(new ArrayList<>(allItems));
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
        if (repository != null) repository.close();
    }
}
