package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;
import com.google.gson.Gson;
import java.io.IOException;

/** Shared by background work and manual refresh; no Android dependencies in the check policy. */
public final class RecallCheckEngine {
    public interface Store {
        PantryRecallStatus load(String barcode);
        boolean saveIfCurrent(Product product, PantryRecallStatus status);
        boolean alreadyNotified(String barcode, String recallId);
        void markNotified(String barcode, String recallId, long now);
    }
    public interface Notifier { boolean notify(Product product, FoodRecallRecord recall); }
    private final FoodRecallRepository repository;
    private final Store store;
    private final Notifier notifier;
    private final String userId;

    public RecallCheckEngine(String userId, FoodRecallRepository repository, Store store, Notifier notifier) {
        this.userId = userId; this.repository = repository; this.store = store; this.notifier = notifier;
    }

    public PantryRecallStatus check(Product product, boolean force, long now) throws IOException {
        // Serializes manual/background calls, including the notification ledger transition.
        synchronized (RecallCheckEngine.class) {
            PantryRecallStatus status = store.load(product.barcode);
            if (status == null) {
                status = new PantryRecallStatus(); status.userId = userId; status.barcode = product.barcode;
            }
            if (!force && status.isCurrent(product, now)) {
                if (!store.saveIfCurrent(product, status)) throw new IOException("Product changed during check");
                notifyNew(product, status, now);
                return status;
            }
            status.lastAttemptAt = now;
            try {
                FoodRecallCheckResult result = repository.check(product);
                status.resultJson = new Gson().toJson(result);
                status.fingerprint = PantryRecallStatus.identity(product);
                status.lastSuccessfulAt = now;
                status.failed = false;
            } catch (IOException | RuntimeException error) {
                status.failed = true;
                store.saveIfCurrent(product, status);
                if (error instanceof IOException) throw (IOException) error;
                throw new IOException("Recall check failed", error);
            }
            if (!store.saveIfCurrent(product, status)) throw new IOException("Product changed during check");
            notifyNew(product, status, now);
            return status;
        }
    }

    private void notifyNew(Product product, PantryRecallStatus status, long now) {
        FoodRecallCheckResult result = status.result();
        if (result == null) return;
        for (FoodRecallRecord record : result.matches()) {
            String id = record.recallNumber;
            if (!store.alreadyNotified(product.barcode, id) && notifier.notify(product, record)) {
                store.markNotified(product.barcode, id, now);
            }
        }
    }
}
