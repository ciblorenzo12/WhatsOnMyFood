# M4-02 - Complete product findings test guide

## What this feature does

The product-detail fragment now shows the complete deduplicated set of deterministic and AI-assisted findings. Warning, informational, and positive cards remain visible together, each card includes a useful explanation, and a saved Bitwise explanation appears when one is available. If findings cannot be produced, the shopper sees a clear reason instead of a blank section.

## Which console to use

Use the **Terminal** tab in Android Studio or Windows PowerShell.

1. Open the repository folder named `YourHealtyPantry`.
2. If the console already ends in `YourHealtyPantry\app`, do not run `cd app` again.
3. Confirm that a connected device appears before a device test:

   ```powershell
   adb devices
   ```

## Unit test - Findings rules and display states

This fast test does not need an emulator, account, or internet connection.

From the repository root, run:

```powershell
cd app
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.analysis.ProductFindingsDisplayTest" --tests "com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultDeduplicatorTest" --tests "com.ciblorenzo.whatsonmyfood.AiInsightCacheTest"
```

Expected console result:

```text
BUILD SUCCESSFUL
```

This proves that:

- warning, informational, and positive findings are all preserved;
- overlapping rule and AI findings become one card;
- the stronger duplicate is retained;
- an empty completed report uses the no-findings state;
- missing ingredients and analysis failures use different fallback states;
- a saved Bitwise explanation can be encoded and decoded safely.

## Automated device test - Fragment rendering

Connect an Android device or start an emulator. From the `app` folder, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductFindingsDisplayFlowTest
```

Expected result:

```text
Finished 3 tests
BUILD SUCCESSFUL
```

The automated test opens the same `fragment_product_details.xml` used by the scan flow and verifies:

- one positive, one warning, and one informational card are rendered;
- a duplicate input does not create a duplicate card;
- the Bitwise explanation remains visible;
- the no-findings message appears for a valid empty report;
- the analysis-unavailable message appears while the product name remains visible.

## Manual test 1 - Complete findings and explanation

Use the safe debug preview so the result is repeatable:

```powershell
.\gradlew.bat installDebug
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity
```

Check the following:

1. Open **At a glance** and find the Key findings list.
2. Confirm **Whole grain oats** has a green **GOOD** card.
3. Confirm **Added sugar** has an orange **WATCH** card.
4. Confirm **Allergen information** has a blue **INFO** card.
5. Confirm **Added sugar appears only once**, even though the preview intentionally sends it twice.
6. Tap each card and confirm a full explanation opens.
7. Confirm the Bitwise explanation appears below the findings and is not replaced by a duplicate result card.

Pass result: three different findings are visible, there are no repeated cards, and every card has a readable explanation.

## Manual test 2 - Valid product with no findings

Run:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es findings_scenario empty
```

Check that Key findings shows a message beginning with **No specific warnings or positive findings were identified**. Confirm the product name, source status, scores, explanation, ingredients, and other details remain available.

Pass result: the section is not blank and the rest of the product remains usable.

## Manual test 3 - Analysis unavailable

Run:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es findings_scenario unavailable
```

Check that Key findings says **Product findings could not be loaded** and suggests refreshing. Confirm the product name and available details remain visible.

Pass result: the failure has friendly wording and does not hide the product.

## Manual test 4 - Missing ingredients

Run:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es findings_scenario missing
```

Check that the message asks for the ingredient list and mentions Ingredients mode. The app must not present an empty findings area or invent a review without ingredients.

## End-to-end scan check

After the repeatable preview tests, open the normal app and scan a product that has several rule findings.

1. Compare the visible cards with the ingredients shown on the same screen.
2. Confirm warning, positive, and informational cards are all included when relevant.
3. Confirm the same rule/ingredient is not shown twice after Bitwise finishes.
4. Turn off the network after a Bitwise explanation has been saved, reopen the same product, and confirm the saved explanation remains visible.
5. Tap each finding to confirm the complete explanation and source are accessible when provided.

## Final quality command

From the `app` folder, run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The ticket passes when all automated tests succeed and the four manual states are understandable without a blank or duplicated findings section.

## Verified screenshot evidence

- `docs/testing/evidence/m4-02-complete-findings.png` shows the GOOD, WATCH, and INFO cards once each together with the Bitwise explanation.
- `docs/testing/evidence/m4-02-empty-state.png` shows the friendly no-findings state while the product result remains visible.
