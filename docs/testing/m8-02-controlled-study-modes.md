# M8-02 - Controlled static and AI-assisted mode verification

## Status

**In progress — automated checks passed; study sign-off is blocked.**

Verification date: September 8, 2026, Eastern Time (test artifacts use September 9 UTC).
Milestone: M8. Estimate: 3 hours.
Ticket: <https://trello.com/c/tnMQkLZr/39-m8-02-verify-controlled-static-and-ai-assisted-modes>.
Dependency: [M8-01 product freeze](https://trello.com/c/mbxrQcRk/38-m8-01-freeze-the-validation-product-set).

This record concerns actual code inspection and automated tests. No participant responses,
mock survey answers, or earlier milestone results are counted as evidence for this run.
No live participant session or current device test was performed.

## Recorded build

| Field | Value |
| --- | --- |
| Package | `com.ciblorenzo.whatsonmyfood` |
| Version | `1.11.0`, code `14` |
| Variant | `debug` |
| Baseline commit | `acb3ea1c643818f76b7d693e5c5750866be940d5` |
| Source revision | Baseline plus the uncommitted M8-02 patch captured under `logs/m8-02/verified-source-changes.patch` |
| Built APK | `app/build/outputs/apk/debug/app-debug.apk` |
| APK SHA-256 | `346d93aee1057ead20ab30123f0631c951ffcc2c4957b3271942419a9cf63a7d` |
| Environment | Windows, Java 25.0.1, Gradle wrapper 9.5.0, Android compile/target API 35, minimum API 26 |

This is a newly built verification APK, **not** the previously published RC release APK.
The version string alone does not identify these fixes; use the APK hash and source patch.
The signed release was not replaced, published, or installed. The machine-readable
[manifest](logs/m8-02/build-and-test-manifest.json) also records instrumentation APK and
modified source-file hashes, protocol hash, test counts, and suite timestamps.

## Procedure and comparison inputs

Reviewed `Food_Nutrition_Application_User_Test_Procedure.docx`, protocol version 1.0,
August 7, 2026. The document remains unchanged.

The procedure specifies the same P01–P20 order and task sequence for all four applications:
scan, try normal search after two unsuccessful scan attempts, confirm product identity,
review ingredients/nutrition/ratings/explanations, and make a buy/avoid/investigate decision.
Application order is counterbalanced across groups A–D. The 90-second stop rule is a
**time-to-usable-result** rule; deterministic information can become usable before AI finishes.
TC-01 is the primary participant environment; TC-02–TC-06 are separate validation cases.

| ID | Product in the supplied procedure |
| --- | --- |
| P01 | Cheerios Original |
| P02 | Honey Nut Cheerios |
| P03 | Kellogg's Frosted Flakes |
| P04 | Quaker Instant Oatmeal Original |
| P05 | Coca-Cola Original Taste |
| P06 | Diet Coke |
| P07 | Gatorade Lemon-Lime |
| P08 | Chobani Plain Non-Fat Greek Yogurt |
| P09 | Yoplait Original Strawberry Yogurt |
| P10 | Jif Creamy Peanut Butter |
| P11 | Skippy Creamy Peanut Butter |
| P12 | Lay's Classic Potato Chips |
| P13 | Doritos Nacho Cheese |
| P14 | Campbell's Condensed Chicken Noodle Soup |
| P15 | Kraft Original Macaroni & Cheese |
| P16 | Nature Valley Oats 'n Honey Bars |
| P17 | KIND Dark Chocolate Nuts & Sea Salt Bar |
| P18 | Oreo Original Cookies |
| P19 | Almond Breeze Unsweetened Original Almondmilk |
| P20 | Impossible Burger Plant-Based Patties |

**Input-freeze gap:** all 20 Frozen UPC cells say `Record before study`; package sizes
are absent. Named products and common task wording are confirmed, but identical source
records/formulations across modes cannot yet be verified. No barcodes were guessed from
product names. The [product audit](logs/m8-02/protocol-product-audit.json) records this
explicitly with null verified UPCs and package sizes.

The supplied procedure compares four applications, not two explicitly assigned runs of
What's On My Food. It does not specify how a facilitator selects static mode, restores
the same input/cache state, or records mode assignment. The technical contract below
defines the existing application's flags; it is not an amendment to the participant script.

## Expected behavior of the existing mode flags

| Check | Static (`AI_ENABLED=false` / fragment `newInstance(barcode, false)`) | AI-assisted (`true`) |
| --- | --- | --- |
| Product input | Same frozen product, package, ingredient and nutrition evidence | Same evidence at start of each paired case |
| Baseline | Product identity, source status, ingredient/nutrition fields and deterministic findings remain available | The same baseline appears before the AI explanation |
| Translation | No automatic model-based ingredient translation | Existing translation may run; freeze English input or record this difference before comparing cases |
| AI summary | Hidden, including saved AI text; no explanation request or retry | Valid cached explanation may be reused; otherwise request a validated explanation |
| Invalid/missing ingredients | Honest missing-data state and supported manual contribution | Review/capture ingredients before requesting a confident explanation |
| Offline saved product | Show saved data and freshness limitation | Same baseline; a usable saved explanation may be shown, otherwise an understandable failure |
| Offline cache miss | No fabricated product or successful result | Same requirement |
| AI/provider failure | Does not affect baseline | Keep deterministic results and usable navigation; reject invalid content and bound retries |
| Sources | Distinguish current database, saved, stale, recovered and fallback product data | Preserve that provenance in the request; accept only usable source-backed explanations |

The flag controls product-detail AI features; it is **not an app-wide network-off switch**.
Product lookup, supporting-source recovery, retail requests, and user-invoked features
have their own paths. Ordinary scan and pantry navigation default to AI enabled. The
flag alone does not constitute a facilitator-accessible, locked study mode.

## Fixes made in this verification

1. Both detail screens previously called model-based ingredient translation even when
   AI was disabled. They now gate that path on the mode flag. Direct explanation entry
   points also reject disabled-mode calls, including retry entry points.
2. Initial lookup and refresh callbacks now apply source status **before** rendering
   product details and starting analysis. This avoids sending an empty or previous
   product's provenance and subsequently overwriting an immediately reported AI failure.
3. The full activity previously omitted `Source status` from its AI context. Both screens
   now use the same formatter. Three request-level regression tests verify that offline,
   recovered/fallback, and missing provenance reach the structured request with appropriate
   uncertainty, while preserving the ingredient input.

## Executed checks

| Check | Evidence | Result and boundary |
| --- | --- | --- |
| Android unit suite | `android-checks.txt`, suite counts/timestamps in manifest | **235 passed**, zero failures/errors/skips; includes three new provenance regressions |
| Backend suite | `backend-tests.txt` | **43 passed**, zero failures |
| Fresh/stale/offline saved data and cache miss | `ProductRepositoryCacheBehaviorTest`, `ProductRepositoryRefreshPolicyTest`, `SourceStatusResolverTest` | Passed policy-level tests; physical airplane-mode transitions not run |
| Deterministic findings and AI merge/failure baseline | `ProductFindingsDisplayTest` | Passed display-model tests; no assertion of a current device UI pass |
| Saved explanation/source round trip | `AiInsightCacheTest` | Passed cache tests; static-mode exclusion inspected in both callers |
| AI content/source validation | `AiExplanationResponseValidatorTest`, backend `bitwiseGemini.test.js` | Valid, incomplete, malformed, unsafe and source-invalid content checks passed with controlled data |
| Timeouts, 429, HTML and gateway response policy | `BitwiseBackendClientTest`, `ResilientRequestPolicyTest` | Passed bounded-retry/error tests; these do not measure current hosted latency |
| Local provider fallback | Backend `bitwiseGemini.test.js` | Passed controlled fallback tests; not a live Gemini result |
| Android lint | `android-checks.txt` | Passed, no blocking lint failure |
| Debug and instrumentation APK assembly | `android-checks.txt` and manifest hashes | Passed compilation/packaging; instrumentation not executed |
| Android devices and AVDs | `device-inventory.txt`, `avd-inventory.txt` | No connected device; no configured AVD |
| P01–P20 paired execution | Supplied protocol audit | **Blocked** by unfrozen UPCs/package sizes and missing mode setup |

Commands executed from the repository root:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --console=plain
node --test backend/retailer/test/*.test.js
```

## Remaining blockers and closure checks

| ID | Blocker | Required closure evidence |
| --- | --- | --- |
| B01 | M8-01 product inputs are not frozen | Record and verify package size, UPC/source identity and the same ingredient/nutrition snapshot for each P01–P20 |
| B02 | Controlled mode administration is unspecified | Record the exact launcher/flag, assignment method and cache/reset procedure; confirm whether the card means two app modes or static competitor apps versus What's On My Food |
| B03 | No device or current paired run | Install the hash-identified build and execute the cases below; record device/OS, mode and result |
| B04 | Remaining attribution and input-parity risks in existing AI display | Verify/fix after mode contract is settled: backend `local-fallback` provider metadata is logged but discarded before rendering, cached AI is not separately labeled as cached AI, and AI can replace displayed product names/brands and fragment nutrition values. Do not claim a frozen, explanation-only comparison while these paths are active |

For B03, run the same frozen product/task input in both modes. Check static with and
without a pre-existing AI cache and with non-English ingredients; confirm the AI card
stays hidden and translation does not begin. For AI-assisted mode, check a valid response,
usable saved response, missing/invalid sources, provider failure, and retry. Compare
identity/ingredients/nutrition before and after the response and when reopening the product.

Map network checks to the supplied procedure: TC-03 uses P02/P07/P14/P20 on a weak network;
TC-04 opens a previously cached product, attempts an uncached one, reconnects and retries;
TC-06 uses a separately identified approved incomplete-data product. Preserve deterministic
results on failure and check that source indicators match the actual state. Record the
observable category and timing rather than credentials, prompts, or participant data.

The existing `ProductSourceStatusFlowTest` can supplement these checks on a device, but
it uses a debug preview screen. A passing preview test is not proof that the production
mode launcher, physical scans or hosted AI pass the paired study.

## Checklist state

- [x] Define expected behavior for the existing technical mode flags.
- [ ] Confirm exact tasks and frozen products match across administered modes — task wording and product names confirmed; identifiers and administration still pending.
- [ ] Test static content and offline fallback behavior — code inspection and unit checks passed; production device execution pending.
- [ ] Test AI responses and source indicators — controlled validation tests passed; paired live/cached/fallback UI attribution pending.
- [ ] Record the build version and resolve blockers — version/hash recorded; B01–B04 remain open.

M8-02 must remain **in Progress**, not Done. The outstanding checks are not inferred to
pass from mock responses, previous releases, successful compilation, or unit-test counts.
