package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class FoodRecallMatcherTest {
    private final FoodRecallMatcher matcher = new FoodRecallMatcher();

    @Test
    public void exactUpcConfirmsTheRecallEvenWhenFormattedWithSpaces() {
        Product product = product("100001000297", "Spicy Breakfast Burrito", "Fresh Ready Foods", "8.2 oz");
        FoodRecallRecord record = record(
                "Spicy Breakfast Burrito, 8.2 oz. UPC 1 00001 00029 7",
                "Fresh & Ready Foods", "UPC 1 00001 00029 7", "Ongoing"
        );

        FoodRecallCheckResult result = matcher.match(product, dataset(record));

        assertEquals(FoodRecallState.CONFIRMED_MATCH, result.state);
        assertEquals(100, result.confidenceScore);
        assertSame(record, result.record);
    }

    @Test
    public void strongNameAndBrandEvidenceConfirmsWithoutAListedUpc() {
        Product product = product("999999999999", "Cookie Dough Ice Cream", "Straus Family Creamery", "1 pint");
        FoodRecallRecord record = record(
                "Straus Family Creamery Cookie Dough Ice Cream, one pint",
                "Straus Family Creamery", "Lot 26A", "Ongoing"
        );

        FoodRecallCheckResult result = matcher.match(product, dataset(record));

        assertEquals(FoodRecallState.CONFIRMED_MATCH, result.state);
        assertSame(record, result.record);
    }

    @Test
    public void partialProductIdentityIsOnlyPossible() {
        Product product = product("999999999999", "Chocolate Cookie Dough Ice Cream", "Unknown Brand", "1 pint");
        FoodRecallRecord record = record(
                "Cookie Dough Ice Cream, one pint",
                "Different Creamery", "Lot 26A", "Ongoing"
        );

        assertEquals(FoodRecallState.POSSIBLE_MATCH, matcher.match(product, dataset(record)).state);
    }

    @Test
    public void unrelatedRecordReturnsNoKnownMatch() {
        Product product = product("999999999999", "Whole Grain Oat Cereal", "Morning Mill", "12 oz");
        FoodRecallRecord record = record(
                "Vanilla ice cream, one pint", "Different Creamery", "Lot 26A", "Ongoing"
        );

        FoodRecallCheckResult result = matcher.match(product, dataset(record));

        assertEquals(FoodRecallState.NO_KNOWN_MATCH, result.state);
        assertNull(result.record);
    }

    @Test
    public void terminatedRecordIsNotPresentedAsActive() {
        Product product = product("100001000297", "Spicy Breakfast Burrito", "Fresh Ready Foods", "8.2 oz");
        FoodRecallRecord record = record(
                "Spicy Breakfast Burrito UPC 1 00001 00029 7",
                "Fresh Ready Foods", "UPC 1 00001 00029 7", "Terminated"
        );

        assertEquals(FoodRecallState.NO_KNOWN_MATCH, matcher.match(product, dataset(record)).state);
    }

    @Test
    public void highestConfidenceActiveRecordWins() {
        Product product = product("100001000297", "Spicy Breakfast Burrito", "Fresh Ready Foods", "8.2 oz");
        FoodRecallRecord possible = record(
                "Spicy Breakfast Burrito", "Other Foods", "Lot 9", "Ongoing"
        );
        FoodRecallRecord confirmed = record(
                "Spicy Breakfast Burrito UPC 1 00001 00029 7",
                "Fresh Ready Foods", "UPC 1 00001 00029 7", "Ongoing"
        );

        FoodRecallCheckResult result = matcher.match(
                product,
                new FoodRecallDataset(Arrays.asList(possible, confirmed), "2026-08-19")
        );

        assertSame(confirmed, result.record);
    }

    private static FoodRecallDataset dataset(FoodRecallRecord record) {
        return new FoodRecallDataset(Collections.singletonList(record), "2026-08-19");
    }

    private static FoodRecallRecord record(String description, String firm, String codes, String status) {
        return new FoodRecallRecord(
                "F-0001-2026", description, firm, "Class I", "Undeclared allergen", codes,
                "20260819", status
        );
    }

    private static Product product(String barcode, String name, String brand, String quantity) {
        return new Product(
                barcode, name, brand, quantity, "", "", "", "", "", "", "", ""
        );
    }
}
