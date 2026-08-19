package com.ciblorenzo.whatsonmyfood.retail;

import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarketplaceNavigationContractTest {

    @Test
    public void completeProductCanOpenAlternatives() {
        assertTrue(MarketplaceNavigation.isAvailable(product("012345678905", "Oat cereal")));
    }

    @Test
    public void missingProductIdentityCannotOpenAlternatives() {
        assertFalse(MarketplaceNavigation.isAvailable(null));
        assertFalse(MarketplaceNavigation.isAvailable(product("", "Oat cereal")));
        assertFalse(MarketplaceNavigation.isAvailable(product("012345678905", "Unknown product")));
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
