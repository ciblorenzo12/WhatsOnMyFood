package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class FoodRecallRepositoryTest {

    @Test
    public void repositoryCombinesTrustedRetrievalWithLocalMatching() throws Exception {
        FoodRecallRecord record = new FoodRecallRecord(
                "F-0001-2026",
                "Sample Oat Cereal UPC 012345678905",
                "Sample Foods",
                "Class II",
                "Undeclared allergen",
                "UPC 012345678905",
                "20260819",
                "Ongoing"
        );
        FoodRecallDataSource source = product -> new FoodRecallDataset(
                Collections.singletonList(record),
                "2026-08-19"
        );
        FoodRecallRepository repository = new FoodRecallRepository(source, new FoodRecallMatcher());

        FoodRecallCheckResult result = repository.check(product());

        assertEquals(FoodRecallState.CONFIRMED_MATCH, result.state);
        assertEquals("2026-08-19", result.sourceUpdatedAt);
    }

    private static Product product() {
        return new Product(
                "012345678905", "Sample Oat Cereal", "Sample Foods", "12 oz", "", "", "",
                "", "", "", "", ""
        );
    }
}
