# M3-05 source-aware AI prompts and response validation

## Automated verification

Run the backend suite:

```powershell
cd backend\retailer
npm test
```

The tests verify that the protected backend:

- builds the model prompt from normalized ingredients, product data, source status,
  uncertainty, and deterministic findings;
- requests a concise grounded explanation without allowing the model to override the
  deterministic result;
- accepts a complete response with verified HTTPS sources; and
- rejects blank, HTML, malformed, incomplete, uncited, and unsafe medical output.

Run the Android suite:

```powershell
cd app
.\gradlew.bat testDebugUnitTest
```

The Android tests verify the structured request fields and the final defensive response
validation before an explanation can be displayed or cached.

## Manual test 1: grounded product explanation

1. Start the protected backend with the Gemini credential configured.
2. Install the hosted debug build on a physical Android device.
3. Scan a product with a readable ingredient list and at least one deterministic finding,
   such as a cereal whose label lists oats, sugar, and salt.
4. Open the Bitwise explanation.

Expected result:

- the product and normalized ingredient names match the supplied label;
- the explanation uses the `Why this rating`, `Portion guidance`, and `Fact check`
  sections;
- deterministic findings remain visible and are not contradicted;
- any limitation in the source status is stated without overstating certainty; and
- at least one verified HTTPS source is displayed and opens successfully.

## Manual test 2: unsafe or invalid provider response

1. In a local test environment, replace the provider response with each of the following:
   an HTML startup page, malformed JSON, a blank summary, and a claim such as
   `This product can cure diabetes.`
2. Request the same product explanation after each response change.

Expected result:

- none of the invalid provider text is displayed or cached;
- the backend rejects the provider output and uses the controlled local fallback;
- the product data and deterministic findings remain available; and
- the app shows safe fallback wording if a usable explanation cannot be produced.
