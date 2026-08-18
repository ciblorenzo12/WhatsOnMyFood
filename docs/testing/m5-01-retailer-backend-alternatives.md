# M5-01 - Retailer backend alternatives test guide

## What this feature does

The marketplace sends the scanned product's barcode, name, brand, category, and optional location to the retailer backend. The backend returns retailer availability and alternative products. It also identifies whether each result came from a live provider or the development mock provider.

Retailer credentials stay on the backend. The Android app contains only the backend base URL and never declares Walmart or Amazon retailer secrets.

## Which console to use

Use the **Terminal** tab in Android Studio or Windows PowerShell. Start in the repository folder named `YourHealtyPantry`.

## Automated test 1 - Android request and safe parsing contract

From the repository root, run:

```powershell
cd app
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.retail.RetailerBackendContractTest" --tests "com.ciblorenzo.whatsonmyfood.retail.MarketplacePresentationTest"
```

Expected result:

```text
BUILD SUCCESSFUL
```

This proves that:

- the alternatives URL includes the barcode, product name, brand, category, ZIP code, latitude, and longitude;
- the request uses the protected backend URL instead of a retailer API URL;
- the Android build does not declare Walmart or Amazon retailer credentials;
- incomplete retailer fields use safe fallback text instead of crashing; and
- missing product links are not treated as clickable retailer links.

## Automated test 2 - Backend response contract

Return to the repository root and run:

```powershell
cd ..\backend\retailer
npm test
```

Expected result: the final summary reports `fail 0`.

The retailer-service tests confirm that every alternative receives a `providerName`, the response reports `live`, `mock`, `mixed`, or `empty`, and null or incomplete provider results are safe.

## Direct backend test

### Console 1 - Start the local backend

From `backend\retailer`, run:

```powershell
npm start
```

Leave this console open.

### Console 2 - Request a supported product

Open a second PowerShell window in `YourHealtyPantry` and run:

```powershell
$response = Invoke-RestMethod "http://localhost:8787/api/retail/products/012000161155/alternatives?productName=Cola&brand=Example&category=soft%20drinks&zip=32789&lat=28.60&lng=-81.30"
$response | ConvertTo-Json -Depth 6
```

Confirm that:

1. `barcode` is `012000161155`.
2. `resultMode` is `mock`, `live`, or `mixed`.
3. `results` contains alternative products.
4. Every returned item has a non-empty `providerName`.
5. The response does not contain a retailer API key, private key, client secret, or refresh token.

Pass result: a supported product returns alternatives through the backend and exposes only safe result data.

## Android device test

1. For an emulator, set this value in the ignored file `app/local.properties`:

   ```properties
   RETAILER_BACKEND_BASE_URL=http://10.0.2.2:8787
   ```

   For a physical phone, use the computer's LAN IP instead of `10.0.2.2`.

2. From `YourHealtyPantry\app`, run:

   ```powershell
   .\gradlew.bat installDebug
   ```

3. Open the app and scan or reopen a soda, cereal, yogurt, snack, bread, pasta, sauce, nut butter, milk, or beverage product.
4. Open **Marketplace Comparison**.
5. Confirm that alternative cards appear.
6. Confirm each card identifies `LIVE PROVIDER` or `DEVELOPMENT SAMPLE`.
7. Confirm prices, distance, and missing brand information use readable fallback text.
8. Tap a card with a retailer link and confirm it opens. Confirm a card without a link does nothing and does not crash.
9. Use the back arrow and confirm the product-detail screen remains available.

Pass result: the Android screen displays the backend alternatives, remains safe with incomplete records, and keeps retailer credentials off the device.

## Credential boundary check

From the repository root, run:

```powershell
rg -n "WALMART_CONSUMER_ID|WALMART_PRIVATE_KEY|AMAZON_SP_API_CLIENT_SECRET|AMAZON_SP_API_REFRESH_TOKEN" app
```

Expected result: no Android source or configuration declaration is returned. Retailer credential names may appear under `backend\retailer`, where they belong.

## Final pass condition

M5-01 passes when both automated test groups succeed, the direct backend request returns alternatives with source metadata, the Android marketplace displays them, navigation still works, and no retailer credential is present in the Android application.
