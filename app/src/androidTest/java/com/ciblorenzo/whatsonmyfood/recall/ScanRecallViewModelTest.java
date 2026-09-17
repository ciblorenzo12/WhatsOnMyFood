package com.ciblorenzo.whatsonmyfood.recall;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ciblorenzo.whatsonmyfood.Product;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ScanRecallViewModelTest {
    private Product product() {
        return new Product("012345678905", "Oat Cereal", "Sample Foods", "12 oz", "", "", "", "", "", "", "", "");
    }
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private ScanRecallViewModel start(FoodRecallDataSource source, CountDownLatch done) {
        ScanRecallViewModel model = new ScanRecallViewModel(new FoodRecallRepository(source, new FoodRecallMatcher()));
        main(() -> {
            model.state().observeForever(value -> { if (value.state != FoodRecallState.CHECKING) done.countDown(); });
            model.check(product());
        });
        return model;
    }
    @Test public void automaticallyChecksAndPresentsMatchOnlyOnceDespiteRebinding() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        ScanRecallViewModel model = start(p -> {
            calls.incrementAndGet();
            return new FoodRecallDataset(Collections.singletonList(new FoodRecallRecord("H-test",
                    "Oat Cereal UPC 012345678905", "Sample Foods", "Class I", "Undeclared milk",
                    "Lot 1", "20260901", "Ongoing")), "2026-09-01");
        }, done);
        try {
            assertTrue(done.await(5, TimeUnit.SECONDS));
            main(() -> {
                assertEquals(FoodRecallState.CONFIRMED_MATCH, model.state().getValue().state);
                assertTrue(model.consumeNotice());
                model.check(product());
                assertFalse(model.consumeNotice());
                assertTrue(model.state().getValue().checkedAt > 0);
            });
            assertEquals(1, calls.get());
        } finally { main(model::onCleared); }
    }
    @Test public void failedScanCheckIsNeverReportedAsNoMatch() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        ScanRecallViewModel model = start(p -> { throw new IOException("offline"); }, done);
        try {
            assertTrue(done.await(5, TimeUnit.SECONDS));
            main(() -> {
                assertEquals(FoodRecallState.ERROR, model.state().getValue().state);
                assertEquals(0, model.state().getValue().checkedAt);
                assertFalse(model.consumeNotice());
            });
        } finally { main(model::onCleared); }
    }
    @Test public void successfulNoMatchDoesNotRaiseRecallAlert() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        ScanRecallViewModel model = start(p -> new FoodRecallDataset(Collections.emptyList(), "2026-09-01"), done);
        try {
            assertTrue(done.await(5, TimeUnit.SECONDS));
            main(() -> {
                assertEquals(FoodRecallState.NO_KNOWN_MATCH, model.state().getValue().state);
                assertFalse(model.consumeNotice());
            });
        } finally { main(model::onCleared); }
    }
}
