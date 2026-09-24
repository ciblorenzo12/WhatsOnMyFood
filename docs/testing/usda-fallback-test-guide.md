# USDA FoodData Central fallback: setup and test guide

## What this change does

The app tries Open Food Facts first. When it needs another product source, it asks
our backend to look up the barcode in USDA FoodData Central. Other existing fallback
providers still run when USDA cannot supply the product. The USDA key stays on the
backend; it is not part of the Android build.

This guide separates repeatable tests using fixed responses from optional checks
against the live USDA database. A live product can change or disappear, so a live
lookup is not a substitute for the fixed-response tests.

## 1. Configure a local backend safely

If the key is already configured on your backend, leave it there and use this section
only when you need a separate local test server. Do not copy the key into the app.

Use Node.js 18 or newer. Open PowerShell in the repository root, then run:

```powershell
Set-Location .\backend\retailer

function Set-ProcessSecret {
    param([Parameter(Mandatory = $true)][string]$Name)
    $usdaSecureValue = Read-Host "Enter $Name (hidden)" -AsSecureString
    $usdaSecretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($usdaSecureValue)
    try {
        [Environment]::SetEnvironmentVariable(
            $Name,
            [Runtime.InteropServices.Marshal]::PtrToStringBSTR($usdaSecretPointer),
            'Process'
        )
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($usdaSecretPointer)
        $usdaSecureValue.Dispose()
    }
}

Set-ProcessSecret -Name FDC_API_KEY
Set-ProcessSecret -Name BITWISE_APP_TOKEN
node src/server.js
```

Enter your USDA key at the first hidden prompt and the backend application token at
the second. Use the same application token configured for the Android app. The key
is held in the local process environment while this terminal remains open; the
commands do not write it to a file or command history. Do not print the environment
or share diagnostic output that contains credentials.

Keep this terminal open while testing. Press **Ctrl+C** when finished, then close
the terminal to discard its process environment. The server does not read a `.env`
file automatically. Do not put `FDC_API_KEY` in Android `local.properties`.

For a hosted backend, enter the key and application token through the host's secret
or environment settings, then restart the service. Use HTTPS for the hosted URL.
This guide does not imply that a deployment or live-key test has already happened.

If you use `backend/retailer/scripts/deploy-runpod.ps1`, its USDA key prompt is masked.
The script first checks its local configuration and the current process environment.
If it asks for the key, enter it to enable USDA. **A blank answer explicitly disables
USDA in that deployment; it does not retain an existing server key.**

The backend's `/health` and `/ready` responses include `usdaKeyConfigured`. This
boolean reports the presence of a nonblank, non-demo value without revealing it. It does not
prove that the credential is valid, and USDA is not required for overall readiness.

## 2. Run the repeatable tests

These tests use fixed responses, need no real USDA key, and must not contact the
live USDA service.

From the repository root, run the backend suite:

```powershell
Push-Location .\backend\retailer
try {
    npm test
} finally {
    Pop-Location
}
```

Expected result: the test runner exits successfully with no failed tests.

For only the USDA proxy tests, run this from `backend/retailer`:

```powershell
node --test test/foodDataCentralProxy.test.js
```

Run the Android unit tests and build a debug APK:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest assembleDebug
```

Expected result: **BUILD SUCCESSFUL**. Read the unit-test report at
`app/build/reports/tests/testDebugUnitTest/index.html` for the individual test results.

For just the Android USDA client checks, run:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.FoodDataCentralClientTest"
```

The backend allows up to 9 seconds for the USDA request. The Android client has an
18-second total call limit. A failure should return control to the existing lookup
flow, not keep it waiting indefinitely.

The controlled checks should cover these outcomes:

| Test case | Expected result |
| --- | --- |
| Exact branded-product barcode | The backend returns the matching product, `status: 1`, and source `USDA FoodData Central`. Android accepts the product. |
| UPC with equivalent leading-zero GTIN representation | The exact product still matches after normalization. |
| Similar product name but different barcode | The product is rejected; no fuzzy replacement is shown. |
| Matching barcode on a record outside the United States market | It is not accepted as the US branded-product match. |
| Invalid barcode length, characters, or check digit | HTTP 400; the backend does not call USDA. |
| Missing or incorrect application token | HTTP 401; the request is rejected without sending the USDA key or calling the provider. |
| Missing backend USDA key, or a configured demo key | HTTP 503 instead of an anonymous or demo-credential request. |
| No exact USDA result | HTTP 200 with `status: 0`; Android can continue to its next provider. |
| Missing nutrient | The missing value stays unknown, not zero. |
| Total sugar and added sugar supplied separately | The values remain in their own fields; total sugar is not copied into added sugar. |
| Sodium in milligrams | The normalizer converts it only to the unit expected by the app. |
| Nutrition supplied per 100 mL | It is not treated as per 100 g. |
| Missing serving-unit basis | Per-100-g nutrient values remain unknown; the response records the limitation. |
| HTTP 429 from the provider | A bounded rate-limit response; no retry loop or credential disclosure. |
| Provider timeout | HTTP 504; Android can continue its fallback flow. |
| Malformed or unusable provider response | HTTP 502; no crash or invented product data. |

## 3. Make one optional live request

Keep the backend terminal running. Open a **second PowerShell terminal**. Repeat the
`Set-ProcessSecret` function definition from section 1, then run:

