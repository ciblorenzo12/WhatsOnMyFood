package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** M7-10 regression coverage for representative scan and pantry checks. */
public class FoodRecallScannedSavedProductTest {

    @Test
    public void scannedProductWithExactActiveUpcProducesConfirmedMatch() throws Exception {
        Product scannedProduct = product(
                "030223075653",
                "Pineapple Mango Salsa Mild",
                "Taylor Farms",
                "10 oz"
        );
        FoodRecallRecord activeRecall = record(
                "H-1354-2026",
                "Pineapple Mango Salsa Mild. Net Wt: 10 oz. UPC: 030223075653",
                "Taylor Fresh Foods Inc",
                "Class I",
                "Potential contamination with Salmonella.",
                "TFIC211, 8/7/2026; TFTX211, 8/9/2026",
                "20260923",
                "Ongoing"
        );
        FoodRecallRepository repository = repositoryReturning(activeRecall);

        FoodRecallCheckResult result = repository.check(scannedProduct);

        assertEquals(FoodRecallState.CONFIRMED_MATCH, result.state);
        assertEquals(100, result.confidenceScore);
        assertEquals("H-1354-2026", result.record.recallNumber);
        assertTrue(result.record.officialUrl().startsWith(
                "https://api.fda.gov/food/enforcement.json"
        ));
        assertTrue(result.record.officialUrl().contains("H-1354-2026"));
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
