# M6-02 - Online, stale-cache, and offline verification

## Ticket

- **Milestone:** M6
- **Estimate:** 2.5 hours
- **Target:** Week 6
- **Labels:** M6, F1, F7, Verification, High
- **Target release:** `v0.31.0-rc1`
- **Result:** **Pass**
- **Run date:** August 25, 2026

## What I tested

I tested the product lookup and source-status behavior under five controlled network and cache conditions: an online database result, a recent saved result, a stale saved result, an offline saved result, and offline mode with no saved result. The checks covered both the repository decision and the wording displayed on a Samsung SM-X800 tablet running Android 16.

During testing, I found that a complete cached product could be returned before the repository checked whether the device was online. That meant a saved product opened offline could be labeled fresh or stale instead of clearly being identified as an offline copy. I corrected the lookup order so connectivity is included in the cache decision. Offline cached results now always use the **OFFLINE COPY** state and explain that freshness cannot be confirmed.

## Scenario results

| Scenario | Expected and observed behavior | Result | Evidence |
| --- | --- | --- | --- |
| Online lookup with current data | A successful product lookup is attributed to the product database. The screen says **PRODUCT DATABASE** and **Updated from product database**, while explaining that completeness depends on the source record rather than promising guaranteed freshness. | **Pass** | `ProductSourceStatusFlowTest.onlineCurrentData_isAttributedWithoutClaimingGuaranteedFreshness` and `SourceStatusResolverTest` |
| Valid fresh-cache result | A recent cached result uses `FRESH`, **RECENT SAVED RESULT**, and **Fresh cached result**. It is clearly presented as saved data, not as a new network response. | **Pass** | `ProductRepositoryCacheBehaviorTest.onlineFreshCache_isReturnedAsFreshSavedData` and `ProductSourceStatusFlowTest.freshCache_isClearlyLabeledAsRecentSavedData` |
| Stale-cache result | A cache older than the refresh window uses `STALE`, **REFRESH RECOMMENDED**, and wording that the saved information may be outdated or older. It does not imply that the data is current. | **Pass** | `ProductRepositoryCacheBehaviorTest.onlineStaleCache_isReturnedWithAnOutdatedWarning` and `ProductSourceStatusFlowTest.staleCache_requestsRefreshWithoutClaimingCurrentData` |
| Offline with cached data | Any cached result opened without connectivity uses `OFFLINE`, **OFFLINE COPY**, and an explanation that freshness cannot be confirmed while offline. This is true even when the saved result is less than 24 hours old. | **Pass** | `ProductRepositoryCacheBehaviorTest.offlineCache_isNeverPresentedAsCurrent` and `ProductSourceStatusFlowTest.offlineCopy_isClearlyDistinguishedFromStaleCache` |
| Offline without cached data | The lookup stops with the understandable message: “You are offline. No saved result is available for this product.” It does not show an empty or supposedly current product result. | **Pass** | `ProductRepositoryCacheBehaviorTest.offlineWithoutCache_hasAnUnderstandableUnavailableMessage` |

## Test run summary

| Suite | Environment | Result |
| --- | --- | --- |
| Targeted Android unit tests | Windows 11, Gradle 9.5.0 | **19 passed, 0 failed** |
| Android source-status device tests | Samsung SM-X800 tablet, Android 16 | **6 passed, 0 failed** |
| **Total** | Repository, presentation, and device verification | **25 passed, 0 failed** |

The tablet run included the five M6-02 source and freshness states plus one regression check confirming that deterministic findings remain visible when the AI explanation is unavailable.

## Commands used

From the repository root:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest `
  --tests ProductRepositoryCacheBehaviorTest `
  --tests SourceStatusResolverTest `
  --tests SourceStatusPresentationTest `
  --tests ProductRepositoryRefreshPolicyTest `
  --tests ProductLookupDispatcherTest

.\app\gradlew.bat -p .\app connectedDebugAndroidTest `
  '-Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductSourceStatusFlowTest'
```

## Evidence

- `docs/testing/evidence/m6-02/m6-02-test-results.txt` records the environment, commands, test counts, and checklist mapping.
- Generated Android unit report: `app/build/reports/tests/testDebugUnitTest/index.html`
- Generated tablet result: `app/build/outputs/androidTest-results/connected/debug/TEST-SM-X800 - 16-_-.xml`

## Checklist completion

- [x] Test online lookup with current data
- [x] Test a valid fresh-cache result
- [x] Test a stale-cache result
- [x] Test offline mode with cached data
- [x] Test offline mode without cached data
- [x] Verify source and freshness messages in every case

## Conclusion

M6-02 meets its done condition. The app now distinguishes live database attribution, recent saved data, stale saved data, and offline copies without implying that cached or offline information is current. When no saved result exists offline, the shopper receives a direct explanation instead of an unusable product screen.
