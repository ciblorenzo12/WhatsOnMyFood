# Scientific Source Quality Verification Method

## 1. Purpose

YourHealthyPantry uses scientific and public-health references to support the explanations shown with a product result. A source link by itself, however, does not tell a shopper whether the publisher is authoritative, whether the reference matches the claim, or whether the source was actually used during the fact-checking step.

The source-quality verification layer addresses that problem by assigning each displayed reference:

- a quality estimate from **0% to 100%**; and
- one plain-language category: **Very strong**, **Strong**, **Moderate**, or **Limited**.

The purpose of this estimate is to communicate how suitable the reference is for checking the product explanation. It does not replace critical appraisal of the full publication.

## 2. Correct interpretation

The displayed percentage is a deterministic **source-quality estimate**. It is not:

- the probability that every statement on the linked page is true;
- the probability that the product verdict is correct;
- a clinical confidence interval;
- a complete assessment of an individual research study;
- medical or nutritional advice; or
- a substitute for systematic review, peer review, or professional judgment.

The word *estimate* is used deliberately. The current score is not calibrated against a labeled dataset of true and false health claims, so it must not be described as a statistically calibrated probability.

## 3. Design requirements

The method was designed to satisfy six requirements:

1. **No additional model latency.** Scoring must not add another Gemini request or expand the generation prompt.
2. **No additional network latency.** The score must use metadata already available after source selection.
3. **Explainability.** Every point must come from an explicit criterion rather than an opaque model judgment.
4. **Safe communication.** The interface must avoid suggesting certainty that the method cannot support.
5. **Consistent fallback behavior.** Older cached results and local rule sources must still receive a reasonable estimate.
6. **Conservative handling of unknown sources.** An unknown publisher must not receive a high score simply because it uses HTTPS.

## 4. Processing architecture

The method operates after the normal Bitwise response has been generated:

```text
Product and rule context
        |
        v
Existing Bitwise request and source grounding
        |
        v
Accepted source list
        |
        v
Deterministic source-quality calculation
        |
        v
Response returned with source verification metadata
        |
        v
Android displays percentage, category, and source link
```

The server calculation is synchronous and local. It reads the source URL, title, internal topic key, product context already in memory, and the existing fact-check status. It does not alter the prompt and does not call the provider again.

Android contains a second deterministic evaluator for:

- explanations saved before server verification metadata existed;
- deterministic rule sources generated locally; and
- controlled local fallback explanations.

If the server supplies a valid score between 0 and 100, Android displays that score. Otherwise, Android calculates the local fallback estimate.

## 5. Server scoring model

The hosted backend uses five components. Their maximum values total 100 points.

| Component | Maximum | Purpose |
|---|---:|---|
| Publisher authority | 30 | Estimates institutional responsibility and expertise. |
| Evidence orientation | 24 | Distinguishes research and official evidence sources from general publishers. |
| Claim or topic match | 25 | Checks whether the selected reference corresponds to the product issue being explained. |
| Fact-check retrieval status | 12 | Distinguishes a source retrieved during grounding from a curated reference selected locally. |
| Secure transport | 8 | Confirms that the displayed reference uses HTTPS. |
| **Maximum raw total** | **99** | The result is clamped to the 0–100 display range. |

The theoretical sum is 99 rather than artificially adding an uninformative point. A highly qualified source can therefore display 99%, while typical authoritative matches display approximately 90–94% depending on their topic match and retrieval status.

### 5.1 Publisher authority

The hostname is normalized by removing `www.` and matching the full domain or a true subdomain. A text fragment elsewhere in the URL is not accepted as a domain match.

| Source type | Points |
|---|---:|
| Named primary public-health authority | 30 |
| Named research repository, DOI service, research journal, or university | 27 |
| Recognized professional health organization | 22 |
| Other government or international domain | 24 |
| Other nonprofit organization | 12 |
| Unclassified publisher | 6 |

The current named public-health authorities include FDA, NIH/NCBI, CDC, USDA, WHO, EFSA, and Cochrane. The list is intentionally explicit and should be reviewed as the product scope changes.

### 5.2 Evidence orientation

This component estimates whether the publisher is structurally associated with scientific evidence or official public-health guidance.

| Source type | Points |
|---|---:|
| Public-health authority, research repository, journal, or university | 24 |
| Recognized professional health organization | 18 |
| Open Food Facts product or methodology reference | 12 |
| Other publisher | 8 |

