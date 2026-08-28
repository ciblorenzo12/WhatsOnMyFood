# M6-01 - End-to-end test matrix

## Ticket

- **Milestone:** M6
- **Estimate:** 2 hours
- **Target:** Week 6
- **Labels:** M6, All features, Verification, High
- **Target release:** `v0.31.0-rc1`
- **Purpose:** Verify every critical scan, analysis, pantry, and marketplace path before the advisor demo.
- **Matrix owner:** Project team
- **Last updated:** August 24, 2026

## Status rules

Every case must use exactly one of these values.

| Status | Meaning |
| --- | --- |
| **Not run** | The case is ready but has not been executed on the named environment. |
| **Pass** | The actual result matches every expected outcome and the evidence is linked. |
| **Fail** | At least one expected outcome was not met. Record a defect and preserve the failure evidence. |
| **Blocked** | An external prerequisite prevents execution, such as unavailable credentials, device, or service. Record the blocker; do not report it as a failure or pass. |
| **Retest** | A failed case has a proposed fix and is waiting for, or actively receiving, the same test again. After execution, change it to **Pass** or **Fail**. |

The initial status is **Not run** because this ticket creates the shared matrix; it does not claim that an advisor-demo test run has already occurred.

## Required environments

| ID | Device or environment | Use |
| --- | --- | --- |
| **D1** | Physical Android phone with rear camera; API 26-35; actual model and OS recorded in the run log | Primary barcode and ingredient-label journeys |
| **D2** | Samsung SM-X800 tablet, Android 16, or the current advisor-demo tablet | Large-screen layout, navigation, and persistence regression |
| **E1** | Android emulator at API 26, the app's minimum supported API | Baseline compatibility and controlled non-camera states |
| **S1** | Protected backend online with matching app token and test Gemini configuration | AI explanation and ingredient-recovery success paths |
| **S2** | Controlled backend or debug preview able to return success, 429, 503, malformed, delayed, empty, mock, and incomplete responses | Repeatable resilience and marketplace state tests |
| **N1** | Stable Wi-Fi or cellular connection | Online product, AI, and marketplace tests |
| **N2** | Network disabled after a result has been cached | Offline and persistence tests |

Before a run, record the APK commit, app version, backend commit and URL, device model, Android version, account, and network state. Use a clean test account unless a case explicitly requires saved data.

## Controlled test data

| ID | Test data | Purpose |
| --- | --- | --- |
| **TD-01** | A known retail barcode with complete product identity, ingredients, and nutrition | Normal barcode-to-result path |
| **TD-02** | A known barcode whose product record has identity but no usable ingredients | Supplemental label and protected RAG recovery |
| **TD-03** | A high-added-sugar product and a product with positive, informational, and warning findings | Verdict and complete findings coverage |
| **TD-04** | Clear, well-lit English ingredient label from the packaged product | Normal ingredient-label OCR path |
| **TD-05** | Long or curved ingredient label containing punctuation, allergens, and line breaks | OCR normalization and deduplication |
| **TD-06** | Blurred, cropped, glare-covered, front-of-package, or otherwise invalid label image | Rejection and retry behavior |
| **TD-07** | Product with barcode, name, brand, and category supported by the alternatives handoff | Marketplace success path |
| **TD-08** | Product missing a usable barcode or name | Marketplace unavailable path |
| **TD-09** | Signed-in test account with an empty pantry | Pantry save, duplicate, sort, removal, and empty state |

For repeatable OCR checks, the ten licensed images under `app/src/androidTest/assets/openfoodfacts-ingredients/` may supplement physical packages. Record the exact filename or physical barcode used.

## End-to-end matrix

### A. Scan to product result

