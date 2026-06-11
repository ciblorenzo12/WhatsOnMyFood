# Retailer API Integration Layer

This folder is the backend-facing commerce layer for What's On My Food.

The Android app should call backend endpoints like these instead of calling retailer APIs directly:

- `GET /api/retail/products/:barcode`
- `GET /api/retail/products/:barcode/availability`
- `GET /api/retail/products/:barcode/alternatives`

The first provider is mock data so the app flow can ship before retailer approvals. Real providers should be added behind this backend in this order:

1. Open Food Facts product lookup
2. Kroger
3. Instacart
4. Walmart
5. Google Places for nearby store discovery only

Retailer API keys belong in backend environment variables, never in the Android app.

Run the mock server:

```bash
node src/server.js
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

## RunPod deployment shape

This backend is ready to run as a small Node service:

```bash
npm start
```

If your RunPod exposes port `8787`, the Android app should use a URL shaped like:

```properties
RETAILER_BACKEND_BASE_URL=https://YOUR-POD-ID-8787.proxy.runpod.net
```

If you prefer to keep one public RunPod port, mount this service behind your existing
port `8000` app and proxy `/api/retail/*` to this Node process.