This component assesses source type, not the internal quality of a specific study. For example, a PubMed Central article receives research-source credit, but the method does not automatically determine its risk of bias, sample quality, or applicability.

### 5.3 Claim or topic match

The backend selects references using the actual product or ingredient context, not generic rule descriptions that might mention unrelated ingredients. Topic groups currently cover:

- nutrition labels and serving information;
- fats and oils;
- added sugars and sweeteners;
- sodium and salt;
- additives, preservatives, colors, emulsifiers, and stabilizers; and
- general healthy-diet context.

| Match | Points |
|---|---:|
| Strong match to a specific detected topic | 25 |
| Curated general nutrition match | 18 |
| Strong title/URL term overlap for a dynamically grounded source | 22 |
| General grounded or search-query relationship | 16 |
| Weak or absent match evidence | 6 |

The internal topic key is used only to calculate the score and is removed before the public response is returned.

### 5.4 Fact-check retrieval status

| Status | Points |
|---|---:|
| Retrieved or grounded during the provider fact-check step | 12 |
| Selected from the curated authoritative reference set | 7 |

A curated source can still be high quality, but the lower value makes the distinction between an actually retrieved source and a source selected as an appropriate authoritative reference visible in the estimate.

### 5.5 Secure transport

An HTTPS source receives 8 points. Non-HTTPS sources receive no transport points. HTTPS protects transport integrity but does not prove that the content is accurate; this is why it represents only a small part of the total.

## 6. Android fallback model

Android cannot always reproduce the server's claim-specific topic selection because older cache records may contain only a name, URL, quotation, and search query. The local fallback therefore uses five related components:

| Component | Maximum |
|---|---:|
| Publisher authority | 30 |
| Evidence orientation | 24 |
| Title/URL relevance to the saved search query | 20 |
| Traceability through a meaningful title and URL path | 10 |
| HTTPS | 8 |
| **Maximum raw total** | **92** |

The fallback intentionally cannot exceed 92. This prevents locally inferred metadata from appearing more certain than a source evaluated with full server context.

Traceability awards up to 5 points for a meaningful source name and up to 5 points for a specific URL path rather than a bare homepage.

## 7. Display categories

The score is converted into a category using fixed thresholds:

| Score | Category | Intended interpretation |
|---:|---|---|
| 90–100 | Very strong | Authoritative or research-oriented source with strong traceability and relevance. |
| 75–89 | Strong | Credible source with useful support, but one or more verification signals are less complete. |
| 60–74 | Moderate | Potentially useful reference that should be interpreted with additional context or checking. |
| 0–59 | Limited | Authority, evidence orientation, relevance, or traceability is insufficient for strong reliance. |

These are communication bands, not clinical evidence grades. The labels intentionally do not use the formal GRADE terms *high*, *moderate*, *low*, and *very low* because the app is evaluating source metadata, not completing a full GRADE evidence assessment.

## 8. Response metadata

The server attaches an object like this to each source:

```json
{
  "name": "FDA - Added Sugars on the Nutrition Facts Label",
  "url": "https://www.fda.gov/food/nutrition-facts-label/added-sugars-nutrition-facts-label",
  "verification": {
    "score": 94,
    "level": "very_strong",
    "basis": [
      "authoritative_or_research_host",
      "strong_topic_match",
      "retrieved_for_fact_check"
    ],
    "method": "source_quality_v1",
    "note": "Evidence-quality estimate, not the probability that every claim is true."
  }
}
```

The `basis` values are safe categorical explanations. They do not contain the full prompt, product image, credential, or sensitive shopper data.

## 9. Scientific and methodological basis

The implementation is an engineering adaptation informed by the following public literature. It is not represented as a complete implementation of any one instrument.

### QUEST

The QUality Evaluation Scoring Tool measures authorship, attribution, conflict of interest, currency, complementarity, and tone. Its validation study reported high inter-rater reliability and convergent validity and emphasized the usefulness of a concise, weighted tool for rapid health-information evaluation.

Reference: Robillard JM, Jun JH, Lai JA, Feng TL. *The QUEST for quality online health information: validation of a short quantitative tool.* <https://pmc.ncbi.nlm.nih.gov/articles/PMC6194721/>

Applied here: explicit weighted criteria, publisher authority, attribution/evidence type, and concise user-facing categories.

Not fully available here: article authorship, conflict disclosure, update date, complementarity, and tone cannot reliably be derived from every source citation without fetching and interpreting the page.

### Online nutrition Quality Assessment Tool

