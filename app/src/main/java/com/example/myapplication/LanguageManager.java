package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LanguageManager {
    private static final String PREFS_NAME = "language_settings";
    private static final String KEY_LANGUAGE_CODE = "language_code";
    private static final String DEFAULT_LANGUAGE_CODE = "en";

    private LanguageManager() {
    }

    public static Context wrap(Context context) {
        String languageCode = getLanguageCode(context);
        Locale locale = Locale.forLanguageTag(languageCode);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        } else {
            configuration.setLocale(locale);
        }
        return context.createConfigurationContext(configuration);
    }

    public static void setLanguageCode(Context context, String languageCode) {
        getPrefs(context)
                .edit()
                .putString(KEY_LANGUAGE_CODE, normalizeLanguageCode(languageCode))
                .apply();
    }

    public static String getLanguageCode(Context context) {
        return normalizeLanguageCode(getPrefs(context).getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE_CODE));
    }

    public static String getLanguageName(Context context) {
        String code = getLanguageCode(context);
        for (LanguageItem item : getSupportedLanguages()) {
            if (item.getLanguageCode().equals(code)) {
                return item.getLanguageName();
            }
        }
        return "English";
    }

    public static int getLanguagePosition(Context context) {
        String code = getLanguageCode(context);
        List<LanguageItem> languages = getSupportedLanguages();
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).getLanguageCode().equals(code)) {
                return i;
            }
        }
        return 0;
    }

    public static List<LanguageItem> getSupportedLanguages() {
        List<LanguageItem> languageList = new ArrayList<>();
        languageList.add(new LanguageItem("English", "en", android.R.drawable.ic_dialog_map));
        languageList.add(new LanguageItem("Espanol", "es", android.R.drawable.ic_dialog_map));
        languageList.add(new LanguageItem("Francais", "fr", android.R.drawable.ic_dialog_map));
        return languageList;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return DEFAULT_LANGUAGE_CODE;
        }
        String normalized = languageCode.trim().toLowerCase(Locale.US);
        if (normalized.startsWith("es")) {
            return "es";
        }
        if (normalized.startsWith("fr")) {
            return "fr";
        }
        return DEFAULT_LANGUAGE_CODE;
    }
}
