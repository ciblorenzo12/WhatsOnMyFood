package com.ciblorenzo.whatsonmyfood.recall;

import android.content.Context;
import com.ciblorenzo.whatsonmyfood.AppDatabase;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.IOException;
import java.util.function.BooleanSupplier;

public final class PantryRecallService {
    private PantryRecallService() {}
    public static PantryRecallStatus check(Context context, String userId, Product product,
                                           boolean force, BooleanSupplier running) throws IOException {
        AppDatabase db = AppDatabase.getDatabase(context);
        BooleanSupplier active = () -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (!running.getAsBoolean() || user == null || !userId.equals(user.getUid())
                    || db.productDao().findPantryItemByBarcode(product.barcode, userId) == null) return false;
            ProductWithDetails latest = db.productDao().getProductWithDetails(product.barcode);
            return latest != null && PantryRecallStatus.identity(product).equals(PantryRecallStatus.identity(latest.product));
        };
        RecallCheckEngine.Store store = new RecallCheckEngine.Store() {
            public PantryRecallStatus load(String barcode) { return db.pantryRecallDao().get(userId, barcode); }
            public boolean saveIfCurrent(Product expected, PantryRecallStatus status) {
                return db.runInTransaction(() -> {
                    if (!active.getAsBoolean()) return false;
                    db.pantryRecallDao().save(status); return true;
                });
            }
            public boolean alreadyNotified(String barcode, String recallId) {
                return db.pantryRecallDao().alertCount(userId, barcode, recallId) > 0;
            }
            public void markNotified(String barcode, String recallId, long now) {
                RecallAlert alert = new RecallAlert(); alert.userId = userId; alert.barcode = barcode;
                alert.recallId = recallId; alert.notifiedAt = now; db.pantryRecallDao().saveAlert(alert);
            }
        };
        if (!active.getAsBoolean()) return null;
        return new RecallCheckEngine(userId, new FoodRecallRepository(), store,
                (item, recall) -> active.getAsBoolean() && RecallNotifications.post(context, userId, item, recall))
                .check(product, force, System.currentTimeMillis());
    }
}