| ID | Priority | Input and environment | Procedure | Expected outcome | Required evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| **E2E-S01** | Critical | TD-01; D1 + N1 | Cold-launch the app, open Barcode mode, scan once, and wait for product details. | One accepted scan opens the matching product; name, brand, barcode, source state, ingredients, nutrition, and actions remain usable; no crash or duplicate navigation occurs. | Recording from launch through details; product screen screenshot; barcode noted in run log. | **Not run** |
| **E2E-S02** | High | TD-01; D1 + N1 | Hold the same barcode in the camera view for several seconds, return to the scanner, and scan it again. | The scan gate prevents repeated detail screens during one detection; a later intentional rescan opens the same product normally. | Recording showing one navigation per intentional scan; logcat excerpt only if privacy-safe. | **Not run** |
| **E2E-S03** | High | Unknown valid barcode; D1 + N1 | Scan a syntactically valid barcode that the configured product source cannot resolve. | The app shows an honest not-found or recovery state, does not invent product data, and permits retry or return to scanning. | Screenshot of not-found/recovery state and recording of successful return. | **Not run** |
| **E2E-S04** | High | Invalid/non-barcode target; D1 | Point Barcode mode at text and random packaging, then present TD-01. | Non-barcodes do not start a lookup; the scanner remains responsive and accepts the later valid barcode. | Continuous recording covering invalid target and subsequent valid scan. | **Not run** |
| **E2E-S05** | Critical | Camera permission reset; D1 | Deny camera permission, revisit the scanner, grant permission from the offered recovery path or system settings, and retry TD-01. | Denial is explained without a crash; after permission is granted, the camera starts and the normal result path works. | Screenshots of denial guidance and recovered scanner; recording of the successful retry. | **Not run** |
| **E2E-S06** | Critical | Cached TD-01; D1 + N2 | Complete S01 online, close the app, disable the network, relaunch, and scan or reopen the same product. | The saved product remains available, is identified as an offline or older copy, and is not presented as freshly verified. | Online and offline screenshots with source labels; relaunch recording. | **Not run** |
| **E2E-S07** | High | Uncached product; D1 + N2 | Clear the test account/app data as appropriate, disable the network, and scan a product not stored locally. | The app gives a friendly connection or unavailable state, keeps navigation usable, and does not show another product's cached result. | Screenshot and recording; test-data and cache-reset method in run log. | **Not run** |

### B. Ingredient-label input and recovery

| ID | Priority | Input and environment | Procedure | Expected outcome | Required evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| **E2E-I01** | Critical | TD-04; D1 + N1 | Switch from Barcode to Ingredients mode, frame the complete label, capture it, and continue to results. | The mode instructions change correctly; OCR returns usable ingredient text; normalization produces readable ingredients and opens analysis without requiring a barcode lookup. | Recording of mode switch, capture, and result; screenshot of parsed ingredients. | **Not run** |
| **E2E-I02** | High | TD-05; D1 + N1 | Capture a long or curved label containing line breaks, punctuation, an allergen statement, and at least one repeated ingredient. | Valid ingredients are ordered, normalized, and deduplicated; header/footer noise is not treated as an ingredient; the allergen or informational finding is retained when supported. | Label photo plus parsed-list and findings screenshots; comparison notes in run log. | **Not run** |
| **E2E-I03** | Critical | TD-06 blurred/cropped/glare image; D1 | Attempt ingredient capture with unusable text, then retake a clear image. | The invalid image does not create a confident analysis or placeholder ingredient; clear retry guidance appears; the retake can complete successfully. | Recording of rejection and successful retake; both input conditions noted. | **Not run** |
| **E2E-I04** | High | TD-06 front-of-package/non-ingredient text; D1 | In Ingredients mode, scan marketing text or the front panel. | The app does not misrepresent front-panel text as a complete ingredient analysis and offers a label-focused retry/review path. | Screenshot of guarded/retry state and the scanned panel. | **Not run** |
| **E2E-I05** | Critical | TD-02; D1 + S1 + N1 | Scan the product barcode, accept the missing-ingredients prompt, capture the package ingredient label, and reopen the product after recovery. | Product identity is preserved; recovered ingredients are normalized, deduplicated, saved, and identified as label/supporting-source data; deterministic analysis resumes. | Full journey recording; before/after ingredient and source screenshots; barcode in run log. | **Not run** |
| **E2E-I06** | High | TD-02; D1 + S2 | Return empty, malformed, 503, and delayed ingredient-recovery responses in separate iterations. | No invalid ingredients are persisted; the original product remains visible; retry is bounded and the failure wording is understandable. | One screenshot and backend request count per response; privacy-safe service log; iteration results in run log. | **Not run** |

