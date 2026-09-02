package com.ciblorenzo.whatsonmyfood.recall;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.ciblorenzo.whatsonmyfood.BaseActivity;
import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.R;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;
import com.google.android.material.card.MaterialCardView;

public class FoodRecallActivity extends BaseActivity {
    public static final String EXTRA_DEBUG_STATE = "food_recall_debug_state";
    private static final String FDA_RECALLS_URL =
            "https://www.fda.gov/safety/recalls-market-withdrawals-safety-alerts";

    private MaterialCardView stateCard;
    private TextView stateBadge;
    private TextView stateTitle;
    private TextView stateMessage;
    private ProgressBar stateProgress;
    private Button primaryAction;
    private Button officialSourceAction;
    private FoodRecallState currentState = FoodRecallState.READY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_recall);

        Toolbar toolbar = findViewById(R.id.food_recall_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.food_recall_title);
        }

        stateCard = findViewById(R.id.food_recall_state_card);
        stateBadge = findViewById(R.id.food_recall_state_badge);
        stateTitle = findViewById(R.id.food_recall_state_title);
        stateMessage = findViewById(R.id.food_recall_state_message);
        stateProgress = findViewById(R.id.food_recall_state_progress);
        primaryAction = findViewById(R.id.food_recall_primary_action);
        officialSourceAction = findViewById(R.id.food_recall_official_source_action);
        GlassMotion.attachPress(primaryAction);
        GlassMotion.attachPress(officialSourceAction);

        ProductWithDetails productDetails = FoodRecallNavigation.readProduct(getIntent());
        if (productDetails == null || productDetails.product == null) {
            Toast.makeText(this, R.string.food_recall_missing_product, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindProduct(productDetails.product, FoodRecallNavigation.readEntryPoint(getIntent()));
        if (BuildConfig.DEBUG) {
            currentState = FoodRecallState.fromName(getIntent().getStringExtra(EXTRA_DEBUG_STATE));
        }
        render(currentState);

        primaryAction.setOnClickListener(view -> handlePrimaryAction());
        officialSourceAction.setOnClickListener(view -> openOfficialSource());
    }

    private void bindProduct(Product product, FoodRecallNavigation.EntryPoint entryPoint) {
        TextView entryContext = findViewById(R.id.food_recall_entry_context);
        TextView productName = findViewById(R.id.food_recall_product_name);
        TextView productBrand = findViewById(R.id.food_recall_product_brand);
        TextView productBarcode = findViewById(R.id.food_recall_product_barcode);

        entryContext.setText(entryPoint == FoodRecallNavigation.EntryPoint.SAVED_PRODUCT
                ? R.string.food_recall_saved_context
                : R.string.food_recall_scanned_context);
        productName.setText(safeText(product.productName, getString(R.string.food_recall_product_fallback)));
        productBrand.setText(safeText(product.brands, getString(R.string.food_recall_brand_fallback)));
        productBarcode.setText(getString(R.string.food_recall_barcode_value, product.barcode));
    }

    private void handlePrimaryAction() {
        if (FoodRecallPresentation.requiresImmediateAttention(currentState)) {
            openOfficialSource();
            return;
        }
        render(FoodRecallState.CHECKING);
        stateCard.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                render(FoodRecallState.UNAVAILABLE);
            }
        }, 450L);
    }

    private void openOfficialSource() {
        LinkHandler.openLink(
                this,
                FDA_RECALLS_URL,
                getString(R.string.food_recall_official_source),
                "Recalls, Market Withdrawals, and Safety Alerts"
        );
    }

    private void render(FoodRecallState state) {
        currentState = state == null ? FoodRecallState.READY : state;
        FoodRecallUiModel model = FoodRecallPresentation.forState(currentState);
        int color = ContextCompat.getColor(this, model.statusColor);

        stateBadge.setText(model.badgeText);
        stateBadge.setBackgroundTintList(ColorStateList.valueOf(color));
        stateTitle.setText(model.titleText);
        stateMessage.setText(model.messageText);
        stateProgress.setVisibility(model.showProgress ? View.VISIBLE : View.GONE);
        primaryAction.setVisibility(model.showPrimaryAction ? View.VISIBLE : View.GONE);
        officialSourceAction.setVisibility(model.showOfficialSource ? View.VISIBLE : View.GONE);
        stateCard.setStrokeColor(color);
        if (model.showPrimaryAction) {
            primaryAction.setText(model.primaryActionText);
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
