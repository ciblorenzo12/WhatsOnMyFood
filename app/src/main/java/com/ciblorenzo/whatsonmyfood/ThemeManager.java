package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {
    private static final String PREFS_NAME = "appearance_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ThemeManager() {
    }

    public static void applySavedMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(getNightMode(context));
    }

    public static int getNightMode(Context context) {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
    }

    public static boolean isDarkMode(Context context) {
        return getNightMode(context) == AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static void setDarkMode(Context context, boolean darkMode) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