### C. Product analysis and AI explanation

| ID | Priority | Input and environment | Procedure | Expected outcome | Required evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| **E2E-A01** | Critical | TD-03 mixed-findings product; D1 + S1 + N1 | Open the product and wait for deterministic analysis and Bitwise completion; open every finding and one source. | Health result and all applicable GOOD, WATCH, and INFO findings appear once; explanations are readable; a usable HTTP(S) source opens; AI does not replace deterministic findings. | Product and expanded-finding screenshots; source URL/domain; end-to-end recording. | **Not run** |
| **E2E-A02** | Critical | TD-03 high-sugar product; D1 + S1 + N1 | Scan the product and compare the verdict, score, findings, ingredients, and nutrition values. | The verdict reflects the high-sugar evidence and does not present the product as healthy because of an unrelated positive attribute; displayed findings agree with visible product data. | Screenshot showing verdict, sugar evidence, and relevant finding; product barcode. | **Not run** |
| **E2E-A03** | High | Controlled valid product with no triggered findings; D2 or S2 preview | Open the completed no-findings state. | The findings section explicitly says no specific findings were identified; product identity and other details remain visible; the section is not blank. | Full-screen screenshot including product identity and no-findings message. | **Not run** |
| **E2E-A04** | Critical | Product missing required analysis inputs; D1 or S2 preview | Open missing-ingredients and analysis-unavailable states separately. | The app distinguishes missing input from analysis failure, asks for ingredient capture or refresh as appropriate, and never invents a score or finding. | Screenshot for each state and actual-result notes. | **Not run** |
| **E2E-A05** | High | Direct, recovered, stale, and offline data; D2 or S2 preview | Open each source/freshness state and compare the wording. | Each state is clearly labeled; recovered data asks for package comparison; stale/offline data is not called current, verified, guaranteed, or 100% accurate. | One screenshot per state or a labeled composite; wording checklist. | **Not run** |
| **E2E-AI01** | Critical | TD-01 or TD-03; D1 + S1 + N1 | Request a Bitwise explanation from the protected backend, close the product, and reopen it. | A validated explanation and source appear and are cached; raw provider output and provider credentials never appear in the UI or client diagnostics. | Initial and reopened screenshots; redacted privacy-safe request summary; backend success record. | **Not run** |
| **E2E-AI02** | Critical | Cached explanation; D1 + N2 | Complete AI01, disable the network, close and reopen the app, and reopen the product. | The accepted explanation remains available from Room; offline/source limitations are visible; deterministic findings remain unchanged. | Relaunch recording and offline product screenshot. | **Not run** |
| **E2E-AI03** | High | TD-03; D1 + S2 | Return 503 then success; separately return three 503 responses and a delayed cold-start response. | Bitwise receives no more than two automatic retries; success appears after recovery; final failure leaves the product and deterministic findings usable with friendly retry wording. | Privacy-safe request count and product screenshot for each sequence. | **Not run** |
| **E2E-AI04** | Critical | TD-03; D1 + S2 | Test 429 with Retry-After, invalid token, malformed JSON, and explanation without a usable source. | There is no 429 retry loop; authentication/configuration errors are friendly; invalid content is neither displayed nor cached; deterministic results remain visible. | Screenshot and request count for each response; cache check after reopen. | **Not run** |

### D. Pantry

