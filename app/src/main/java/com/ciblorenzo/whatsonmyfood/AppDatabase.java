package com.ciblorenzo.whatsonmyfood;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.ciblorenzo.whatsonmyfood.recall.PantryRecallStatus;
import com.ciblorenzo.whatsonmyfood.recall.PantryRecallDao;
import com.ciblorenzo.whatsonmyfood.recall.RecallAlert;

@Database(entities = {Product.class, Nutriments.class, Ingredient.class, Pantry.class, CacheMeta.class, AdditiveEntry.class, PantryRecallStatus.class, RecallAlert.class}, version = 13, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract ProductDao productDao();
    public abstract AdditiveDao additiveDao();
    public abstract PantryRecallDao pantryRecallDao();

    public static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS pantry_recall_status (userId TEXT NOT NULL, barcode TEXT NOT NULL, fingerprint TEXT, resultJson TEXT, lastSuccessfulAt INTEGER NOT NULL, lastAttemptAt INTEGER NOT NULL, failed INTEGER NOT NULL, PRIMARY KEY(userId, barcode))");
            db.execSQL("CREATE TABLE IF NOT EXISTS recall_alerts (userId TEXT NOT NULL, barcode TEXT NOT NULL, recallId TEXT NOT NULL, notifiedAt INTEGER NOT NULL, PRIMARY KEY(userId, barcode, recallId))");
        }
    };

    public static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            // Older saved records keep their data without inventing a provider attribution.
            db.execSQL("ALTER TABLE cache_meta ADD COLUMN sourceName TEXT");
            db.execSQL("ALTER TABLE cache_meta ADD COLUMN usdaNutrientBasis TEXT");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "food_database")
                            .addMigrations(MIGRATION_11_12, MIGRATION_12_13)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
