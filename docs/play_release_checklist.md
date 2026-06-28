# Google Play Release Checklist

This app is wired for Google Play Billing and a production Android release. Use this checklist before uploading an app bundle to Play Console.

## Android build

- Open the `app/` project in Android Studio.
- Confirm `targetSdk = 35` and `compileSdk = 35` in `app/build.gradle.kts`.
- Enroll in Play App Signing and create a separate upload key. Never commit the keystore or passwords.
- Configure these values in `app/local.properties`, Gradle properties, or environment variables:

```text
YHP_UPLOAD_STORE_FILE=C:\secure\your-healthy-pantry-upload.jks
YHP_UPLOAD_STORE_PASSWORD=...
YHP_UPLOAD_KEY_ALIAS=upload
YHP_UPLOAD_KEY_PASSWORD=...
```

- Run the release gates before building the bundle:

```powershell
cd app
.\gradlew.bat testDebugUnitTest lintRelease
```
- Build the release app bundle:

```powershell
cd app
.\gradlew.bat bundleRelease
```

The generated bundle is written under `app/build/outputs/bundle/release/`.

Verify that the final bundle is signed before uploading. An unsigned local bundle must never be submitted.

## Google Play Billing setup

Create this subscription in Play Console before testing purchases:

- Product type: Subscription
- Product ID: `bitwise_plus_monthly`
- Name: `Bitwise Plus`
- Base plan: monthly auto-renewing plan
- Benefits: unlimited Bitwise product explanations, deeper ingredient and nutrition reasoning, priority AI pantry guidance

The Android code queries the product ID above through Google Play Billing and launches the Play-managed purchase sheet. Initial subscription purchases are acknowledged in `BitwiseEntitlementManager`.

Create a Google Cloud service account with Android Publisher access, add it in Play Console, and configure the production backend using `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` or the email/private-key variables in `backend/retailer/runpod.env.example`. The backend `/health` response must show `"playBillingConfigured": true` before purchase testing.

## Testing purchases

- Upload an internal testing build to Play Console.
- Add license testers under Play Console test settings.
- Install the app from the internal testing track, not directly from Android Studio, when validating real Billing behavior.
- On the Bitwise Plus screen, verify:
  - The price loads from Google Play.
  - Subscribe opens the Google Play purchase sheet.
  - Restore purchases refreshes active subscription state.
  - A subscribed account sees `Bitwise Plus active`.
  - A free account gets 3 Bitwise explanations per day before the upgrade prompt.

## Store listing and policy items

- Complete App content in Play Console:
  - Data safety form
  - Privacy policy URL
  - Ads declaration
  - App access instructions, if reviewers need a test account
  - Health apps declaration (declare the ingredient and nutrition guidance accurately)
- Explain that ingredient analysis is informational and not medical advice in the store listing and privacy policy.
- Confirm the app does not expose local development endpoints in release. Release builds set cleartext traffic to `false`; debug builds keep it enabled for local backend testing.
- Enable GitHub Pages from the repository `docs/` folder, then verify these URLs load without signing in:
  - `https://ciblorenzo12.github.io/WhatsOnMyFood/privacy-policy.html`
  - `https://ciblorenzo12.github.io/WhatsOnMyFood/account-deletion.html`
- Confirm `ciblorenzo@gmail.com` is the intended public support/deletion address before publishing.
- Add the Play App Signing SHA-1 and SHA-256 fingerprints to the Firebase Android app, then download the refreshed `google-services.json`.

Current upload-key certificate fingerprints (safe to enter in Firebase and Play Console):

```text
SHA-1:   EC:A9:E0:8C:11:B4:D9:2F:3C:95:1C:06:48:58:6C:CC:D3:26:30:D3
SHA-256: 41:A6:C1:6A:81:E8:DF:9B:D5:42:A1:7F:FC:AD:59:7E:0A:7C:B0:8B:34:F2:E4:86:06:90:67:7C:27:00:7E:23
```

After Play App Signing is enabled, also add Google's separate app-signing certificate fingerprints from Play Console to Firebase.

The local upload key is stored at `C:\Users\ciblo\.your-healthy-pantry\signing\upload.jks`; its credentials are in the ignored `app/keystore.properties`. Back up both together in a secure password manager or encrypted drive before uploading the first production bundle.

## Data safety notes to review

The app may process or store:

- Account identifiers through Firebase Authentication
- Product scans, pantry items, ingredient text, and app activity
- Approximate or precise location when checking nearby retailer availability
- Purchase status through Google Play Billing

Only submit the final Data safety answers after reviewing the actual production backend, Firebase configuration, and privacy policy.

## Production backend gate

- Deploy from `backend/retailer` with a stable HTTPS hostname.
- Set a production `BITWISE_APP_TOKEN`, Gemini credentials, and `PROTECTED_RATE_LIMIT_PER_MINUTE`.
- Configure Google Play verification credentials and confirm `/health` reports billing configured.
- Verify the Android release configuration points both backend URLs to the stable HTTPS service.
- Run `npm test` before deployment.
