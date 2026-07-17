package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BarcodeScanGateTest {

    private static final String[] OPEN_FOOD_FACTS_GTINS = {
            "3017620422003", // Nutella
            "5449000000996", // Coca-Cola
            "7622210449283", // Prince chocolate biscuits
            "8000500310427", // Nutella Biscuits
            "5000159484695", // Twix ice cream
            "8076809513753", // Barilla Pesto Genovese
            "012000161155",  // LIFEWTR
            "028400090896",  // Doritos Nacho Cheese
            "016000275287",  // Cheerios
            "038000138416"   // Pringles Original
    };

    @Test
    public void validatesAllTenOpenFoodFactsFixtureCodes() {
        for (String gtin : OPEN_FOOD_FACTS_GTINS) {
            assertEquals(gtin, BarcodeScanGate.normalizeAndValidate(gtin));
        }
    }

    @Test
    public void acceptsGtin14UsedOnShippingAndMultipackLabels() {
        assertEquals("03017620422003", BarcodeScanGate.normalizeAndValidate("03017620422003"));
    }

    @Test
    public void allowsOnlyOneEventUntilReset() {
        BarcodeScanGate gate = new BarcodeScanGate();

        assertEquals("028400090896", gate.tryAcquire("0 28400-09089 6"));
        assertTrue(gate.isLocked());
        assertNull(gate.tryAcquire("028400090896"));
        assertNull(gate.tryAcquire("3017620422003"));

        gate.reset();
        assertFalse(gate.isLocked());
        assertEquals("3017620422003", gate.tryAcquire("3017620422003"));
    }

    @Test
    public void rejectsCorruptOrUnsupportedValuesWithoutLocking() {
        BarcodeScanGate gate = new BarcodeScanGate();

        assertNull(gate.tryAcquire("3017620422004"));
        assertNull(gate.tryAcquire("not-a-barcode"));
        assertNull(gate.tryAcquire("12345"));
        assertNull(gate.tryAcquire("03017620422004"));
        assertFalse(gate.isLocked());
    }
}
