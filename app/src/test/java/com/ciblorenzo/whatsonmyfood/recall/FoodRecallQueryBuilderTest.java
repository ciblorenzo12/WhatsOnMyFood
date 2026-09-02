package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FoodRecallQueryBuilderTest {

    @Test
    public void queryUsesNarrowProductAndBrandPhrases() {
        String query = FoodRecallQueryBuilder.build(product(
                "Straus Organic Cookie Dough Ice Cream",
                "Straus Family Creamery"
        ));

        assertEquals(
                "product_description:\"straus cookie dough\" OR product_description:\"straus family\"",
                query
        );
    }

    @Test
    public void queryRemovesPunctuationAndGenericWords() {
        String query = FoodRecallQueryBuilder.build(product("Bob's Original Food Product", ""));

        assertTrue(query.contains("bob"));
        assertFalse(query.contains("original"));
        assertFalse(query.contains("food"));
        assertFalse(query.contains("product\""));
    }

    private static Product product(String name, String brand) {
        return new Product(
                "012345678905", name, brand, "12 oz", "", "", "", "", "",
                "", "", ""
        );
    }
}
