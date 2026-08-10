# M4-01 - Product-detail layout test guide

## What this test proves

This guide verifies that the product-detail screen presents the information a shopper needs in a clear order on both phones and tablets. It checks product identity, health findings, the Bitwise explanation, data-source status, ingredients, nutrition details, marketplace access, and pantry actions.

## What changed

- The product image, name, and brand form the opening product identity area.
- Data source and freshness now appear in a labeled status panel instead of isolated text.
- The overall result, Nutri-Score/NOVA/Eco-Score values, warnings, and positive findings are grouped in one **At a glance** card.
- Bitwise has a dedicated plain-language explanation card with scientific sources directly below the explanation.
- Ingredients and nutrition/product metadata are separated into readable cards.
- Product actions are grouped in one clearly labeled card with large touch targets.
- Phone and tablet resources use different hero heights, image padding, and content margins so the content does not become cramped or excessively wide.
- Short helper text explains what each section contains and tells users that findings can be opened for more detail.

## Which console to use

Use the Android Studio terminal or Windows PowerShell. Start from the repository root, the folder named `YourHealtyPantry`. If the Android Studio terminal starts inside `YourHealtyPantry\app`, run `cd ..` first.

## Automated test 1 - Layout contract

This fast test checks the required visual hierarchy and confirms that both phone and tablet dimensions exist.

1. Open a terminal at the repository root.
2. Run:

   ```powershell
   cd app
   .\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.ProductDetailLayoutContractTest"
   ```

3. Confirm the console ends with:

   ```text
   BUILD SUCCESSFUL
   ```

The test fails if an important section is removed, placed in the wrong order, or the tablet-specific spacing is lost.

## Automated test 2 - Android UI flow

This test requires an emulator or Android device with internet access and Firebase anonymous authentication enabled.

1. Connect the device and confirm it appears:

   ```powershell
   adb devices
   ```

2. From the `app` folder, run:

   ```powershell
   .\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductDetailsFlowTest
   ```

3. Confirm the console ends with `BUILD SUCCESSFUL`.

This verifies that a known barcode opens a product result, the product image and name are visible, health findings remain inside their summary card, and the actions card can be reached by scrolling.

## Manual test 1 - Phone layout

Use a phone or emulator between 360 and 480 dp wide.

1. Sign in and scan a known product with a name, brand, image, ingredients, nutrition values, and at least one health finding.
2. Confirm the product image is large and not stretched or cropped.
3. Confirm the product name and brand are the first text shown below the image.
4. Confirm **Data source and freshness** appears when a source status is available.
5. Confirm **At a glance** shows the overall result, available scores, and the warning/positive findings in one card.
6. Tap one finding and confirm its complete explanation opens.
7. Confirm the Bitwise explanation appears after the deterministic findings and that scientific sources remain readable and clickable when provided.
8. Scroll through Ingredients, marketplace access, and Nutrition and product details.
9. Scroll to Product actions and confirm Update product is visible. For a pantry item, confirm Remove from Pantry is visible; for a product with missing ingredients, confirm Add missing ingredients is visible.
10. Rotate the phone once and confirm text is not clipped, buttons do not overlap, and horizontal scrolling is not required.

Expected result: each section is visually distinct, the most important result is understandable quickly, and all controls remain readable and tappable.

## Manual test 2 - Tablet layout

Use a tablet or emulator at least 600 dp wide.

1. Repeat Manual test 1 with the same known product.
2. Confirm the product image has comfortable space around it and remains proportional.
3. Confirm the content uses wider side margins than the phone layout.
4. Confirm cards do not touch the screen edges or stretch text into excessively long lines.
5. Confirm the three available score chips remain aligned in one row.
6. Confirm section headings, findings, explanation, ingredients, nutrition information, and actions remain in the same order as on the phone.

Expected result: the tablet layout feels intentionally spaced rather than like a stretched phone screen.

## Screenshot evidence for Trello

Capture at least these two images after the manual checks:

1. **Phone overview:** product image, name/brand, source panel, and At a glance card.
2. **Phone details:** Bitwise explanation or ingredients plus the Product actions card.
3. **Tablet overview (recommended):** the same product showing the wider tablet margins.

Use screenshots that contain test product information only. Do not include email addresses, account identifiers, API credentials, console secrets, or other private information.

## Pass criteria

- The focused JVM test passes.
- The connected UI test passes when a device is available, or the manual phone procedure is completed when it is not.
- No text is clipped or overlapped on the tested phone and tablet sizes.
- Product identity, findings, explanation, source status, and actions are easy to locate.
- Images keep their original proportions.
