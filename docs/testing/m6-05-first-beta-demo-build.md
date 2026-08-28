# M6-05 - Prepare FIRST BETA demo build

## Ticket

- **Milestone:** M6
- **Estimate:** 2 hours
- **Target:** Week 6
- **Labels:** M6, All features, Demo, High
- **Target release:** `1.0.0-beta1`
- **Owner:** Project team
- **Prepared:** August 28, 2026
- **Base commit:** `ffd66f6`
- **Target device:** Samsung SM-X800 tablet, Android 16

## Accepted Beta 1.0 build

The first Beta 1.0 build uses version code 13 and version name `1.0.0-beta1`. The clean pipeline builds a debug APK for connected verification and a separately signed release APK for distribution. Both variants keep unlimited AI testing enabled for the advisor walkthrough and assigned participant cases.

| Artifact | Purpose | Result |
| --- | --- | --- |
| `app/build/outputs/apk/release/WhatsOnMyFood-1.0.0-beta1.apk` | Signed Beta 1.0 distribution build | **Built and signature-verified** |
| `app/build/outputs/apk/debug/app-debug.apk` | Functionally equivalent on-device rehearsal build that preserves the tablet's existing signed-in debug data | **Built and installed** |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | Connected demo-path verification | **Built** |

Release APK evidence:

- Size: 80,031,401 bytes
- SHA-256: `41DDBDD4D4C255C4AA4FAB58EADC3289C5ADB0D95749C12C6EE3D9AF50A3B297`
- Release certificate SHA-256: `41a6c16a81e8df9bd542a17ffcad597e0a7cb08b34f2e4860690677c27007e23`
- Metadata: version code 13, version name `1.0.0-beta1`

The release and debug APKs use different certificates. The tablet rehearsal uses the Beta debug APK to preserve the current signed-in demo account and local pantry state instead of uninstalling the app and erasing that data.

## Reliable sample products

These products were selected because the repository contains matching licensed ingredient-label images and their Open Food Facts records returned product identity plus ingredient text during the August 28 preflight.

| Order | Product | Barcode | Demo purpose | Local backup label |
| --- | --- | --- | --- | --- |
| 1 | Cheerios | `016000275287` | Primary scan, readable ingredients, analysis, pantry, and marketplace handoff | `016000275287-Cheerios-ingredients.jpg` |
| 2 | Nutella | `3017620422003` | Strong contrast case for high sugar and ultra-processing concerns | `3017620422003-Nutella-ingredients.jpg` |
| 3 | LIFEWTR pH Balanced | `012000161155` | Short ingredient list and positive/neutral finding explanation | `012000161155-LIFEWTR_pH_balanced-ingredients.jpg` |
| 4 | Doritos Nacho Cheese | `028400090896` | Backup processed-food case with a longer ingredient list | `028400090896-Doritos_Nacho_Cheese-ingredients.jpg` |

Keep the physical Cheerios and Nutella packages beside the tablet. The image assets are OCR evidence and a label-reading backup; they do not replace the physical barcode scan during the main walkthrough. Because public product records can change, scan Cheerios once and confirm its name and ingredients immediately before the session.

## Concise walkthrough sequence

Target duration: 8-10 minutes.

| Time | Action | What to explain | Exit condition |
| --- | --- | --- | --- |
| 0:00 | Launch Beta 1.0 on the SM-X800 with Wi-Fi enabled. | This beta permits unlimited assigned AI scans and wakes the protected AI service during startup. | Home screen is responsive. |
| 0:40 | Open Barcode mode and scan Cheerios once. | The scan gate prevents duplicate navigation; product lookup does not invent missing data. | Correct product details open. |
| 1:40 | Review the overall result, Nutri-Score/NOVA/Eco-Score, ingredients, nutrition, and deterministic findings. | The visible result is rule-based; Bitwise explains it but does not replace it. | Product evidence agrees with the package/source record. |
| 2:40 | Open one GOOD or WATCH finding and its source, then read the Bitwise card. | Findings remain usable even if AI is delayed. Sources are shown when validated. | Explanation or clear bounded fallback is visible. |
| 3:40 | Add the product to Pantry, open Pantry, and reopen the saved row. | Saving is explicit, duplicate-safe, and survives navigation. | The same barcode reopens once. |
| 4:40 | Open Compare alternatives and show one comparison card. | Live, mixed, and development sample results are labeled honestly. | Marketplace remains navigable and Back returns to the product. |
| 5:40 | Scan Nutella and compare its concerns with Cheerios. | The verdict follows the product evidence rather than an unrelated positive attribute. | Sugar/processing concerns are readable. |
| 6:50 | Switch to Ingredients mode and frame a clear label or the licensed Cheerios label image. | OCR supports products whose lookup record lacks usable ingredients; the user reviews captured text before analysis. | Parsed ingredient text is readable. |
| 8:00 | Show the recorded AI-unavailable/offline evidence. | External services can fail without hiding the product or deterministic findings. | Retry wording and preserved findings are visible. |
| 9:00 | Return Home and summarize. | Beta 1.0 covers scan, explain, save, compare, OCR, and safe fallback workflows. | Demo ends on a stable screen. |

## Pre-demo preflight

Complete these checks 10-15 minutes before the advisor session:

1. Charge the tablet, disable disruptive notifications, unlock it, and keep it awake.
2. Confirm Wi-Fi and the hosted backend readiness endpoint.
3. Launch the app once so the Bitwise warm-up request starts.
4. Scan Cheerios and confirm identity, ingredients, and a completed product result.
5. Confirm Bitwise returns or reaches the documented safe fallback without hiding findings.
6. Open Pantry and Marketplace, then return to the scanned product.
7. Keep Nutella, the local OCR label images, and the fallback screenshots available.
8. Do not describe a development marketplace sample as current store inventory.

## Current screenshots and fallback evidence

The deterministic debug preview activities use production layouts and controlled data. They make the evidence repeatable without changing the live backend during the demonstration.

| Evidence | File | Demonstrates |
| --- | --- | --- |
| Complete product result | `docs/testing/logs/m6-05/product-details-complete.png` | Overall result, findings, Bitwise area, ingredients, and product actions |
| Pantry with products | `docs/testing/logs/m6-05/pantry-populated.png` | Saved-product list and reopen path |
| Marketplace comparison | `docs/testing/logs/m6-05/marketplace-comparison.png` | Labeled comparison cards and navigation |
| AI unavailable fallback | `docs/testing/logs/m6-05/ai-unavailable-fallback.png` | Deterministic findings remain visible with understandable retry wording |

## Verification and rehearsal record

| Check | Result | Evidence |
| --- | --- | --- |
| Android unit tests | **Pass - 207/207** | Clean `testDebugUnitTest` run |
| Android lint | **Pass** | Clean `lintDebug` run; no blocking error |
| Debug, test, and signed release APKs | **Pass** | Clean Gradle build and APK metadata |
| Retailer backend tests | **Pass - 37/37** | Node test runner |
| Target-tablet demo-path suite | **Pending final unlocked rerun** | Initial run was invalidated by the Samsung lock screen; no product assertion was accepted from that run |
| Complete operator rehearsal | **Pending final unlocked run** | Use the sequence above on the SM-X800 |

Detailed hashes, commands, and final device totals are recorded in `docs/testing/logs/m6-05/m6-05-test-results.txt`.

## Checklist

- [x] Create a clean beta build
- [x] Select reliable sample products and barcodes
- [ ] Capture current screenshots of key workflows
- [x] Prepare test results and fallback evidence plan
- [x] Write the walkthrough sequence
- [ ] Run one complete rehearsal on the target device

