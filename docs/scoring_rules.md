# Ingredient scoring rules

The rule score is deterministic: the same product inputs produce the same ordered findings, score, categories, and explanations.

## How the score is calculated

1. Start at **100 points**.
2. Evaluate active rules in a fixed order.
3. Keep one finding from each scoring group. If multiple findings share a group, keep the largest penalty or positive adjustment.
4. Subtract penalties and apply positive adjustments.
5. Limit the final score to **0 through 100**.

Positive adjustments offset penalties but cannot raise the final score above 100. Informational findings change the score by 0 points.

## Deterministic subject categories

Each rule declares exactly one subject category in code. A test verifies that all 28 active rules are categorized once.

| Subject category | Active rules |
|---|---|
| Sugar | Added sugar, sugar as a main ingredient, high-fructose corn syrup, high total sugar |
| Sodium | High sodium |
| Oils | Hydrogenated oil, measured trans fat, vegetable oils, beneficial oils |
| Additives and preservatives | Artificial colors, artificial sweeteners and sugar alcohols, shelf-life preservatives, nitrites/nitrates, texture additives |
| Flavors | Artificial flavor, natural flavor |
| Processing level | Refined flour |
| Positive ingredient signals | Short ingredient list, whole grains, protein and fiber, organic ingredient, organic wheat, organic milk |
| General nutrition | High saturated fat, low Nutri-Score |
| Ingredient sourcing | Conventional wheat, milk with or without sourcing claims |
| Allergens | Contains statement, May contain advisory |

## Negative findings

A negative finding subtracts points. Warning severity does not determine its category; the actual negative score adjustment does.

| Rule | Trigger | Effect |
|---|---|---:|
| Artificial color | A configured dye or caramel-color term is listed. | -20 once |
| Hydrogenated oil / trans fat | Hydrogenated oil is listed or nutrition data reports measurable trans fat. These share one group. | -30 once |
| Vegetable oil | Sunflower, canola, soybean, corn, palm, and other configured vegetable oils are listed. Olive, avocado, and coconut oil are exempt. | -15 once |
| High-fructose corn syrup | HFCS appears in the first three ingredients or later. It shares the added-sugar group. | -25 / -15 |
| Nitrites or nitrates | Sodium nitrite, sodium nitrate, or potassium nitrite is listed. | -25 |
| Shelf-life preservatives | One or more configured preservatives are listed. | -10 once |
| Added sugar | Added sugar is present, is among the first three ingredients, or exceeds 50 g. All added-sugar rules share one group. | -10 / -20 / -50 |
| Refined flour | Refined flour is among the first three ingredients and is not labeled whole grain. | -10 |
| Artificial sweetener | A configured artificial sweetener is listed. | -5 once |
| High sodium | More than 600 mg sodium per 100 g is reported. | -20 |
| High total sugar | More than 22.5 g total sugar per 100 g is reported. | -15 |
| Texture additives | More than two different configured gums or emulsifiers are listed. | -10 once |
| Artificial flavor | An ingredient contains "artificial" plus "flavor" or "flavour." | -10 once |
| Low Nutri-Score | Open Food Facts supplies grade D or E. | -15 / -25 |
| High saturated fat | More than 5 g saturated fat per 100 g is reported. | -20 |
| Conventional wheat | Wheat is listed without that ingredient being identified as organic. | -20 |
| Milk without sourcing claim | Milk is listed without an organic or Non-GMO claim. | -25 |
| Natural flavor | "Natural flavor" or "natural flavors" is listed. | -5 once |

High total sugar remains separate from added sugar because it measures a different nutrition signal. Added-sugar wording, HFCS, trans-fat evidence, repeated matches within a rule, and organic claims use shared groups where needed so the same concern is not counted twice.

## Informational findings

Informational findings explain a label signal but change the score by 0 points.

| Finding | Trigger | Effect |
|---|---|---:|
| Sugar alcohol | Sorbitol, maltitol, xylitol, or erythritol is listed without a stronger artificial-sweetener finding in the same group. | 0 |
| Milk with sourcing claim | Milk is listed with an organic or Non-GMO claim. | 0 |
| Contains allergens | The package explicitly identifies one or more allergens. | 0 |
| May contain allergens | The package gives a precautionary cross-contact advisory. | 0 |

Allergen statements are parsed into dedicated `containsAllergens` and `mayContainAllergens` lists. They reach the analysis layer as informational findings but are never inserted into the ranked ingredient list or used as ingredient penalties.

## Positive findings

A positive finding restores points after penalties. The maximum final score remains 100.

| Rule | Trigger | Effect |
|---|---|---:|
| Short ingredient list | The product has one to five listed ingredients. A missing list does not qualify. | +10 |
| Whole grains | A configured whole grain is among the first three ingredients. | +10 once |
| Protein and fiber | At least 10 g protein and 5 g fiber are reported, with under 10 g sugar. | +15 |
| Beneficial oil | Olive, avocado, or coconut oil is listed. | +5 once |
| Organic ingredient | Any ingredient is labeled organic. | +5 |
| Organic wheat or milk | Organic wheat or organic milk is listed. These share the general organic group. | +10 instead of +5 |

## Reviewed category decisions

- **Sugar:** ingredient-position sugar rules and quantified added sugar share the `added_sugar` scoring group. Only the strongest applies. High total sugar remains independent.
- **Sodium:** the active engine uses one high-sodium rule. The legacy high-salt rule is disabled because it uses the same threshold after conversion.
- **Oils:** sunflower, canola, soybean, corn, palm, and other configured vegetable oils subtract 15 points once. Olive, avocado, and coconut oil are exempt and can add 5 points once. Hydrogenated oil or measurable trans fat remains a separate 30-point concern.
- **Additives and preservatives:** repeated matches within a rule count once. Sugar alcohols are informational; configured artificial sweeteners remain a small negative.
- **Flavors:** artificial flavor is -10. The broad natural-flavor label term remains a smaller -5 transparency signal.
- **Processing level:** refined flour is the active ingredient-based processing signal. Nutri-Score is categorized as general nutrition, not processing. NOVA 4 is documented but is not an automatic active penalty.
- **Positive signals:** all point-restoring findings are classified as positive, including short ingredient lists and protein-and-fiber findings.

## Legacy rules not used by the active engine

- **High salt** duplicates the active sodium threshold.
- **NOVA 4** is not an automatic penalty; specific ingredient and nutrition findings determine the active score.

Every result derives its effect from its actual adjustment: `NEGATIVE` for a penalty, `INFORMATIONAL` for zero, and `POSITIVE` for an adjustment. Display text cannot change that classification.
