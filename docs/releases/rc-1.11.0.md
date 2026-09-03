# What's On My Food RC 1.11.0

## Release purpose

RC 1.11.0 is the Milestone 7 study build. It gives the next group of participants one stable Android package that includes the new food recall experience. This is a controlled study release candidate, not a public production release.

## What is included

- Users can check recalls from either a newly scanned product or a product already saved in the pantry.
- The app sends product details to the protected backend, which checks the trusted openFDA recall source without placing the openFDA API key in the Android application.
- Recall matching uses exact UPC evidence when it is available and careful product-name and brand matching when it is not. Completed recall records are excluded from active alerts.
- A possible match opens a clear warning with the affected product details, recall reason, classification, dates, and official FDA source.
- No-match, unavailable, incomplete-data, and retry states use plain language. A failed service never becomes a false “safe” result.
- Milestone 7 recall tests cover scanned products, saved products, matching rules, presentation states, and backend responses.

## Build identity

- **Release label:** `RC 1.11.0`
- **Version name:** `1.11.0`
- **Version code:** `14`
- **Package:** `com.ciblorenzo.whatsonmyfood`
- **Minimum Android version:** Android 8.0 (API 26)
- **Target Android version:** Android 15 (API 35)
- **Release APK:** `app/build/outputs/apk/release/WhatsOnMyFood-RC-1.11.0.apk`
- **APK size:** 80,083,226 bytes
- **SHA-256:** `BA37E1B91A064F1956D15555ABA25A96BA5755710E374AFAD409A74386374D05`

## Verification summary

- 232 Android unit tests passed.
- Android debug lint passed without a blocking error.
- Debug, instrumentation-test, and signed release APKs built successfully from a clean build directory.
- The signed APK passed Android APK Signature Scheme v2 verification and has one expected signer.
- 43 retailer-backend tests passed.
- The hosted backend reported healthy Gemini and openFDA services, and its RAG and recall smoke checks passed.
- The hosted-environment check confirmed that the backend-only Gemini and openFDA credentials are not stored in tracked project files or the Android APK.

## Study test product

Open Food Facts contains Kroger Medium Grade A Eggs under UPC `011110609021` (canonical EAN `0011110609021`). The FDA data includes an ongoing Class I recall entry for this product. Testers must still compare the package plant number and date range shown in the official recall because the barcode alone does not prove that a specific carton is affected.

## Known limitations

- No Android device or emulator was connected while this candidate was built, so the instrumentation test APK compiled successfully but was not executed on a device.
- The recall check requires network access and the hosted backend. When it is unavailable, the app shows an honest unavailable state and provides the official FDA recall link.
- FDA recall data can change after this build. Study staff should confirm the test product before each session.
- The remaining install and smoke test on the study Android device must be completed before this card is moved to Done.
