# Buildbros: Construction Material Calculator (Android, Kotlin)

Package: `com.brickbeam.buildbros` · Jetpack Compose · minSdk 26 · targetSdk 36 · compileSdk 37

Bricks, blocks, concrete, rebar, tiles and paint, with local units, standard sizes, bag weights and
tax for all 50 US states + DC and 31 European countries.

## Open & run
1. Android Studio Quail 4 → *Open* → this `Buildbros` folder. Gradle JDK 21.
2. AGP 9.4.0 / Gradle 9.6.0 (wrapper included). Built-in Kotlin is used, so there is no
   `org.jetbrains.kotlin.android` plugin. The Kotlin Gradle plugin is pinned once in
   `gradle/libs.versions.toml` (`kotlin = 2.4.10`) and is shared with the Compose compiler plugin.
3. Run the `app` configuration. **Debug builds use Google's official test ad units**, so you can tap ads safely.
   Release builds use your real AdMob IDs.

Unit tests for the formulas: `./gradlew :app:testDebugUnitTest`.

## Features
| Area | What it does |
|---|---|
| Regions | 50 states + DC (state sales tax, local rate per project) and 31 European countries (VAT shown as included). Each region sets its currency, units, default brick, bag sizes, can sizes and rebar sizes. |
| Bricks / blocks | 23 real unit sizes (US modular, standard, queen, king, utility, CMU 4–12", UK, DE NF/DF/2DF, NL, BE, DK, IT, ES, AAC). Doors/windows deducted, single/double wythe, tied veneer, joint, wastage, pallets, mortar from wall volume minus brick volume, cement bags + sand, wall ties. Shows a to-scale wall drawing. |
| Concrete | Slab, footing, wall, round/square columns, round pad, steps. US psi or EN C-classes, cement, sand, gravel and water. Compares hand-mixing, pre-mixed bags and ready-mix (truck count, ¼-unit rounding, short-load warning) and recommends one. |
| Steel | Rebar mesh with cover and spacing in both directions. US #3–#8 or Ø6–Ø25, stock lengths, lap splices, first-fit-decreasing cut plan with offcut %, weight to buy, tie wire and chairs. |
| Tiles | Row × column count for straight lay, plus cut waste for offset, diagonal and herringbone. Spare %, boxes, adhesive by trowel notch (back-butter for large format) and grout by joint size. Shows a plan view of the pattern. |
| Paint | Room or wall area, doors/windows, coats, ceiling, primer, surface factor. Picks the **cheapest can mix** (e.g. 1×5 L + 1×1 L, or 2 gal + 2 qt) and shows the leftover. |
| Estimates | Projects saved on the device (Room). "Already have it" toggle, edit/add items, contingency, delivery, budget tracker, cost by material. Export to PDF (Letter in the US, A4 elsewhere), CSV or plain text. |
| Prices | Every price is editable per currency. The sample defaults are clearly labelled as samples. |
| Converter | Length, area, volume, weight, psi/MPa, paint coverage, bar weight. |
| UX | 4 themes (Concrete Night, Blueprint, Terracotta, Hi-Vis), animated material graphics, animated multi-part logo, animated splash with a spirit-level loader, exit dialog, animations on/off switch. Works offline with no account. |

## Ads (AdMob) – how it is wired
* **Consent first**: the Google UMP SDK collects GDPR/UK/Swiss and US-state consent. No ad request is made until `canRequestAds()` returns true. A *Privacy & ad choices* entry appears in Settings when required.
* **Banner**: anchored adaptive banner at the bottom (`BannerAdBar`). The first **2 requests** run back to back. If both fail, each further request **waits 15 s**. A successful load resets the counter.
* **Interstitial**: kept pre-loaded and shown only after a completed major action (*Save to project*, *Export PDF*, *Export CSV*). It is capped at **1 per 30 s**. It is never shown at launch, on exit, or in the middle of typing.
* **Native**: a themed card with an "Ad" badge and AdChoices, in free space on Home, under calculator results, in Projects, in the Estimate and in Prices. It only takes up space once an ad has loaded. Ads are cached per slot for 55 minutes.

IDs live in `app/build.gradle.kts` (release) and the AdMob App ID goes into the manifest through `manifestPlaceholders`.

## Before publishing (Google Play / AdMob policy checklist)
1. **Privacy policy**: host one and put its URL in `res/values/strings.xml` → `privacy_policy_url`. It must mention AdMob and the advertising ID.
2. **Data safety form**: declare *Device or other IDs* (advertising ID), *App interactions* / *Diagnostics* collected by the Google Mobile Ads SDK, shared for advertising. Project data stays on the device.
3. **Target audience**: set it to adults/general (not children). The app isn't designed for kids, so Families policy ads settings don't apply.
4. **app-ads.txt**: publish it on the developer website listed in Play Console.
5. In the AdMob console, create the GDPR and US-state messages (Privacy & messaging) so UMP has a form to show.
6. Sign the release with your upload key (`Build → Generate Signed App Bundle`).
7. Prices are labelled as editable samples and every result carries a "not professional advice" note. Keep those to avoid misleading-claims issues.
8. Check that the name "Buildbros" isn't already used as a trademark in your markets before listing.
