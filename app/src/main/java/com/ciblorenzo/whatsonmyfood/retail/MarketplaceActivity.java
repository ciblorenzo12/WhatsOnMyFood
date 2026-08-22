package com.ciblorenzo.whatsonmyfood.retail;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.BaseActivity;
import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.ciblorenzo.whatsonmyfood.ProductRepository;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarketplaceActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_JSON = "extra_product_json";
    public static final String EXTRA_DEBUG_STATE = "extra_debug_marketplace_state";

    private RetailerRepository repository;
    private MarketplaceAdapter adapter;
    private List<MarketplaceItem> allItems = new ArrayList<>();
    private RecyclerView recyclerView;
    private View stateContainer;
    private ProgressBar stateProgress;
    private TextView stateBadge;
    private TextView stateTitle;
    private TextView stateMessage;
    private Button retryButton;
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
            actionBar.setTitle(R.string.marketplace_comparison);
        }

        repository = new RetailerRepository(getApplication());
        recyclerView = findViewById(R.id.marketplace_recycler_view);
        stateContainer = findViewById(R.id.marketplace_state_container);
        stateProgress = findViewById(R.id.marketplace_state_progress);
        stateBadge = findViewById(R.id.marketplace_state_badge);
        stateTitle = findViewById(R.id.marketplace_state_title);
        stateMessage = findViewById(R.id.marketplace_state_message);
        retryButton = findViewById(R.id.marketplace_retry_button);
        retryButton.setOnClickListener(view -> loadData());

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

        String debugState = getIntent().getStringExtra(EXTRA_DEBUG_STATE);
        if (BuildConfig.DEBUG && debugState != null && showDebugState(debugState)) {
            return;
        }

        productDetails = MarketplaceNavigation.readProduct(getIntent());
        if (productDetails != null) {
            loadData();
        } else {
            Toast.makeText(this, R.string.comparison_unavailable, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private boolean showDebugState(String stateName) {
        MarketplaceStateResolver.UiState state;
        try {
            state = MarketplaceStateResolver.UiState.valueOf(stateName.trim().toUpperCase(java.util.Locale.US));
        } catch (IllegalArgumentException error) {
            return false;
        }

        allItems.clear();
        if (state == MarketplaceStateResolver.UiState.LIVE || state == MarketplaceStateResolver.UiState.MOCK) {
            boolean mock = state == MarketplaceStateResolver.UiState.MOCK;
            allItems.add(new MarketplaceItem(
                    mock ? "Development cereal alternative" : "Live cereal alternative",
                    "Test Brand",
                    mock ? "Sample retailer" : "Live retailer",
                    "$3.49",
                    3.49,
                    "1.2 mi",
                    1.2,
                    88,
                    "",
                    "",
                    MarketplaceItem.ItemType.ALTERNATIVE,
                    mock ? "DEVELOPMENT SAMPLE" : "LIVE PROVIDER",
                    MarketplaceItem.ComparisonCue.UNAVAILABLE));
            adapter.submitList(new ArrayList<>(allItems));
        }
        renderState(state);
        return true;
    }

    private void loadData() {
        renderState(MarketplaceStateResolver.UiState.LOADING);
        repository.getMarketplaceData(productDetails, new ProductRepository.RepositoryCallback<RetailerMarketplaceResult>() {
            @Override
            public void onComplete(RetailerMarketplaceResult result) {
                runOnUiThread(() -> {
                    processResults(result);
                    renderState(MarketplaceStateResolver.fromResult(result));
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    allItems.clear();
                    adapter.submitList(new ArrayList<>());
                    renderState(MarketplaceStateResolver.fromError(e));
                });
            }
        });
    }

    private void processResults(RetailerMarketplaceResult result) {
        allItems.clear();
        if (productDetails == null || productDetails.product == null || result == null) return;
        int originalScore = productDetails.product.healthScore != null ? productDetails.product.healthScore : -1;
        boolean defaultMock = result.sourceMode == RetailerEndpointResult.SourceMode.MOCK;

        for (RetailerAvailability a : result.availability) {
            if (a != null) {
                allItems.add(new MarketplaceItem(
                        MarketplacePresentation.safeText(productDetails.product.productName, "Scanned product"),
                        MarketplacePresentation.safeText(productDetails.product.brands, ""),
                        MarketplacePresentation.safeText(a.retailerName, "Retailer availability varies"),
                        MarketplacePresentation.safeText(a.price, "Price unavailable"),
                        a.priceValue,
                        MarketplacePresentation.safeText(a.distance, "Availability varies"),
                        a.distanceValue,
                        originalScore,
                        a.productUrl,
                        productDetails.product.imageUrl,
                        MarketplaceItem.ItemType.ORIGINAL,
                        MarketplacePresentation.sourceLabel(a.providerName, defaultMock),
                        MarketplaceItem.ComparisonCue.REFERENCE
                ));
            }
        }

        for (RetailerAlternative alt : result.alternatives) {
            if (alt != null && alt.productName != null && !alt.productName.trim().isEmpty()) {
                allItems.add(new MarketplaceItem(
                        alt.productName,
                        MarketplacePresentation.safeText(alt.brand, ""),
                        MarketplacePresentation.retailerName(alt),
                        alt.priceValue > 0 ? String.format(java.util.Locale.US, "$%.2f", alt.priceValue) : "Price varies",
                        alt.priceValue,
                        alt.distanceValue > 0 ? String.format(java.util.Locale.US, "%.1f mi", alt.distanceValue) : "Availability varies",
                        alt.distanceValue,
                        alt.healthScore,
                        alt.productUrl,
                        alt.imageUrl,
                        MarketplaceItem.ItemType.ALTERNATIVE,
                        MarketplacePresentation.sourceLabel(alt.providerName, defaultMock),
                        MarketplacePresentation.comparisonCue(alt.healthScore, originalScore)
                ));
            }
        }

        sortItems(2);
    }

    private void renderState(MarketplaceStateResolver.UiState state) {
        boolean hasResults = state == MarketplaceStateResolver.UiState.LIVE
                || state == MarketplaceStateResolver.UiState.MOCK;
        recyclerView.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        stateProgress.setVisibility(state == MarketplaceStateResolver.UiState.LOADING ? View.VISIBLE : View.GONE);
        retryButton.setVisibility(state == MarketplaceStateResolver.UiState.EMPTY
                || state == MarketplaceStateResolver.UiState.TIMEOUT
                || state == MarketplaceStateResolver.UiState.ERROR ? View.VISIBLE : View.GONE);

        switch (state) {
            case LOADING:
                stateBadge.setText(R.string.marketplace_state_loading_badge);
                stateTitle.setText(R.string.marketplace_state_loading_title);
                stateMessage.setText(R.string.marketplace_state_loading_message);
                break;
            case LIVE:
                stateBadge.setText(R.string.marketplace_state_live_badge);
                stateTitle.setText(R.string.marketplace_state_live_title);
                stateMessage.setText(R.string.marketplace_state_live_message);
                break;
            case MOCK:
                stateBadge.setText(R.string.marketplace_state_mock_badge);
                stateTitle.setText(R.string.marketplace_state_mock_title);
                stateMessage.setText(R.string.marketplace_state_mock_message);
                break;
            case EMPTY:
                stateBadge.setText(R.string.marketplace_state_empty_badge);
                stateTitle.setText(R.string.marketplace_state_empty_title);
                stateMessage.setText(R.string.marketplace_state_empty_message);
                break;
            case TIMEOUT:
                stateBadge.setText(R.string.marketplace_state_timeout_badge);
                stateTitle.setText(R.string.marketplace_state_timeout_title);
                stateMessage.setText(R.string.marketplace_state_timeout_message);
                break;
            case ERROR:
            default:
                stateBadge.setText(R.string.marketplace_state_error_badge);
                stateTitle.setText(R.string.marketplace_state_error_title);
                stateMessage.setText(R.string.marketplace_state_error_message);
                break;
        }
    }

    private void sortItems(int position) {
        if (allItems.isEmpty()) {
            adapter.submitList(new ArrayList<>());
            return;
        }

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
