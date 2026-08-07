# M3-04 - Hosted backend environment test guide

## What this test proves

This test confirms that the Android app can reach the hosted AI and RAG services over HTTPS without storing provider credentials in the app or repository. It also verifies health, readiness, authentication, device connectivity, and an end-to-end product request.

Validated environment: production-compatible RunPod deployment, checked on 2026-08-06.

## Hosted service information

Base URL:

```text
https://hmkmdx3vqpzid6-8000.proxy.runpod.net
```

Available routes:

- AI analysis: `POST /v1/bitwise/analyze`
- RAG recovery: `GET /api/retail/products/:barcode/ingredients/rag`
- Liveness: `GET /health`
- Readiness: `GET /ready`

AI and RAG share the same HTTPS origin. Gemini and other provider credentials remain in the protected RunPod environment. They must never be copied into Android properties, Java source, resources, an APK, or Git.

## Which console to use

Use Windows PowerShell or the Android Studio terminal. Start from the repository root, `YourHealtyPantry`. If the Android Studio terminal starts inside `app`, run `cd ..` first.

For the physical-device test, USB debugging must be enabled and the device must appear when you run `adb devices`.

## Configuration required before testing

The ignored file `app/local.properties` should contain:

```properties
RETAILER_BACKEND_BASE_URL=https://hmkmdx3vqpzid6-8000.proxy.runpod.net
BITWISE_LLM_BASE_URL=https://hmkmdx3vqpzid6-8000.proxy.runpod.net
BITWISE_APP_TOKEN=<matching application token>
```

The ignored file `backend/retailer/runpod.local.env` supplies deployment values such as:

- `GEMINI_API_KEY` - server only;
- `GEMINI_MODEL=gemini-3.1-pro-preview`;
- `BITWISE_APP_TOKEN`;
- `PUBLIC_BASE_URL`;
- `NODE_ENV=production`; and
- `PORT`.

Do not print or copy credential values into test screenshots or Trello comments.

## Automated test 1 - Backend suite

1. Open a terminal at the repository root.
2. Run:

   ```powershell
   cd backend\retailer
   npm test
   ```

3. Confirm the summary reports zero failed tests.

## Automated test 2 - Hosted environment validation

From the same `backend\retailer` console, run:

```powershell
.\scripts\validate-hosted-environment.ps1
```

### Expected result

The script confirms that:

1. Android AI and RAG URLs match the hosted HTTPS origin.
2. Required server credentials exist in the ignored environment file.
3. Local Android and server configuration files are ignored by Git.
4. Provider secrets are absent from tracked files and the built APK.
5. `/health` reports the configured provider.
6. `/ready` confirms HTTPS, authentication, AI configuration, and a RAG provider.
7. Protected AI and RAG requests without the app token return HTTP 401.
8. An authenticated RAG request returns usable ingredients and a source.

If the script reports a failure, read the first failing check, correct that configuration, and run the script again. Never paste a credential into the console to demonstrate the fix.

## Automated test 3 - Android build and unit tests

1. Return to the repository root:

   ```powershell
   cd ..\..
   ```

2. Enter the Android project and run the tests:

   ```powershell
   cd app
   .\gradlew.bat testDebugUnitTest installDebug
   ```

3. Confirm the console ends with:

   ```text
   BUILD SUCCESSFUL
   ```

## Automated test 4 - Physical-device connectivity

1. Keep the phone connected by USB with USB debugging enabled.
2. Check the connection:

   ```powershell
   adb devices
   ```

3. If only one device appears, run:

   ```powershell
   .\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.HostedBackendConnectivityTest
   ```

4. If several devices appear, select the correct serial first:

   ```powershell
   $env:ANDROID_SERIAL="<device-serial-from-adb-devices>"
   .\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.HostedBackendConnectivityTest
   ```

### Expected result

The test must end with `BUILD SUCCESSFUL`. It confirms the physical Android device can reach the hosted protected RAG service over HTTPS.

## Manual end-to-end device test

1. Install the debug build containing the hosted URLs.
2. Connect the phone to the internet.
3. Open an uncached product that already has ingredients and deterministic findings.
4. Confirm the product name, ingredients, rating, and deterministic findings appear before Bitwise finishes.
5. Confirm the Pro Bitwise explanation and at least one scientific source appear.
6. Open a product with no usable database ingredients.
7. Confirm RAG either supplies normalized, source-identified ingredients or fails without blocking the product details.
8. Disconnect the phone from the network.
9. Open another uncached product.
10. Confirm friendly fallback wording appears while deterministic results remain visible.
11. Reconnect and use the retry action.

### Pass criteria

- The device reaches the hosted service over HTTPS.
- Pro explanations and scientific sources render.
- RAG failure does not block product details.
- No provider credential appears in the APK, repository, console output, or screenshots.
- No crash or indefinite loading occurs.

## Evidence to capture for Trello

- Backend test summary with zero failures.
- Final success output from `validate-hosted-environment.ps1` without credential values.
- Android `BUILD SUCCESSFUL` output.
- Physical-device connectivity test success.
- Product screen showing the Pro explanation and scientific source.
- Product screen showing deterministic information preserved during offline fallback.
