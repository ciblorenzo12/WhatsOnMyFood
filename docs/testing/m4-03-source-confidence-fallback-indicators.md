# M4-03 - Source, confidence, and fallback indicator test guide

## What this feature does

The product-detail screen now explains where its information came from and when the shopper should review or refresh it. It distinguishes a product database response, a recent saved result, a supporting source, recovered ingredients, an older cached result, an offline copy, and an unavailable Bitwise explanation.

The app does not display a confidence percentage or claim that information is verified. Instead, it gives a short context note that explains the limitation of the current source. Product details and deterministic rule findings remain visible when Bitwise is unavailable.

## Which console to use

Use the **Terminal** tab in Android Studio or Windows PowerShell.

1. Open the repository folder named `YourHealtyPantry`.
2. Move into the Android project only once:

   ```powershell
   cd app
   ```

3. If the prompt already ends in `YourHealtyPantry\app`, skip the `cd app` command.
4. Before a device test, confirm that Android Debug Bridge can see the phone, tablet, or emulator:

   ```powershell
   adb devices
   ```

The device must appear with the word `device`, not `offline` or `unauthorized`.

## Unit test - State mapping and careful wording

This test is fast and does not need an Android device, account, camera, or internet connection.

From the `app` folder, run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.SourceStatusResolverTest" --tests "com.ciblorenzo.whatsonmyfood.SourceStatusPresentationTest"
```

Expected console result:

```text
BUILD SUCCESSFUL
```

This proves that:

- direct database data and recent saved data have distinct source indicators;
- recovered ingredients request comparison with the package label;
- an older cached result asks the shopper to refresh;
- an offline result is not presented as current information;
- the offline state does not repeat a second stale indicator;
- an unavailable Bitwise explanation preserves the rule-based fallback;
- the presentation describes source limitations instead of claiming a statistical confidence level.

## Automated device test - Product-detail fragment rendering

Connect an Android device or start an emulator. From the `app` folder, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductSourceStatusFlowTest
```

Expected result:

```text
Finished 4 tests
BUILD SUCCESSFUL
```

The test opens the same `fragment_product_details.xml` layout used by the scan flow and verifies that:

- recovered ingredients show both their source and the package-review note;
- stale and offline states use different visible labels;
- the offline note says freshness cannot be confirmed;
- the AI-unavailable state shows friendly retry wording;
- product identity and deterministic findings remain visible without Bitwise.

## Manual test 1 - Database and recovered ingredients

Install the current debug build and open the repeatable preview:

```powershell
.\gradlew.bat installDebug
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es source_scenario recovered
```

Check the **Data source and freshness** panel near the product name:

1. Confirm the label shows **PRODUCT DATABASE • RECOVERED INGREDIENTS**.
2. Confirm the original source messages say the product was updated from a database and the ingredients were recovered.
3. Confirm the context note recommends comparing recovered ingredients with the package label.
4. Confirm the wording does not say “verified,” “guaranteed,” or “100% accurate.”
5. Confirm the panel does not cover the product name, result, or findings.

Pass result: the shopper can identify the data source and understands that recovered ingredients should be reviewed against the package.

## Manual test 2 - Older cached result

Run:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es source_scenario stale
```

Check that:

1. the visible label says **REFRESH RECOMMENDED**;
2. the source message says the information may be outdated;
3. the context note explains that the saved information may be older than the current package;
4. the product details remain readable.

Pass result: stale data is visible but is never presented as current.

## Manual test 3 - Offline saved copy

Run:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es source_scenario offline
```

Check that:

1. the label says **OFFLINE COPY**;
2. the source message says **Saved offline result** and **Information may be outdated**;
3. the context note says the app cannot confirm freshness while offline;
4. **REFRESH RECOMMENDED** is not shown as a duplicate indicator;
5. the existing product result remains available.

Pass result: the offline limitation is clear and the cached product stays usable.

## Manual test 4 - Bitwise unavailable

Run:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es source_scenario ai_unavailable
```

Check that:

1. the source labels include **AI UNAVAILABLE**;
2. the source message says that the rule-based product findings remain available;
3. the Bitwise card gives friendly retry wording;
4. the product name, score, warnings, positive findings, ingredients, and other details remain visible;
5. the app does not invent an AI explanation or hide the deterministic result.

Pass result: an unavailable AI service is clearly disclosed and does not block the shopping result.

## End-to-end scan check

After the repeatable preview tests, open the normal app and scan a product.

1. With internet access, confirm the panel identifies a database result or recent saved result.
2. Scan a product whose ingredients need recovery and confirm the recovery indicator appears after the ingredients are accepted.
3. Reopen a saved product while offline and confirm the offline or older-data wording appears.
4. If Bitwise times out or is unavailable, confirm the retry message appears while the deterministic rating and findings remain visible.
5. Restore the connection, tap the Bitwise message to retry, and confirm the **AI UNAVAILABLE** indicator disappears after a usable explanation is returned.

## Final quality command

From the `app` folder, run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The ticket passes when all automated tests succeed, every source state is understandable, and no wording overstates the certainty or completeness of product information.

## Screenshot evidence

- `docs/testing/evidence/m4-03-source-states.png` shows the product database and recovered-ingredient indicators with the package-review note.
- `docs/testing/evidence/m4-03-ai-fallback.png` shows the AI-unavailable indicator and friendly Bitwise fallback while deterministic findings remain visible.
