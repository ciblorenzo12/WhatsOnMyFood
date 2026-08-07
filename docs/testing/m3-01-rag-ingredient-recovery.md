# M3-01 - RAG ingredient recovery test guide

## What this test proves

This test confirms that the app can recover a missing ingredient list from the protected RAG service. Recovered ingredients must be parsed, normalized, deduplicated, saved, and identified by source. A failed recovery must not prevent the product from opening.

## Which console to use

Use either:

- Android Studio: **View > Tool Windows > Terminal**; or
- Windows PowerShell.

The commands below start from the repository root, the folder named `YourHealtyPantry`. If the Android Studio terminal opens inside `YourHealtyPantry\app`, run `cd ..` first.

## Automated test

### Steps

1. Open a terminal at the repository root.
2. Enter the Android project:

   ```powershell
   cd app
   ```

3. Run the M3-01 tests:

   ```powershell
   .\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.RagIngredientRecoveryTest"
   ```

### Expected result

The console must end with:

```text
BUILD SUCCESSFUL
```

The tests verify two important paths:

- **Valid recovery:** the response is parsed with the existing ingredient parser, normalized, deduplicated, ranked, and converted into ingredient records.
- **Safe failure:** empty, missing, invalid, unavailable, or warning-only responses do not create recovered ingredients or replace the valid product result.

## Manual test 1 - Successful recovery and persistence

### Before starting

You need:

- a valid `BITWISE_APP_TOKEN` on the backend and Android app;
- `RETAILER_BACKEND_BASE_URL` configured in `app/local.properties`; and
- a test product that has a recognizable name and barcode but no usable ingredient list in its primary product record.

The protected RAG response should contain mixed capitalization and at least one duplicate ingredient so normalization can be verified.

### Console 1 - Start the backend

From the repository root:

```powershell
cd backend\retailer
npm start
```

Leave this console running.

### Steps in the app

1. Install or launch the Android app with network access.
2. Scan the missing-ingredient product.
3. Wait for product loading and RAG recovery to finish.
4. Open the product details.
5. Confirm the ingredients are readable, normalized, and not duplicated.
6. Confirm the source message says the ingredients came from a label or supporting service.
7. Close the product.
8. Disable the backend or the device network.
9. Open the same product again.

### Expected result

- The product is recognized even though its original database record had no ingredients.
- Recovered ingredients appear once, with consistent formatting.
- Deterministic analysis uses the recovered list.
- The source of the recovered ingredients is identified.
- The ingredients remain available from the local Room cache after the network is disabled.

## Manual test 2 - Recovery failure

This test requires a controlled backend or mock response. Test these responses separately:

- `status: 0`;
- an empty ingredient field;
- malformed JSON;
- HTTP 503; and
- an unreachable backend.

### Steps

1. Configure one failure response.
2. Scan the same missing-ingredient product.
3. Wait for the lookup to finish.
4. Confirm the product name and other available product information remain visible.
5. Confirm no empty or malformed ingredient entry is displayed or saved.
6. Repeat for each failure response.

### Expected result

- A RAG failure does not block or replace the product result.
- The app remains usable and offers its normal missing-ingredient recovery option.
- Invalid or empty ingredients are not persisted.

If a controlled backend is unavailable, use the automated test as evidence for the failure paths.

## Evidence to capture for Trello

- A screenshot of `BUILD SUCCESSFUL`.
- A screenshot showing the recognized product and deduplicated recovered ingredients.
- A screenshot showing that the product remains visible when recovery fails.
