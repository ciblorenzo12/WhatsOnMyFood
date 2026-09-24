# Retailer API Integration Layer

This folder is the backend-facing commerce layer for What's On My Food.

The Android app should call backend endpoints like these instead of calling retailer APIs directly:

- `GET /api/retail/products/:barcode`
- `GET /api/retail/products/:barcode/availability`
- `GET /api/retail/products/:barcode/alternatives`
- `GET /api/retail/products/:barcode/ingredients/rag`
- `POST /v1/bitwise/analyze`
- `GET /v1/food-recalls?barcode=:barcode&productName=:name&brand=:brand`
- `GET /v1/food-data/usda?barcode=:barcode`
- `GET /health`
- `GET /ready`
- `POST /v1/billing/google-play/verify`

The first provider is mock data so the app flow can ship before retailer approvals. Real providers should be added behind this backend in this order:

1. Open Food Facts product lookup
2. Kroger
3. Instacart
4. Walmart
5. Google Places for nearby store discovery only

Retailer API keys belong in backend environment variables, never in the Android app.
The availability and alternatives endpoints share the protected-service rate limit. Their
responses include `providerMode`, a normalized `resultMode` (`live`, `mock`, `mixed`,
or `empty`), and a `providerName` on each returned item so the app can label the source.
The ingredient RAG endpoint is protected by the same `X-APP-TOKEN` header and
rate limit used by the other protected application services.

## Bitwise AI with Google Gemini

Bitwise sends its prompts and optional label images to this backend. The backend then
calls Google Gemini, so the Gemini key is never included in the Android or iOS app.

Create an API key in Google AI Studio, then set these server environment variables:

```text
GEMINI_API_KEY=your-google-ai-studio-api-key
GEMINI_MODEL=gemini-3.1-pro-preview
```

`gemini-3.1-pro-preview` is the default when `GEMINI_MODEL` is omitted. It is used with
low thinking and structured output so shopper explanations remain nuanced while URL
Context verifies the selected scientific sources. If no Gemini key is configured, Bitwise
returns its deterministic local analysis so label scans still work.
`BITWISE_APP_TOKEN` is optional; if you rotate it, update the matching mobile client
configuration before deploying the backend.

The Android request includes normalized ingredients, deterministic findings, source
status, and an explicit uncertainty statement. The backend converts that data into a
source-aware prompt and instructs Gemini not to contradict deterministic findings or
overstate recovered, cached, fallback, or unknown evidence. Before a provider response
can reach the app, the backend rejects blank, HTML, malformed, incomplete, uncited, and
unsafe medical output. Android repeats the essential checks before displaying or caching
the explanation.

`/health` is a liveness check. `/ready` additionally verifies the hosted HTTPS URL,
server-side AI and food-recall credentials, application authentication, and RAG provider
without exposing credential values.

## Food recall service with openFDA

The Android application calls the protected `GET /v1/food-recalls` endpoint. This
backend builds the bounded product query and calls the official openFDA Food Enforcement
API. The response is reduced to the FDA fields used by the app before it is returned.

Set the provider key only in the backend environment:

```text
OPENFDA_API_KEY=your-openfda-api-key
```

The endpoint requires the same `X-APP-TOKEN` header as the other protected application
services. The Android build contains the backend URL and application token, but it does
not contain the openFDA provider key. If the backend key is missing, `/ready` fails and
recall requests return a safe unavailable response instead of making an anonymous call.

## USDA FoodData Central fallback

The Android product lookup tries Open Food Facts first, then asks this backend for
an exact USDA branded-product match. The remaining product providers stay available
if USDA has no match or cannot respond. The application calls
`GET /v1/food-data/usda?barcode=:barcode` with `X-APP-TOKEN`; it does not call USDA
directly or include a USDA key in the APK.

Configure `FDC_API_KEY` in the **backend process environment**. Requests must include
the application token expected by the server. If you configure `BITWISE_APP_TOKEN`,
it must match the Android backend configuration; otherwise the existing application
token behavior is unchanged.
Keep the real USDA key out of Android `local.properties`, source code, chat messages,
screenshots, and version control. This server does not automatically load `.env` files.
There is no anonymous or `DEMO_KEY` fallback when the key is missing.

For local setup, use the masked PowerShell prompts in the
[USDA setup and test guide](../../docs/testing/usda-fallback-test-guide.md).
On a hosted backend, set the values through the host's protected secret/environment
configuration and restart the service. Editing the code does not deploy it or install
the key on the server.

The endpoint checks the barcode's GTIN length and check digit, compares normalized
14-digit values, and requires a United States branded-food record. It never substitutes
a similarly named food. A successful exact match returns HTTP 200 with `status: 1`
and `source: "USDA FoodData Central"`; no exact match returns HTTP 200
with `status: 0`. Invalid input returns 400, missing configuration returns 503, rate
limiting returns 429, a provider timeout returns 504, and an unusable provider response
returns 502. Invalid application authentication is rejected before the provider is called.
The provider request has a 9-second timeout; the Android call has an 18-second total
limit so the product lookup cannot wait indefinitely on this fallback.

