# M4-04 - Pantry save and remove test guide

## What this feature does

Opening or scanning a product no longer saves it automatically. The product-detail screen now shows **Add to Pantry** when the product is not saved and **Remove from Pantry** when it is saved. Room uses the product barcode and user ID as one unique pantry key, so pressing save again cannot create a duplicate entry.

Saving retains the stored product details, health score, normalized ingredients, nutrition data, and saved Bitwise insight. Removing deletes only the pantry membership; it does not erase the product record while the detail screen is open. The pantry list refreshes after the shopper returns from a changed product.

## Which console to use

Use the **Terminal** tab in Android Studio or Windows PowerShell.

1. Open the repository folder named `YourHealtyPantry`.
2. Move into the Android project only once:

   ```powershell
   cd app
   ```

3. If the prompt already ends in `YourHealtyPantry\app`, skip `cd app`.
4. Before device tests, confirm the device or emulator is connected:

   ```powershell
   adb devices
   ```

The device must appear with the word `device`.

## Unit test - Save and remove outcomes

This fast test does not require a device, account, camera, or internet connection.

From the `app` folder, run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.PantryOperationResultTest"
```

Expected result:

```text
BUILD SUCCESSFUL
```

This verifies that:

- a new Room row is reported as saved;
- Room's ignored duplicate result is reported as already saved;
- a deleted row is reported as removed;
- trying to remove a missing row is handled as already removed.

## Automated device test - Room database behavior

Connect an Android device or start an emulator. Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.PantryDaoFlowTest
```

Expected result:

```text
Finished 1 test
BUILD SUCCESSFUL
```

The test uses a temporary Room database on the device. It saves the same product twice and confirms that:

- the first insert creates a pantry membership;
- the second insert is ignored;
- only one pantry item exists;
- product name, score, saved Bitwise insight, and ingredients remain intact;
- removal deletes the membership;
- a repeated removal is safe and does not delete another record.

## Automated device test - Product-detail controls

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.PantryActionDisplayFlowTest
```

Expected result:

```text
Finished 3 tests
BUILD SUCCESSFUL
```

This opens the real `fragment_product_details.xml` layout and verifies that:

- an unsaved product shows only an enabled **Add to Pantry** button;
- a saved product shows only **Remove from Pantry**;
- the save button is disabled while Room is processing, preventing repeated requests.

## Manual test 1 - Save a scanned product

1. Install and open the current debug build:

   ```powershell
   .\gradlew.bat installDebug
   adb shell monkey -p com.ciblorenzo.whatsonmyfood 1
   ```

2. Sign in if needed.
3. Scan a valid product that is not already in the pantry.
4. Scroll to **Product actions**.
5. Confirm **Add to Pantry** is visible and **Remove from Pantry** is hidden.
6. Tap **Add to Pantry** once.
7. Confirm the button briefly shows **Saving...**, then changes to **Remove from Pantry**.
8. Open the Pantry screen and confirm the product appears once with its name and score.

Pass result: the product appears once and the detail screen immediately changes to the remove state.

## Manual test 2 - Duplicate prevention

1. Reopen the same product from the pantry.
2. Confirm **Add to Pantry** is not available because the product is already saved.
3. Return to the scanner and scan the same barcode again.
4. Confirm the screen still shows **Remove from Pantry**.
5. Return to the pantry and count the product entries.

Pass result: the same barcode appears only once for the signed-in user.

## Manual test 3 - Preserve score and Bitwise insight

1. Use a product that has a visible health result and a completed Bitwise explanation.
2. Save it to the pantry.
3. Open it from the pantry.
4. Confirm the same product name, ingredients, health score, and saved Bitwise explanation are still present.
5. Close and reopen the app, then check the same product again.

Pass result: saving does not replace the product with an incomplete record or erase the score or explanation.

## Manual test 4 - Remove and refresh

1. Open a saved product from the Pantry screen.
2. Scroll to **Product actions** and tap **Remove from Pantry**.
3. Confirm the button briefly shows **Removing...**, then changes to **Add to Pantry**.
4. Press Back to return to the pantry.
5. Confirm the removed product disappears without restarting the app.
6. Reopen the product by scanning it and confirm its product details remain available and it can be saved again.

Pass result: pantry membership is removed, the pantry list refreshes, and the reusable product record is not corrupted.

## Repeatable visual preview

To inspect the two interface states without scanning or signing in, run:

```powershell
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es pantry_state available --ez pantry_focus true
```

Then run:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.ProductDetailLayoutPreviewActivity --es pantry_state saved --ez pantry_focus true
```

The first preview must show **Add to Pantry**. The second must show **Remove from Pantry**. Only one pantry action should be visible at a time.

## Final quality command

From the `app` folder, run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The ticket passes when all automated tests succeed and the four manual flows work without duplicate pantry entries or lost product fields.
