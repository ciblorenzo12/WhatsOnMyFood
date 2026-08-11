package com.ciblorenzo.whatsonmyfood;

import android.view.View;
import android.widget.Button;

/** Keeps the save/remove controls synchronized with the persisted pantry membership state. */
public final class PantryActionViewBinder {

    public enum State {
        LOADING,
        AVAILABLE_TO_SAVE,
        SAVING,
        SAVED,
        REMOVING
    }

    private PantryActionViewBinder() {
    }

    public static void bind(Button addButton, Button removeButton, State state) {
        if (addButton == null || removeButton == null || state == null) return;

        addButton.setVisibility(View.GONE);
        removeButton.setVisibility(View.GONE);
        addButton.setEnabled(true);
        removeButton.setEnabled(true);
        addButton.setText(R.string.add_to_pantry);
        removeButton.setText(R.string.remove_from_pantry);

        switch (state) {
            case AVAILABLE_TO_SAVE:
                addButton.setVisibility(View.VISIBLE);
                break;
            case SAVING:
                addButton.setVisibility(View.VISIBLE);
                addButton.setEnabled(false);
                addButton.setText(R.string.pantry_saving);
                break;
            case SAVED:
                removeButton.setVisibility(View.VISIBLE);
                break;
            case REMOVING:
                removeButton.setVisibility(View.VISIBLE);
                removeButton.setEnabled(false);
                removeButton.setText(R.string.pantry_removing);
                break;
            case LOADING:
            default:
                break;
        }
    }
}
