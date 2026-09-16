# M9-09 — Official food classification badges

## Behavior

Both Android product detail screens share `ProductCertificateBadgeRenderer`.
The renderer uses bundled artwork only for explicit aliases in
`ProductCertificateLogoCatalog`. Generic Vegan, Vegetarian and Gluten-Free claims
use app-owned dietary icons with a readable label and a bottom "Not verified"
banner. These leaf, carrot and crossed-wheat icons are not certification marks.
Other generic claims remain readable text.
Unknown organizations and unsupported certification variants retain their source
label rather than receiving another organization's mark. In particular, OU-D,
OU-DE and OU-P must not receive the plain OU mark.

Removed the Canvas-drawn organic, Non-GMO and kosher imitations and the colored
text-box fallback. Logos preserve their aspect ratio, use a contrasting neutral backing for
dark-mode contrast, and have an accessible source-label description. No network
request is needed to display a badge.

## Newly sourced assets

Retrieved September 15, 2026. Artwork belongs to its respective organization.
These assets identify certifications reported by product data; they do not
independently verify a product's current certification.

| Resource | Official source | Treatment |
| --- | --- | --- |
| `cert_ou_kosher.png` | [OU Kosher symbols page](https://oukosher.org/blog/industrial-kosher/all-ou-symbols-explained/), [original PNG](https://res.cloudinary.com/orthodox-union/image/upload/v1690366751/kosher-assets/oukosher.logo.png) | Original PNG, unchanged |
| `cert_gfco.png` | [GFCO certification page](https://gfco.org/certification/), [original PNG](https://gfco.org/wp-content/uploads/2022/04/GFCO_FullLogo_PurpleGreen_CMYK-R-300x206.png) | Original PNG, unchanged |
| `cert_vegan_action.xml` | [Vegan Action consumer page](https://vegan.org/certification/consumer-info), [original SVG](https://vegan.org/wp-content/uploads/2025/06/vegan-icon-color.svg) | Exact SVG path and 500×500 viewport transferred to Android VectorDrawable; currentColor resolved to black |

Existing bundled assets remain in use for explicitly identified USDA Organic,
Non-GMO Project, Fair Trade USA, Rainforest Alliance, B Corp, Regenerative Organic
Certified, Certified Humane, Green Dot and Triman marks. The legacy GFCO image,
Vegan Action website banner, generic halal symbol and unspecified whole-grain
stamp are no longer selected by the Android renderer.

## Validation

- `ProductCertificateParserTest`: existing label parsing behavior.
- `ProductCertificateLogoCatalogTest`: exact organization matching, generic and
  ambiguous labels, OU variant fallback, and order-independent selection of a
  known logo over a generic claim.
- `ProductCertificateBadgeRendererTest`: both production layouts in light and
  dark configurations, actual drawable loading, image versus text behavior,
  accessible descriptions, dietary icons and "Not verified" banners, measured
  row bounds, rebinding and empty input.
  Writes row previews to the test app's external files `m9-09` directory.

Run from `app` in PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.ciblorenzo.whatsonmyfood.ProductCertificateParserTest --tests com.ciblorenzo.whatsonmyfood.ProductCertificateLogoCatalogTest
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ciblorenzo.whatsonmyfood.ProductCertificateBadgeRendererTest'
```
