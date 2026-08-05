# M3-01 — RAG ingredient recovery test plan

## Automated tests

Run from the `app` directory:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.ciblorenzo.whatsonmyfood.RagIngredientRecoveryTest
```

1. **Valid recovery:** verifies that a successful RAG payload is parsed with the existing ingredient parser, normalized, deduplicated, assigned stable ranks, and converted to ingredient entities.
2. **Safe failure:** verifies that missing, unavailable, empty, and warning-only responses produce no recovered ingredients. The repository therefore keeps the product result instead of replacing it with invalid data.

## Manual test 1 — successful recovery and persistence

**Setup**

- Run the protected backend with a valid `BITWISE_APP_TOKEN`.
- Configure `RETAILER_BACKEND_BASE_URL` and the matching `BITWISE_APP_TOKEN` in `app/local.properties`.
- Use a barcode whose primary product response has a valid product name but no ingredient list, while the RAG endpoint returns a list containing mixed capitalization and at least one duplicate.

**Steps**

1. Launch the Android app with network access and scan the barcode.
2. Open the product details and wait for recovery to finish.
3. Confirm the ingredient list is visible, normalized, and contains no duplicate entries.
4. Confirm the source message says the ingredients were recovered from a label or supporting service.
5. Close the product, disable the backend or network, and open the same product again.

**Expected result**

- The recovered ingredients remain available from Room after reopening the product.
- Deterministic ingredient analysis uses the recovered list.
- The initial online result identifies that a supporting service supplied the ingredients.

## Manual test 2 — empty, invalid, and unavailable recovery

**Setup**

- Use the same missing-ingredient product.
- Repeat the test with the RAG endpoint returning: `status: 0`, an empty ingredient field, malformed JSON, and HTTP 503 or an unreachable server.

**Steps**

1. Scan the barcode for each failure response.
2. Wait until the product lookup finishes.
3. Confirm the product name and any other available product data are still displayed.
4. Confirm no invalid ingredient row or recovered-source message is saved.

**Expected result**

- Recovery failure does not block or replace the product result.
- The app remains usable and shows the normal missing-ingredient recovery option.
- No empty, malformed, or warning-only ingredient data is persisted.
