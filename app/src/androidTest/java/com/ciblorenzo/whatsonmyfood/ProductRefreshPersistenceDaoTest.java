package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
public class ProductRefreshPersistenceDaoTest {

    private static final String BARCODE = "m4-06-dao-product";
    private static final String USER_ID = "m4-06-user";

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
    public void savingAndLoadingReturnsThePantryProduct() {
        saveInitialProduct();

        List<ProductWithDetails> products = dao.getPantryProductsWithDetails(USER_ID);

        assertEquals(1, products.size());
        assertEquals(BARCODE, products.get(0).product.barcode);
        assertEquals("Original Pantry Product", products.get(0).product.productName);
        assertEquals("original ingredient", products.get(0).ingredients.get(0).text);
    }

    @Test
    public void removingMembershipLeavesReusableProductDataIntact() {
        saveInitialProduct();

        assertEquals(1, dao.deletePantryProduct(BARCODE, USER_ID));

        assertNull(dao.findPantryItemByBarcode(BARCODE, USER_ID));
        assertNotNull(dao.getProductWithDetails(BARCODE));
    }

    @Test
    public void refreshUpdatesExternalDataAndPreservesAllLocalState() {
        saveInitialProduct();
        ProductWithDetails refreshed = details(
                "Updated Product Name",
                "refreshed ingredient",
                null,
                null,
                0,
                false
        );

        dao.insertRefreshedProductWithDetails(refreshed);

        ProductWithDetails stored = dao.getProductWithDetails(BARCODE);
        assertNotNull(stored);
        assertEquals("Updated Product Name", stored.product.productName);
        assertEquals("refreshed ingredient", stored.ingredients.get(0).text);
        assertTrue(stored.product.isFavorite);
        assertEquals(Integer.valueOf(93), stored.product.healthScore);
        assertEquals("{\"summary\":\"Saved Bitwise insight\"}", stored.product.aiInsight);
        assertEquals(Integer.valueOf(40), stored.product.userIngredientRiskScore);
        assertNotNull(dao.findPantryItemByBarcode(BARCODE, USER_ID));
        assertEquals(1, dao.countPantryProducts(USER_ID));
    }

    private void saveInitialProduct() {
        dao.insertProductWithDetails(details(
                "Original Pantry Product",
                "original ingredient",
                "{\"summary\":\"Saved Bitwise insight\"}",
                93,
                40,
                true
        ));
        dao.insertPantry(new Pantry(BARCODE, USER_ID));
    }

    private ProductWithDetails details(
            String name,
            String ingredientText,
            String aiInsight,
            Integer healthScore,
            Integer userScore,
            boolean favorite
    ) {
        Product product = new Product(
                BARCODE, name, "Test Foods", "12 oz", "", "", "Box", "Test",
                "1 serving", "b", "2", "b", aiInsight, healthScore, userScore
        );
        product.isFavorite = favorite;
        ProductWithDetails details = new ProductWithDetails();
        details.product = product;
        details.ingredients = Collections.singletonList(new Ingredient(BARCODE, ingredientText, 0));
        return details;
    }
}
