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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private View recallDetails;
    private TextView recallNumber;
    private TextView recallFirm;
    private TextView recallClassification;
    private TextView recallDescription;
    private TextView recallReason;
    private TextView recallCodes;
    private TextView recallReportDate;
    private FoodRecallState currentState = FoodRecallState.READY;
    private Product currentProduct;
    private final ExecutorService recallExecutor = Executors.newSingleThreadExecutor();
    private final FoodRecallRepository recallRepository = new FoodRecallRepository();

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
        recallDetails = findViewById(R.id.food_recall_details);
        recallNumber = findViewById(R.id.food_recall_number);
        recallFirm = findViewById(R.id.food_recall_firm);
        recallClassification = findViewById(R.id.food_recall_classification);
        recallDescription = findViewById(R.id.food_recall_description);
        recallReason = findViewById(R.id.food_recall_reason);
        recallCodes = findViewById(R.id.food_recall_codes);
        recallReportDate = findViewById(R.id.food_recall_report_date);
        GlassMotion.attachPress(primaryAction);
        GlassMotion.attachPress(officialSourceAction);

        ProductWithDetails productDetails = FoodRecallNavigation.readProduct(getIntent());
        if (productDetails == null || productDetails.product == null) {
            Toast.makeText(this, R.string.food_recall_missing_product, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentProduct = productDetails.product;
        bindProduct(currentProduct, FoodRecallNavigation.readEntryPoint(getIntent()));
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
        recallExecutor.execute(() -> {
            try {
                FoodRecallCheckResult result = recallRepository.check(currentProduct);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    render(result.state);
                    bindRecallDetails(result);
                });
            } catch (FoodRecallServiceException error) {
                showFailure(error.temporarilyUnavailable
                        ? FoodRecallState.UNAVAILABLE
                        : FoodRecallState.ERROR);
            } catch (IOException | RuntimeException error) {
                showFailure(FoodRecallState.ERROR);
            }
        });
    }

    private void showFailure(FoodRecallState state) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            render(state);
            bindRecallDetails(null);
        });
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
        if (currentState != FoodRecallState.POSSIBLE_MATCH
                && currentState != FoodRecallState.CONFIRMED_MATCH) {
            recallDetails.setVisibility(View.GONE);
        }
    }

    private void bindRecallDetails(FoodRecallCheckResult result) {
        FoodRecallRecord record = result == null ? null : result.record;
        if (record == null) {
            recallDetails.setVisibility(View.GONE);
            return;
        }
        recallNumber.setText(getString(
                R.string.food_recall_detail_number,
                safeText(record.recallNumber, getString(R.string.food_recall_detail_not_provided))
        ));
        recallFirm.setText(getString(
                R.string.food_recall_detail_firm,
                safeText(record.recallingFirm, getString(R.string.food_recall_detail_not_provided))
        ));
        recallClassification.setText(getString(
                R.string.food_recall_detail_classification,
                safeText(record.classification, getString(R.string.food_recall_detail_not_provided))
        ));
        recallDescription.setText(getString(
                R.string.food_recall_detail_product,
                safeText(record.productDescription, getString(R.string.food_recall_detail_not_provided))
        ));
        recallReason.setText(getString(
                R.string.food_recall_detail_reason,
                safeText(record.reasonForRecall, getString(R.string.food_recall_detail_not_provided))
        ));
        recallCodes.setText(getString(
                R.string.food_recall_detail_codes,
                safeText(record.codeInfo, getString(R.string.food_recall_detail_not_provided))
        ));
        recallReportDate.setText(getString(
                R.string.food_recall_detail_report_date,
                formatDate(record.reportDate)
        ));
        recallDetails.setVisibility(View.VISIBLE);
    }

    private String formatDate(String value) {
        if (value != null && value.matches("\\d{8}")) {
            return value.substring(4, 6) + "/" + value.substring(6, 8) + "/" + value.substring(0, 4);
        }
        return safeText(value, getString(R.string.food_recall_detail_not_provided));
    }

    @Override
    protected void onDestroy() {
        recallExecutor.shutdownNow();
        super.onDestroy();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
