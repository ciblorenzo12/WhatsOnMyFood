# M7-08 - Trusted Food Recall Source and Matching Logic

## Purpose

M7-08 turns the M7-07 recall screen into a live check. The app now retrieves food enforcement records from the U.S. Food and Drug Administration and compares those records with the product the shopper scanned or saved.

## Trusted source

- Source: FDA Recall Enterprise System records published through the openFDA Food Enforcement API.
- Endpoint: `https://api.fda.gov/food/enforcement.json`
- Transport: HTTPS only.
- Update schedule: the FDA documents this dataset as updating weekly.
- Authentication: `OPENFDA_API_KEY` is required in the hosted backend environment. It is never added to Android configuration, the APK, or source control.
- Application boundary: Android sends the product identity to the protected backend endpoint using the existing application token. Only the backend calls openFDA and attaches the provider key.
- Request boundary: each check requests at most 100 of the most recent records matching a narrow product-name or brand phrase.

## Matching rules

The API search only finds candidate records. A local matcher makes the final decision so that a loose text search is never presented as a confirmed recall.

1. **Confirmed match:** the product barcode matches a UPC written in the FDA product description or code information.
2. **Confirmed match:** at least two meaningful product-name words strongly overlap and the brand also matches.
3. **Possible match:** the name partially overlaps, or a brand match is supported by at least one meaningful product-name word.
4. **No known match:** no active record has enough identity evidence.
5. Records marked `Completed` or `Terminated` are not presented as active matches.
6. When several records match, the record with the strongest evidence wins, with an identifier match ranked highest.

Generic words and punctuation are removed before text comparison. The matcher still asks the shopper to compare the official lot, date, size, and package details because the product database does not always contain those values.

## Failure behavior

- A valid openFDA response with no records becomes **No known match**.
- Rate limiting and temporary FDA server failures receive one bounded retry, then become **Check unavailable**.
- Network, parsing, and unexpected response failures become **Check failed**.
- The official FDA recall page remains available after every non-loading state.
- A no-known-match result continues to state clearly that it is not a safety guarantee.

## Result details shown

For possible and confirmed matches, the screen shows the FDA recall number, recalling firm, classification, product description, recall reason, affected codes/lots/dates, and report date. Missing FDA fields are labeled rather than guessed.

## Definition of done

- The recall action makes a protected HTTPS request to the application backend, which makes the real HTTPS request to the official FDA dataset.
- No API key or credential is committed.
- Candidate retrieval is bounded and product-specific.
- Barcode, name, brand, quantity, record status, and competing-match behavior are covered by deterministic tests.
- Possible and confirmed matches display the official record details needed for package comparison.
- Empty, temporarily unavailable, and failed requests resolve to honest user-visible states.
