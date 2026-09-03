# M7-10 - Food Recall Tests for Scanned and Saved Products

## Purpose

Confirm that the food recall feature behaves consistently whether a shopper opens it immediately after scanning a product or from a product already saved in the pantry.

## Test products

| Entry path | Product | Barcode | Expected result |
| --- | --- | --- | --- |
| Scanned product | Pillsbury Bread Rolls, Hard Roll Dough | `721582132834` | An exact UPC in an ongoing FDA record produces a confirmed recall match. |
| Saved pantry product | Jif Creamy Peanut Butter | `051500255162` | Historical or terminated candidates are not presented as active recalls. |

## Automated verification

The local regression tests use fixed FDA-shaped records so a future dataset update cannot make the build unreliable.

- The scanned product keeps its barcode, name, and brand through the repository and matches the formatted UPC in record `H-1154-2026`.
- The saved product receives a matching historical record, but its `Terminated` status correctly produces **No known match**.
- Android device tests launch the shared recall screen through both entry points and verify the correct scan or pantry context, product identity, and result wording.
- The same matcher and result presentation are used for both paths; only the entry-context sentence changes.

## Live backend evidence

The deployed protected backend was checked on September 2, 2026 against the openFDA dataset dated August 19, 2026.

| Check | Live observation |
| --- | --- |
| Scanned Pillsbury sample | The backend returned 14 bounded candidates. UPC `721582132834` matched ongoing record `H-1154-2026`. |
| Saved Jif sample | The backend returned 24 bounded candidates, including historical Jif records. No exact active record matched barcode `051500255162`. |
| Protection | The endpoint requires the application token and keeps the openFDA key on the backend. |

Candidate counts can change as the FDA updates the dataset. The automated tests therefore assert the matching rules and user-visible outcomes rather than a permanent live count.

## Device-test status

The device-test source compiles as part of the Android test build. No Android device or emulator was connected during this run, so the two Espresso tests were not executed on hardware. They are ready to run with:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.recall.FoodRecallScanSavedFlowTest
```

## Definition of done

- Scanned and saved products both reach the shared recall screen with the correct context.
- A strong active match becomes a clear confirmed alert.
- A terminated record is never shown as an active recall.
- The official-source and cautious no-match language remain available through the shared presentation model.
- Local regression tests pass, device-test code compiles, and live backend observations are recorded separately from deterministic assertions.
