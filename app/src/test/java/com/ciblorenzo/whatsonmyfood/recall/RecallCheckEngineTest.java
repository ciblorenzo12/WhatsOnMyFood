package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;
import com.google.gson.Gson;
import org.junit.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

public class RecallCheckEngineTest {
    @Test public void findsBarcodeWhenNoticeListsMultipleUpcs() {
        assertTrue(FoodRecallMatcher.containsBarcode("012345678905", "UPC 099999999999 012345678905 088888888888"));
        assertTrue(FoodRecallMatcher.containsBarcode("012345678905", "UPC 0012345678905"));
        assertFalse(FoodRecallMatcher.containsBarcode("012345678905", "UPC 012345678906"));
    }
    private final Product product = new Product("012345678905", "Oat Cereal", "Sample Foods", "12 oz", "", "", "", "", "", "", "", "");
    private static final long NOW = 100_000;
    private static FoodRecallRecord recall(String id) {
        return new FoodRecallRecord(id, "Sample Foods Oat Cereal UPC 012345678905", "Sample Foods",
                "Class I", "Undeclared allergen", "Lot 123", "20260901", "Ongoing");
    }
    private static class FakeStore implements RecallCheckEngine.Store {
        PantryRecallStatus state;
        boolean active = true;
        Set<String> alerts = new HashSet<>();
        public PantryRecallStatus load(String barcode) { return state == null ? null : new Gson().fromJson(new Gson().toJson(state), PantryRecallStatus.class); }
        public boolean saveIfCurrent(Product p, PantryRecallStatus next) { if (!active) return false; state = next; return true; }
        public boolean alreadyNotified(String barcode, String id) { return alerts.contains(barcode + id); }
        public void markNotified(String barcode, String id, long now) { alerts.add(barcode + id); }
    }
    private static class Source implements FoodRecallDataSource {
        List<FoodRecallRecord> records = new ArrayList<>();
        boolean failure;
        int calls;
        Runnable duringFetch = () -> {};
        public FoodRecallDataset search(Product p) throws IOException {
            calls++; duringFetch.run(); if (failure) throw new IOException("offline");
            return new FoodRecallDataset(records, "2026-09-01");
        }
    }
    private RecallCheckEngine engine(Source source, FakeStore store, RecallCheckEngine.Notifier notifier) {
        return new RecallCheckEngine("user", new FoodRecallRepository(source, new FoodRecallMatcher()), store, notifier);
    }
    @Test public void detectsAllMatchesAndAlertsOnceEvenAfterNoMatchAndRestart() throws Exception {
        Source source = new Source(); FakeStore store = new FakeStore(); List<String> notifications = new ArrayList<>();
        RecallCheckEngine.Notifier notifier = (p,r) -> { notifications.add(r.recallNumber); return true; };
        source.records = Arrays.asList(recall("A"), recall("B"));
        engine(source, store, notifier).check(product, false, NOW);
        engine(source, store, notifier).check(product, true, NOW + 1);
        assertEquals(Arrays.asList("A","B"), notifications);
        source.records = Collections.emptyList();
        engine(source, store, notifier).check(product, true, NOW + 2);
        assertEquals(FoodRecallState.NO_KNOWN_MATCH, store.state.displayState(product, NOW + 2));
        source.records = Arrays.asList(recall("A"), recall("C"));
        engine(source, store, notifier).check(product, true, NOW + 3);
        assertEquals(Arrays.asList("A","B","C"), notifications);
    }
    @Test public void failurePreservesMatchAndLastSuccessfulTime() throws Exception {
        Source source = new Source(); FakeStore store = new FakeStore(); source.records = Collections.singletonList(recall("A"));
        RecallCheckEngine engine = engine(source, store, (p,r) -> true);
        engine.check(product, false, NOW);
        source.failure = true;
        assertThrows(IOException.class, () -> engine.check(product, true, NOW + 10));
        assertTrue(store.state.failed); assertEquals(NOW, store.state.lastSuccessfulAt);
        assertEquals("A", store.state.result().record.recallNumber);
        assertEquals(FoodRecallState.CONFIRMED_MATCH, store.state.displayState(product, NOW + 10));
    }
    @Test public void failureAfterClearDoesNotDisplayNoMatchAndRetriesRecover() throws Exception {
        Source source = new Source(); FakeStore store = new FakeStore();
        RecallCheckEngine engine = engine(source, store, (p,r) -> true);
        engine.check(product, false, NOW);
        source.failure = true;
        assertThrows(IOException.class, () -> engine.check(product, true, NOW + 10));
        assertEquals(FoodRecallState.ERROR, store.state.displayState(product, NOW + 10));
        source.failure = false;
        engine.check(product, false, NOW + 20);
        assertFalse(store.state.failed); assertEquals(NOW + 20, store.state.lastSuccessfulAt);
    }
    @Test public void skipsFreshChecksButRefreshesChangedIdentityAndDailyAndManual() throws Exception {
        Source source = new Source(); FakeStore store = new FakeStore();
        RecallCheckEngine engine = engine(source, store, (p,r) -> true);
        engine.check(product, false, NOW); engine.check(product, false, NOW + 1);
        assertEquals(1, source.calls);
        product.brands = "New brand";
        assertEquals(FoodRecallState.STALE, store.state.displayState(product, NOW + 2));
        engine.check(product, false, NOW + 2); assertEquals(2, source.calls);
        engine.check(product, true, NOW + 3); assertEquals(3, source.calls);
        engine.check(product, false, NOW + 3 + PantryRecallStatus.RECHECK_MS); assertEquals(4, source.calls);
    }
    @Test public void deniedNotificationsRemainEligibleWithoutRepeatingNetworkRequest() throws Exception {
        Source source = new Source(); FakeStore store = new FakeStore(); source.records = Collections.singletonList(recall("A"));
        engine(source, store, (p,r) -> false).check(product, false, NOW);
        assertTrue(store.alerts.isEmpty());
        engine(source, store, (p,r) -> true).check(product, false, NOW + 1);
        assertEquals(1, source.calls); assertEquals(1, store.alerts.size());
    }
    @Test public void removedOrChangedProductCannotPublishAnInFlightResult() {
        Source source = new Source(); FakeStore store = new FakeStore(); source.records = Collections.singletonList(recall("A"));
        source.duringFetch = () -> store.active = false;
        assertThrows(IOException.class, () -> engine(source, store, (p,r) -> { fail("must not notify"); return true; }).check(product, false, NOW));
        assertNull(store.state); assertTrue(store.alerts.isEmpty());
    }
    @Test public void firstFailureIsUnknownAndOverdueClearIsStale() throws Exception {
        Source source = new Source(); FakeStore store = new FakeStore(); source.failure = true;
        RecallCheckEngine engine = engine(source, store, (p,r) -> true);
        assertThrows(IOException.class, () -> engine.check(product, false, NOW));
        assertEquals(0, store.state.lastSuccessfulAt); assertEquals(FoodRecallState.ERROR, store.state.displayState(product, NOW));
        source.failure = false; engine.check(product, false, NOW + 1);
        assertEquals(FoodRecallState.STALE, store.state.displayState(product, NOW + 1 + PantryRecallStatus.RECHECK_MS));
    }
}