```powershell
Set-ProcessSecret -Name BITWISE_APP_TOKEN
$usdaTestBarcode = Read-Host 'Enter the barcode from a product you have'
$usdaTestUrl = 'http://localhost:8787/v1/food-data/usda?barcode=' +
    [Uri]::EscapeDataString($usdaTestBarcode)
$usdaTestResult = Invoke-RestMethod -Method Get -Uri $usdaTestUrl -Headers @{
    'X-APP-TOKEN' = $env:BITWISE_APP_TOKEN
}
$usdaTestResult | ConvertTo-Json -Depth 10
```

This terminal only needs the application token, not the USDA key.

If `status` is **1**, compare the returned barcode, brand, product name, ingredients,
serving units, and available nutrients with the package in your hand. Source should
be **USDA FoodData Central**. Check the response's provenance, nutrient basis, and
warnings as well. The basis is `per100g`, `per100ml_not_converted`, or `unknown`.
Only `per100g` permits numbers in the app's per-100-g nutrition fields.
If `status` is **0**, no exact US branded-food match was available; that is an expected
outcome, not a server failure. Do not assume a specific store product will always
exist in the live dataset.

For a hosted check, replace only `http://localhost:8787` with your actual HTTPS
backend URL. Never append the USDA key to this client-facing URL.

## 4. Check authentication and invalid input

Using the second terminal and a running local backend:

1. Call the same URL without the `-Headers` argument. Expect an authentication
   rejection, with no product or credential in the response.
2. Restore the header and set `$usdaTestBarcode = 'not-a-barcode'`. Rebuild
   `$usdaTestUrl` using the command above, then call it. Expect HTTP **400**.
3. For a missing-key test, use a separate local test process without `FDC_API_KEY`.
   Expect HTTP **503**, not an anonymous USDA request. Do not remove a key from a
   shared or production server to simulate a failure.

PowerShell displays an error for non-2xx responses. That is expected for these tests.
Do not deliberately exhaust the live USDA quota or disable a shared service to
produce 429, 502, or 504. Use the controlled tests in section 2 for those cases.

## 5. Check the app

1. Configure the debug app's backend URL and matching application token. For the
   Android emulator, the local computer is `http://10.0.2.2:8787`. On a physical
   device, use your hosted HTTPS URL, or use `adb reverse tcp:8787 tcp:8787` with
   `http://localhost:8787` in a debug build. Remote plain HTTP is rejected.
2. Install the new debug build and scan a product.
3. If Open Food Facts already supplies a usable result, expect that source. USDA is
   a fallback, so it is not requested simply because it is configured.
4. To verify the USDA branch reliably, use the Android controlled-response tests
   from section 2. For a manual live check, use a product that lacks a usable Open
   Food Facts result but has an exact USDA record; availability is not guaranteed.
5. When USDA supplies the product, check the product identity, ingredients, available
   nutrition, and source information. The source panel identifies **USDA FoodData
   Central** and explains that the record contains manufacturer-supplied label data,
   not a USDA endorsement. An mL/unknown-basis or empty-nutrition record should also
   show the warning that usable per-100-g values are unavailable. A missing photo or
   nutrient should not crash the app or become an invented value.
   If total sugar, saturated fat, or sodium/salt are missing, the app must not show
   a full numeric score or an unsupported Healthy label. It explains the missing
   data, keeps any known negative findings, and does not reuse an old AI explanation.
   AI output must not replace the database nutrition values.
6. Confirm that back navigation and the existing product workflow still work.

A saved product can be returned from the app's cache without a new provider request.
Use **Update product** on the product screen, or a fresh disposable test installation, before
deciding that the fallback did not run. Do not clear the user's real pantry just to
test the API. If USDA fails, another existing provider or the app's unavailable-data
flow may supply the final result; do not expect an HTTP error screen in the app.

Newly saved USDA source information also appears when reopening the cached product
online or offline. Older saved records do not acquire a guessed provider name;
refresh them to record their source. The local `UsdaSourceAttributionTest` and
`UsdaCacheMigrationDefinitionTest` cover source labels and migration definitions.
The separate Android device test `UsdaCacheMigrationTest` checks the database
upgrade; compiling it is not the same as running it on a device.

## 6. Record what actually passed

Verified product example (September 22, 2026): **Kar's Nut 'N Berry Mix**,
UPC **077034085228**, [USDA record 2105222](https://fdc.nal.usda.gov/food-details/2105222/nutrients).
The official public search and detail API confirmed that barcode, US market, and
gram serving basis. This verifies a sample record, not your backend key or deployment.
It may also exist in Open Food Facts, so a normal scan does not force the USDA path.
To check the USDA endpoint specifically, use that UPC in section 3 after deployment.

Record the date, app build, backend version, test command, barcode used for any live
check, and result. Mark unrun device, live-key, or deployment checks as **Not run**.
Never include keys, application tokens, or full environment dumps in the evidence.

This integration improves the number of sources available. It does not establish
that every USDA record is more accurate than Open Food Facts, or validate the AI's
explanations. The app's source-quality indicators are not measured factual accuracy.
USDA notes that branded-food data come from industry providers, may be incomplete,
and can reflect label rounding. Its data may be supplied per 100 g or per 100 mL;
those bases are not interchangeable without appropriate conversion information.

Official documentation: [USDA API guide](https://fdc.nal.usda.gov/api-guide/) and
[USDA branded-food documentation](https://fdc.nal.usda.gov/GBFPD_Documentation/).
