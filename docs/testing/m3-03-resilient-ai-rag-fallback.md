# M3-03 - Resilient AI and RAG fallback test plan

## Automated tests

Run the Android unit tests from the `app` directory:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.ciblorenzo.whatsonmyfood.api.BitwiseBackendClientTest --tests com.ciblorenzo.whatsonmyfood.api.ResilientRequestPolicyTest --tests com.ciblorenzo.whatsonmyfood.RagIngredientLookupClientTest --tests com.ciblorenzo.whatsonmyfood.analysis.BitwiseAnalysisServiceTest
```

1. **Bounded transient recovery:** verifies explicit AI and RAG timeouts and permits only one retry for a timeout, connection failure, HTML startup page, or HTTP 502, 503, and 504 response.
2. **Non-retryable rejection:** verifies HTTP 429 never enters a retry loop, its `Retry-After` value receives friendly wording, invalid content is rejected, and protocol or TLS errors are not retried.

## Manual test 1 - transient AI failure with deterministic fallback

**Setup**

- Use a product with ingredients and at least one deterministic rule finding.
- Point `BITWISE_LLM_BASE_URL` to a controlled test server.
- Configure the server to return HTTP 503 once and a valid protected explanation on the next request.

**Steps**

1. Open the product details screen.
2. Confirm the product identity, ingredients, rating, and rule findings render before the AI explanation.
3. Confirm the client sends exactly one retry and then displays the valid explanation.
4. Repeat with two HTTP 503 responses, a delayed response beyond the read timeout, HTTP 429 with `Retry-After: 30`, and an HTML startup page.

**Expected result**

- HTTP 503, timeout, and the HTML startup page receive no more than one controlled retry.
- HTTP 429 receives no automatic retry.
- When both attempts fail, the card states that the detailed explanation is temporarily unavailable and offers a tap-to-retry action.
- The product rating and deterministic rule findings remain visible and unchanged.

## Manual test 2 - RAG unavailable with usable product result

**Setup**

- Use a known barcode whose product record has a valid name and brand but no usable ingredient list.
- Configure the protected RAG endpoint to return, in separate runs, HTTP 502, 503, 504, 429, invalid JSON, and an HTML startup page.

**Steps**

1. Scan the barcode for each failure response.
2. Count the RAG requests in the test-server log.
3. Observe the product screen after the bounded RAG attempt finishes.
4. Restore a valid RAG response and scan again.

**Expected result**

- HTTP 502, 503, 504, timeout, and startup HTML receive at most one retry; HTTP 429 and invalid JSON receive none.
- A failed RAG recovery does not replace or corrupt cached product identity, nutrition, or deterministic data.
- The product result remains usable and offers the existing ingredient-contribution or retry path.
- After recovery is restored, validated ingredients are normalized, deduplicated, saved, and marked by source.
