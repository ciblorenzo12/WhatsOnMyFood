package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class PantryRestartPersistenceTest {

    private static final String DATABASE_NAME = "m4-06-restart-persistence";
    private static final String BARCODE = "m4-06-restart-product";
    private static final String USER_ID = "m4-06-restart-user";

    private final Context context = ApplicationProvider.getApplicationContext();
    private AppDatabase database;

    @After
    public void cleanDatabase() {
        if (database != null && database.isOpen()) database.close();
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void pantryAndLocalInsightsSurviveDatabaseCloseAndReopenAfterRefresh() {
        context.deleteDatabase(DATABASE_NAME);
        database = openDatabase();
        ProductDao dao = database.productDao();

        Product savedProduct = product("Saved Product", "Saved insight", 87, 20, true);
        ProductWithDetails savedDetails = details(savedProduct, "saved ingredient");
        dao.insertProductWithDetails(savedDetails);
        dao.insertPantry(new Pantry(BARCODE, USER_ID));

        Product refreshedProduct = product("Refreshed Product", null, null, 0, false);
        dao.insertRefreshedProductWithDetails(details(refreshedProduct, "refreshed ingredient"));

        database.close();
        database = openDatabase();
        ProductDao reopenedDao = database.productDao();
        ProductWithDetails restored = reopenedDao.getProductWithDetails(BARCODE);

        assertNotNull(reopenedDao.findPantryItemByBarcode(BARCODE, USER_ID));
        assertEquals(1, reopenedDao.countPantryProducts(USER_ID));
        assertNotNull(restored);
        assertEquals("Refreshed Product", restored.product.productName);
        assertEquals("refreshed ingredient", restored.ingredients.get(0).text);
        assertTrue(restored.product.isFavorite);
        assertEquals(Integer.valueOf(87), restored.product.healthScore);
        assertEquals("Saved insight", restored.product.aiInsight);
        assertEquals(Integer.valueOf(20), restored.product.userIngredientRiskScore);
    }

    private AppDatabase openDatabase() {
        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                .allowMainThreadQueries()
                .build();
    }

    private Product product(
            String name,
            String insight,
            Integer healthScore,
            Integer userScore,
            boolean favorite
    ) {
        Product product = new Product(
                BARCODE, name, "Test Foods", "12 oz", "", "", "Box", "Test",
                "1 serving", "b", "2", "b", insight, healthScore, userScore
        );
        product.isFavorite = favorite;
        return product;
    }

    private ProductWithDetails details(Product product, String ingredientText) {
        ProductWithDetails details = new ProductWithDetails();
        details.product = product;
        details.ingredients = Collections.singletonList(new Ingredient(BARCODE, ingredientText, 0));
        return details;
    }
}
