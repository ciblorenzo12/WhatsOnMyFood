# M7-09 - Clear Food Recall Alerts, Details, and Fallback States

## Purpose

M7-09 makes every recall result easy to understand and act on. It builds on the trusted openFDA connection from M7-08 without overstating what an automated match can prove.

## Clear alerts

- Each state uses a distinct badge, title, message, and severity color.
- Possible and confirmed matches are announced as urgent accessibility updates.
- Every non-loading result includes a short **What to do next** section.
- A possible match asks the shopper to pause and compare the UPC, lot, date, size, and packaging.
- A confirmed match tells the shopper not to use the product and to follow the official notice.
- A no-known-match result continues to say that the result is not a safety guarantee.

## Official record details

Possible and confirmed matches show the match explanation, recall number, recalling firm, FDA classification and status, product description, recall reason, affected codes or lots, FDA report date, and dataset update date. Missing fields are labeled as not provided rather than guessed.

## Fallback and recovery

Stale, temporarily unavailable, and failed checks display a dedicated recovery panel. The shopper can retry the automatic check or open the official FDA recall page and search with the product name, brand, UPC, and package details.

## Definition of done

- The ready, checking, no-match, possible-match, confirmed-match, stale, unavailable, and error states remain visually and verbally distinct.
- Possible and confirmed alerts provide direct, safe next steps.
- Official FDA record details explain both the match and the source freshness.
- Recovery states keep retry and official-source options visible.
- Alert updates are accessible to screen-reader users.
- Unit and layout-contract tests cover the new guidance, fallback, and detail elements.
