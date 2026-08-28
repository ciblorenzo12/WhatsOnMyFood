# M6-03 - Backend failure and timeout verification

## Ticket

- **Milestone:** M6
- **Estimate:** 2.5 hours
- **Target:** Week 6
- **Labels:** M6, F3, F4, F6, Verification, High
- **Target release:** `v0.31.0-rc1`
- **Result:** **Pass**
- **Run date:** August 25, 2026

## What I tested

I tested the protected Bitwise and retailer failure paths with controlled unit, backend, and Android device tests. The tests intentionally supplied timeouts, rate limits, invalid payloads, HTML startup pages, gateway failures, and unavailable-service states. They did not interrupt a production service.

The core product experience stayed available in every tested failure state. Product identity, the health result, and deterministic GOOD, WATCH, and INFO findings remained visible when Bitwise was unavailable. Marketplace timeout and unavailable-service states kept the toolbar and retry action available instead of displaying a blank or broken screen.

## Scenario results

| Scenario | Simulation and expected behavior | Result | Evidence |
| --- | --- | --- | --- |
| Timeout | Supplied `SocketTimeoutException`. Bitwise allows two bounded cold-start retries (three attempts total), then shows “Bitwise took too long to respond. Please try again.” The marketplace shows a separate **REQUEST TIMED OUT** state with **Try again**. | **Pass** | `ResilientRequestPolicyTest`, `BitwiseBackendClientTest`, `MarketplaceStateResolverTest`, and `MarketplaceStateDisplayFlowTest` |
| Rate limit | Supplied HTTP 429 with `Retry-After: 30`. The app does not start a retry loop and explains when the shopper can try again. The backend rate limiter and privacy-safe `rate_limit` classification also passed. | **Pass** | `BitwiseBackendClientTest`, `ResilientRequestPolicyTest`, `PrivacySafeRequestDiagnosticsTest`, and backend rate-limiter tests |
| Invalid response | Supplied non-JSON, empty, malformed, incomplete, and uncited provider responses. Invalid content is rejected and is not presented as a valid AI explanation. | **Pass** | `ResilientRequestPolicyTest` and `backend/retailer/test/bitwiseGemini.test.js` |
| HTML startup response | Supplied a RunPod-style `<!DOCTYPE html>` startup page. Bitwise retries it up to twice, rejects it after the bounded retry budget, and uses friendly startup wording. The backend rejects HTML provider output and returns its controlled fallback. | **Pass** | `ResilientRequestPolicyTest`, `BitwiseBackendClientTest`, and `bitwiseGemini.test.js` |
| HTTP 502, 503, and 504 | Supplied each transient gateway status. Bitwise receives no more than two retries and resolves to the same safe shopper message without exposing the raw provider body. | **Pass** | `BitwiseBackendClientTest.gatewayFailuresUseTheSameFriendlyBoundedFallback` |
| AI unavailable | Opened the tablet product preview in the AI-unavailable state. The source indicator and retry explanation appeared while the product name, health card, and rule findings stayed visible. | **Pass** | `ProductSourceStatusFlowTest.aiUnavailable_keepsProductAndRuleBasedFallbackVisible` and `evidence/m6-03/ai-fallback-rule-findings-tablet.png` |
| Retailer unavailable | Opened the controlled marketplace timeout and error states. Both used distinct, understandable wording; the toolbar remained visible; and **Try again** was available. | **Pass** | `MarketplaceStateDisplayFlowTest.everyMarketplaceStateIsClearAndKeepsNavigationAvailable` |

## Test run summary

| Suite | Environment | Result |
| --- | --- | --- |
| Targeted Android unit tests | Windows 11, Gradle 9.5.0 | **24 passed, 0 failed** |
| Retailer backend tests | Node test runner | **37 passed, 0 failed** |
| Android device tests | Samsung SM-X800 tablet, Android 16 | **5 passed, 0 failed** |
| **Total** | Unit, backend, and device verification | **66 passed, 0 failed** |

The device run covered four product source/fallback tests and one marketplace test that checks live, mock, empty, timeout, and unavailable-service presentation states.

## Commands used

From the repository root:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest `
  --tests "com.ciblorenzo.whatsonmyfood.api.BitwiseBackendClientTest" `
  --tests "com.ciblorenzo.whatsonmyfood.api.ResilientRequestPolicyTest" `
  --tests "com.ciblorenzo.whatsonmyfood.api.PrivacySafeRequestDiagnosticsTest" `
  --tests "com.ciblorenzo.whatsonmyfood.analysis.ProductFindingsDisplayTest" `
  --tests "com.ciblorenzo.whatsonmyfood.retail.MarketplaceStateResolverTest" `
  --tests "com.ciblorenzo.whatsonmyfood.retail.RetailerBackendContractTest"

.\app\gradlew.bat -p .\app connectedDebugAndroidTest `
  "-Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductSourceStatusFlowTest,com.ciblorenzo.whatsonmyfood.MarketplaceStateDisplayFlowTest"

Push-Location backend\retailer
npm test
Pop-Location
```

## Evidence

- `docs/testing/evidence/m6-03/m6-03-test-results.txt` records the environment, commands, counts, and checklist mapping.
- `docs/testing/evidence/m6-03/ai-fallback-rule-findings-tablet.png` shows the friendly AI-unavailable state while the deterministic product result remains visible.
- Generated Android unit report: `app/build/reports/tests/testDebugUnitTest/index.html`
- Generated device result: `app/build/outputs/androidTest-results/connected/debug/TEST-SM-X800 - 16-_-.xml`

## Checklist completion

- [x] Simulate timeout responses
- [x] Simulate rate-limit responses
- [x] Simulate invalid and HTML startup responses
- [x] Simulate 502, 503, and 504 responses
- [x] Verify rule-based results remain visible
- [x] Verify retry and fallback messages are understandable

## Conclusion

M6-03 meets its done condition. Temporary AI and retailer failures do not replace or hide deterministic product results, and each tested failure produces bounded retry behavior or an understandable fallback state.
