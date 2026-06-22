package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProductDaoTest {

    private AppDatabase db;
    private ProductDao productDao;
    private static final String TEST_USER_ID = "test-user-123";

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        productDao = db.productDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    // CORRECTED: The constructor call now has the correct number of arguments (67 doubles) to match the Nutriments entity.
    private Nutriments createDefaultNutriments(String barcode) {
        return new Nutriments(barcode, 
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, // 15
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, // 30
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, // 45
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, // 60
            null, null, null, null, null, null, null); // 67
    }

    @Test
    public void insertAndReadProductWithDetails() throws Exception {
        String barcode = "123456789";
        Product product = new Product(barcode, "Test Soda", "Brand", "12 oz", null, "organic", null, null, "100g", "A", "4", "B");
        Nutriments nutriments = createDefaultNutriments(barcode);
        nutriments.energy = 150.0;
        nutriments.sugars = 39.0;
        Ingredient ingredient = new Ingredient(barcode, "Carbonated water, High fructose corn syrup, Caramel color", 1);
        
        ProductWithDetails originalProductDetails = new ProductWithDetails();
        originalProductDetails.product = product;
        originalProductDetails.nutriments = nutriments;
        originalProductDetails.ingredients = Collections.singletonList(ingredient);

        productDao.insertProductWithDetails(originalProductDetails);
        ProductWithDetails retrievedProductDetails = productDao.getProductWithDetails(barcode);

        assertNotNull(retrievedProductDetails);
        assertEquals("Test Soda", retrievedProductDetails.product.productName);
        assertEquals(1, retrievedProductDetails.ingredients.size());
        assertEquals(150.0, retrievedProductDetails.nutriments.energy, 0.0);
    }
    
    @Test
    public void getProductWithDetails_forNonExistentBarcode_returnsNull() {
        ProductWithDetails retrievedProductDetails = productDao.getProductWithDetails("non-existent-barcode");
        assertNull(retrievedProductDetails);

    }

    @Test
    public void insertAndReadCacheMeta_storesAndRetrievesTimestamp() {
        String barcode = "test-cache-barcode";
        long timestamp = System.currentTimeMillis();
        CacheMeta cacheMeta = new CacheMeta(barcode, timestamp);

        productDao.insertCacheMeta(cacheMeta);
        CacheMeta retrievedMeta = productDao.getCacheMeta(barcode);

        assertNotNull(retrievedMeta);
        assertEquals(barcode, retrievedMeta.barcode);
        assertEquals(timestamp, retrievedMeta.lastUpdated);
    }

    @Test
    public void insertAndDeletePantryItem_worksCorrectly() {
        // ARRANGE
        String barcode = "pantry-item-barcode";
        Product product = new Product(barcode, "Pantry Product", null, null, null, null, null, null, null, null, null, null);
        Pantry pantryItem = new Pantry(barcode, TEST_USER_ID);
        
        productDao.insertProduct(product);

        // ACT & ASSERT (Insert)
        productDao.insertPantry(pantryItem);
        Pantry retrievedItem = productDao.findPantryItemByBarcode(barcode, TEST_USER_ID);
        assertNotNull("Pantry item should exist after insertion", retrievedItem);

        // ACT & ASSERT (Delete)
        productDao.deletePantryProduct(barcode, TEST_USER_ID);
        Pantry retrievedItemAfterDelete = productDao.findPantryItemByBarcode(barcode, TEST_USER_ID);
        assertNull("Pantry item should be null after deletion", retrievedItemAfterDelete);
    }
}
