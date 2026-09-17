# M9-06: Automatic pantry recall checks

## Behavior

- Opening a scanned product starts a fresh recall check immediately when its product details load, even before saving it to the pantry. A match displays an in-app dialog with a **View notice** action; no notification permission is needed for this warning. The recall section shows checking, success, or error status and the successful check time.
- Scan checks survive view recreation and do not repeat when AI/ingredient UI updates render the same product again. Each scan screen shows the match dialog once per product identity. Manual recall checks remain available after a failed scan check.
- Adding pantry items or changing stored product identity schedules a check. Room observers cover manual entry, scanning, OCR, and product refresh paths.
- WorkManager schedules a daily pantry sweep for the signed-in user. Each item requires a network connection; Android may defer execution for power or connectivity reasons. This is not an exact-time alarm.
- Successful results are cached for 24 hours. Changes to barcode, name, brand, or quantity invalidate freshness. Manual **Check again** bypasses the cache.
- Pantry rows and product recall entry points show status and the last successful check time. Tapping a pantry status opens details, with navigation between multiple notices and a link to the official FDA record.
- Failed checks retain prior results and timestamps, display failure/outdated status, and retry with exponential backoff starting at 15 minutes. A failed or incomplete response never becomes a successful no-match result.
- Alerts are recorded by account, barcode, and FDA recall number. Repeat checks do not repost recorded alerts. Denied notification permission leaves alerts pending; the pantry screen provides permission/settings access. Stable notification identifiers also prevent duplicate visible notifications during overlapping attempts.
- Sign-out/account changes cancel the previous account's scheduled work. Removed or changed items cannot receive an in-flight stale result.

## Deployment and scope

Ship the Android update and the changes to `backend/retailer/src/foodRecallProxy.js` together. The existing backend must have `OPENFDA_API_KEY` configured. The proxy includes barcode searches, validates responses, and retrieves all pages up to its complete-check limit; incomplete or oversized result sets fail rather than report no matches.

This feature uses the existing FDA food enforcement source. A product match still requires comparing the notice's lot/code details with the user's package; a no-known-match result does not guarantee that a product is safe or cover every recall authority. Notifications require the user's Android notification permission and an enabled recall notification channel.

## Automated validation

- `testDebugUnitTest`: 259 tests passed. Recall coverage includes matching and no matches, multiple notices, equivalent barcodes, repeat alerts, permission-denied alerts, cache expiry, identity changes, manual refresh, failed checks, recovery, and changes/removal during a request.
- `node --test backend/retailer/test/foodRecallProxy.test.js`: 10 tests passed, including barcode queries, official empty results, malformed responses, pagination, and interrupted pagination.
- `connectedDebugAndroidTest` restricted to `PantryRecallPersistenceTest` and `FoodRecallScanSavedFlowTest`: 3 tests passed on SM-X800 / Android 16. The isolated migration test preserves pantry data from schema 11, checks persisted results and the alert ledger, and verifies account separation. Screen tests cover scanned and saved product recall entry contexts.
- `lintDebug`: blocked by four existing `MissingTranslation` errors in `values/dietary_badges.xml`. New recall strings include English, Spanish, and French translations.

## Release smoke checks

0. Scan the barcode of a matching product without saving it. Verify the request starts automatically and the recall dialog appears while the scan result is open. Re-render/rotate the screen and confirm no duplicate alert. Test a no-match response and an offline failure. Dedicated `ScanRecallViewModelTest` checks automatic matching, duplicate suppression, no matches, and failure handling; these three tests and the two existing recall screen tests passed on SM-X800 / Android 16.
1. With a signed-in account, add a product matching a known active test notice. Verify a check runs without tapping the button, the pantry row is flagged, and the official record opens from details.
2. Edit the product name/brand/quantity and verify it is checked again. Remove an item during a request and confirm no stale result/alert is published.
3. Enable notifications, detect a new notice, and repeat manual/background checks. Confirm only one alert for the same product/notice, including after restarting the app.
4. Disable notifications, detect a match, and confirm the pantry still shows it. Re-enable notifications and return to the pantry to deliver a pending alert.
5. Make the source unavailable after a successful match and after a no-match result. Confirm the successful timestamp stays unchanged, the failure is visible, previous matches remain visible, and recovery updates the timestamp.
6. Check offline behavior and delayed scheduled work under Android battery restrictions. Confirm ordinary pantry use remains responsive and manual refresh remains available.

Live-provider recall detection, OS notification delivery, and elapsed daily scheduling still require these release smoke checks; automated tests use controlled fixtures rather than asserting current FDA data.
