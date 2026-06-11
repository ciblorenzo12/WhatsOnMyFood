# YourHealthyPantry iOS

Native SwiftUI port of the Android application.

## Requirements

- macOS with Xcode 15 or newer
- iOS 17 simulator/device target recommended

## Run

1. Copy `ios/YourHealthyPantryIOS` to your Mac.
2. Open `YourHealthyPantryIOS.xcodeproj` in Xcode.
3. Select an iPhone simulator or physical iPhone.
4. Update the bundle identifier/team if Xcode asks.
5. Press Run.

## Backend

The app uses the same default backend URL currently configured in the Android project:

`https://x7amycb9govesb-8787.proxy.runpod.net`

You can change the backend URL from the iOS app Settings/Profile tab.

## Implemented Native Features

- Barcode scanner using `AVFoundation`
- Open Food Facts product lookup
- Product detail screen with ingredients, nutrition, labels/certifications, Nutri-Score, NOVA, Eco-Score
- Original certification logo assets for USDA Organic, Non-GMO Project, Green Dot, Triman, and other supported labels
- Bitwise AI summary through `/v1/chat/completions`
- Pantry save/remove using local persisted storage
- Pantry risk insights and charts using SwiftUI `Charts`
- Healthy vs concern ingredient breakdown
- Marketplace availability and alternatives through the same retailer backend endpoints
- Additive/ingredient database search
- Profile/settings screen for backend configuration and local pantry reset
