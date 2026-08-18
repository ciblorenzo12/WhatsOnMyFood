# M5-02 - Marketplace response-state test guide

## What this feature does

The marketplace always explains its current state. Live retailer results, development samples, no alternatives, request timeouts, and backend failures each have different wording. Empty, timeout, and error states include a **Try again** button. The toolbar and back arrow remain available in every state.

## Which console to use

Use the **Terminal** tab in Android Studio or Windows PowerShell. Start in the repository folder named `YourHealtyPantry`.

## Automated state tests

Run:

```powershell
cd app
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.retail.MarketplaceStateResolverTest" --tests "com.ciblorenzo.whatsonmyfood.retail.MarketplacePresentationTest" --tests "com.ciblorenzo.whatsonmyfood.retail.MarketplaceLayoutContractTest"
```

Expected result:

```text
BUILD SUCCESSFUL
```

These tests verify that:

- live and mixed provider responses show the live state;
- mock responses show the development-sample state;
- a response with no alternatives shows `No alternatives found.`;
- a socket timeout does not become a generic backend error;
- other backend failures show the unavailable state;
- incomplete retailer records cannot crash the presentation mapping; and
- the toolbar comes before the state content, so navigation is not covered by a loading or error overlay.

## Repeatable interface previews

The debug build accepts a test-only state extra. The extra is guarded by `BuildConfig.DEBUG` and is not active in release builds.

### Set up once

Connect an Android device or start an emulator. From `YourHealtyPantry\app`, run:

```powershell
.\gradlew.bat installDebug
adb devices
```

Confirm the device appears with the word `device`.

For every preview below, confirm the marketplace toolbar and back arrow stay visible. Press the back arrow after each preview and confirm navigation works.

### Preview 1 - Live provider

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state live
```

Expected result:

- badge: `LIVE RETAILER DATA`;
- title: `Alternatives are ready`;
- the sample card says `LIVE PROVIDER`; and
- no retry button is shown.

### Preview 2 - Development mock

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state mock
```

Expected result:

- badge: `DEVELOPMENT SAMPLE`;
- title: `Showing simulated results`;
- the message says the results are not current retailer availability; and
- the sample card also says `DEVELOPMENT SAMPLE`.

### Preview 3 - Empty alternatives

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state empty
```

Expected result:

- badge: `NO RESULTS`;
- title: `No alternatives found.`;
- the reason and next step are understandable; and
- **Try again** is visible.

### Preview 4 - Timeout

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state timeout
```

Expected result:

- badge: `REQUEST TIMED OUT`;
- the message recommends checking the connection;
- **Try again** is visible; and
- the state is not described as an empty search or a generic crash.

### Preview 5 - Backend error

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state error
```

Expected result:

- badge: `SERVICE UNAVAILABLE`;
- the message explains that the retailer service could not respond;
- **Try again** is visible; and
- the app remains open without a crash.

## Real retry test

This test confirms that **Try again** starts a new backend request rather than only changing the text.

1. Stop the local backend or temporarily set `RETAILER_BACKEND_BASE_URL` in the ignored `app/local.properties` file to an unavailable test URL.
2. Rebuild and install the debug app.
3. Open a product and then **Marketplace Comparison**.
4. Confirm the unavailable state appears.
5. Restore the correct backend URL and start the backend.
6. Rebuild and reinstall if the URL changed.
7. Open the same marketplace screen and tap **Try again**.
8. Confirm the loading state appears and then changes to live, mock, or empty.

Pass result: retry makes a new request and the correct response state replaces the error.

## Incomplete record check

The automated presentation test supplies an alternative with a missing brand, retailer hint, product URL, and image URL. It confirms safe fallback wording. For a manual backend check, return one test record with those fields omitted and keep `productName` present.

Expected result:

- the card uses `Brand not provided`, `Multiple retailers`, and other readable fallbacks;
- the card without a URL is not clickable; and
- scrolling, sorting, retry, and back navigation do not crash.

## Final quality command

From `YourHealtyPantry\app`, run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

M5-02 passes when all five previews match their expected wording, retry starts another request, incomplete records remain safe, navigation works in every state, and the final quality command succeeds.
