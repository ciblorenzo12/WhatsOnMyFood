package com.ciblorenzo.whatsonmyfood.retail;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class MarketplaceLayoutContractTest {

    @Test
    public void statePanelProvidesRetryAndKeepsToolbarOutsideContentStates() throws Exception {
        String layout = readProjectFile("src/main/res/layout/activity_marketplace.xml");
        String strings = readProjectFile("src/main/res/values/strings.xml");
        String activity = readProjectFile("src/main/java/com/ciblorenzo/whatsonmyfood/retail/MarketplaceActivity.java");

        assertOrdered(layout,
                "@+id/marketplace_toolbar",
                "@+id/marketplace_state_container",
                "@+id/marketplace_state_badge",
                "@+id/marketplace_state_title",
                "@+id/marketplace_state_message",
                "@+id/marketplace_retry_button",
                "@+id/marketplace_recycler_view");
        assertTrue(strings.contains("No alternatives found."));
        assertTrue(strings.contains("DEVELOPMENT SAMPLE"));
        assertTrue(strings.contains("REQUEST TIMED OUT"));
        assertTrue(strings.contains("SERVICE UNAVAILABLE"));
        assertTrue("Preview states must remain debug-only",
                activity.contains("BuildConfig.DEBUG && debugState != null"));
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

    private static String readProjectFile(String relativePath) throws Exception {
        Path modulePath = Path.of(relativePath);
        Path repositoryPath = Path.of("app").resolve(relativePath);
        Path selected = Files.exists(modulePath) ? modulePath : repositoryPath;
        return new String(Files.readAllBytes(selected), StandardCharsets.UTF_8);
    }
}
