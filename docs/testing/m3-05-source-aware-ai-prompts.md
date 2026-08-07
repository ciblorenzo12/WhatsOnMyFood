# M3-05 - Source-aware AI prompt and validation test guide

## What this test proves

This test confirms that the backend grounds Bitwise explanations in the supplied product data, normalized ingredients, deterministic findings, source status, and uncertainty. It also confirms that blank, malformed, unsafe, or unsupported provider responses are rejected.

## Which console to use

Use the Android Studio terminal or Windows PowerShell. Start from the repository root, `YourHealtyPantry`. If the terminal starts inside `app`, run `cd ..` first.

## Automated test 1 - Backend prompt and response validation

1. Open a terminal at the repository root.
2. Run:

   ```powershell
   cd backend\retailer
   npm test
   ```

3. Confirm the final test summary reports zero failed tests.

The backend tests verify that:

- product data and normalized ingredients are included;
- deterministic findings, source status, and uncertainty are preserved;
- the explanation uses concise, plain language;
- the model cannot override deterministic findings;
- complete responses with verified HTTPS sources are accepted; and
- blank, HTML, malformed, incomplete, uncited, or unsafe medical responses are rejected.

## Automated test 2 - Android structured request

1. Return to the repository root:

   ```powershell
   cd ..\..
   ```

2. Run the Android suite:

   ```powershell
   cd app
   .\gradlew.bat testDebugUnitTest
   ```

3. Confirm the console ends with:

   ```text
   BUILD SUCCESSFUL
   ```

The Android tests verify the structured request fields and the final defensive validation before an explanation is displayed or cached.

## Manual test 1 - Grounded product explanation

### Before starting

- Configure the protected backend with its Gemini credential.
- Install the hosted debug build on a physical Android device.
- Use a product with a readable ingredient list and at least one deterministic finding, such as cereal containing oats, sugar, and salt.

### Steps

1. Scan the product.
2. Confirm the product name and ingredient list are recognized correctly.
3. Open the Bitwise explanation.
4. Read the verdict and compare it with the deterministic findings.
5. Open at least one scientific source.

### Expected result

- Product and normalized ingredient names match the supplied label.
- The explanation contains `Why this rating`, `Portion guidance`, and `Fact check` sections.
- Deterministic findings remain visible and are not contradicted.
- Source limitations or uncertainty are explained without overstating confidence.
- At least one verified HTTPS source is displayed and opens successfully.
- If the product has no ingredients, the app asks the user to type them or use Ingredient Mode instead of returning a `Needs Review` score.

## Manual test 2 - Invalid or unsafe provider response

This test requires a controlled local provider response. Test these values separately:

- HTML startup page;
- malformed JSON;
- blank summary;
- missing source information; and
- an unsafe claim such as `This product can cure diabetes.`

For each response:

1. Request the same product explanation.
2. Observe the Bitwise explanation area.
3. Close and reopen the product to check the cache.

### Expected result

- Invalid provider text is not displayed or cached.
- The backend rejects unsafe output and uses the controlled fallback when possible.
- Product data and deterministic findings remain available.
- The app uses safe fallback wording if no usable explanation can be produced.

If provider output cannot be modified manually, use the backend automated tests as evidence for these rejection paths.

## Evidence to capture for Trello

- Backend test summary with zero failures.
- Android `BUILD SUCCESSFUL` output.
- Product screen showing all three explanation sections and a scientific source.
- Product screen showing the ingredient-entry or Ingredient Mode instruction when ingredients are missing.
