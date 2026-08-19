# M5-05 - Marketplace workflow test guide

## What this test covers

This guide checks the complete marketplace path: opening alternatives from product details, identifying live and development data honestly, handling no results, explaining timeouts and backend failures, retrying, and returning to the original product without a crash.

## 1. Run the automated checks

Open PowerShell or the Android Studio **Terminal** in the repository folder and run:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest lintDebug assembleDebug
.\app\gradlew.bat -p .\app connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.MarketplaceNavigationFlowTest,com.ciblorenzo.whatsonmyfood.MarketplaceStateDisplayFlowTest"
Push-Location backend\retailer
npm test
Pop-Location
```

Expected result:

- Gradle reports `BUILD SUCCESSFUL`;
- the backend test run reports no failed tests;
- the navigation tests preserve barcode, name, brand, and category;
- the provider tests distinguish live, mixed, mock, and empty results;
- timeout and other backend failures resolve to different interface states; and
- incomplete retailer records are converted to safe display text instead of causing a crash.

## 2. Install the debug build

```powershell
.\app\gradlew.bat -p .\app installDebug
adb devices
```

Confirm the connected phone or tablet is listed with the word `device`.

## 3. Test navigation first

Follow both the supported and unsupported device tests in [M5-04](m5-04-product-details-alternatives.md). Do not continue until the supported product opens the marketplace and the unsupported product explains why it cannot.

## 4. Test every provider state

These preview commands are debug-only. They give every tester the same screen without pretending that sample data is live.

### Live-provider display

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state live
```

Confirm the badge says `LIVE RETAILER DATA`, an alternative card says `LIVE PROVIDER`, and the back arrow remains available.

For a real live-provider verification, configure the protected backend with a supported retailer provider, open a real product from product details, and confirm the backend response has `resultMode` equal to `live` or `mixed`. If no live retailer is configured, record this check as **not configured**; do not report development data as a live result.

### Development mock display

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state mock
```

Confirm the badge and card say `DEVELOPMENT SAMPLE`. The message must explain that the results are simulated and are not current retailer availability.

### No alternatives

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state empty
```

Confirm the title says `No alternatives found.`, **Try again** is visible, and the screen remains usable.

### Timeout

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state timeout
```

Confirm the badge says `REQUEST TIMED OUT`, the message suggests checking the connection, and **Try again** is visible.

### Backend error

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state error
```

Confirm the badge says `SERVICE UNAVAILABLE`, the message explains that the retailer service could not respond, and **Try again** is visible.

For each state, press the back arrow and confirm the app closes the marketplace normally instead of freezing or crashing.

## 5. Test recovery with a real request

1. Stop the local retailer backend or temporarily point the ignored `RETAILER_BACKEND_BASE_URL` setting to an unavailable test address.
2. Rebuild, install, and open alternatives for a supported product.
3. Confirm the unavailable or timeout message appears.
4. Restore the correct backend address and start the backend.
5. Rebuild if the address changed.
6. Tap **Try again**.
7. Confirm the loading message appears and then changes to live, mock, or no results.

Pass result: retry starts a new request and replaces the failure message with the current response state.

## 6. Record defects and retest fixes

For each problem, record the product, state, steps, expected result, actual result, and the commit containing the fix. After the fix, repeat the exact same steps and record **Passed on retest**. If no defect is found, write **No defects found in this run** rather than leaving the result unclear.

## Final pass condition

M5-05 passes when navigation, configured live behavior, mock behavior, empty results, timeout, backend error, retry, incomplete data, and return navigation are documented and tested without crashes or misleading labels.

The latest recorded run is in [M5-05 marketplace test results](m5-05-marketplace-test-results.md).
