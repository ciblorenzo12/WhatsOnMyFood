# M3-02 - Protected AI explanation test guide

## What this test proves

This test confirms that Android sends structured product and rule information to the protected backend, receives a usable Bitwise explanation, validates it, saves it, and displays it without storing the model provider credential on the device.

## Which console to use

Use the Android Studio terminal or Windows PowerShell. Start from the repository root, `YourHealtyPantry`. If the Android Studio terminal starts inside `app`, run `cd ..` first.

## Automated tests

### Test A - Android client and response validation

1. Open a terminal at the repository root.
2. Run:

   ```powershell
   cd app
   .\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.api.BitwiseBackendClientTest" --tests "com.ciblorenzo.whatsonmyfood.analysis.AiExplanationResponseValidatorTest" --tests "com.ciblorenzo.whatsonmyfood.AiInsightCacheTest"
   ```

3. Confirm the console ends with:

   ```text
   BUILD SUCCESSFUL
   ```

These tests verify the structured request, response validation, URL configuration, and local insight cache.

### Test B - Protected backend

1. Return to the repository root:

   ```powershell
   cd ..
   ```

2. Run the backend tests:

   ```powershell
   cd backend\retailer
   npm test
   ```

3. Confirm the final summary reports zero failed tests.

These tests verify valid explanations, HTTP 401 for an invalid token, HTTP 400 for malformed structured context, safe sources, and controlled fallback behavior.

## Manual test 1 - Successful explanation and cache

### Before starting

Confirm that:

- `BITWISE_LLM_BASE_URL` is configured in `app/local.properties`;
- Android and the backend use the same `BITWISE_APP_TOKEN`;
- `GEMINI_API_KEY` exists only in the backend environment; and
- the test product has a readable ingredient list and at least one deterministic finding.

### Console 1 - Start the backend

From the repository root:

```powershell
cd backend\retailer
npm start
```

Leave this console running.

### Steps in the app

1. Launch the Android app with network access.
2. Scan or open the test product.
3. Wait for the Bitwise explanation. A warm backend should normally respond within about 20 seconds; a cold hosted backend may take longer.
4. Confirm the explanation, verdict, deterministic findings, and at least one scientific source are visible.
5. Open the source and confirm it uses HTTP or HTTPS.
6. Close the product and open it again without requesting another analysis.

### Expected result

- Bitwise displays a complete explanation with usable sources.
- Deterministic findings remain visible.
- The accepted explanation reappears from the local Room cache.
- The Android request contains product and rule context, but no Gemini credential.

## Manual test 2 - Invalid response or configuration

Use a controlled test environment and test these conditions separately:

- mismatched `BITWISE_APP_TOKEN`;
- empty `BITWISE_LLM_BASE_URL`;
- malformed JSON;
- incomplete explanation; and
- response without a usable source.

For each condition:

1. Open the same product.
2. Observe the Bitwise explanation area.
3. Retry once if the interface offers a retry action.
4. Close and reopen the product.

### Expected result

- Authentication and configuration failures use friendly wording.
- Raw server responses are not shown to the shopper.
- Invalid content is not displayed or saved.
- The product and deterministic analysis remain available.

If these failures cannot be created manually, use the automated Android and backend tests as evidence.

## Evidence to capture for Trello

- `BUILD SUCCESSFUL` from Android.
- The backend test summary with zero failures.
- The product screen showing the Bitwise explanation and scientific source.
- The product screen remaining usable during a controlled failure.
