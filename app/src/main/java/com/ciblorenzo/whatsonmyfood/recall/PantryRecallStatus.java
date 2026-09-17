package com.ciblorenzo.whatsonmyfood.recall;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import com.ciblorenzo.whatsonmyfood.Product;
import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;

/** The last successful evidence is retained even when a later refresh fails. */
@Entity(tableName = "pantry_recall_status", primaryKeys = {"userId", "barcode"})
public class PantryRecallStatus {
    @NonNull public String userId = "";
    @NonNull public String barcode = "";
    public String fingerprint = "";
    public String resultJson;
    public long lastSuccessfulAt;
    public long lastAttemptAt;
    public boolean failed;
    public static final long RECHECK_MS = TimeUnit.HOURS.toMillis(24);

    public FoodRecallCheckResult result() {
        try { return new Gson().fromJson(resultJson, FoodRecallCheckResult.class); }
        catch (RuntimeException invalid) { return null; }
    }

    public boolean isCurrent(Product product, long now) {
        return !failed && result() != null && lastSuccessfulAt > 0
                && now >= lastSuccessfulAt && now - lastSuccessfulAt < RECHECK_MS
                && identity(product).equals(fingerprint);
    }

    public FoodRecallState displayState(Product product, long now) {
        FoodRecallCheckResult result = result();
        // A previous match must stay visible during an outage or after details change.
        if (result != null && result.record != null) return result.state;
        if (failed) return FoodRecallState.ERROR;
        if (lastSuccessfulAt == 0 || result == null) return FoodRecallState.READY;
        return isCurrent(product, now) ? result.state : FoodRecallState.STALE;
    }

    public static String identity(Product product) {
        return new Gson().toJson(new String[] {product.barcode, product.productName,
                product.brands, product.quantity});
    }
}
