# AquaDose — Pool Chemistry Calculator (Android, Kotlin)

Native Android app built with **Kotlin + Jetpack Compose (Material 3)**.

- **Package name / applicationId:** `com.aquadoselab.poolchemistry`
- **Min SDK** 26 (Android 8.0) · **Target/Compile SDK** 35
- **Version** 1.0.0 (code 1)

## Open & run
1. Open the `AquaDose` folder in **Android Studio** (Ladybug 2024.2 or newer).
2. Let Gradle sync (it downloads Gradle 8.11.1, AGP 8.7.3, Kotlin 2.1.0).
3. Run on a device or emulator. Ads show Google **test ads** out of the box.

## Features
- **Animated splash screen** (system splash + animated logo, waves and progress) while ad consent loads.
- **Pool setup:** rectangle, round, oval, kidney or known volume; feet or metres; chlorine or salt (SWG); plaster, vinyl or fiberglass; indoor/outdoor; editable target levels with recommended defaults; the chlorine, acid and calcium products you use.
- **Water test entry:** FC, CC, pH, TA, CYA, CH, salt and temperature, with − / + steppers. Optional fields left blank reuse your last result.
- **Dosing plan:** step-by-step doses in the right order, with wait times, product-specific amounts (fl oz / cups / gal / oz / lb or mL / L / g / kg), side effects (e.g. TA drop from acid, CYA from dichlor), urgent flags, a "safe to swim" verdict, shock (SLAM) handling, partial-drain advice, and sharing the plan as text.
  - Chlorine targets scale with CYA (shock level = 40% of CYA).
  - Acid and soda-ash doses use a carbonate/cyanurate buffer model, so they account for alkalinity and stabilizer.
  - Water balance is shown as a **CSI** (Calcite Saturation Index), now and after the plan.
- **History:** trend chart per reading with the target band shaded, stats, test log, delete, and **CSV export**.
- **Guide:** what every reading means, ranges, how to raise or lower each, chemical order, testing tips, green-water fix, safety.
- **Multiple pools**, US/metric units, °F/°C.
- **Exit popup** on back from Home, with a native ad, "Rate us", Stay and Exit.

## Ads (AdMob)
| Placement | Where |
|---|---|
| Adaptive **banner** | Bottom of every screen (above the tab bar) |
| **Interstitial** | After "Calculate dosing plan", "Save pool", "Mark chemicals added", "Export CSV" (60-second cooldown between shows) |
| **Native** | Home (below readings / welcome), Dosing plan, History log, Guide (two slots), Exit dialog |

Native ads only appear once loaded, so they never leave a blank gap. Consent is gathered with Google's **UMP SDK** (GDPR / US states) before any ad request, and Settings shows "Ad privacy choices" where required.

### ⚠️ Before publishing, replace the TEST ids
1. `app/src/main/res/values/strings.xml` → `admob_app_id` (your App ID, with `~`).
2. `app/src/main/java/com/aquadoselab/poolchemistry/ads/AdsManager.kt` → `AdIds.BANNER`, `INTERSTITIAL`, `NATIVE` (your ad unit ids, with `/`).
3. `ui/screens/SettingsScreen.kt` → `PRIVACY_POLICY_URL` (a privacy policy is required by Play and AdMob).
4. In AdMob, set up the GDPR and US state messages under **Privacy & messaging**.

## Release build
`Build › Generate Signed App Bundle / APK…` → create a keystore → choose `release`. Minify and resource shrinking are on, and the ProGuard rules for kotlinx.serialization and Ads are included.

## Store assets (`store-assets/`)
- `playstore_icon_512.png` — Play Store icon (512×512)
- `feature_graphic_1024x500.png` — Play Store feature graphic
- `logo.svg`, `logo_512.png` — rounded logo for web/social
- The launcher icon is an adaptive vector (`res/drawable/ic_launcher_*.xml`). `tools/generate_logo.py` regenerates every logo file from one source.

## Project layout
```
app/src/main/java/com/aquadoselab/poolchemistry/
  MainActivity.kt, AquaDoseApp.kt
  ads/      AdsManager (consent, init, interstitial), AdViews (banner, native)
  chem/     Chemistry (volume, ranges, buffer model, CSI), DosingEngine, Products, Units
  data/     Models (Pool, WaterTest, settings), Repository (JSON file storage)
  ui/       AppRoot (navigation, bottom bar), AppViewModel, theme/, components/, screens/
```

## Disclaimer
Doses are estimates. Results depend on accurate tests, correct pool volume and product strength. Users should always follow product labels.
