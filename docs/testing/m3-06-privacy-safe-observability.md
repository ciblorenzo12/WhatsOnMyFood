# M3-06 - Privacy-safe AI and RAG observability test guide

## What this test proves

This test confirms that AI and RAG requests can be traced during development without exposing private request information. Android and the backend use the same correlation ID and record only an approved set of diagnostic fields.

## Information allowed in diagnostic output

| Field | Meaning |
| --- | --- |
| `event` | Stable backend event name: `protected_request` |
| `correlationId` or `correlation_id` | Random identifier shared by Android and backend |
| `route` | Safe route template, never a product-specific URL |
| `outcome` | `success`, `empty_result`, `fallback_success`, `failure`, or `rate_limited` |
| `status` | HTTP status, or `0` when no response was received |
| `latencyMs` or `latency_ms` | Total request duration |
| `errorCategory` or `error_category` | Safe category such as `timeout`, `rate_limit`, or `provider_unavailable` |

The output must not contain credentials, authorization headers, full prompts, response bodies, images, barcodes, product names, brands, ingredient text, raw exception messages, IP addresses, or query strings.

## Which console to use

Use the Android Studio terminal or Windows PowerShell. Start from the repository root, `YourHealtyPantry`. If the terminal starts inside `app`, run `cd ..` first.

## Test 1 - Console-only demonstration

Use this test when a phone, emulator, or manual service failure is unavailable.

1. Open a terminal at the repository root.
2. Run:

   ```powershell
   cd backend\retailer
   npm run demo:observability
   ```

3. Confirm the console displays four examples:

   - successful AI request;
   - AI timeout;
   - RAG rate limit; and
   - RAG provider unavailable.

4. Confirm the final line is:

   ```text
   PASS: only allowlisted diagnostic fields were printed; sensitive request data was excluded.
   ```

If the script reports `FAIL`, do not use the output as evidence. Review the diagnostic formatter before continuing.

## Test 2 - Backend automated tests

From the same `backend\retailer` console, run:

```powershell
npm test
```

Expected result: the summary reports zero failed tests. The M3-06 tests verify route normalization, correlation ID preservation, allowed fields, and timeout/rate-limit/provider classifications.

## Test 3 - Android automated tests

1. Return to the repository root:

   ```powershell
   cd ..\..
   ```

2. Run the focused Android test:

   ```powershell
   cd app
   .\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.api.PrivacySafeRequestDiagnosticsTest"
   ```

3. Confirm the console ends with:

   ```text
   BUILD SUCCESSFUL
   ```

## Test 4 - Manual Android and backend correlation

This test uses two consoles and a connected Android phone or emulator.

### Console 1 - Start the backend

From the repository root:

```powershell
cd backend\retailer
npm start
```

Leave this console running so backend diagnostic events remain visible.

### Console 2 - Show Android diagnostics

1. Open another Android Studio terminal or PowerShell window.
2. Confirm the device is connected:

   ```powershell
   adb devices
   ```

3. Display only the privacy-safe Android events:

   ```powershell
   adb logcat -s PrivacySafeRequest
   ```

4. Leave this console running.

### Steps in the app

1. Scan a normal product to exercise the protected AI route.
2. Use Ingredient Mode or a product with missing ingredients to exercise the RAG route.
3. Compare the Android and backend consoles.

### Expected result

- Android and backend show the same correlation ID for each request.
- Every completed request shows route, outcome, status, latency, and safe error category.
- The RAG route appears as `/api/retail/products/:barcode/ingredients/rag` rather than containing the real barcode.
- No console line contains product details, ingredient text, prompt text, images, credentials, or raw server errors.

## Optional controlled failure test

Only perform this in a test environment:

- use a nonresponsive provider to produce `timeout`;
- exceed the protected test rate limit to produce `rate_limit`; or
- stop the provider while leaving the backend running to produce `provider_unavailable`.

Restore the normal provider configuration after testing. If these failures cannot be reproduced safely, use `npm run demo:observability` as the approved substitute.

## Evidence to capture for Trello

- The console-only demonstration ending in `PASS`.
- Backend test summary with zero failures.
- Android `BUILD SUCCESSFUL` output.
- Matching correlation IDs from Android and backend, with sensitive information excluded.
