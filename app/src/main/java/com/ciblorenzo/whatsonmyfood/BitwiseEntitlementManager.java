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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BitwiseEntitlementManager implements PurchasesUpdatedListener {
    public interface Listener {
        void onEntitlementChanged();

        void onBillingMessage(String message);
    }

    public enum AccessState {
        FREE,
        CONNECTING,
        VERIFYING,
        PENDING,
        ACTIVE,
        ERROR
    }

    public static final String PRODUCT_ID_BITWISE_PLUS = "bitwise_plus_monthly";
    private static final String TAG = "BitwiseEntitlement";
    private static final String PREFS = "bitwise_plus";
    private static final String KEY_PREMIUM = "premium_active";
    private static final String KEY_ACCOUNT_HASH = "verified_account_hash";
    private static final String KEY_EXPIRY_MS = "verified_expiry_ms";
    private static final String KEY_LAST_VERIFIED_MS = "last_verified_ms";
    private static final String KEY_SECURITY_VERSION = "security_version";
    private static final String KEY_USAGE_DAY = "usage_day";
    private static final String KEY_DAILY_USAGE = "daily_usage";
    private static final int CURRENT_SECURITY_VERSION = 1;
    private static final int FREE_DAILY_LIMIT = 3;
    private static final long MAX_OFFLINE_VERIFICATION_AGE_MS = 72L * 60L * 60L * 1000L;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final BillingClient billingClient;
    private final BitwiseBillingVerificationClient verificationClient;
    @Nullable private Listener listener;
    @Nullable private ProductDetails productDetails;
    @Nullable private String verifyingPurchaseToken;
    private boolean connectionStarted;
    private boolean productQueryComplete;
    private boolean purchasePending;
    private boolean verificationInProgress;
    private AccessState accessState = AccessState.CONNECTING;
    private String lastBillingMessage = "";

    public BitwiseEntitlementManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        verificationClient = new BitwiseBillingVerificationClient();
        migrateUnverifiedPrototypeEntitlement();
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .enablePrepaidPlans()
                        .build())
                .enableAutoServiceReconnection()
                .build();
        accessState = isPremiumActive() ? AccessState.ACTIVE : AccessState.CONNECTING;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (billingClient.isReady()) {
            queryProductDetails();
            refreshPurchases();
            return;
        }
        if (connectionStarted) return;
        connectionStarted = true;
        accessState = isPremiumActive() ? AccessState.ACTIVE : AccessState.CONNECTING;
        notifyChanged();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                connectionStarted = false;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                    refreshPurchases();
                } else {
                    accessState = isPremiumActive() ? AccessState.ACTIVE : AccessState.ERROR;
                    setMessage("Google Play billing is unavailable: " + billingResult.getDebugMessage(), true);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                connectionStarted = false;
                if (!isPremiumActive()) accessState = AccessState.CONNECTING;
                notifyChanged();
            }
        });
    }

    public void end() {
        if (billingClient.isReady()) billingClient.endConnection();
    }

    public boolean isPremiumActive() {
        if (!prefs.getBoolean(KEY_PREMIUM, false)) return false;
        String accountHash = accountIdHash();
        if (accountHash.isEmpty() || !accountHash.equals(prefs.getString(KEY_ACCOUNT_HASH, ""))) return false;
        long expiryMs = prefs.getLong(KEY_EXPIRY_MS, 0L);
        long lastVerifiedMs = prefs.getLong(KEY_LAST_VERIFIED_MS, 0L);
        long now = System.currentTimeMillis();
        return expiryMs > now && now - lastVerifiedMs <= MAX_OFFLINE_VERIFICATION_AGE_MS;
    }

    public boolean isBillingReady() {
        return billingClient.isReady();
    }

    public boolean isProductDetailsLoaded() {
        return productDetails != null;
    }

    public boolean isProductQueryComplete() {
        return productQueryComplete;
    }

    public boolean isPurchasePending() {
        return purchasePending;
    }

    public boolean isVerificationInProgress() {
        return verificationInProgress;
    }

    public AccessState getAccessState() {
        return isPremiumActive() ? AccessState.ACTIVE : accessState;
    }

    public String getLastBillingMessage() {
        return lastBillingMessage;
    }

    public boolean canUseBitwise() {
        return isPremiumActive() || getRemainingFreeUses() > 0;
    }

    public void recordBitwiseUse() {
        if (isPremiumActive()) return;
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
        ProductDetails.PricingPhase recurringPhase = recurringPricingPhase();
        if (recurringPhase == null) return appContext.getString(R.string.billing_price_in_google_play);
        return recurringPhase.getFormattedPrice() + billingPeriodSuffix(recurringPhase.getBillingPeriod());
    }

    public String getRenewalTerms() {
        ProductDetails.PricingPhase recurringPhase = recurringPricingPhase();
        if (recurringPhase == null) {
            return appContext.getString(R.string.billing_final_terms_in_google_play);
        }
        return appContext.getString(
                R.string.billing_renewal_terms,
                recurringPhase.getFormattedPrice() + billingPeriodSuffix(recurringPhase.getBillingPeriod())
        );
    }

    public void launchPurchase(Activity activity) {
        String accountHash = accountIdHash();
        if (accountHash.isEmpty()) {
            setMessage("Sign in before purchasing Bitwise Plus.", true);
            return;
        }
        if (!billingClient.isReady()) {
            start();
            setMessage("Connecting to Google Play. Try again in a moment.", true);
            return;
        }
        ProductDetails.SubscriptionOfferDetails offer = selectedOffer();
        if (productDetails == null || offer == null) {
            queryProductDetails();
            setMessage("Bitwise Plus is not available from Google Play for this build or account.", true);
            return;
        }

        List<BillingFlowParams.ProductDetailsParams> detailsParams = new ArrayList<>();
        detailsParams.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offer.getOfferToken())
                .build());
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(detailsParams)
                .setObfuscatedAccountId(accountHash)
                .build();
        BillingResult result = billingClient.launchBillingFlow(activity, params);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            setMessage("Could not open Google Play: " + result.getDebugMessage(), true);
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases);
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            setMessage("Purchase canceled. No charge was made.", true);
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            refreshPurchases();
        } else {
            setMessage("Purchase update failed: " + billingResult.getDebugMessage(), true);
        }
    }

    private void queryProductDetails() {
        if (!billingClient.isReady()) return;
        productQueryComplete = false;
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_BITWISE_PLUS)
                .setProductType(BillingClient.ProductType.SUBS)
                .build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            productQueryComplete = true;
            productDetails = null;
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && result != null
                    && !result.getProductDetailsList().isEmpty()) {
                productDetails = result.getProductDetailsList().get(0);
            } else {
                Log.w(TAG, "Product details unavailable: " + billingResult.getDebugMessage());
            }
            notifyChanged();
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
            } else {
                accessState = isPremiumActive() ? AccessState.ACTIVE : AccessState.ERROR;
                setMessage("Could not refresh purchases: " + billingResult.getDebugMessage(), true);
            }
        });
    }

    private void processPurchases(List<Purchase> purchases) {
        Purchase purchased = null;
        boolean pending = false;
        for (Purchase purchase : purchases) {
            if (!purchase.getProducts().contains(PRODUCT_ID_BITWISE_PLUS)) continue;
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) purchased = purchase;
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) pending = true;
        }

        purchasePending = purchased == null && pending;
        if (purchased != null) {
            verifyPurchase(purchased);
        } else if (purchasePending) {
            clearEntitlement();
            accessState = AccessState.PENDING;
            setMessage("Payment is pending in Google Play. Bitwise Plus will activate after payment completes.", false);
        } else {
            clearEntitlement();
            accessState = AccessState.FREE;
            notifyChanged();
        }
    }

    private void verifyPurchase(Purchase purchase) {
        String accountHash = accountIdHash();
        if (accountHash.isEmpty()) {
            clearEntitlement();
            accessState = AccessState.ERROR;
            setMessage("Sign in to verify your Bitwise Plus purchase.", true);
            return;
        }
        if (purchase.getPurchaseToken().equals(verifyingPurchaseToken)) return;
        verifyingPurchaseToken = purchase.getPurchaseToken();
        verificationInProgress = true;
        purchasePending = false;
        accessState = AccessState.VERIFYING;
        notifyChanged();

        verificationClient.verify(purchase.getPurchaseToken(), accountHash,
                new BitwiseBillingVerificationClient.Callback() {
                    @Override
                    public void onResult(BitwiseBillingVerificationClient.VerificationResult result) {
                        verifyingPurchaseToken = null;
                        verificationInProgress = false;
                        purchasePending = result.pending;
                        if (result.verified && result.entitlementActive) {
                            saveVerifiedEntitlement(accountHash, result.expiryTime);
                            accessState = AccessState.ACTIVE;
                            if (result.acknowledgementPending) acknowledgeIfNeeded(purchase);
                            setMessage("Bitwise Plus is active on this account.", false);
                        } else if (result.pending) {
                            clearEntitlement();
                            accessState = AccessState.PENDING;
                            setMessage("Payment is pending in Google Play. Access will activate automatically.", false);
                        } else {
                            clearEntitlement();
                            accessState = AccessState.FREE;
                            setMessage("Google Play did not confirm an active Bitwise Plus subscription for this account.", true);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        verifyingPurchaseToken = null;
                        verificationInProgress = false;
                        accessState = isPremiumActive() ? AccessState.ACTIVE : AccessState.ERROR;
                        setMessage(message, true);
                    }
                });
    }

    private void acknowledgeIfNeeded(Purchase purchase) {
        if (purchase.isAcknowledged()) return;
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Purchase acknowledgement failed: " + billingResult.getDebugMessage());
                setMessage("Your purchase is active, but Google Play acknowledgement will be retried.", true);
            }
        });
    }

    @Nullable
    private ProductDetails.SubscriptionOfferDetails selectedOffer() {
        if (productDetails == null || productDetails.getSubscriptionOfferDetails() == null) return null;
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        for (ProductDetails.SubscriptionOfferDetails offer : offers) {
            if (offer.getOfferId() == null) return offer;
        }
        return offers.isEmpty() ? null : offers.get(0);
    }

    @Nullable
    private ProductDetails.PricingPhase recurringPricingPhase() {
        ProductDetails.SubscriptionOfferDetails offer = selectedOffer();
        if (offer == null || offer.getPricingPhases() == null) return null;
        List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
        return phases.isEmpty() ? null : phases.get(phases.size() - 1);
    }

    private String billingPeriodSuffix(String billingPeriod) {
        if ("P1W".equals(billingPeriod)) return appContext.getString(R.string.billing_period_week);
        if ("P1M".equals(billingPeriod)) return appContext.getString(R.string.billing_period_month);
        if ("P3M".equals(billingPeriod)) return appContext.getString(R.string.billing_period_three_months);
        if ("P6M".equals(billingPeriod)) return appContext.getString(R.string.billing_period_six_months);
        if ("P1Y".equals(billingPeriod)) return appContext.getString(R.string.billing_period_year);
        return "";
    }

    private void saveVerifiedEntitlement(String accountHash, String expiryTime) {
        long expiryMs;
        try {
            expiryMs = Instant.parse(expiryTime).toEpochMilli();
        } catch (Exception ignored) {
            expiryMs = 0L;
        }
        prefs.edit()
                .putBoolean(KEY_PREMIUM, expiryMs > System.currentTimeMillis())
                .putString(KEY_ACCOUNT_HASH, accountHash)
                .putLong(KEY_EXPIRY_MS, expiryMs)
                .putLong(KEY_LAST_VERIFIED_MS, System.currentTimeMillis())
                .apply();
        notifyChanged();
    }

    private void clearEntitlement() {
        prefs.edit()
                .putBoolean(KEY_PREMIUM, false)
                .remove(KEY_ACCOUNT_HASH)
                .remove(KEY_EXPIRY_MS)
                .remove(KEY_LAST_VERIFIED_MS)
                .apply();
    }

    private void migrateUnverifiedPrototypeEntitlement() {
        if (prefs.getInt(KEY_SECURITY_VERSION, 0) >= CURRENT_SECURITY_VERSION) return;
        prefs.edit()
                .putInt(KEY_SECURITY_VERSION, CURRENT_SECURITY_VERSION)
                .putBoolean(KEY_PREMIUM, false)
                .remove(KEY_ACCOUNT_HASH)
                .remove(KEY_EXPIRY_MS)
                .remove(KEY_LAST_VERIFIED_MS)
                .apply();
    }

    String accountIdHash() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "" : sha256(user.getUid());
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte valueByte : bytes) builder.append(String.format(Locale.US, "%02x", valueByte & 0xff));
            return builder.toString();
        } catch (Exception error) {
            Log.e(TAG, "Could not hash the billing account identifier", error);
            return "";
        }
    }

    private void resetUsageWindowIfNeeded() {
        String today = todayKey();
        if (!today.equals(prefs.getString(KEY_USAGE_DAY, ""))) {
            prefs.edit().putString(KEY_USAGE_DAY, today).putInt(KEY_DAILY_USAGE, 0).apply();
        }
    }

    private String todayKey() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.DAY_OF_YEAR);
    }

    private void notifyChanged() {
        if (listener != null) listener.onEntitlementChanged();
    }

    private void setMessage(String message, boolean notifyUser) {
        lastBillingMessage = message == null ? "" : message;
        notifyChanged();
        if (notifyUser && listener != null) listener.onBillingMessage(lastBillingMessage);
    }
}
