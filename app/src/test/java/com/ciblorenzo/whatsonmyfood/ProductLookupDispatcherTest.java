package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProductLookupDispatcherTest {

    @Test
    public void passesTheScannedUpcUnchangedToRepositoryBoundary() {
        BarcodeScanGate gate = new BarcodeScanGate();
        RecordingRepository repository = new RecordingRepository();
        RecordingCallback callback = new RecordingCallback();

        String scannedUpc = gate.tryAcquire("028400090896");
        ProductLookupDispatcher.dispatch(scannedUpc, repository, callback);

        assertEquals("028400090896", repository.lastBarcode);
        assertEquals(1, repository.callCount);
        assertNull(callback.error);
    }

    @Test
    public void invalidBarcodeNeverReachesRepository() {
        RecordingRepository repository = new RecordingRepository();
        RecordingCallback callback = new RecordingCallback();

        ProductLookupDispatcher.dispatch("028400090897", repository, callback);

        assertEquals(0, repository.callCount);
        assertNotNull(callback.error);
    }

    private static final class RecordingRepository implements ProductLookupGateway {
        String lastBarcode;
        int callCount;

        @Override
        public void getProductByBarcode(
                String barcode,
                ProductRepository.RepositoryCallback<ProductRepository.ProductResult> callback
        ) {
            lastBarcode = barcode;
            callCount++;
        }
    }

    private static final class RecordingCallback
            implements ProductRepository.RepositoryCallback<ProductRepository.ProductResult> {
        Exception error;

        @Override
        public void onComplete(ProductRepository.ProductResult result) {
        }

        @Override
        public void onError(Exception e) {
            error = e;
        }
    }
}
