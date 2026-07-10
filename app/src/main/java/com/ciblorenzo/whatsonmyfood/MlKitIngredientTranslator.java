package com.ciblorenzo.whatsonmyfood;

import androidx.annotation.NonNull;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.Locale;

public final class MlKitIngredientTranslator {
    private MlKitIngredientTranslator() {
    }

    public interface Callback {
        void onSuccess(TranslationResult result);

        void onError(@NonNull Exception error);
    }

    public static void translateToEnglish(String text, Callback callback) {
        String original = text == null ? "" : text.trim();
        if (original.isEmpty()) {
            callback.onSuccess(new TranslationResult("und", original, "", false));
            return;
        }

        LanguageIdentifier identifier = LanguageIdentification.getClient();
        identifier.identifyLanguage(original)
                .addOnSuccessListener(languageTag -> {
                    identifier.close();
                    translateIdentifiedText(original, languageTag, callback);
                })
                .addOnFailureListener(error -> {
                    identifier.close();
                    callback.onError(error);
                });
    }

    private static void translateIdentifiedText(
            String original,
            String languageTag,
            Callback callback
    ) {
        String sourceLanguage = normalizeLanguageTag(languageTag);
        if (TranslateLanguage.ENGLISH.equals(sourceLanguage)) {
            callback.onSuccess(new TranslationResult(sourceLanguage, original, original, false));
            return;
        }
        if (sourceLanguage == null) {
            callback.onSuccess(new TranslationResult(languageTag, original, "", false));
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build();
        Translator translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(ignored -> translator.translate(original)
                        .addOnSuccessListener(english -> {
                            translator.close();
                            callback.onSuccess(new TranslationResult(
                                    sourceLanguage,
                                    original,
                                    english.trim(),
                                    true
                            ));
                        })
                        .addOnFailureListener(error -> {
                            translator.close();
                            callback.onError(error);
                        }))
                .addOnFailureListener(error -> {
                    translator.close();
                    callback.onError(error);
                });
    }

    static String normalizeLanguageTag(String languageTag) {
        if (languageTag == null || languageTag.trim().isEmpty() || languageTag.equals("und")) {
            return null;
        }
        String supportedLanguage = TranslateLanguage.fromLanguageTag(languageTag);
        if (supportedLanguage != null) return supportedLanguage;
        int separator = languageTag.indexOf('-');
        if (separator > 0) {
            return TranslateLanguage.fromLanguageTag(
                    languageTag.substring(0, separator).toLowerCase(Locale.US)
            );
        }
        return null;
    }

    public static final class TranslationResult {
        public final String sourceLanguage;
        public final String originalText;
        public final String englishText;
        public final boolean translated;

        TranslationResult(
                String sourceLanguage,
                String originalText,
                String englishText,
                boolean translated
        ) {
            this.sourceLanguage = sourceLanguage;
            this.originalText = originalText;
            this.englishText = englishText;
            this.translated = translated;
        }
    }
}
