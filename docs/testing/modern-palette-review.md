# Modern UI implementation

Approved direction: light gray surfaces, opaque white cards, cobalt actions, centered Scan navigation, and brief motion. Existing account appearance preferences are preserved; new installations default to light mode.

## Screen coverage

| Screen family | Shared treatment |
| --- | --- |
| Home and profile | Neutral surfaces, cobalt actions, readable secondary text; Home / Pantry / Scan / Database / Profile navigation on Home |
| Product activity and detail fragment | Theme-aware card backgrounds, borders, bottom sheets and drag surfaces |
| Ingredient database and analysis | Shared activity palette and neutral content surfaces |
| Pantry, insights and AR | Shared surrounding controls and surfaces; camera imagery and semantic chart colors retained |
| Scanner | Blue focus frame and primary control; camera scrims retained for legibility |
| Marketplace | Neutral alternative cards, blue category chips, separate theme-aware availability badges |
| Subscription | Cobalt header with white text, shared content surfaces |
| Sign-in and splash | Shared palette, softened blue AI artwork |
| Sources, web and privacy | Shared app chrome; local privacy stylesheet updated; external publisher pages retain their own styling |
| Add product and recalls | Shared theme and surfaces; distinct safety/status colors retained |

## Motion

- Content entrance: 240 ms, 8 dp movement; stagger delays capped at 120 ms.
- Existing press feedback remains brief.
- Scanner sweep and AI decoration honor disabled Android animators.
- Successful pantry saves reveal the resulting action after persistence succeeds.
- Theme changes use Android activity recreation; a live crossfade of every individual color is not implemented.

## Layout and controls

- Home uses a prominent Scan action, a two-column shortcut row, and a full-width Marketplace shortcut. Scan remains the center navigation destination.
- Shared buttons use 12 dp corners, sentence case and a minimum 48 dp height. Profile action heights expand with text.
- Outlined inputs use matching 12 dp corners; primary content cards use restrained rounding and flat surfaces.
- Standard activity content is centered and capped at 1040 dp on wide displays. Account, sign-in, add-product and subscription forms are capped at 640 dp; camera surfaces remain full-screen.
- Scanner recovery panels are capped at 560 dp and use the current theme's readable surface; camera controls are capped at 640 dp.
- Standard toolbars use neutral surfaces; blue and camera headers use light icon themes. System bars are kept clear of content.
- Pantry and Marketplace use distinct vector icons. Selected controls use the shared blue palette instead of Material's default purple.

## Device review

Review device: Samsung SM-X800, 2800 x 1752 landscape. Home and Profile were inspected on-device; the constrained profile form and primary Scan hierarchy render correctly. Pantry, Subscription and the debug product-detail layout fixture were also inspected. The fixture verifies content styling, not live product retrieval.

Visual findings addressed during review: over-wide forms, touching Home shortcut cards, fixed-height profile actions, low-contrast toolbar icons, stretched scanner recovery UI, and default purple selection fills. No account data was edited.

The final installed build was rechecked on Home, Pantry and Subscription. Profile was checked in dark mode and restored to light. Database cards and the scanner's camera-permission recovery panel were also visually reviewed. Camera permission was unavailable, so live capture was not verified.

Installed debug APK SHA-256: `AB9DBE7D44186D002804DF8F084C33A4068A04D934ED635DE31099424B9A0EB2`. Final checks: 239 tests passed; lint had zero errors and 220 warnings. `git diff --check` passed.

This is a tablet visual review, not exhaustive certification of every phone size, font scale, network-dependent state, purchase flow or camera hardware path.

## Verification

- 239 unit tests passed, zero failures or skipped tests.
- Android lint completed with no errors; existing warnings remain.
- Automated contrast audit: primary/secondary text, primary accent on cards, and success badges exceed 4.5:1 in both themes.
- Menu order verified with Scan in position three of five.
- Device visual verification is recorded separately from automated checks; build success does not establish every screen's visual correctness.

Baseline: 1.11.0 (14), working tree based on acb3ea1c643818f76b7d693e5c5750866be940d5. Changes from earlier M8 tasks remain in the working tree.
