# M5-03 - Alternative comparison cards test guide

## What this feature does

Each marketplace card presents information in the same order: product identity, app comparison, retailer context, availability, and a clear retailer action. The comparison describes only the app score. It does not claim that an alternative is medically better for the shopper.

Missing data remains honest. A missing score says **Score unavailable**, a missing image uses the pantry placeholder, a missing brand line is hidden, and a missing retailer URL produces a disabled **Retailer link unavailable** button.

## Automated test

Open PowerShell or the Android Studio **Terminal** in the `YourHealtyPantry` repository and run:

```powershell
.\app\gradlew.bat -p .\app testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.retail.MarketplacePresentationTest" --tests "com.ciblorenzo.whatsonmyfood.retail.MarketplaceItemLayoutContractTest"
```

Expected result:

```text
BUILD SUCCESSFUL
```

These tests verify that:

- every card includes product, comparison, retailer, availability, and action sections;
- a difference of five points or more is described as higher or lower;
- a smaller difference is described as similar;
- a missing product or comparison score is never changed into `0/100` or a guessed score;
- score copy does not call an alternative medically superior or the healthiest choice; and
- the card says that the comparison is not medical advice.

## Repeatable card preview

Connect a phone, tablet, or emulator. Then run:

```powershell
.\app\gradlew.bat -p .\app installDebug
adb devices
adb shell am force-stop com.ciblorenzo.whatsonmyfood
adb shell am start -n com.ciblorenzo.whatsonmyfood/.retail.MarketplaceActivity --es extra_debug_marketplace_state mock
```

Confirm the device appears as `device`, then check the card from top to bottom:

1. The card starts with **ALTERNATIVE OPTION**.
2. The product name, brand, and pantry image placeholder are visible.
3. The provider is clearly marked **DEVELOPMENT SAMPLE**.
4. The comparison says to compare ingredients and package details because this preview has no scanned-product reference score.
5. The score is `88/100`; it is not described as medical advice or proof that the product is better for the shopper.
6. Retailer, distance, and price information are grouped together.
7. The last control says **Retailer link unavailable** and is disabled because the preview intentionally has no URL.
8. The toolbar back arrow remains available and returns safely.

Pass result: all eight checks match and the screen does not crash.

## Live-data comparison test

1. Start the retailer backend and configure the debug app as described in `m5-01-retailer-backend-alternatives.md`.
2. Open a supported scanned product and tap **Compare alternatives**.
3. Confirm the scanned product cards say **SCANNED PRODUCT** and **This is the product you scanned.**
4. Confirm each returned alternative says **ALTERNATIVE OPTION**.
5. Compare each alternative score with the scanned score:
   - five or more points above says **higher**;
   - five or more points below says **lower**;
   - a difference from minus four through plus four says **similar**.
6. Confirm the wording refers to the **app score**, not to a medical benefit or a guarantee that the product is healthy.
7. On a result with a URL, tap **View retailer** and confirm the expected retailer page opens.

Pass result: the labels and comparison wording match the displayed scores, and the retailer action opens only when a URL is available.

## Incomplete-record test

Return one development alternative with `productName` present and omit `brand`, `imageUrl`, `healthScore`, and `productUrl`.

Confirm that:

1. the product name remains visible;
2. no empty brand row or `null` text appears;
3. the pantry placeholder appears with an accessible missing-image description;
4. the card says **Score unavailable**, never `0/100` or `100/100`;
5. the comparison says to compare ingredients and package details;
6. retailer and availability values use readable fallback text; and
7. **Retailer link unavailable** is disabled.

Pass result: the incomplete record is understandable and no tap, scroll, or sort action crashes the app.

## Final pass condition

M5-03 passes when the automated tests succeed, the mock preview matches all eight checks, the live comparison wording matches the score differences, incomplete records never show invented values, and retailer actions behave correctly.
