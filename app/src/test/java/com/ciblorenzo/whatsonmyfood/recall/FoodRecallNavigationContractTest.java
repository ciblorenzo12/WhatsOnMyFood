package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FoodRecallNavigationContractTest {

    @Test
    public void identifiedProductCanOpenRecallFlow() {
        assertTrue(FoodRecallNavigation.isAvailable(product("012345678905", "Oat cereal")));
    }

    @Test
    public void missingIdentityCannotOpenRecallFlow() {
        assertFalse(FoodRecallNavigation.isAvailable(null));
        assertFalse(FoodRecallNavigation.isAvailable(product("", "Oat cereal")));
        assertFalse(FoodRecallNavigation.isAvailable(product("012345678905", "Unknown product")));
    }

    private static ProductWithDetails product(String barcode, String name) {
        ProductWithDetails details = new ProductWithDetails();
        details.product = new Product(
                barcode,
                name,
                "Sample brand",
                "12 oz",
                "",
                "",
                "Box",
                "Breakfast cereals",
                "1 cup",
                "b",
                "2",
                "b"
        );
        return details;
    }
}
