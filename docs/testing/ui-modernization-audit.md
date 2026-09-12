# UI modernization audit and implementation record

## Repository inspection (before this redesign)

1. UI: Java Activities and XML Views, RecyclerView adapters and custom Canvas views. No Compose or ViewModel usage found. Material Components 1.12 supplies Material 3 widgets.
2. Navigation: explicit Intents, Activity Result launchers, product-detail fragment/bottom-sheet flows. Five-item bottom navigation currently lives on Home. Debug-only preview activities exist.
3. Themes: Material3.DayNight.NoActionBar in values and values-night; semantic colors plus legacy glass-named drawables. ThemeManager persists appearance; styles.xml contains button/input/profile styles. Previous UI work is uncommitted and will be retained.
4. Reusable UI: GlassMotion, PantryActionViewBinder, PantryListStateViewBinder, ProductFindingsViewBinder, source-status presentation, retailer card adapters and custom scanner/AI views.
5. State: Activity fields, executor services, callbacks and main-thread handlers. No central navigation graph or observable ViewModel architecture. Preserve callbacks and lifecycle behavior rather than introduce a framework migration.
6. Database: Room AppDatabase v11, ProductDao/AdditiveDao; Product, Nutriments, Ingredient, Pantry, CacheMeta and AdditiveEntry entities. Existing destructive-migration fallback makes schema changes especially undesirable. This redesign will not change schema.
7. Data: ProductRepository and lookup providers, Retrofit/OkHttp, Bitwise backend/AI services, retailer and recall repositories. Source freshness and fallback logic have tests.
8. Scanner: CameraX camera lifecycle/preview plus ML Kit barcode, OCR, language identification and translation; permission, photo and recovery paths already exist.
9. Localization: values, values-es and values-fr; LanguageManager wraps Activity context and persists the locale. New UI labels will use resources. Catalog and generated source content can remain in its supplied language.
10. Tests: 239 JVM tests passed at baseline; instrumentation covers Room persistence, pantry navigation, product findings, sources, marketplace, scanner and OCR. Lint has preexisting warnings. Baseline version 1.11.0 (14).

## Planned changes

Modify BaseActivity, MainActivity, PantryActivity/Adapter, AdditiveDatabaseActivity/Adapter, product-detail layouts and presentation callbacks, themes/colors/styles/strings, and related XML cards. Preserve IDs required by existing feature binders and tests.

Create ui/ components (responsive container, navigation, score ring, state panel, section and card presentation), adaptive screen layouts, local scan-history presentation without a Room migration, ingredient detail presentation, and focused UI/presentation regression tests.

## Risks and presentation rules

- Numeric healthScore and rule verdict are different fields. Display existing values and explicit provenance; never manufacture a score from the reference image.
- Home currently has hardcoded featured product scores and links. Replace unsupported personalized claims with existing-data states and truthful alternative navigation.
- No dedicated scan-history model was found. Record successful viewed scan results locally from this build onward; do not relabel cache refresh timestamps as scan times.
- Preserve pantry save/delete/export, user rating, auth, scanner, AI/RAG and source callbacks. Filters must not change the underlying export list or delete the wrong item.
- Keep ingredient cards concise; show existing evidence fields in details and explicitly mark missing regulatory information. Do not invent scientific claims.
- Test state restoration, longer Spanish labels, font scaling, compact and expanded layouts. Device visual evidence is distinct from compile/test success.

## Order and gates

1. Audit (this document).
2. Tokens; 3. shared components; 4. responsive container; 5. navigation.
6. Home; 7. scan result; 8. Pantry; 9. ingredient database.
10. Profile; 11. states; 12. accessibility/responsive QA; 13. regression tests.

Build and JVM checks after each major screen. Resolve compiler errors before the next screen. Record actual verification and limitations below as work completes.

## Phase record

| Phase | Files and result |
| --- | --- |
| 2–3: tokens/components | Updated values/colors.xml, values-night/colors.xml, styles.xml, shared backgrounds; added ui_strings.xml in EN/ES/FR, ui_dimens.xml, ScoreRing, ExpandableSection, ContentStateView, LoadingSkeleton and card/button/search/chip styles. Build and JVM suite passed. |
| 4–5: responsive/navigation | Added ResponsiveContentContainer, AdaptiveColumns and AppNavigation; refactored BaseActivity to host the shared shell. Compact windows use bottom navigation; expanded windows use a rail. No navigation-graph migration. Build and JVM suite passed. |
| 6: Home | Reworked MainActivity and activity_main.xml; added RecentProductAdapter, ui_recent_product.xml and account-scoped ScanHistory. Both scanner entry points work through the existing scanner. Existing retailer repository supplies suggestions after a successful scan. Hardcoded featured scores were removed. Daily tips and paired sources remain. Build and JVM suite passed. |
| 7: result | Updated activity_product_details.xml, fragment_product_details.xml and their presentation callbacks. Added ProductPresentation for existing rule scores, label values, catalog name matches and expanded layout. Existing findings/scoring/repository calls remain. Ingredients, nutrition, additives, AI explanation and sources use disclosures. Build and JVM suite passed. |
| 8: Pantry | Updated PantryActivity, PantryAdapter, pantry_list_item.xml; added expanded activity_pantry.xml and PantryPresentation. Search and filters operate on copies, preserving the export list. Existing risk calculation drives the review count; unknown scores are not labeled good. Insight actions retain their IDs/callbacks. Pantry refreshes after shared-navigation scans. Build and JVM suite passed. |
| 9: ingredients | Updated AdditiveDatabaseActivity/Adapter and layouts; added expanded master/detail layout, IngredientPresentation and IngredientDetailsView. Concise cards open actual record details and existing source links. Selection has a visible outline. Remote-result presentation honors the current search/filter. Build and JVM suite passed. |
| 10–11: profile/states | Profile uses constrained form width and shared controls/navigation. Existing profile/auth actions remain. Loading placeholders replace decorative loading imagery; scanner permission/error handling remains. Missing data is explicit. Build, JVM tests and lint passed after fixing API-26 style compatibility and translations. |
| 12–13: QA | Added ModernPresentationTest (filter immutability, unknown-score handling, EN/ES category mapping) and ModernUiLayoutTest. 242 JVM tests pass. The device layout test passes for six layouts at 360/1000 dp, both themes, English and Spanish, font scale 1.3. This structural test is separate from live Activity visual QA. |

