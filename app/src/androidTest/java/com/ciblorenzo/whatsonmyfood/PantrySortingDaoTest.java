package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class PantrySortingDaoTest {

    private AppDatabase database;
    private ProductDao dao;
    private static final String USER_ID = "m4-05-user";

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.productDao();

        save(product("m4-05-zed", "Zed Crackers", null));
        save(product("m4-05-banana", "banana cereal", 82));
        save(product("m4-05-apple", "Apple Oats", 82));
        save(product("m4-05-apricot", "Apple Oats", 82));
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void recentSortReturnsNewestPantryMembershipFirst() {
        assertBarcodes(
                dao.getPantryProducts(USER_ID),
                "m4-05-apricot", "m4-05-apple", "m4-05-banana", "m4-05-zed"
        );
    }

    @Test
    public void nameSortIsCaseInsensitiveAndStable() {
        assertBarcodes(
                dao.getPantryProductsByName(USER_ID),
                "m4-05-apple", "m4-05-apricot", "m4-05-banana", "m4-05-zed"
        );
    }

    @Test
    public void healthSortShowsScoresFirstAndUsesNameForTies() {
        assertBarcodes(
                dao.getPantryProductsByHealthScore(USER_ID),
                "m4-05-apple", "m4-05-apricot", "m4-05-banana", "m4-05-zed"
        );
    }

    private void save(Product product) {
        dao.insertProduct(product);
        dao.insertPantry(new Pantry(product.barcode, USER_ID));
    }

    private Product product(String barcode, String name, Integer healthScore) {
        return new Product(
                barcode, name, "Test Foods", "12 oz", "", "", "Box", "Test",
                "1 serving", "b", "2", "b", null, healthScore
        );
    }

    private void assertBarcodes(List<Product> products, String... expected) {
        List<String> actual = products.stream()
                .map(product -> product.barcode)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(expected), actual);
    }
}
