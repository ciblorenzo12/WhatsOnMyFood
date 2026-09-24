package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class UsdaCacheMigrationTest {

    @Test
    public void migration12To13_preservesSavedRowsAndAddsNullableProvenance() {
        Context context = ApplicationProvider.getApplicationContext();
        String databaseName = "usda-migration-" + UUID.randomUUID() + ".db";
        try {
            try (SupportSQLiteOpenHelper oldHelper = helper(context, databaseName, 12)) {
                SupportSQLiteDatabase database = oldHelper.getWritableDatabase();
                database.execSQL("INSERT INTO cache_meta (barcode, lastUpdated) VALUES ('012345678905', 123456)");
            }
            try (SupportSQLiteOpenHelper newHelper = helper(context, databaseName, 13)) {
                SupportSQLiteDatabase database = newHelper.getWritableDatabase();
                try (Cursor row = database.query("SELECT barcode, lastUpdated, sourceName, usdaNutrientBasis FROM cache_meta")) {
                    assertTrue(row.moveToFirst());
                    assertEquals("012345678905", row.getString(0));
                    assertEquals(123456L, row.getLong(1));
                    assertTrue(row.isNull(2));
                    assertTrue(row.isNull(3));
                    assertEquals(1, row.getCount());
                }
                database.execSQL("UPDATE cache_meta SET sourceName = 'FoodDataCentralClient', usdaNutrientBasis = 'per100g'");
            }
            try (SupportSQLiteOpenHelper reopenedHelper = helper(context, databaseName, 13);
                 Cursor row = reopenedHelper.getReadableDatabase().query("SELECT sourceName, usdaNutrientBasis FROM cache_meta")) {
                assertTrue(row.moveToFirst());
                assertEquals("FoodDataCentralClient", row.getString(0));
                assertEquals("per100g", row.getString(1));
            }
        } finally {
            context.deleteDatabase(databaseName);
        }
    }

    private SupportSQLiteOpenHelper helper(Context context, String databaseName, int version) {
        return new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(databaseName)
                        .callback(new SupportSQLiteOpenHelper.Callback(version) {
                            @Override public void onCreate(SupportSQLiteDatabase database) {
                                database.execSQL("CREATE TABLE cache_meta (barcode TEXT NOT NULL PRIMARY KEY, lastUpdated INTEGER NOT NULL)");
                            }

                            @Override public void onUpgrade(SupportSQLiteDatabase database, int oldVersion, int newVersion) {
                                assertEquals(12, oldVersion);
                                assertEquals(13, newVersion);
                                AppDatabase.MIGRATION_12_13.migrate(database);
                            }
                        }).build());
    }
}