The OQAT was developed for online nutrition information. Its criteria include currency, author identity and credentials, peer-reviewed attribution, specialist quotation, disclosure, adequate background, accurate headlines, avoidance of unsupported generalization, and avoidance of undue harm or optimism.

Reference: Denniss E, Lindberg R, Marchese LE, McNaughton SA. *Development and validation of a quality assessment tool to assess online nutrition information.* <https://pmc.ncbi.nlm.nih.gov/articles/PMC10357061/>

Applied here: explicit attention to nutrition-source credibility, high-quality evidence attribution, traceability, and conservative scoring of unknown publishers.

Not fully available here: the current fast method does not inspect the complete page for author credentials, disclosure, publication date, headline accuracy, or unsupported generalization.

### DISCERN

DISCERN is a validated instrument for judging consumer health information about treatment choices. Its development demonstrates that structured criteria can discriminate between higher- and lower-quality information, while acknowledging that some assessment remains subjective.

Reference: Charnock D, Shepperd S, Needham G, Gann R. *DISCERN: an instrument for judging the quality of written consumer health information on treatment choices.* <https://pmc.ncbi.nlm.nih.gov/articles/PMC1756830/>

Applied here: transparent criteria, cautious interpretation, and explicit acknowledgment that a numeric summary cannot eliminate judgment.

### GRADE

GRADE separates quality of evidence from strength of recommendations and describes evidence quality as a continuum. It also recognizes that discrete categories involve some arbitrariness but can improve simplicity and transparency.

Reference: Guyatt GH, Oxman AD, Vist GE, et al. *GRADE: an emerging consensus on rating quality of evidence and strength of recommendations.* <https://pmc.ncbi.nlm.nih.gov/articles/PMC2335261/>

Applied here: separation of source quality from product recommendations, fixed transparent categories, and prominent acknowledgment of uncertainty.

Not applied here: the app does not perform GRADE's full evaluation of study limitations, inconsistency, indirectness, imprecision, and reporting bias.

## 10. Validation strategy

The current implementation is verified at three levels:

1. **Backend unit tests** confirm that official, topic-matched FDA guidance receives a very strong estimate and that an unclassified, weakly matched publisher remains limited.
2. **Android unit tests** confirm consistent handling of FDA guidance, a peer-reviewed repository, an unclassified publisher, and server-provided scores.
3. **Device verification** confirms that the explanation, percentage, category, and clickable source render correctly in the product-detail interface without duplicates.

The repeatable commands and manual steps are documented in [the source verification test guide](testing/source-quality-verification.md).

## 11. Known limitations

The present version has important limitations:

- Domain reputation is a proxy and cannot establish the validity of a specific claim.
- A credible organization can publish a page that is general, outdated, or not directly applicable.
- A lesser-known publisher can host strong peer-reviewed evidence and still receive a conservative score.
- The fast method does not inspect authors, credentials, funding, conflicts, publication dates, study design, sample size, effect estimates, confidence intervals, retractions, or risk of bias.
- Topic matching uses deterministic terms and may miss synonyms or nuanced relationships.
- Curated sources are selected for relevance but may not have been retrieved during a provider request.
- The thresholds are transparent engineering thresholds, not clinically calibrated probability cutoffs.
- Source quality is not the same as the quality of the complete body of evidence supporting a product verdict.

## 12. Future validation and improvement

Before describing the percentage as a calibrated probability, the project would need a separate validation study. A responsible next phase would:

1. create a representative set of nutrition claims and linked sources;
2. have at least two trained reviewers independently score each source using a predefined reference rubric;
3. measure inter-rater agreement;
4. compare app scores with reviewer consensus;
5. recalibrate weights and thresholds on a training set;
6. evaluate calibration and discrimination on a held-out test set;
7. audit performance across government, academic, nonprofit, commercial, and community sources; and
8. publish the validation protocol, disagreements, limitations, and versioned changes.

Possible future metadata checks—performed asynchronously or during source curation rather than during prompt generation—include publication date, author credentials, DOI/PubMed identifiers, Crossref retraction status, peer-review status, declared conflicts, and evidence-design classification.

## 13. Versioning and governance

The response identifies the current implementation as `source_quality_v1`. Any change to domains, weights, thresholds, topic terms, or labels should:

- increment the method version;
- update this document;
- add or update automated tests;
- record the reason for the change;
- verify that unknown publishers remain conservative; and
- review the user-facing wording for unsupported certainty.

This keeps the score reproducible, reviewable, and understandable to future project teams.
