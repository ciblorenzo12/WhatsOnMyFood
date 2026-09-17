package com.ciblorenzo.whatsonmyfood.recall;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ciblorenzo.whatsonmyfood.Product;
import com.google.gson.Gson;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One fresh request per scanned identity; retained across view recreation and AI updates. */
public class ScanRecallViewModel extends ViewModel {
    public static final class State {
        public final Product product;
        public final FoodRecallState state;
        public final FoodRecallCheckResult result;
        public final long checkedAt;
        State(Product product, FoodRecallState state, FoodRecallCheckResult result, long checkedAt) {
            this.product = product; this.state = state; this.result = result; this.checkedAt = checkedAt;
        }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final FoodRecallRepository repository;
    private String identity;
    private volatile int generation;
    private boolean noticeShown;

    public ScanRecallViewModel() { this(new FoodRecallRepository()); }
    ScanRecallViewModel(FoodRecallRepository repository) { this.repository = repository; }

    public LiveData<State> state() { return state; }

    public void check(Product product) {
        String next = PantryRecallStatus.identity(product);
        if (next.equals(identity)) return;
        identity = next;
        noticeShown = false;
        int request = ++generation;
        Product snapshot = new Gson().fromJson(new Gson().toJson(product), Product.class);
        state.setValue(new State(snapshot, FoodRecallState.CHECKING, null, 0));
        executor.execute(() -> {
            State completed;
            try {
                FoodRecallCheckResult result = repository.check(snapshot);
                completed = new State(snapshot, result.state, result, System.currentTimeMillis());
            } catch (Exception failure) {
                completed = new State(snapshot, FoodRecallState.ERROR, null, 0);
            }
            State value = completed;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (request == generation) state.setValue(value);
            });
        });
    }

    public boolean consumeNotice() {
        State value = state.getValue();
        if (noticeShown || value == null || !FoodRecallPresentation.requiresImmediateAttention(value.state)) return false;
        noticeShown = true;
        return true;
    }

    @Override protected void onCleared() { generation++; executor.shutdownNow(); }
}
