# Retailer API Integration Layer

This folder is the backend-facing commerce layer for What's On My Food.

The Android app should call backend endpoints like these instead of calling retailer APIs directly:

- `GET /api/retail/products/:barcode`
- `GET /api/retail/products/:barcode/availability`
- `GET /api/retail/products/:barcode/alternatives`
- `POST /v1/bitwise/analyze`
- `POST /v1/billing/google-play/verify`

The first provider is mock data so the app flow can ship before retailer approvals. Real providers should be added behind this backend in this order:

1. Open Food Facts product lookup
2. Kroger
3. Instacart
4. Walmart
5. Google Places for nearby store discovery only

Retailer API keys belong in backend environment variables, never in the Android app.

## Bitwise AI with Google Gemini

Bitwise sends its prompts and optional label images to this backend. The backend then
calls Google Gemini, so the Gemini key is never included in the Android or iOS app.

Create an API key in Google AI Studio, then set these server environment variables:

```text
GEMINI_API_KEY=your-google-ai-studio-api-key
GEMINI_MODEL=gemini-3.1-pro-preview
```

`gemini-3.1-pro-preview` is the default when `GEMINI_MODEL` is omitted. If no Gemini key is
configured, Bitwise returns its deterministic local analysis so label scans still work.
`BITWISE_APP_TOKEN` is optional; if you rotate it, update the matching mobile client
configuration before deploying the backend.

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
`GEMINI_API_KEY` blank; the script requests it as a masked local prompt and sends it
only to the RunPod server. The local file is ignored by Git.

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

The Gemini key stays on the server. Do not put it in Android `local.properties`, source
code, or a committed configuration file.
