# M3-06 Privacy-safe AI and RAG observability

## Diagnostic contract

Every protected AI and RAG request receives an `X-Correlation-ID`. The Android client creates it, the backend preserves and returns it, and both sides write a single completion diagnostic that can be matched during testing.

Only these fields are allowed:

| Field | Purpose |
| --- | --- |
| `event` | Stable event name (`protected_request`) on the backend |
| `correlationId` / `correlation_id` | Random request trace identifier |
| `route` | Allowlisted route template, never a product-specific URL |
| `outcome` | `success`, `empty_result`, `fallback_success`, `failure`, or `rate_limited` |
| `status` | HTTP status, or `0` when no response was received |
| `latencyMs` / `latency_ms` | Total elapsed request time |
| `errorCategory` / `error_category` | Safe classification such as `timeout`, `rate_limit`, or `provider_unavailable` |

Credentials, authorization headers, full prompts, response bodies, images, barcodes, product names, brands, ingredient text, raw exception messages, IP addresses, and query strings are excluded by construction.

## Manual diagnostic test

1. Start the retailer backend and keep its console visible.
2. In a second terminal, show only Android diagnostic events:

   ```powershell
   adb logcat -s PrivacySafeRequest
   ```

3. From the app, scan a product that uses the normal product path, then use ingredient mode or a missing-ingredient product to exercise RAG.
4. Confirm the app and backend show the same correlation ID for the request.
5. Confirm each line includes the route template, outcome, status, latency, and safe error category.
6. Confirm no line contains label text, a barcode, product name, image data, prompt content, or credentials.
7. To exercise failure categories in a controlled environment, temporarily use a nonresponsive provider URL for `timeout`, exceed the protected request limit for `rate_limit`, or stop the provider while leaving the backend running for `provider_unavailable`. Restore the normal configuration after the test.

## Console-only substitute

When a device or manual service failure cannot be reproduced, run:

```powershell
Set-Location backend/retailer
npm run demo:observability
```

The script prints representative AI success, timeout, RAG rate-limit, and provider-unavailable events. It exits with failure if its console output contains any forbidden fixture representing a credential, prompt, image, barcode, or product identifier. A valid run ends with:

```text
PASS: only allowlisted diagnostic fields were printed; sensitive request data was excluded.
```

## Automated verification

Backend:

```powershell
Set-Location backend/retailer
npm test
```

Android:

```powershell
Set-Location app
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.api.PrivacySafeRequestDiagnosticsTest"
```

The automated coverage verifies correlation ID preservation, route normalization, the timeout/rate-limit/provider categories, exact allowlisted fields, and rejection of unexpected sensitive values.
