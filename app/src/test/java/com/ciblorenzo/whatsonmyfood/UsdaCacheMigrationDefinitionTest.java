package com.ciblorenzo.whatsonmyfood;

import androidx.sqlite.db.SupportSQLiteDatabase;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UsdaCacheMigrationDefinitionTest {

    @Test
    public void version12To13_onlyAddsNullableSourceColumns() {
        List<String> statements = new ArrayList<>();
        SupportSQLiteDatabase database = (SupportSQLiteDatabase) Proxy.newProxyInstance(
                SupportSQLiteDatabase.class.getClassLoader(),
                new Class<?>[]{SupportSQLiteDatabase.class},
                (proxy, method, arguments) -> {
                    assertEquals("execSQL", method.getName());
                    statements.add((String) arguments[0]);
                    return null;
                });

        assertEquals(12, AppDatabase.MIGRATION_12_13.startVersion);
        assertEquals(13, AppDatabase.MIGRATION_12_13.endVersion);
        AppDatabase.MIGRATION_12_13.migrate(database);

        assertEquals(Arrays.asList(
                "ALTER TABLE cache_meta ADD COLUMN sourceName TEXT",
                "ALTER TABLE cache_meta ADD COLUMN usdaNutrientBasis TEXT"), statements);
    }
}
