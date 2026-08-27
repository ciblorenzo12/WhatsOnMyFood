# M6-04 - Critical defect triage

## Ticket

- **Milestone:** M6
- **Estimate:** 2.5 hours
- **Target:** Week 6
- **Labels:** M6, All features, Quality, High
- **Target release:** `v0.31.0-rc1`
- **Owner:** Project team
- **Run date:** August 26, 2026
- **Base commit:** `237a3821ae095396e53ca84ffb4cd65a9698f20e`
- **Result:** **Pass - no reproducible blocker remains in the tested demo flow**

The M6-01 matrix at commit `d13f2bc` was reviewed together with the M6-02 online/cache/offline results and the M6-03 backend-failure results. This run repeated the automated companion checks on the current Samsung SM-X800 advisor-demo tablet, corrected the release identity defect found during triage, and rebuilt and retested the candidate.

This result does not claim that all 38 manual M6-01 cases were executed. Physical camera, package-label, permission-reset, live-provider, and API-26 checks remain explicit pre-demo activities below; none is a currently reproduced failure.

## Severity rules

| Severity | Triage rule |
| --- | --- |
| **Critical** | Blocks a core demo journey, crashes or loses the selected product, hides deterministic results, or materially misrepresents product/source state. Must be fixed or have a tested safe mitigation before the demo. |
| **High** | Produces a major incorrect result or release-control problem but leaves a safe core journey available. Fix for the candidate when reproducible. |
| **Medium** | Degrades a secondary state or depends on an external/manual environment. Record an owner and preflight or workaround. |
| **Low** | Cosmetic, tooling, or coverage issue with no expected advisor-demo impact. Track after the demo unless it worsens. |

## Defect register

| ID | Matrix impact | Severity | Owner | Reproduction | Resolution or mitigation | Retest evidence | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| **DEF-M6-04-001** | E2E-S06, E2E-AI02, E2E-P04, E2E-X02 | **Critical** | Project team | Cache a complete product while online, disable the network, and reopen it before the cache-age decision. Before M6-02, the repository could return the complete cache before checking connectivity, allowing an offline copy to appear fresh or merely stale. | Fixed in `237a382`: connectivity is now part of the cache decision and every offline cached result uses the honest `OFFLINE COPY` state. | M6-02 recorded 19 focused unit tests and 6 tablet checks passing. The final M6-04 rerun also passed all 205 unit tests and the 30-test tablet demo-path suite, including source-state and pantry persistence coverage. | **Resolved** |
| **DEF-M6-04-002** | Run-log identity and all evidence tied to the candidate APK | **High** | Project team | Build the debug APK and inspect `output-metadata.json`; the candidate reported `0.31.-rc1` instead of the matrix target `0.31.0-rc1`. | Corrected `versionName` and added `ReleaseIdentityTest` so the expected release-candidate name is enforced by unit tests. | Forced Android unit/lint/build run passed; generated APK metadata reports version code `12` and version name `0.31.0-rc1`; final tablet run passed 30/30. | **Resolved** |
| **DEF-M6-04-003** | All assigned AI-supported scan and explanation cases | **High** | Project team | Use three free Bitwise explanations in one day, then attempt the next assigned AI-supported case. The normal freemium gate prevents the participant from continuing without a subscription. | Debug participant APKs now set `UNLIMITED_AI_TESTING=true`, skip billing startup and usage counting, and display an unlimited testing state. Release APKs set the flag to `false` and retain the normal freemium policy. | Policy tests cover testing, paid, and production-free behavior. `ParticipantTestingAccessTest` completed 20 consecutive uses without increasing usage; the complete tablet suite passed 30/30. | **Resolved** |

## Matrix failure review

| Matrix area | Triage result |
| --- | --- |
| Scan, product result, ingredient input | No automated failure reproduced. Barcode handoff, ingredient mode, OCR sample parsing, findings display, and source-state tests passed on the tablet. Physical camera quality, permission reset, and package framing remain manual preflight items. |
| Analysis and Bitwise | M6-03 timeout, 429, invalid/HTML response, 502/503/504, and AI-unavailable simulations passed. Deterministic findings remain visible and retries are bounded. Participant debug APKs now permit unlimited assigned AI-supported cases without changing release entitlement behavior. |
| Pantry and offline persistence | Save/remove, duplicate prevention, list display, navigation, sorting, process restart, and offline/source-state checks passed. DEF-M6-04-001 remains resolved. |
| Marketplace | Navigation plus live/mock/empty/timeout/unavailable presentation checks passed. Back navigation and retry remain usable in the tested states. |
| Build and release identity | Unit tests, lint, debug APK, and instrumentation APK succeeded. DEF-M6-04-002 was corrected and verified in generated metadata. |

## Verification summary

| Check | Environment | Result |
| --- | --- | --- |
| Android unit tests | Windows 11, Gradle 9.5.0 | **205 passed, 0 failed** |
| Android lint | Debug variant | **Pass, 0 blocking errors** |
| Android application and test APK builds | Debug and release variants | **Pass** |
| Connected demo-path tests | Samsung SM-X800, Android 16 | **30 passed, 0 failed** |
| Retailer backend tests | Node test runner | **37 passed, 0 failed** |
| **Automated total** | Android unit + connected + backend | **272 passed, 0 failed** |

Detailed command, artifact, and hash evidence is in `docs/testing/evidence/m6-04/m6-04-test-results.txt`.

## Remaining noncritical limitations and pre-demo mitigations

| ID | Severity | Owner | Limitation | Safe demo mitigation |
| --- | --- | --- | --- | --- |
| **LIM-M6-04-001** | Medium | Demo operator | This run did not physically aim the camera at every TD-01 through TD-08 package condition or reset camera permission. | Before the advisor session, use the exact demo device to scan the known barcode and clear label once, deny/grant camera permission once, and keep those packages available. Use manual product entry only if the physical label is damaged. |
| **LIM-M6-04-002** | Medium | Backend/demo operator | Live Gemini and retailer availability depend on current HTTPS deployment, credentials, quota, and network. Controlled failure behavior is tested, but live success was not revalidated in this run. | Run the backend readiness check before the demo. If a provider is unavailable, continue with the visible deterministic analysis and use only the clearly disclosed development marketplace sample; do not describe sample availability as live. |
| **LIM-M6-04-003** | Low | QA | The connected UI rerun used the advisor tablet on Android 16; API 26 and a separate phone were not connected for this triage run. | Use the tested SM-X800 candidate for the advisor demo. Keep API-26/phone coverage as release follow-up rather than changing the demo device. |
| **LIM-M6-04-004** | Low | Build owner | The connected-test harness prints an `androidx.test.services` app-ops setup warning on the tablet. Tests still install, execute, and report 30/30. | No demo action is required; investigate the test-services package only if future connected tests stop launching. |

## Demo exit decision

- **Open Critical defects:** 0
- **Open High defects:** 0
- **Known blocker for the tested tablet demo flow:** None
- **Decision:** Use the rebuilt `0.31.0-rc1` candidate after completing the short physical-camera and live-backend preflight above.

## Checklist completion

- [x] Review failures from the end-to-end matrix
- [x] Assign severity and reproduction steps
- [x] Prioritize demo-blocking defects
- [x] Implement or document a safe mitigation
- [x] Retest each resolved defect
- [x] Record remaining noncritical limitations
