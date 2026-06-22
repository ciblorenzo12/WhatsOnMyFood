package com.ciblorenzo.whatsonmyfood;

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
    private View usageMeterFill;
    private Button subscribeButton;

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
        usageMeterFill = findViewById(R.id.usage_meter_fill);
        subscribeButton = findViewById(R.id.subscribe_button);
        Button restoreButton = findViewById(R.id.restore_button);

        entitlementManager = new BitwiseEntitlementManager(this);
        entitlementManager.setListener(this);
        entitlementManager.start();

        subscribeButton.setOnClickListener(v -> entitlementManager.launchPurchase(this));
        restoreButton.setOnClickListener(v -> {
            entitlementManager.refreshPurchases();
            Toast.makeText(this, "Checking Google Play purchases...", Toast.LENGTH_SHORT).show();
        });

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

        planStatusText.setText(premium ? "Bitwise Plus active" : "Free Bitwise access");
        usageText.setText(premium
                ? "Unlimited Bitwise explanations are available on this account."
                : remaining + " of " + limit + " free Bitwise explanations left today. Resets " + entitlementManager.getResetLabel() + ".");
        priceText.setText(entitlementManager.getPriceLabel());
        subscribeButton.setText(premium ? "Bitwise Plus is active" : getString(R.string.subscribe_bitwise_plus));
        subscribeButton.setEnabled(!premium);

        usageMeterFill.post(() -> {
            View parent = (View) usageMeterFill.getParent();
            int parentWidth = parent.getWidth();
            float ratio = premium ? 1f : Math.min(1f, Math.max(0f, (float) (limit - used) / (float) limit));
            ViewGroup.LayoutParams params = usageMeterFill.getLayoutParams();
            params.width = Math.max(0, Math.round(parentWidth * ratio));
            usageMeterFill.setLayoutParams(params);
        });
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
