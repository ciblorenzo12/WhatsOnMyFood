# M3-03 - Resilient AI and RAG fallback test guide

## What this test proves

This test confirms that temporary AI or RAG problems do not block the product result. The app uses short timeouts, retries only safe transient failures, avoids retry loops for rate limits, rejects invalid startup responses, and keeps deterministic findings visible.

## Which console to use

Use the Android Studio terminal or Windows PowerShell. Start from the repository root, `YourHealtyPantry`. If the terminal starts inside `app`, run `cd ..` first.

## Automated test

1. Open a terminal at the repository root.
2. Enter the Android project:

   ```powershell
   cd app
   ```

3. Run:

   ```powershell
   .\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.api.BitwiseBackendClientTest" --tests "com.ciblorenzo.whatsonmyfood.api.ResilientRequestPolicyTest" --tests "com.ciblorenzo.whatsonmyfood.RagIngredientLookupClientTest" --tests "com.ciblorenzo.whatsonmyfood.analysis.BitwiseAnalysisServiceTest"
   ```

4. Confirm the console ends with:

   ```text
   BUILD SUCCESSFUL
   ```

The test verifies:

- one bounded retry for timeout, connection failure, HTTP 502/503/504, or an HTML startup page;
- no retry loop for HTTP 429;
- friendly use of `Retry-After`;
- rejection of invalid responses; and
- no retry for protocol or TLS errors.

## Manual test 1 - AI transient failure

### Before starting

Use a product with ingredients and deterministic findings. This test also requires a controlled backend or mock server that can return a planned sequence of responses.

### Test sequence A - Recovery after one retry

Configure the server to return:

1. HTTP 503 on the first request.
2. A valid Bitwise response on the second request.

Then:

1. Open the product details.
2. Confirm product identity, ingredients, rating, and rule findings appear before the explanation.
3. Count the requests in the backend console.
4. Confirm exactly two requests occur: the original request and one retry.
5. Confirm the valid explanation appears after the retry.

### Test sequence B - Final fallback

Repeat separately with:

- two HTTP 503 responses;
- a response delayed beyond the timeout;
- HTTP 429 with `Retry-After: 30`; and
- an HTML startup page.

### Expected result

- HTTP 503, timeout, and startup HTML receive no more than one retry.
- HTTP 429 receives no automatic retry.
- After a final failure, friendly fallback wording and a tap-to-retry option appear.
- Product data, rating, and deterministic findings remain visible and unchanged.

## Manual test 2 - RAG unavailable

### Before starting

Use a barcode with a valid product name and brand but no usable ingredient list. Configure the RAG endpoint to return these responses in separate runs:

- HTTP 502, 503, 504, or 429;
- invalid JSON;
- HTML startup page; and
- delayed timeout.

### Steps

1. Scan the barcode for one configured failure.
2. Count the RAG calls in the backend console.
3. Wait for the bounded recovery attempt to finish.
4. Confirm the product screen remains usable.
5. Repeat for each failure response.
6. Restore a valid RAG response and scan again.

### Expected result

- Transient failures receive at most one retry.
- HTTP 429 and invalid JSON receive no retry.
- Failed RAG recovery does not corrupt cached product identity or deterministic data.
- After service recovery, valid ingredients are normalized, deduplicated, saved, and identified by source.

If a controlled backend is unavailable, use the automated test as evidence for the error paths.

## Evidence to capture for Trello

- `BUILD SUCCESSFUL`.
- Backend request counts showing no more than one retry.
- Product details remaining visible during failure.
- Friendly fallback wording and the retry action.
