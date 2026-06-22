package com.ciblorenzo.whatsonmyfood;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BitwiseEntitlementManager implements PurchasesUpdatedListener {
    public interface Listener {
        void onEntitlementChanged();
        void onBillingMessage(String message);
    }

    public static final String PRODUCT_ID_BITWISE_PLUS = "bitwise_plus_monthly";
    private static final String TAG = "BitwiseEntitlement";
    private static final String PREFS = "bitwise_plus";
    private static final String KEY_PREMIUM = "premium_active";
    private static final String KEY_USAGE_DAY = "usage_day";
    private static final String KEY_DAILY_USAGE = "daily_usage";
    private static final int FREE_DAILY_LIMIT = 3;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final BillingClient billingClient;
    @Nullable private Listener listener;
    @Nullable private ProductDetails productDetails;
    private boolean connectionStarted;

    public BitwiseEntitlementManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .enablePrepaidPlans()
                        .build())
                .enableAutoServiceReconnection()
                .build();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (billingClient.isReady() || connectionStarted) {
            return;
        }
        connectionStarted = true;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                connectionStarted = false;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                    refreshPurchases();
                } else {
                    notifyMessage("Billing is not ready: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                connectionStarted = false;
            }
        });
    }

    public void end() {
        if (billingClient.isReady()) {
            billingClient.endConnection();
        }
    }

    public boolean isPremiumActive() {
        return prefs.getBoolean(KEY_PREMIUM, false);
    }

    public boolean canUseBitwise() {
        return isPremiumActive() || getRemainingFreeUses() > 0;
    }

    public void recordBitwiseUse() {
        if (isPremiumActive()) {
            return;
        }
        resetUsageWindowIfNeeded();
        prefs.edit().putInt(KEY_DAILY_USAGE, getDailyUsage() + 1).apply();
    }

    public int getDailyLimit() {
        return FREE_DAILY_LIMIT;
    }

    public int getDailyUsage() {
        resetUsageWindowIfNeeded();
        return prefs.getInt(KEY_DAILY_USAGE, 0);
    }

    public int getRemainingFreeUses() {
        return Math.max(0, FREE_DAILY_LIMIT - getDailyUsage());
    }

    public String getResetLabel() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(tomorrow.getTime());
    }

    public String getPriceLabel() {
        if (productDetails == null || productDetails.getSubscriptionOfferDetails() == null
                || productDetails.getSubscriptionOfferDetails().isEmpty()) {
            return "Price shown in Google Play";
        }
        ProductDetails.SubscriptionOfferDetails offer = productDetails.getSubscriptionOfferDetails().get(0);
        if (offer.getPricingPhases() == null || offer.getPricingPhases().getPricingPhaseList().isEmpty()) {
            return "Price shown in Google Play";
        }
        return offer.getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
    }

    public void launchPurchase(Activity activity) {
        if (!billingClient.isReady()) {
            start();
            notifyMessage("Connecting to Google Play. Try again in a moment.");
            return;
        }
        if (productDetails == null) {
            queryProductDetails();
            notifyMessage("Bitwise Plus is still loading from Google Play.");
            return;
        }
        if (productDetails.getSubscriptionOfferDetails() == null
                || productDetails.getSubscriptionOfferDetails().isEmpty()) {
            notifyMessage("Create the bitwise_plus_monthly subscription in Play Console first.");
            return;
        }

        String offerToken = productDetails.getSubscriptionOfferDetails().get(0).getOfferToken();
        List<BillingFlowParams.ProductDetailsParams> productDetailsParams = new ArrayList<>();
        productDetailsParams.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build());

        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParams)
                .build();
        BillingResult result = billingClient.launchBillingFlow(activity, params);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            notifyMessage("Could not open Google Play: " + result.getDebugMessage());
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases);
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            notifyMessage("Purchase canceled.");
        } else {
            notifyMessage("Purchase update failed: " + billingResult.getDebugMessage());
        }
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_BITWISE_PLUS)
                .setProductType(BillingClient.ProductType.SUBS)
                .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && result != null
                    && !result.getProductDetailsList().isEmpty()) {
                productDetails = result.getProductDetailsList().get(0);
                notifyChanged();
            } else {
                Log.d(TAG, "Product details unavailable: " + billingResult.getDebugMessage());
            }
        });
    }

    public void refreshPurchases() {
        if (!billingClient.isReady()) {
            start();
            return;
        }
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases);
            }
        });
    }

    private void processPurchases(List<Purchase> purchases) {
        boolean active = false;
        for (Purchase purchase : purchases) {
            if (purchase.getProducts().contains(PRODUCT_ID_BITWISE_PLUS)
                    && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                active = true;
                acknowledgeIfNeeded(purchase);
            }
        }
        prefs.edit().putBoolean(KEY_PREMIUM, active).apply();
        notifyChanged();
    }

    private void acknowledgeIfNeeded(Purchase purchase) {
        if (purchase.isAcknowledged()) {
            return;
        }
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Purchase acknowledgement failed: " + billingResult.getDebugMessage());
            }
        });
    }

    private void resetUsageWindowIfNeeded() {
        String today = todayKey();
        if (!today.equals(prefs.getString(KEY_USAGE_DAY, ""))) {
            prefs.edit()
                    .putString(KEY_USAGE_DAY, today)
                    .putInt(KEY_DAILY_USAGE, 0)
                    .apply();
        }
    }

    private String todayKey() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.DAY_OF_YEAR);
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onEntitlementChanged();
        }
    }

    private void notifyMessage(String message) {
        if (listener != null) {
            listener.onBillingMessage(message);
        }
    }
}
