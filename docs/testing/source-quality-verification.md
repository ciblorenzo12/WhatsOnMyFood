# Scientific source quality verification

The full scoring formula, scientific basis, interpretation, limitations, and governance rules are documented in [Scientific Source Quality Verification Method](../source-quality-verification-method.md).

## What changed

Each scientific source now shows a **Verification estimate from 0% to 100%** and a plain-language level: **Very strong**, **Strong**, **Moderate**, or **Limited**.

The estimate is calculated after Bitwise finishes generating its response. It does not add text to the prompt, make another model request, or wait for another network response. Android also calculates a local fallback estimate for older saved explanations and deterministic rule sources that do not yet contain server metadata.

The number estimates the quality of a source for verification. It is **not** a statistical probability that every statement on the page—or every Bitwise claim—is true.

## Evidence-informed method

The lightweight calculation uses only information already available with the source:

- publisher authority, such as a public-health regulator, research repository, university, or recognized health organization;
- evidence type, distinguishing official guidance and research sources from unclassified publishers;
- relevance to the product claim or ingredient topic;
- whether the source was retrieved during fact-checking or selected from the curated reference set;
- HTTPS and enough title/URL information to trace the reference.

The criteria were adapted for fast automated use from publicly available research:

- QUEST validates a concise quantitative approach based on authorship, attribution, conflicts, currency, complementarity, and tone: <https://pmc.ncbi.nlm.nih.gov/articles/PMC6194721/>
- OQAT was developed specifically for online nutrition information and emphasizes currency, credibility, high-quality peer-reviewed attribution, disclosure, and avoiding unsupported generalization: <https://pmc.ncbi.nlm.nih.gov/articles/PMC10357061/>
- DISCERN demonstrates that structured criteria can distinguish higher- and lower-quality consumer health information, while also acknowledging that some judgment remains subjective: <https://pmc.ncbi.nlm.nih.gov/articles/PMC1756830/>
- GRADE separates evidence quality from recommendation strength and warns that discrete categories simplify an underlying continuum: <https://pmc.ncbi.nlm.nih.gov/articles/PMC2335261/>

This implementation deliberately does not claim to be a complete automated QUEST, OQAT, DISCERN, or GRADE assessment. Those methods inspect information that is not always available in a citation URL, including publication dates, author credentials, conflicts of interest, study design, bias, and uncertainty.

## Automated backend test

Open PowerShell or the Android Studio Terminal from the repository root:

```powershell
cd backend\retailer
npm test
```

Pass result:

- official, topic-matched FDA guidance receives a very strong estimate;
- an unclassified publisher with weak claim fit remains limited;
- accepted sources contain the verification metadata;
- existing prompt, grounding, safety, fallback, and observability tests continue to pass.

## Automated Android test

From the repository root:

```powershell
cd app
.\gradlew.bat testDebugUnitTest --tests "com.ciblorenzo.whatsonmyfood.SourceReliabilityEvaluatorTest"
```

Pass result:

```text
BUILD SUCCESSFUL
```

The four cases cover FDA guidance, a peer-reviewed research repository, an unclassified publisher, and safe handling of a server-provided score.

## Manual product-detail test

1. Install and open the current debug build.
2. Scan a product that produces a Bitwise explanation.
3. Scroll to **SCIENTIFIC SOURCES** in the product-detail fragment.
4. Confirm the short disclaimer explains what the estimate means.
5. Confirm every source shows a percentage and one quality level.
6. Tap the source name and confirm the original page still opens.
7. Return to the product and confirm no duplicate source was added.
8. Reopen a saved Pantry product and confirm older cached sources also receive an estimate.

Pass result: the estimate is visible, understandable, and clickable sources continue to work. The Bitwise request is still a single provider request; no second model call is made for scoring.

## Full quality check

Run:

```powershell
cd app
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Then run the backend suite again with `npm test` from `backend\retailer`.
