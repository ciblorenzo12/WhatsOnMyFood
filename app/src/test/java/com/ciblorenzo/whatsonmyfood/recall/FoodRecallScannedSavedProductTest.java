package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** M7-10 regression coverage for representative scan and pantry checks. */
public class FoodRecallScannedSavedProductTest {

    @Test
    public void scannedProductWithExactActiveUpcProducesConfirmedMatch() throws Exception {
        Product scannedProduct = product(
                "721582132834",
                "Pillsbury Bread Rolls Hard Roll Dough",
                "Pillsbury",
                "2.25 oz"
        );
        FoodRecallRecord activeRecall = record(
                "H-1154-2026",
                "Pillsbury Bread Rolls, Hard Roll Dough. Package UPC 7 21582-13283 4.",
                "General Mills James Ford Bell Technical Center",
                "Class II",
                "Possible foreign material",
                "Package UPC 7 21582-13283 4; Better if Used by OCT 12 26",
                "20260819",
                "Ongoing"
        );
        FoodRecallRepository repository = repositoryReturning(activeRecall);

        FoodRecallCheckResult result = repository.check(scannedProduct);

        assertEquals(FoodRecallState.CONFIRMED_MATCH, result.state);
        assertEquals(100, result.confidenceScore);
        assertEquals("H-1154-2026", result.record.recallNumber);
    }

    @Test
    public void savedProductDoesNotPresentTerminatedCandidateAsActive() throws Exception {
        Product savedProduct = product(
                "051500255162",
                "Creamy Peanut Butter",
                "Jif",
                "16 oz"
        );
        FoodRecallRecord historicalRecall = record(
                "F-1126-2022",
                "JIF CREAMY PEANUT BUTTER UPC 0 51500 25516 2",
                "The JM Smucker Company LLC",
                "Class I",
                "Potential Salmonella contamination",
                "UPC 0 51500 25516 2",
                "20220523",
                "Terminated"
        );
        FoodRecallRepository repository = repositoryReturning(historicalRecall);

        FoodRecallCheckResult result = repository.check(savedProduct);

        assertEquals(FoodRecallState.NO_KNOWN_MATCH, result.state);
        assertNull(result.record);
    }

    private static FoodRecallRepository repositoryReturning(FoodRecallRecord record) {
        FoodRecallDataSource source = product -> new FoodRecallDataset(
                Collections.singletonList(record),
                "2026-08-19"
        );
        return new FoodRecallRepository(source, new FoodRecallMatcher());
    }

    private static FoodRecallRecord record(
            String number,
            String description,
            String firm,
            String classification,
            String reason,
            String codes,
            String reportDate,
            String status
    ) {
        return new FoodRecallRecord(
                number, description, firm, classification, reason, codes, reportDate, status
        );
    }

    private static Product product(String barcode, String name, String brand, String quantity) {
        return new Product(
                barcode, name, brand, quantity, "", "", "", "", "", "", "", ""
        );
    }
}
