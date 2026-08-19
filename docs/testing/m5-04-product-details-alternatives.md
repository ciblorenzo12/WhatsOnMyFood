# M5-04 - Product details to alternatives test guide

## What this feature does

The product-detail screen includes a **Compare alternatives** button. It sends the product's barcode, name, brand, and category to the marketplace, so the shopper does not have to scan the item again. The normal Android back path is preserved, so returning from the marketplace shows the same product details.

If the product is missing a usable barcode or name, the button is disabled and the screen explains why. It does not open an empty or misleading marketplace screen.

## Automated test

Open PowerShell or the Android Studio **Terminal** in the repository folder and run:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest
.\app\gradlew.bat -p .\app connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.MarketplaceNavigationFlowTest"
```

Expected result: the build ends with `BUILD SUCCESSFUL`.

The navigation tests confirm that:

- a complete product can open the marketplace;
- a missing or placeholder product identity cannot open it;
- barcode `012345678905`, product name `Whole Grain Oat Cereal`, brand `Sample Market Foods`, and category `Breakfast cereals, whole grain foods` arrive unchanged;
- closing the marketplace returns to the same product; and
- an unsupported product keeps the action disabled and shows a clear explanation.

## Device test - supported product

1. Connect the Android phone or tablet and confirm that `adb devices` lists it as `device`.
2. Install the current debug build:

   ```powershell
   .\app\gradlew.bat -p .\app installDebug
   adb shell am force-stop com.ciblorenzo.whatsonmyfood
   adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity
   ```

3. Scroll to **Compare alternatives**.
4. Confirm the button is enabled and there is no unavailable warning.
5. Tap the button.
6. Confirm **Marketplace Comparison** opens without asking for another scan.
7. Press the toolbar back arrow or the Android back button.
8. Confirm the same `Whole Grain Oat Cereal` product detail is still visible.

Pass result: the product opens the alternatives workflow with its original context and returns safely to the product details.

## Device test - unsupported product

Run:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es comparison_scenario unsupported
```

Scroll to the marketplace action and confirm:

1. **Compare alternatives** is disabled.
2. The screen says the product is missing a barcode or product name.
3. Tapping the disabled button does not open another screen or crash the app.

Pass result: the app explains why comparison is unavailable instead of starting a broken workflow.

## Final pass condition

M5-04 passes when the automated test succeeds and both device scenarios match the expected results above.
