package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfileExperienceContractTest {

    @Test
    public void profileLayout_groupsSettingsInExpectedOrder() throws Exception {
        String layout = readProjectFile("src/main/res/layout/activity_profile.xml");

        assertOrdered(layout,
                "@string/account_information",
                "@+id/name_edit_text",
                "@+id/update_profile_button",
                "@string/preferences",
                "@+id/language_spinner_profile",
                "@+id/theme_toggle_group",
                "@string/account_and_privacy",
                "@+id/change_password_button",
                "@+id/privacy_policy_button",
                "@+id/clear_cached_data_button",
                "@string/session",
                "@+id/logout_button",
                "@+id/delete_account_button"
        );
        assertTrue(layout.contains("android:contentDescription=\"@string/change_profile_photo\""));
        assertTrue(layout.contains("app:singleSelection=\"true\""));
        assertTrue(layout.contains("app:selectionRequired=\"true\""));
    }

    @Test
    public void lightAndDarkResources_shareTheSameDayNightTheme() throws Exception {
        String lightTheme = readProjectFile("src/main/res/values/themes.xml");
        String darkTheme = readProjectFile("src/main/res/values-night/themes.xml");
        String darkBackground = readProjectFile("src/main/res/drawable-night/glass_activity_background.xml");

        assertTrue(lightTheme.contains("Theme.Material3.DayNight.NoActionBar"));
        assertTrue(darkTheme.contains("Theme.Material3.DayNight.NoActionBar"));
        assertTrue(darkTheme.contains("android:windowLightStatusBar\">false"));
        assertTrue(darkBackground.contains("<gradient"));
    }

    @Test
    public void dailyTipsAreLocalizedAndRemainPairedWithSources() throws Exception {
        String mainActivity = readProjectFile(
                "src/main/java/com/ciblorenzo/whatsonmyfood/MainActivity.java"
        );
        String english = readProjectFile("src/main/res/values/strings.xml");
        String spanish = readProjectFile("src/main/res/values-es/strings.xml");
        String french = readProjectFile("src/main/res/values-fr/strings.xml");

        assertTrue(mainActivity.contains("R.array.daily_health_tips"));
        assertTrue(mainActivity.contains("R.array.daily_health_tip_sources"));
        assertFalse(mainActivity.contains("new HealthTip("));
        assertEquals(countItems(english, "daily_health_tips"),
                countItems(english, "daily_health_tip_sources"));
        assertEquals(countItems(spanish, "daily_health_tips"),
                countItems(spanish, "daily_health_tip_sources"));
        assertEquals(countItems(french, "daily_health_tips"),
                countItems(french, "daily_health_tip_sources"));
        assertTrue(spanish.contains("contraseña"));
        assertTrue(readProjectFile(
                "src/main/java/com/ciblorenzo/whatsonmyfood/LanguageManager.java"
        ).contains("Español"));
    }

    @Test
    public void languageAndThemeSelectionsApplyImmediately() throws Exception {
        String profileActivity = readProjectFile(
                "src/main/java/com/ciblorenzo/whatsonmyfood/ProfileActivity.java"
        );
        String baseActivity = readProjectFile(
                "src/main/java/com/ciblorenzo/whatsonmyfood/BaseActivity.java"
        );

        assertTrue(profileActivity.contains("LanguageManager.setLanguageCode"));
        assertTrue(profileActivity.contains("recreate();"));
        assertTrue(profileActivity.contains("ThemeManager.setDarkMode"));
        assertTrue(baseActivity.contains("ThemeManager.applySavedMode"));
    }

    private static int countItems(String xml, String arrayName) {
        int start = xml.indexOf("<string-array name=\"" + arrayName + "\">");
        int end = xml.indexOf("</string-array>", start);
        assertTrue("Missing string array: " + arrayName, start >= 0 && end > start);
        String arrayXml = xml.substring(start, end);
        int count = 0;
        int offset = 0;
        while ((offset = arrayXml.indexOf("<item>", offset)) >= 0) {
            count++;
            offset += 6;
        }
        return count;
    }

    private static void assertOrdered(String source, String... values) {
        int previous = -1;
        for (String value : values) {
            int current = source.indexOf(value);
            assertTrue("Missing layout marker: " + value, current >= 0);
            assertTrue("Layout marker is out of order: " + value, current > previous);
            previous = current;
        }
    }

    private static String readProjectFile(String relativePath) throws IOException {
        Path modulePath = Path.of(relativePath);
        Path repositoryPath = Path.of("app").resolve(relativePath);
        Path selected = Files.exists(modulePath) ? modulePath : repositoryPath;
        return new String(Files.readAllBytes(selected), StandardCharsets.UTF_8);
    }
}
