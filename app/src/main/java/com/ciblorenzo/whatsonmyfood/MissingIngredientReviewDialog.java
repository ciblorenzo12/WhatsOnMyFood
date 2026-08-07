package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientReviewGate;

/** Collects missing ingredients before a product is sent to Bitwise for review. */
public final class MissingIngredientReviewDialog {

    public interface Listener {
        void onIngredientsEntered(String ingredients);

        void onIngredientScanRequested();
    }

    private MissingIngredientReviewDialog() {
    }

    public static AlertDialog show(Context context, Listener listener) {
        int padding = Math.round(24 * context.getResources().getDisplayMetrics().density);
        int spacing = Math.round(12 * context.getResources().getDisplayMetrics().density);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, spacing, padding, 0);

        TextView message = new TextView(context);
        message.setText(R.string.ingredients_required_message);
        content.addView(message, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        EditText input = new EditText(context);
        input.setHint(R.string.ingredients_manual_hint);
        input.setMinLines(3);
        input.setMaxLines(7);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.topMargin = spacing;
        content.addView(input, inputParams);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.ingredients_required_title)
                .setView(content)
                .setCancelable(false)
                .setPositiveButton(R.string.analyze_ingredients, null)
                .setNegativeButton(R.string.use_ingredients_mode, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String entered = input.getText() == null ? "" : input.getText().toString().trim();
                if (IngredientReviewGate.parseManualIngredients(entered).isEmpty()) {
                    input.setError(context.getString(R.string.ingredients_manual_error));
                    input.requestFocus();
                    return;
                }
                dialog.dismiss();
                listener.onIngredientsEntered(entered);
            });
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
                dialog.dismiss();
                listener.onIngredientScanRequested();
            });
        });
        dialog.show();
        return dialog;
    }
}