Nutrition parsing keeps total sugar separate from added sugar, preserves missing data
as unknown, checks nutrient units, and does not relabel values per 100 mL as per 100 g.
Records with an mL or unknown serving-unit basis do not populate per-100-g numbers.
The response includes provenance, its nutrient basis (`per100g`,
`per100ml_not_converted`, or `unknown`), and applicable warnings.
USDA is an additional data source, not a guarantee that a branded-food record or an AI
explanation is correct. Industry providers supply branded-food records, and USDA
documents missing values and label-rounding limitations. Compare the exact product
and its package label when checking accuracy.

Official references: [USDA API guide](https://fdc.nal.usda.gov/api-guide/) and
[branded-food documentation](https://fdc.nal.usda.gov/GBFPD_Documentation/).

`/health` and `/ready` expose only a `usdaKeyConfigured` boolean, never the key.
This reports whether a nonblank, non-demo value exists, not whether USDA has accepted it.
USDA is optional and does not determine the overall readiness result.

Run the mock server:

```bash
node src/server.js
```

Run the backend tests:

```bash
npm test
```

## Walmart Affiliates taxonomy smoke test

After Walmart gives you a Consumer ID, upload only your public key in the Walmart
portal and keep the private key on the backend machine.

PowerShell:

```powershell
$env:WALMART_CONSUMER_ID="your-consumer-id"
$env:WALMART_KEY_VERSION="1"
node scripts/queryWalmartTaxonomy.js
```

The script signs this Walmart Affiliates endpoint:

```text
https://developer.api.walmart.com/api-proxy/service/affil/product/v2/taxonomy
```

By default it reads your private key from:

```text
C:\Users\ciblo\.walmart-api-keys\walmart_private_key_pkcs8.pem
```

If you move the private key to another server, point the script at it:

```powershell
$env:WALMART_PRIVATE_KEY_PATH="C:\path\to\walmart_private_key_pkcs8.pem"
```

Run the backend with Walmart enabled:

```powershell
$env:WALMART_CONSUMER_ID="your-consumer-id"
$env:WALMART_KEY_VERSION="1"
node src/server.js
```

On RunPod, prefer storing the private key as a secret environment variable:

```text
WALMART_PRIVATE_KEY_PEM=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
```

Local development can keep using the default private key file path.

Then test a UPC:

```powershell
Invoke-RestMethod "http://localhost:8787/api/retail/products/012000161155"
```

For the Android emulator, add this to `app/local.properties`:

```properties
RETAILER_BACKEND_BASE_URL=http://10.0.2.2:8787
```

Use your computer's LAN IP instead of `10.0.2.2` when testing on a physical phone.
For production, set `RETAILER_BACKEND_BASE_URL` to your HTTPS RunPod/backend URL.
Do not put `WALMART_CONSUMER_ID`, `WALMART_KEY_VERSION`, or the private key in
the Android app.

## Google Play subscription verification

Bitwise Plus purchase tokens are verified on this backend before the Android app
enables paid access. Create a Google Cloud service account, grant it access to the
app in Play Console, enable the Google Play Android Developer API, and configure:

```text
GOOGLE_PLAY_PACKAGE_NAME=com.ciblorenzo.whatsonmyfood
GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID=bitwise_plus_monthly
GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL=play-billing-verifier@your-project.iam.gserviceaccount.com
GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
```

`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` can be used instead of the email and private
key variables. It accepts either the raw service-account JSON or its base64 form.
Never add that credential file or value to the repository or mobile application.

## RunPod deployment

The PowerShell deployment script packages this backend, uploads it over RunPod SSH,
starts it, and confirms that the public health check reports `google-gemini`. It uses
the RunPod SSH connection, which does not need SCP or SFTP.

One-time setup:

```powershell
Copy-Item runpod.local.env.example runpod.local.env
```

Edit `runpod.local.env` with the exact SSH command details from the pod's **Connect**
tab and its HTTP proxy URL. For **SSH over exposed TCP**, use `root@HOST` as
`RUNPOD_SSH_TARGET` and put the mapped port in `RUNPOD_SSH_PORT`. Keep
`GEMINI_API_KEY` and `OPENFDA_API_KEY` blank if desired; the script requests each one as
a masked local prompt and sends it only to the RunPod server. The local file is ignored
by Git.

The script also accepts `FDC_API_KEY` from its local configuration or current process
environment. If neither is set, it requests the USDA key using a masked prompt.
**Leaving that prompt blank disables USDA for this deployment**; it does not preserve
an existing server key. To keep a backend-only key private, use the masked prompt
rather than adding it to a shared file. Configure the matching application token if
your server uses a custom `BITWISE_APP_TOKEN`.

Deploy or update the backend with one command:

```powershell
.\scripts\deploy-runpod.ps1
```

For a pod exposing port `8000`, the Android configuration should use the corresponding
HTTPS proxy URL:

```properties
RETAILER_BACKEND_BASE_URL=https://YOUR-POD-ID-8000.proxy.runpod.net
BITWISE_LLM_BASE_URL=https://YOUR-POD-ID-8000.proxy.runpod.net
```

The Gemini, openFDA, and USDA keys stay on the server. Do not put them in Android
`local.properties`, source code, or a committed configuration file.
