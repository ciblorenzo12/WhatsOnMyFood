# M3-04 - Hosted backend environment validation

Validated environment: production-compatible RunPod deployment, checked on 2026-08-06.

## Hosted service URLs

Base URL: `https://hmkmdx3vqpzid6-8000.proxy.runpod.net`

- AI analysis: `POST /v1/bitwise/analyze`
- RAG ingredient recovery: `GET /api/retail/products/:barcode/ingredients/rag`
- Liveness: `GET /health`
- Readiness: `GET /ready`

AI and RAG are intentionally hosted behind the same HTTPS origin. The Android app stores only the hosted base URLs and the application access token. Gemini and other server-provider credentials stay in the protected RunPod process environment and are never returned by health responses.

Hosted RAG recovery uses Open Food Facts as its always-available product-data provider, with optional Walmart and Amazon providers added when their server-side credentials are configured.

## Server configuration

The ignored `backend/retailer/runpod.local.env` file supplies deployment values without committing them. The deployment script writes the following values into the remote process environment:

- `GEMINI_API_KEY` - required provider credential; server only.
- `GEMINI_MODEL` - currently `gemini-3.1-pro-preview`.
- `BITWISE_APP_TOKEN` - optional rotated application access token shared with the Android build.
- `PUBLIC_BASE_URL` - hosted HTTPS origin used by readiness validation.
- `NODE_ENV=production` and `PORT`.

Never copy `GEMINI_API_KEY`, Google Play service-account material, Walmart private keys, or Amazon secret keys into Android properties, Gradle source, Java source, resources, or version control.

## Android configuration

The ignored `app/local.properties` file contains:

```properties
RETAILER_BACKEND_BASE_URL=https://hmkmdx3vqpzid6-8000.proxy.runpod.net
BITWISE_LLM_BASE_URL=https://hmkmdx3vqpzid6-8000.proxy.runpod.net
BITWISE_APP_TOKEN=<matching application token when rotated>
```

`RETAILER_BACKEND_BASE_URL` is used by RAG recovery. `BITWISE_LLM_BASE_URL` is used by protected AI analysis and falls back to the retailer URL when omitted. Release builds disallow cleartext traffic.

## Automated test solution

From the repository root:

```powershell
cd backend\retailer
npm test
.\scripts\validate-hosted-environment.ps1

cd ..\..\app
$env:ANDROID_SERIAL='<connected-device-serial>'
.\gradlew.bat testDebugUnitTest installDebug
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.HostedBackendConnectivityTest
```

The hosted validation script confirms:

1. Android AI and RAG URLs match the deployed HTTPS origin.
2. The provider key exists in the ignored server environment file.
3. Android and server local configuration files are ignored by Git.
4. The configured Gemini credential is absent from tracked repository files and the built APK; tracked Android files also contain no server-provider credential names.
5. `/health` reports the Google Gemini provider.
6. `/ready` confirms HTTPS, AI credentials, application authentication, and a RAG provider.
7. AI and RAG endpoints reject requests without the application token.
8. An authenticated RAG request for Jif UPC `051500255162` returns usable ingredients and a source.
9. The Android instrumentation smoke test reaches that RAG endpoint directly from the physical device.

## End-to-end device smoke test

1. Install the debug APK built with the hosted URLs and connect the Android device to the internet.
2. Open an uncached pantry product that already has deterministic ingredients and rule findings.
3. Confirm product identity, ingredients, rating, and deterministic findings appear before AI finishes.
4. Confirm Bitwise returns a Pro explanation and at least one clickable scientific source.
5. Scan or open a product with no usable database ingredients and confirm RAG recovery either supplies normalized ingredients with a source or fails without blocking product details.
6. Disconnect the network, open another uncached product, and confirm the friendly fallback leaves deterministic results visible; reconnect and tap to retry.

Pass criteria: the hosted service is reachable from the device over HTTPS, Pro explanations and sources render, RAG failures do not block product details, provider credentials never appear in the APK or repository, and no crash or indefinite loading occurs.

## Trello ticket test solution

Automated: run `npm test`, `scripts/validate-hosted-environment.ps1`, `gradlew testDebugUnitTest installDebug`, and the `HostedBackendConnectivityTest` instrumentation test. Expected: backend and Android tests pass; `/health` reports `google-gemini`; `/ready` confirms HTTPS, provider credential, app authentication, and RAG provider; protected AI/RAG calls without the app token return 401; authenticated RAG recovers Jif ingredients from Open Food Facts on both the host and physical Android device; tracked Android files contain no server-provider credential names.

Manual device: install the hosted debug build, open an uncached product, verify deterministic details remain visible, then confirm the Pro Bitwise explanation and scientific sources appear. Test a missing-ingredient product for RAG recovery and repeat once offline to confirm friendly fallback and tap-to-retry without losing product results.