| ID | Priority | Input and environment | Procedure | Expected outcome | Required evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| **E2E-P01** | Critical | TD-01 + TD-09; D1 | From product details, tap **Add to Pantry**, open My Pantry, and select the new row. | The action progresses through Saving to **Remove from Pantry**; the product appears once with its name and score; its row opens the same barcode. | Recording of save, list, and reopen; product/list screenshots. | **Not run** |
| **E2E-P02** | High | Saved TD-01; D1 | Reopen and rescan the saved product and attempt the save path again if available. | Only **Remove from Pantry** is offered for the saved item and no duplicate membership or list row is created. | Before/after pantry screenshots and row count. | **Not run** |
| **E2E-P03** | High | Three saved products with distinct names/scores/save times; D1 | Apply Recently saved, Product name, and Health score sorts; leave and reopen the Pantry after each selection. | Each order follows its rule, missing scores appear last, ties are stable, and the chosen sort persists on return. | Screenshot for each sort and reopened state; expected order in run log. | **Not run** |
| **E2E-P04** | Critical | Saved analyzed product; D1 + N2 | Save after Bitwise completes, force-close the app, disable the network, relaunch, and open the pantry row. | Membership, exact product, normalized ingredients, health result, and saved explanation persist after process and network loss. | Recording across force-close/relaunch; offline detail screenshot. | **Not run** |
| **E2E-P05** | Critical | Saved TD-01; D1 | Tap **Remove from Pantry**, return to the list, scan the same barcode again, and optionally save it again. | The row disappears without restart; reusable product data is not corrupted; the detail screen returns to **Add to Pantry** and supports a later save. | Recording of removal, refreshed list, rescan, and action state. | **Not run** |
| **E2E-P06** | Medium | Empty TD-09; D1 and D2 | Remove all test items, open My Pantry, then save one product and return. | Empty guidance appears only when the list is empty; the populated list replaces it without overlap on phone or tablet. | Empty and populated screenshots on each required device. | **Not run** |

### E. Marketplace

| ID | Priority | Input and environment | Procedure | Expected outcome | Required evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| **E2E-M01** | Critical | TD-07; D1 + S1 + N1 | From product details, tap **Compare alternatives**, inspect the loading/result screen, and press Back. | Barcode, name, brand, and category reach the marketplace unchanged; Back returns to the same product without a crash or reset. | Recording of handoff and return; source and destination screenshots. | **Not run** |
| **E2E-M02** | High | TD-07; D1 + configured live provider | Request alternatives using a real configured provider. | Live or mixed results are honestly labeled, each card remains usable with incomplete optional fields, and no development sample is claimed as current retailer availability. | Screenshot, sanitized response `resultMode`, provider/configuration note, and product barcode. | **Not run** |
| **E2E-M03** | High | TD-07; D2 + S2 mock state | Open the development mock result. | Screen and cards say **DEVELOPMENT SAMPLE** and explain that results are simulated rather than current availability. | Full-screen screenshot containing both disclosure locations. | **Not run** |
| **E2E-M04** | High | TD-07; D1 + S2 empty response | Open alternatives with no results, then press Back. | **No alternatives found** and **Try again** are visible; the screen remains usable and Back returns normally. | Screenshot and navigation recording. | **Not run** |
| **E2E-M05** | Critical | TD-07; D1 + S2 | Run timeout and backend-error states; restore service and tap **Try again**. | Timeout and service unavailable use distinct wording; retry starts a new loading request and replaces the error with the current live, mock, or empty response. | Failure and recovered screenshots; request count; retry recording. | **Not run** |
| **E2E-M06** | High | Incomplete alternative record; D1 + S2 | Return records missing retailer, price, image, or optional metadata and inspect all cards. | Safe display values replace missing optional fields; source labels remain accurate; no card or screen crashes. | Response fixture identifier and screenshots of every incomplete card. | **Not run** |
| **E2E-M07** | High | TD-08; D1 | Open product details for a product without a usable barcode or name and inspect Product actions. | Compare alternatives is disabled or withheld and the reason is explained; an empty marketplace is not opened. | Product-actions screenshot and attempted-navigation recording. | **Not run** |

### F. Complete critical journeys

