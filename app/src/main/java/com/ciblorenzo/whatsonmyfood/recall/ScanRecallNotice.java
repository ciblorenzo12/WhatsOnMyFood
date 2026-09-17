package com.ciblorenzo.whatsonmyfood.recall;

import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.R;

/** Scan warnings are in-app and do not require saving the product or notification permission. */
public final class ScanRecallNotice {
    private ScanRecallNotice() {}
    private static final class Binding implements Runnable {
        final String identity;
        final Runnable cleanup;
        Binding(String identity, Runnable cleanup) { this.identity = identity; this.cleanup = cleanup; }
        @Override public void run() { cleanup.run(); }
    }
    static boolean isBound(View root, ProductWithDetails product) {
        TextView status = root.findViewById(R.id.food_recall_saved_status);
        return status != null && status.getTag() instanceof Binding && product != null && product.product != null
                && ((Binding) status.getTag()).identity.equals(PantryRecallStatus.identity(product.product));
    }

    public static void bind(Fragment fragment, View root, ProductWithDetails product) {
        if (isBound(root, product)) return;
        TextView status = root.findViewById(R.id.food_recall_saved_status);
        if (status == null || !FoodRecallNavigation.isAvailable(product)) return;
        ScanRecallViewModel model = new ViewModelProvider(fragment).get(ScanRecallViewModel.class);
        LifecycleOwner owner = fragment.getViewLifecycleOwner();
        model.check(product.product);
        Observer<ScanRecallViewModel.State> observer = value -> {
            FoodRecallUiModel ui = FoodRecallPresentation.forState(value.state);
            status.setVisibility(View.VISIBLE);
            status.setTextColor(ContextCompat.getColor(fragment.requireContext(), ui.statusColor));
            PantryRecallStatus checked = new PantryRecallStatus();
            checked.lastSuccessfulAt = value.checkedAt;
            status.setText(fragment.getString(ui.titleText) + "\n" + fragment.getString(ui.messageText)
                    + (value.checkedAt > 0 ? "\n" + RecallStatusText.checked(fragment.requireContext(), checked) : ""));
            status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        };
        model.state().observe(owner, observer);
        // bindEntry removes the previous binding when product data is rendered again.
        DefaultLifecycleObserver alerts = new DefaultLifecycleObserver() {
            private AlertDialog dialog;
            private final Observer<ScanRecallViewModel.State> changes = value -> showNotice();
            private void showNotice() {
                if (!owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)
                        || !model.consumeNotice()) return;
                ScanRecallViewModel.State value = model.state().getValue();
                FoodRecallUiModel ui = FoodRecallPresentation.forState(value.state);
                ProductWithDetails details = new ProductWithDetails(); details.product = value.product;
                dialog = new AlertDialog.Builder(fragment.requireContext())
                        .setTitle(ui.titleText)
                        .setMessage(value.product.productName + "\n\n" + fragment.getString(ui.messageText)
                                + "\n\n" + value.result.record.reasonForRecall)
                        .setPositiveButton(R.string.food_recall_view_notice, (d, which) ->
                                fragment.startActivity(FoodRecallNavigation.createIntent(fragment.requireContext(),
                                        details, FoodRecallNavigation.EntryPoint.SCAN_RESULT)))
                        .setNegativeButton(android.R.string.ok, null).create();
                dialog.show();
            }
            @Override public void onCreate(LifecycleOwner ignored) { model.state().observe(owner, changes); }
            @Override public void onResume(LifecycleOwner ignored) { showNotice(); }
            @Override public void onDestroy(LifecycleOwner ignored) {
                if (dialog != null) dialog.dismiss();
                model.state().removeObserver(changes);
            }
        };
        owner.getLifecycle().addObserver(alerts);
        status.setTag(new Binding(PantryRecallStatus.identity(product.product), () -> {
            model.state().removeObserver(observer);
            alerts.onDestroy(owner);
            owner.getLifecycle().removeObserver(alerts);
        }));
    }
}
