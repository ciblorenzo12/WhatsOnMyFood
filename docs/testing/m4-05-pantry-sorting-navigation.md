# M4-05 - Pantry sorting and navigation test guide

## What this feature does

The Pantry toolbar now provides three supported sort options:

- **Recently saved** shows the newest pantry membership first.
- **Product name** sorts names alphabetically without treating uppercase and lowercase as different groups.
- **Health score: highest first** shows scored products from highest to lowest, uses product name to break equal scores, and places products without a score at the end.

Every query includes a final barcode tie-breaker, so the order remains stable when names or scores are equal. The selected option is saved on the device and is reused when the shopper returns to the Pantry.

Selecting a pantry row opens `ProductDetailsActivity` with that row's exact barcode. An empty Pantry now shows useful guidance instead of a blank list.

## Which console to use

Use the **Terminal** tab in Android Studio or Windows PowerShell.

1. Open the repository folder named `YourHealtyPantry`.
2. Move into the Android project:

   ```powershell
   cd app
   ```

3. If the prompt already ends in `YourHealtyPantry\app`, do not run `cd app` again.
4. For device tests, confirm that the device or emulator is connected:

   ```powershell
   adb devices
   ```

The device must appear with the status `device`.

## Unit test - Supported sort choices

This test does not require a device, account, camera, or internet connection.

From the `app` folder, run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.PantrySortOptionTest"
```

Expected result:

```text
BUILD SUCCESSFUL
```

This verifies that the three saved option values restore the correct sort and an unknown or missing value safely returns to **Recently saved**.

## Automated device test - Stable Room sorting

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.PantrySortingDaoTest
```

Expected result:

```text
Finished 3 tests
BUILD SUCCESSFUL
```

The test creates a temporary Room database and confirms:

- newest pantry memberships appear first;
- product names sort alphabetically and case-insensitively;
- equal health scores use product name as a stable tie-breaker;
- products without a health score appear after scored products.

## Automated device test - Correct detail destination

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.PantryNavigationTest
```

Expected result:

```text
Finished 2 tests
BUILD SUCCESSFUL
```

This verifies that a pantry product creates an intent for `ProductDetailsActivity` with its exact barcode and that a missing barcode cannot open an unrelated product.

## Automated device test - Empty and populated Pantry

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.PantryDisplayFlowTest
```

Expected result:

```text
Finished 2 tests
BUILD SUCCESSFUL
```

This opens the production `activity_pantry.xml` and `pantry_list_item.xml` layouts. It confirms that the empty guidance and product list never overlap, a populated list contains all sample products, and clicking each row selects that row's own barcode.

## Run all M4-05 device tests together

```powershell
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.PantrySortingDaoTest,com.ciblorenzo.whatsonmyfood.PantryNavigationTest,com.ciblorenzo.whatsonmyfood.PantryDisplayFlowTest'
```

Expected result:

```text
Starting 7 tests
Finished 7 tests
BUILD SUCCESSFUL
```

## Manual test 1 - Sort by product name

1. Save at least three products whose names begin with different letters.
2. Open **My Pantry**.
3. Tap the sort icon in the toolbar.
4. Select **Product name**.
5. Confirm names appear from A to Z.
6. Leave the Pantry and open it again.

Pass result: the alphabetical order is correct and **Product name** remains selected.

## Manual test 2 - Sort by recently saved

1. Save three products one after another. If they were already saved, remove and save them again in a known order.
2. Open **My Pantry** and select **Recently saved**.
3. Confirm the last product saved is first.
4. Close and reopen the Pantry.

Pass result: newest membership remains first and the order does not change between reloads.

## Manual test 3 - Sort by health score

1. Use at least two products with visible health scores and one product whose score is unavailable.
2. Open **My Pantry** and select **Health score: highest first**.
3. Confirm the highest score is first.
4. Confirm products with equal scores are ordered by product name.
5. Confirm the product without a score is after all scored products.

Pass result: scored products are ordered highest to lowest, ties are stable, and missing scores are last.

## Manual test 4 - Open the correct product

1. Note the name and barcode of three saved products.
2. Tap the first product and verify that its detail screen shows the same product name and brand.
3. Return to the Pantry and repeat for the second and third products.
4. Remove one product from its detail screen and return.

Pass result: every row opens its own product and the removed item disappears when returning to the Pantry.

## Manual test 5 - Empty Pantry

1. Remove every saved product from the test account.
2. Return to **My Pantry**.
3. Confirm the product list is hidden and the empty-Pantry guidance is visible.
4. Save one product and return to the Pantry.

Pass result: the guidance appears only when empty and the product list returns after saving an item.

## Repeatable visual preview

Install the debug build:

```powershell
.\gradlew.bat installDebug
```

Show the populated state:

```powershell
adb shell am start -n com.ciblorenzo.whatsonmyfood/.PantryLayoutPreviewActivity
```

Show the empty state:

```powershell
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.PantryLayoutPreviewActivity --ez empty true
```

## Final quality command

From the `app` folder, run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

The ticket passes when the final quality command succeeds, all seven device tests pass, and the five manual flows behave as described.
