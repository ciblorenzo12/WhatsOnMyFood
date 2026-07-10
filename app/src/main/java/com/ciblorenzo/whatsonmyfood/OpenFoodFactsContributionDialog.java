package com.ciblorenzo.whatsonmyfood;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public final class OpenFoodFactsContributionDialog {
    private static final String PREFS_NAME = "open_food_facts_contribution";
    private static final String PREF_USERNAME = "username";
    private static final String ACCOUNT_URL = "https://world.openfoodfacts.org/cgi/user.pl";

    private OpenFoodFactsContributionDialog() {
    }

    public interface Callback {
        void onSubmitted(String verifiedIngredients);
    }

    public static void show(
            Activity activity,
            String barcode,
            List<String> suggestedIngredients,
            Callback callback
    ) {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_contribute_ingredients, null);
        TextInputLayout ingredientsLayout = content.findViewById(R.id.off_ingredients_layout);
        TextInputLayout englishIngredientsLayout = content.findViewById(R.id.off_english_ingredients_layout);
        TextInputLayout usernameLayout = content.findViewById(R.id.off_username_layout);
        TextInputLayout passwordLayout = content.findViewById(R.id.off_password_layout);
        TextInputEditText ingredientsInput = content.findViewById(R.id.off_ingredients_edit_text);
        TextInputEditText englishIngredientsInput = content.findViewById(R.id.off_english_ingredients_edit_text);
        TextInputEditText usernameInput = content.findViewById(R.id.off_username_edit_text);
        TextInputEditText passwordInput = content.findViewById(R.id.off_password_edit_text);
        MaterialCheckBox confirmation = content.findViewById(R.id.off_label_confirmation);
        View progress = content.findViewById(R.id.off_submission_progress);
        View createAccount = content.findViewById(R.id.off_create_account_link);
        Button translateButton = content.findViewById(R.id.off_translate_button);
        TextView translationStatus = content.findViewById(R.id.off_translation_status);
        String[] detectedLanguage = {"und"};

        SharedPreferences preferences = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        ingredientsInput.setText(OpenFoodFactsContributionValidator.joinSuggestedIngredients(suggestedIngredients));
        usernameInput.setText(preferences.getString(PREF_USERNAME, ""));
        createAccount.setOnClickListener(v -> activity.startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(ACCOUNT_URL))
        ));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.off_contribute_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.off_submit_update, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            translateButton.setOnClickListener(v -> translateDraft(
                    activity,
                    ingredientsInput,
                    englishIngredientsInput,
                    translationStatus,
                    translateButton,
                    submitButton,
                    detectedLanguage
            ));
            translateDraft(
                    activity,
                    ingredientsInput,
                    englishIngredientsInput,
                    translationStatus,
                    translateButton,
                    submitButton,
                    detectedLanguage
            );
            submitButton.setOnClickListener(v -> {
                clearErrors(ingredientsLayout, englishIngredientsLayout, usernameLayout, passwordLayout);
                String originalIngredients = textOf(ingredientsInput);
                String englishIngredients = textOf(englishIngredientsInput);
                String username = textOf(usernameInput);
                String password = textOf(passwordInput);
                OpenFoodFactsContributionValidator.ValidationResult validation =
                        OpenFoodFactsContributionValidator.validate(
                                barcode,
                                originalIngredients,
                                englishIngredients,
                                username,
                                password,
                                confirmation.isChecked()
                        );
                if (!validation.valid) {
                    showValidationError(
                            activity,
                            validation.field,
                            ingredientsLayout,
                            englishIngredientsLayout,
                            usernameLayout,
                            passwordLayout
                    );
                    return;
                }

                setSubmitting(dialog, content, progress, true);
                new Thread(() -> submit(
                        activity,
                        dialog,
                        barcode.trim(),
                        originalIngredients,
                        englishIngredients,
                        detectedLanguage[0],
                        username,
                        password,
                        callback,
                        content,
                        progress
                )).start();
            });
        });
        dialog.show();
    }

    private static void submit(
            Activity activity,
            AlertDialog dialog,
            String barcode,
            String originalIngredients,
            String englishIngredients,
            String sourceLanguage,
            String username,
            String password,
            Callback callback,
            View content,
            View progress
    ) {
        try {
            OpenFoodFactsApiClient client = new OpenFoodFactsApiClient(
                    activity.getCacheDir(),
                    LanguageManager.getLanguageCode(activity)
            );
            OpenFoodFactsApiClient.SubmissionResult result = client.updateIngredients(
                    barcode,
                    originalIngredients,
                    englishIngredients,
                    sourceLanguage,
                    username,
                    password
            );
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (result.successful) {
                    activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                            .edit()
                            .putString(PREF_USERNAME, username)
                            .apply();
                    Toast.makeText(activity, R.string.off_update_submitted, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    callback.onSubmitted(englishIngredients);
                } else {
                    setSubmitting(dialog, content, progress, false);
                    Toast.makeText(activity, result.message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception error) {
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                setSubmitting(dialog, content, progress, false);
                Toast.makeText(activity, R.string.off_update_failed, Toast.LENGTH_LONG).show();
            });
        }
    }

    private static void setSubmitting(AlertDialog dialog, View content, View progress, boolean submitting) {
        progress.setVisibility(submitting ? View.VISIBLE : View.GONE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!submitting);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!submitting);
        content.findViewById(R.id.off_ingredients_edit_text).setEnabled(!submitting);
        content.findViewById(R.id.off_english_ingredients_edit_text).setEnabled(!submitting);
        content.findViewById(R.id.off_translate_button).setEnabled(!submitting);
        content.findViewById(R.id.off_username_edit_text).setEnabled(!submitting);
        content.findViewById(R.id.off_password_edit_text).setEnabled(!submitting);
        content.findViewById(R.id.off_label_confirmation).setEnabled(!submitting);
    }

    private static void clearErrors(TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) layout.setError(null);
    }

    private static void showValidationError(
            Activity activity,
            OpenFoodFactsContributionValidator.Field field,
            TextInputLayout ingredientsLayout,
            TextInputLayout englishIngredientsLayout,
            TextInputLayout usernameLayout,
            TextInputLayout passwordLayout
    ) {
        switch (field) {
            case ORIGINAL_INGREDIENTS:
                ingredientsLayout.setError(activity.getString(R.string.off_ingredients_required));
                break;
            case INGREDIENTS:
                englishIngredientsLayout.setError(activity.getString(R.string.off_ingredients_required));
                break;
            case USER_ID:
                usernameLayout.setError(activity.getString(R.string.off_username_required));
                break;
            case PASSWORD:
                passwordLayout.setError(activity.getString(R.string.off_password_required));
                break;
            case CONFIRMATION:
                Toast.makeText(activity, R.string.off_confirmation_required, Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(activity, R.string.off_invalid_barcode, Toast.LENGTH_LONG).show();
        }
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static void translateDraft(
            Activity activity,
            TextInputEditText originalInput,
            TextInputEditText englishInput,
            TextView status,
            Button translateButton,
            Button submitButton,
            String[] detectedLanguage
    ) {
        String original = textOf(originalInput);
        if (original.isEmpty()) {
            status.setText(R.string.off_ingredients_required);
            return;
        }
        status.setText(R.string.off_detecting_language);
        translateButton.setEnabled(false);
        submitButton.setEnabled(false);
        englishInput.setEnabled(false);
        MlKitIngredientTranslator.translateToEnglish(original, new MlKitIngredientTranslator.Callback() {
            @Override
            public void onSuccess(MlKitIngredientTranslator.TranslationResult result) {
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    detectedLanguage[0] = result.sourceLanguage;
                    if (!result.englishText.isEmpty()) {
                        englishInput.setText(result.englishText);
                    }
                    if (result.translated) {
                        status.setText(activity.getString(
                                R.string.off_translation_ready,
                                result.sourceLanguage
                        ));
                    } else if ("en".equals(result.sourceLanguage)) {
                        status.setText(R.string.off_already_english);
                    } else {
                        status.setText(R.string.off_translation_manual);
                    }
                    translateButton.setEnabled(true);
                    submitButton.setEnabled(true);
                    englishInput.setEnabled(true);
                });
            }

            @Override
            public void onError(Exception error) {
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    detectedLanguage[0] = "und";
                    status.setText(R.string.off_translation_manual);
                    translateButton.setEnabled(true);
                    submitButton.setEnabled(true);
                    englishInput.setEnabled(true);
                });
            }
        });
    }
}
