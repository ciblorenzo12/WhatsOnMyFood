package com.ciblorenzo.whatsonmyfood;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

public class SubscriptionActivity extends BaseActivity implements BitwiseEntitlementManager.Listener {
    private BitwiseEntitlementManager entitlementManager;
    private TextView planStatusText;
    private TextView usageText;
    private TextView priceText;
    private TextView renewalTermsText;
    private TextView billingStateText;
    private View usageMeterFill;
    private Button subscribeButton;
    private Button restoreButton;
    private Button manageSubscriptionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        Toolbar toolbar = findViewById(R.id.subscription_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        planStatusText = findViewById(R.id.plan_status_text);
        usageText = findViewById(R.id.usage_text);
        priceText = findViewById(R.id.price_text);
        renewalTermsText = findViewById(R.id.renewal_terms_text);
        billingStateText = findViewById(R.id.billing_state_text);
        usageMeterFill = findViewById(R.id.usage_meter_fill);
        subscribeButton = findViewById(R.id.subscribe_button);
        restoreButton = findViewById(R.id.restore_button);
        manageSubscriptionButton = findViewById(R.id.manage_subscription_button);

        entitlementManager = new BitwiseEntitlementManager(this);
        entitlementManager.setListener(this);
        entitlementManager.start();

        subscribeButton.setOnClickListener(v -> entitlementManager.launchPurchase(this));
        restoreButton.setOnClickListener(v -> {
            entitlementManager.refreshPurchases();
            Toast.makeText(this, R.string.checking_google_play_purchases, Toast.LENGTH_SHORT).show();
        });
        manageSubscriptionButton.setOnClickListener(v -> openGooglePlaySubscriptions());

        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (entitlementManager != null) {
            entitlementManager.refreshPurchases();
        }
    }

    @Override
    protected void onDestroy() {
        if (entitlementManager != null) {
            entitlementManager.setListener(null);
            entitlementManager.end();
        }
        super.onDestroy();
    }

    @Override
    public void onEntitlementChanged() {
        runOnUiThread(this::updateUi);
    }

    @Override
    public void onBillingMessage(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private void updateUi() {
        boolean premium = entitlementManager.isPremiumActive();
        int used = entitlementManager.getDailyUsage();
        int limit = entitlementManager.getDailyLimit();
        int remaining = entitlementManager.getRemainingFreeUses();
        boolean billingReady = entitlementManager.isBillingReady();
        boolean productLoaded = entitlementManager.isProductDetailsLoaded();
        boolean productQueryComplete = entitlementManager.isProductQueryComplete();
        BitwiseEntitlementManager.AccessState accessState = entitlementManager.getAccessState();

        planStatusText.setText(premium ? R.string.bitwise_plan_active : R.string.bitwise_plan_free);
        usageText.setText(premium
                ? getString(R.string.bitwise_usage_unlimited)
                : getString(R.string.bitwise_usage_remaining, remaining, limit, entitlementManager.getResetLabel()));
        priceText.setText(entitlementManager.getPriceLabel());
        renewalTermsText.setText(entitlementManager.getRenewalTerms());

        String billingMessage = entitlementManager.getLastBillingMessage();
        billingStateText.setText(billingMessage);
        billingStateText.setVisibility(billingMessage.isEmpty() ? View.GONE : View.VISIBLE);
        if (premium) {
            subscribeButton.setText(R.string.bitwise_plus_active);
        } else if (accessState == BitwiseEntitlementManager.AccessState.VERIFYING) {
            subscribeButton.setText(R.string.billing_verifying_purchase);
        } else if (accessState == BitwiseEntitlementManager.AccessState.PENDING) {
            subscribeButton.setText(R.string.billing_payment_pending);
        } else if (!billingReady) {
            subscribeButton.setText(R.string.billing_connecting);
        } else if (!productQueryComplete) {
            subscribeButton.setText(R.string.billing_loading_product);
        } else if (!productLoaded) {
            subscribeButton.setText(R.string.billing_product_unavailable);
        } else {
            subscribeButton.setText(R.string.subscribe_bitwise_plus);
        }
        subscribeButton.setEnabled(!premium
                && accessState != BitwiseEntitlementManager.AccessState.VERIFYING
                && accessState != BitwiseEntitlementManager.AccessState.PENDING
                && billingReady
                && productLoaded);
        restoreButton.setEnabled(billingReady && !entitlementManager.isVerificationInProgress());

        usageMeterFill.post(() -> {
            View parent = (View) usageMeterFill.getParent();
            int parentWidth = parent.getWidth();
            float ratio = premium ? 1f : Math.min(1f, Math.max(0f, (float) (limit - used) / (float) limit));
            ViewGroup.LayoutParams params = usageMeterFill.getLayoutParams();
            params.width = Math.max(0, Math.round(parentWidth * ratio));
            usageMeterFill.setLayoutParams(params);
        });
    }

    private void openGooglePlaySubscriptions() {
        Uri uri = Uri.parse("https://play.google.com/store/account/subscriptions")
                .buildUpon()
                .appendQueryParameter("sku", BitwiseEntitlementManager.PRODUCT_ID_BITWISE_PLUS)
                .appendQueryParameter("package", getPackageName())
                .build();
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
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
