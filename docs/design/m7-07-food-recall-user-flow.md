# M7-07 - Food Recall Feature and User Flow

## Goal

Give a shopper one clear place to check recall information for a scanned product or a product saved in the pantry. The flow must communicate uncertainty honestly and must never imply that no match guarantees a product is safe.

## Entry points

1. **Scanned product:** The product-details bottom sheet shows a Food recall status card after the product is identified.
2. **Saved pantry product:** The full product-details screen shows the same card when a pantry item is opened.
3. Both entry points open the same recall screen and pass the product name, brand, and barcode. The screen also tells the user whether it was opened from a scan or the pantry.

## User flow

1. The shopper opens a recognized product.
2. The shopper selects **Check food recalls**.
3. The recall screen confirms which product will be checked and shows a short explanation of the matching process.
4. The recall result is shown as one of the states below.
5. For any possible or confirmed match, the shopper is directed to the official notice before taking action.
6. The official FDA recall page remains available when automatic matching is unavailable.

## Result states

| State | What the user sees | Expected action |
| --- | --- | --- |
| Ready | The product identity and a clear Check food recalls action | Start the check or open the official source |
| Checking | A progress indicator and calm loading copy | Wait for the result |
| No known match | A green status with a visible safety limitation | Review package details and check again when needed |
| Possible match | An amber warning asking the user to compare lot, date, size, and package details | Open the official notice |
| Confirmed match | A red warning telling the user not to use the product yet | Follow the official return, disposal, or contact instructions |
| Stale result | A warning that the saved result may be old | Reconnect and check again |
| Unavailable | A clear explanation that automatic matching is unavailable | Use the official FDA recall source |
| Error | A recoverable failure message | Retry or use the official source |

## Language rules

- Use calm, direct language instead of alarming or vague wording.
- Never say that a product is safe only because no recall was found.
- Distinguish a possible name/brand match from a confirmed identifier match.
- Ask the shopper to compare the lot code, date, size, and package details with the official notice.
- Keep the official source visible whenever it can help the shopper verify a result.

## Scope boundary

M7-07 establishes the shared screen, navigation, state model, wording, and entry points. M7-08 will connect the trusted recall source and matching logic. Until that connection is complete, the app clearly labels automatic matching as unavailable and provides the official FDA recall page instead of showing a simulated result.

## Definition of done

- The scan and pantry paths open the same recall screen.
- The product context is visible before a check starts.
- All eight UI states have distinct, understandable wording and visual severity.
- Possible and confirmed matches lead to the official notice.
- The no-known-match state includes a clear limitation.
- Missing product identity disables the recall action safely.
- Automated checks are not presented as live until M7-08 connects a source.
