# M3-02 - Protected AI explanation endpoint test plan

## Automated tests

Run the Android tests from the `app` directory:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.ciblorenzo.whatsonmyfood.api.BitwiseBackendClientTest --tests com.ciblorenzo.whatsonmyfood.analysis.AiExplanationResponseValidatorTest --tests com.ciblorenzo.whatsonmyfood.AiInsightCacheTest
```

Run the protected-backend tests from `backend/retailer`:

```powershell
npm test
```

1. **Valid protected explanation:** verifies that Android builds a versioned request containing separate product and deterministic-rule context, accepts a complete source-backed response, and round-trips the accepted explanation through the local cache format.
2. **Invalid or unauthorized explanation:** verifies that missing configuration has no hardcoded URL fallback, malformed or incomplete responses are rejected, unsafe sources are not accepted, invalid app tokens return HTTP 401, and malformed structured context returns HTTP 400.

## Manual test 1 - successful protected explanation and persistence

**Setup**

- Configure `BITWISE_LLM_BASE_URL` in `app/local.properties` with the protected backend URL.
- Configure matching `BITWISE_APP_TOKEN` values for the Android build and backend.
- Configure `GEMINI_API_KEY` only in the backend environment. Do not place it in Android properties or source code.
- Use a product with a readable ingredient list and at least one deterministic finding.

**Steps**

1. Start the backend and launch the Android app with network access.
2. Scan or open the product and wait for Bitwise analysis. On a warm backend, the grounded explanation should normally appear within 20 seconds; a cold backend may take longer.
3. Confirm the explanation, verdict, deterministic findings, and at least one clickable source are displayed with the product.
4. Close and reopen the product without requesting another analysis.

**Expected result**

- The backend receives `requestVersion`, `productContext`, and `rules` in addition to the generated prompt.
- The explanation displayed is complete and includes a usable HTTP or HTTPS source.
- The accepted explanation reappears from the Room-backed product cache.
- No Gemini or model credential is present in the APK request or Android configuration.

## Manual test 2 - invalid response, authentication, and configuration failure

**Setup**

- Repeat the test with: a mismatched `BITWISE_APP_TOKEN`, an empty `BITWISE_LLM_BASE_URL`, malformed JSON content, an incomplete summary, and a response without a usable source.

**Steps**

1. Open the same product for each failure condition.
2. Observe the Bitwise explanation area and retry behavior.
3. Close and reopen the product after each rejected response.

**Expected result**

- Authentication failure is reported without exposing the raw server body.
- Missing URL configuration fails clearly and never uses a source-code fallback endpoint.
- Malformed, incomplete, or unverified content is not displayed as an accepted explanation and is not saved to the product cache.
- The product and deterministic analysis remain available when the protected explanation fails.