| ID | Priority | Input and environment | Procedure | Expected outcome | Required evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| **E2E-X01** | Critical | TD-03 + TD-07; D1 + S1 + N1 | Barcode scan -> product result -> complete Bitwise explanation -> save to Pantry -> reopen from Pantry -> compare alternatives -> return to product. | One product identity is preserved across every screen; analysis and explanation remain visible; pantry contains one row; marketplace receives the correct product; Back returns without data loss or crash. | One uninterrupted recording plus key screenshots at result, pantry, and marketplace. | **Not run** |
| **E2E-X02** | Critical | TD-02; D1 + S1 + N1, then N2 | Barcode scan -> missing-ingredient prompt -> ingredient-label capture -> analysis -> save -> force-close -> offline reopen from Pantry. | Supplemental ingredients attach to the original product, drive findings, persist with an honest source label, and remain available after relaunch without a network. | One recording through save and a second offline-reopen recording; before/after screenshots. | **Not run** |
| **E2E-X03** | Critical | Saved analyzed TD-01; D1 + S1 + N1 | Save the product, note local state, use **Update product data**, return to Pantry, force-close, relaunch, and reopen it. | Refreshed external fields may update, but pantry membership, favorite/rating when present, health result, and saved Bitwise explanation are not erased or copied to another barcode. | Before/after/relaunch screenshots; fields compared in run log. | **Not run** |

## Automated companion checks

Automated checks support the matrix but do not replace the physical camera, full navigation, and wording observations above.

From the repository root:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
.\app\gradlew.bat -p .\app connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.IngredientModeUiTest,com.ciblorenzo.whatsonmyfood.BarcodeProductDetailsNavigationTest,com.ciblorenzo.whatsonmyfood.OpenFoodFactsOcrSampleTest,com.ciblorenzo.whatsonmyfood.ProductFindingsDisplayFlowTest,com.ciblorenzo.whatsonmyfood.ProductSourceStatusFlowTest,com.ciblorenzo.whatsonmyfood.PantryActionDisplayFlowTest,com.ciblorenzo.whatsonmyfood.PantryDaoFlowTest,com.ciblorenzo.whatsonmyfood.PantryDisplayFlowTest,com.ciblorenzo.whatsonmyfood.PantryNavigationTest,com.ciblorenzo.whatsonmyfood.PantryRestartPersistenceTest,com.ciblorenzo.whatsonmyfood.PantrySortingDaoTest,com.ciblorenzo.whatsonmyfood.MarketplaceNavigationFlowTest,com.ciblorenzo.whatsonmyfood.MarketplaceStateDisplayFlowTest'
Push-Location backend\retailer
npm test
Pop-Location
```

Save console output or screenshots under `docs/testing/evidence/m6-01/<run-id>/` and link them from the run log.

## Test run log

Add one row for each execution session. Do not overwrite earlier runs.

| Run ID | Date/time | Tester | App commit/version | Backend commit/URL | Device/OS | Network | Cases | Result summary | Evidence folder | Defects/blockers |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `M6-RUN-001` | Not scheduled | Unassigned | To record | To record | To record | To record | To record | Not run | To create | None recorded |

## Status summary

Update this table whenever case statuses change.

| Status | Count |
| --- | ---: |
| Pass | 0 |
| Fail | 0 |
| Blocked | 0 |
| Retest | 0 |
| Not run | 38 |
| **Total** | **38** |

## Advisor-demo exit criteria

The release is ready for the advisor demo when:

1. All 38 cases have a linked evidence artifact and no case remains **Not run**.
2. Every **Critical** case is **Pass**.
3. No open **Fail**, **Blocked**, or **Retest** case can break or misrepresent a scan, analysis, pantry, marketplace, privacy, or source-disclosure path.
4. Any accepted non-critical limitation has an owner, defect ID, impact statement, and advisor-demo workaround.
5. The final run log identifies the exact APK, backend, devices, accounts, and test data used.

## Checklist coverage

- [x] Critical scan-to-result scenarios listed
- [x] Barcode and ingredient-label inputs included
- [x] Product analysis and AI explanation paths included
- [x] Pantry and marketplace paths included
- [x] Expected outcome and required evidence defined for every case
- [x] Pass, fail, blocked, retest, and not-run states defined and tracked
