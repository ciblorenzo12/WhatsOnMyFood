package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PantryDaoFlowTest {

    private AppDatabase database;
    private ProductDao dao;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.productDao();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void saveTwice_createsOneEntryAndPreservesProductFields_thenRemoveClearsMembership() {
        String barcode = "m4-04-012345";
        String userId = "m4-04-user";
        Product product = new Product(
                barcode,
                "Whole Grain Test Cereal",
                "Test Foods",
                "12 oz",
                "https://example.test/product.png",
                "Whole grain",
                "Box",
                "Breakfast cereal",
                "1 cup",
                "b",
                "2",
                "b",
                "{\"summary\":\"Saved Bitwise insight\"}",
                82
        );
        ProductWithDetails details = new ProductWithDetails();
        details.product = product;
        details.ingredients = Collections.singletonList(new Ingredient(barcode, "whole grain oats", 0));

        dao.insertProductWithDetails(details);
        long firstInsert = dao.insertPantry(new Pantry(barcode, userId));
        long duplicateInsert = dao.insertPantry(new Pantry(barcode, userId));

        assertNotEquals(-1L, firstInsert);
        assertEquals(-1L, duplicateInsert);
        assertEquals(1, dao.countPantryProducts(userId));

        List<ProductWithDetails> savedProducts = dao.getPantryProductsWithDetails(userId);
        assertEquals(1, savedProducts.size());
        assertNotNull(savedProducts.get(0).product);
        assertEquals("Whole Grain Test Cereal", savedProducts.get(0).product.productName);
        assertEquals(Integer.valueOf(82), savedProducts.get(0).product.healthScore);
        assertEquals("{\"summary\":\"Saved Bitwise insight\"}", savedProducts.get(0).product.aiInsight);
        assertEquals(1, savedProducts.get(0).ingredients.size());

        assertEquals(1, dao.deletePantryProduct(barcode, userId));
        assertEquals(0, dao.countPantryProducts(userId));
        assertEquals(0, dao.deletePantryProduct(barcode, userId));
    }
}
