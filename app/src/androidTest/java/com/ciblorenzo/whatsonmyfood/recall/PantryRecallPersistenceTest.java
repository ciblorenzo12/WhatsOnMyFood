package com.ciblorenzo.whatsonmyfood.recall;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ciblorenzo.whatsonmyfood.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PantryRecallPersistenceTest {
    @Test public void upgradePreservesPantryAndRecallEvidenceSurvivesRestart() {
        Context context = ApplicationProvider.getApplicationContext();
        String name = "m9-recall-migration-" + System.nanoTime();
        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, name).allowMainThreadQueries().build();
        try {
            Product product = new Product("012345678905", "Oat Cereal", "Sample Foods", "12 oz", "", "", "", "", "", "", "", "");
            db.productDao().insertProduct(product);
            db.productDao().insertPantry(new Pantry(product.barcode, "owner"));
            db.close();
            // Reconstruct the previous schema: v12 only adds these two tables.
            try (SQLiteDatabase old = SQLiteDatabase.openDatabase(context.getDatabasePath(name).getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
                old.execSQL("DROP TABLE pantry_recall_status");
                old.execSQL("DROP TABLE recall_alerts");
                old.setVersion(11);
            }
            db = Room.databaseBuilder(context, AppDatabase.class, name).allowMainThreadQueries()
                    .addMigrations(AppDatabase.MIGRATION_11_12).build();
            assertEquals(1, db.productDao().countPantryProducts("owner"));
            assertEquals("Oat Cereal", db.productDao().getProductWithDetails(product.barcode).product.productName);
            PantryRecallStatus status = new PantryRecallStatus(); status.userId = "owner"; status.barcode = product.barcode;
            status.lastSuccessfulAt = 12345; status.lastAttemptAt = 23456; status.failed = true;
            status.fingerprint = PantryRecallStatus.identity(product);
            status.resultJson = new com.google.gson.Gson().toJson(FoodRecallCheckResult.possible(new FoodRecallRecord(
                    "F-TEST", "Oat Cereal", "Sample Foods", "Class I", "Allergen", "Lot 1", "20260901", "Ongoing"), 60, "2026-09-01"));
            db.pantryRecallDao().save(status);
            RecallAlert alert = new RecallAlert(); alert.userId = "owner"; alert.barcode = product.barcode; alert.recallId = "F-TEST";
            db.pantryRecallDao().saveAlert(alert); db.pantryRecallDao().saveAlert(alert);
            db.close();
            db = Room.databaseBuilder(context, AppDatabase.class, name).allowMainThreadQueries().build();
            PantryRecallStatus restored = db.pantryRecallDao().get("owner", product.barcode);
            assertEquals(12345, restored.lastSuccessfulAt); assertTrue(restored.failed);
            assertEquals(FoodRecallState.POSSIBLE_MATCH, restored.displayState(product, 23456));
            assertEquals("F-TEST", restored.result().record.recallNumber);
            assertEquals(1, db.pantryRecallDao().alertCount("owner", product.barcode, "F-TEST"));
            assertEquals(0, db.pantryRecallDao().alertCount("other-user", product.barcode, "F-TEST"));
            assertNull(db.pantryRecallDao().get("other-user", product.barcode));
        } finally { db.close(); context.deleteDatabase(name); }
    }
}