## Reusable component map

- AppTopBar, PrimaryButton, SecondaryButton, SearchField, FilterChip, ProductCard, AlternativeProductCard, IngredientCard, SectionHeader, SourceLink and ScoreBadge are reusable XML styles.
- ScoreRing, ResponsiveContentContainer, AdaptiveColumns and ExpandableSection are code-backed Views.
- ContentStateView supplies empty/loading/error/offline copy and retry actions; LoadingSkeleton supplies static product placeholders.
- AppNavigation owns the five destinations and switches between bottom navigation and rail.
- Existing RetailerAlternativeAdapter, PantryAdapter and source/findings binders remain the data-binding components.

## Data boundaries

The Room schema, network request/response contracts, scoring rules, auth and billing implementation were not rewritten. Successful barcode result presentation records device-local history; it does not reconstruct older scan events. Nutrition drivers display supplied values without inferring missing ones. Additive catalog matches are explicitly non-exhaustive and do not alter scoring. Regulatory information absent from a record is marked absent, with the existing source available for review. EN/ES/FR chrome is localized; externally supplied catalog prose retains its source language.

Visual reviews so far: live tablet Home, Pantry and ingredient master/detail. The Home gutter and ingredient selection outline were corrected from those reviews. Final installed-build checks are recorded below.

## Final build verification — 2026-09-09

- Build: 1.11.0 (14), debug, based on acb3ea1c643818f76b7d693e5c5750866be940d5 plus the working-tree changes. Existing unrelated M8 changes are retained.
- APK SHA-256: `13E28B860A8FEDEF4BF0550DDAA8DB1BAF6FA2DBFB57BA5990EE55C52452CD01`.
- Gradle assembleDebug, testDebugUnitTest, lintDebug and assembleDebugAndroidTest succeeded. JVM results: 242 tests, zero failures. Lint: zero errors, 275 warnings; warnings include both existing and new findings and are not represented as a clean warning baseline.
- ModernUiLayoutTest passed on Samsung SM-X800: six layouts × two widths (360/1000 dp) × two themes × EN/ES, with 1.3 font scale. Checks cover inflation, adaptive detail-pane presence, scan target heights and disclosure toggling. This is one parameter-loop test, not 48 independent end-to-end tests.
- Live Activity captures verified Home spacing and centered compact Scan navigation, Pantry and ingredient browsing in Spanish at 360 dp/130% text. The initial Pantry insights panel constrained the list; the compact layout now retains a single insights action. The tablet keeps its full panel.
- Device display settings (size, density, rotation and font scale) were restored after compact-window checks. Screen sharing uses a virtual display; captures of the physical display were black and were discarded as visual evidence.
- Evidence directory: `C:/Users/ciblo/.codex/visualizations/2026/09/07/01a07ceb-a154-7c73-8d25-6334ec1d22b1/`. Files ending `-compact-final.png` and `home-tablet-final.png` are live Activity captures. Earlier detached layout screenshots are structural-test artifacts, not proof of production rendering.

### Remaining verification limits

Live camera acquisition was not exercised because camera access is denied on this device. This UI pass does not certify live AI/network availability, purchases, every authenticated profile mutation, or every scanner recovery path end to end. Existing JVM feature tests pass, but the full device suite was not run against the user's populated database. Externally sourced catalog content retains its supplied language. These limits should remain visible in release/study sign-off.


Portrait follow-up: reduced Home header and hero padding; replaced the three stacked quick-action buttons with equal-width icon tiles; reduced empty-state padding and body type. Compact Pantry now uses one insights action, leaving the product list more height. Added regression assertions for aligned quick actions and minimum compact Pantry list height.

Portrait follow-up verified: final APK installed successfully; expanded ModernUiLayoutTest passed with the Pantry viewport and tile alignment assertions. Live Home/Pantry captures at 360 dp and 130% text are MainActivity-portrait-fix.png and PantryActivity-portrait-fix.png in the evidence directory. The full saved product row is now visible above the compact insights action. Display overrides were restored. These supersede the earlier compact Home/Pantry captures.
