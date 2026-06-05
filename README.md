# What's On My Food

What's On My Food is an Android app for scanning food labels, reviewing ingredient risks, saving pantry items, and comparing cleaner alternatives. The app combines barcode lookup, ingredient OCR, rule-based analysis, Bitwise AI summaries, scientific source links, and retailer availability tools.

## Features

- Barcode scanning with CameraX and ML Kit
- Ingredient-list scanning with text recognition
- Product lookup from Open Food Facts and fallback product APIs
- Ingredient parsing that filters label claims and warning text from real ingredients
- Rule-based health scoring for additives, sugars, oils, sodium, sweeteners, preservatives, and other common label concerns
- Bitwise AI product summaries written as natural shopper notes
- Pantry saving, offline cache support, and manual product entry
- Additive and ingredient database search
- Certificate and label badge detection
- Marketplace and retailer comparison views
- English, Spanish, and French string resources

## Project Structure

```text
.
|-- app/                         Android application project
|   |-- src/main/java/           App, analysis, product, scanner, and retailer code
|   |-- src/main/res/            Layouts, drawables, strings, and app assets
|   |-- src/test/java/           Unit tests
|   `-- build.gradle.kts         Android build configuration
|-- backend/
|   |-- retailer/                Node retailer API integration layer
|   `-- bitwise-fallback/        Local Bitwise fallback summary service
`-- .github/workflows/           Backend publishing workflow
```

## Requirements

- Android Studio
- JDK 21, or the JetBrains Runtime bundled with Android Studio
- Android SDK 35
- Node.js 20+ for the backend services
- Firebase project configuration for authentication

## Local Android Setup

Open the `app/` folder in Android Studio.

Add your Firebase config file at:

```text
app/google-services.json
```

Create or update `app/local.properties` with any local API keys you want enabled:

```properties
sdk.dir=C\:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
FDC_API_KEY=your-food-data-central-key
NUTRITIONIX_APP_ID=your-nutritionix-app-id
NUTRITIONIX_APP_KEY=your-nutritionix-app-key
BARCODE_LOOKUP_API_KEY=your-barcode-lookup-key
UPCITEMDB_USER_KEY=your-upcitemdb-key
RETAILER_BACKEND_BASE_URL=http://10.0.2.2:8787
BITWISE_LLM_BASE_URL=http://10.0.2.2:8788
GOOGLE_MAPS_API_KEY=your-google-maps-key
```

Most keys are optional during development. Open Food Facts works without a key, and the retailer layer can fall back to mock data.

## Build and Test

From the `app/` directory:

```powershell
.\gradlew.bat assembleDebug
```

Run all unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run focused analysis tests:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.example.myapplication.analysis.IngredientTextParserTest --tests com.example.myapplication.analysis.rules.SugarAsMainIngredientRuleTest
```

## Retailer Backend

The Android app can call a local Node backend instead of connecting directly to retailer APIs.

From `backend/retailer/`:

```powershell
npm install
npm start
```

For the Android emulator, use:

```properties
RETAILER_BACKEND_BASE_URL=http://10.0.2.2:8787
```

For a physical device, use your computer's LAN IP address instead of `10.0.2.2`.

More backend details are in `backend/retailer/README.md`.

## Bitwise Fallback Service

The fallback summary service can run locally while testing Bitwise wording without depending on a remote model endpoint.

From `backend/bitwise-fallback/`:

```powershell
node server.js
```

Then point the Android app at that service with:

```properties
BITWISE_LLM_BASE_URL=http://10.0.2.2:8788
```

## Data and Privacy Notes

- Retailer secrets should stay in backend environment variables.
- Android API keys should be kept in `app/local.properties` or environment variables, not hardcoded in source files.
- `app/local.properties`, generated build folders, and IDE files should remain ignored.
- `app/google-services.json` is needed for local Firebase builds but should be reviewed before committing, depending on the project policy.

## Main Technologies

- Java
- AndroidX AppCompat, Material Components, ConstraintLayout
- CameraX
- Google ML Kit barcode scanning and text recognition
- Firebase Authentication
- Room
- Retrofit, OkHttp, Gson, Picasso
- Node.js backend services
