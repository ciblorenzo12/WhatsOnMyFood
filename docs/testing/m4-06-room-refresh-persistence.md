# M4-06 - Room DAO and refresh persistence test guide

## What this feature protects

A product refresh can replace external information such as the product name, brand, ingredients, nutrition data, labels, and packaging. It must not erase state created or retained on the shopper's device.

The refresh transaction now preserves:

- Pantry membership, stored separately by barcode and user ID;
- favorite state;
- deterministic health score;
- saved Bitwise insight;
- user ingredient-risk rating.

The transaction first reads the saved product, copies the local fields into the refreshed product, and then replaces the external product details. It rejects an attempt to copy state between different barcodes.

## Which console to use

Use the **Terminal** tab in Android Studio or Windows PowerShell.

1. Open the repository folder named `YourHealtyPantry`.
2. Move into the Android project:

   ```powershell
   cd app
   ```

3. If the prompt already ends in `YourHealtyPantry\app`, do not run `cd app` again.
4. For device tests, confirm the Android device or emulator is connected:

   ```powershell
   adb devices
   ```

The device must appear with the status `device`.

## Unit test - Repository refresh policy

This fast test does not require a device, account, camera, or internet connection.

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.ProductRepositoryRefreshPolicyTest"
```

Expected result:

```text
BUILD SUCCESSFUL
```

The three cases verify that:

- refreshed external fields are accepted while local state is retained;
- a product with no saved record keeps its normal defaults;
- one barcode cannot inherit another product's saved state.

## Automated DAO test - Save, load, remove, and refresh

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductRefreshPersistenceDaoTest
```

Expected result:

```text
Finished 3 tests
BUILD SUCCESSFUL
```

This temporary Room database verifies:

- saving and loading a complete Pantry product;
- removing Pantry membership without deleting reusable product data;
- accepting refreshed name and ingredients while preserving Pantry membership, favorite state, health score, Bitwise insight, and user rating.

## On-device persistence check - Close and reopen Room

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.PantryRestartPersistenceTest
```

Expected result:

```text
Finished 1 test
BUILD SUCCESSFUL
```

Unlike the temporary in-memory tests, this check creates a Room database file on the Android device. It saves a product, refreshes it, closes Room, reopens the same database, and confirms that both the refreshed external data and protected local state remain intact. The test removes its database file when it finishes.

## Existing DAO regression tests

Run the original Product DAO checks:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductDaoTest
```

Run the Pantry duplicate/save/remove flow:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.PantryDaoFlowTest
```

These confirm that the new refresh transaction does not break existing product, cache, Pantry, duplicate-prevention, or removal behavior.

## Run all M4-06 device checks together

```powershell
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductDaoTest,com.ciblorenzo.whatsonmyfood.PantryDaoFlowTest,com.ciblorenzo.whatsonmyfood.ProductRefreshPersistenceDaoTest,com.ciblorenzo.whatsonmyfood.PantryRestartPersistenceTest'
```

Expected result:

```text
Starting 9 tests
Finished 9 tests
BUILD SUCCESSFUL
```

## Manual test 1 - Refresh a saved Pantry product

1. Install and open the current debug build:

   ```powershell
   .\gradlew.bat installDebug
   adb shell monkey -p com.ciblorenzo.whatsonmyfood 1
   ```

2. Sign in and scan a product with ingredients.
3. Wait for the health result and Bitwise explanation to finish.
4. Save the product to the Pantry.
5. Note its product name, health result, explanation, and Pantry status.
6. Tap **Update product data** on the detail screen.
7. Wait for the refresh to finish.
8. Return to **My Pantry** and reopen the same product.

Pass result: the product remains in the Pantry; refreshed product data is visible; the prior health result and saved explanation are still available unless a newer completed analysis replaces them.

## Manual test 2 - Close and reopen the app

1. Complete manual test 1.
2. Close the app from the recent-apps screen.
3. Open the app again and return to **My Pantry**.
4. Open the refreshed product.

Pass result: Pantry membership, product details, health result, and saved explanation remain available after reopening the app.

## Manual test 3 - Remove only the Pantry membership

1. Open the saved product from **My Pantry**.
2. Tap **Remove from Pantry**.
3. Return to the Pantry and confirm the item is gone.
4. Scan the same barcode again.

Pass result: the product is no longer a Pantry member, but its reusable product information remains available and it can be saved again.

## Final quality command

From the `app` folder, run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

The ticket passes when the final quality command succeeds, all nine device tests pass, and the three manual flows behave as described.
