# M11-05 Recall Information and Interface Verification

Date: September 30, 2026

## Implemented behavior

- The scan-result and saved-pantry entry points continue to open the same recall activity and keep their entry context visible.
- Product name, brand, package size, and barcode appear before the recall result. Missing values use explicit labels rather than inferred data.
- Recall details follow this reading order: recalled product, recall status and classification, recall reason, affected codes/lots/dates, then FDA source metadata and dates.
- The automatic pantry check and manual **Check again** action remain available.
- Loading, possible match, confirmed match, no known match, stale, unavailable, and error presentations remain distinct.
- If a refresh fails or a saved result is stale, the current state is shown while prior successful evidence is clearly labeled. Prior recall matches remain visible for safety.
- The official action opens the exact FDA enforcement record when a match exists and the general FDA recall page otherwise.
- No-known-match copy explicitly states that the result is not a safety guarantee.
- English, Spanish, and French resources include the new labels and state explanations.
- The scan fixture uses UPC `030223075653`, which resolves in Open Food Facts to Taylor Farms Pineapple Mango Salsa Mild and exactly matches ongoing FDA recall `H-1354-2026`.

## Automated evidence

| Verification | Result |
| --- | --- |
| Focused recall unit tests (`testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.recall.*"`) | Passed |
| Full debug unit suite (`testDebugUnitTest`) | Passed: 291 tests, 0 failures, 0 errors, 0 skipped |
| Debug APK build (`assembleDebug`) | Passed |
| Android lint (`lintDebug`) | Passed |
| Recall Espresso source compilation (`compileDebugAndroidTestJavaWithJavac`) | Passed |
| Layout order, cautious no-match copy, package-size fallback, and prior-result labels | Covered by `FoodRecallLayoutContractTest` |
| Scan exact-active match and saved terminated-record behavior | Covered by `FoodRecallScannedSavedProductTest` |
| Official FDA record URL construction | Covered by `FoodRecallScannedSavedProductTest` |
| State actions and recovery-state guidance | Covered by `FoodRecallPresentationTest` |
| Automatic refresh, stale cache, failure retention, and manual forced refresh | Covered by `RecallCheckEngineTest` |

## Presentation-device evidence

No Android device or emulator was connected, and no local Android virtual device was configured during this run. The Espresso tests compile, but device execution, long-text visual review, navigation tapping, and screenshots are therefore pending. No screenshots were fabricated.

Run the device verification with:

```powershell
cd app
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.recall.FoodRecallScanSavedFlowTest
```

Capture these screenshots on the presentation device under `docs/testing/logs/m11-05/`:

1. Newly scanned product with a confirmed or possible match and long recall text.
2. Saved pantry product with no known match and the safety disclaimer visible.
3. Missing package/detail fields showing the explicit “not provided” labels.
4. Stale saved result with the previous-result notice.
5. Unavailable or failed refresh with its next action and retained prior evidence.
6. Official FDA notice opened from a matched record.

For each screen, confirm scrolling, text wrapping, back navigation, **Check again**, and the official-source action.
