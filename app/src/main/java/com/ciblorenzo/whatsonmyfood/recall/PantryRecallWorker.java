package com.ciblorenzo.whatsonmyfood.recall;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ciblorenzo.whatsonmyfood.AppDatabase;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.IOException;

public class PantryRecallWorker extends Worker {
    public PantryRecallWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }
    @NonNull @Override public Result doWork() {
        String userId = getInputData().getString("user");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (userId == null || user == null || !userId.equals(user.getUid())) return Result.success();
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        String barcode = getInputData().getString("barcode");
        if (barcode == null) {
            for (Product product : db.productDao().getPantryProducts(userId)) {
                if (isStopped()) return Result.success();
                PantryRecallScheduler.item(getApplicationContext(), userId, product.barcode);
            }
            return Result.success();
        }
        ProductWithDetails details = db.productDao().getProductWithDetails(barcode);
        if (details == null || db.productDao().findPantryItemByBarcode(barcode, userId) == null) return Result.success();
        try {
            PantryRecallService.check(getApplicationContext(), userId, details.product, false, () -> !isStopped());
            // A product may have changed while the request was in flight; don't publish that old response.
            ProductWithDetails latest = db.productDao().getProductWithDetails(barcode);
            if (latest != null && !PantryRecallStatus.identity(latest.product).equals(PantryRecallStatus.identity(details.product))) {
                return Result.retry();
            }
            return Result.success();
        } catch (IOException | RuntimeException failure) {
            return Result.retry();
        }
    }
}
