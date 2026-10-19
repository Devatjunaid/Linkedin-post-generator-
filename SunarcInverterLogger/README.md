# Sunarc – Solar Inverter Logger (Android, Kotlin)

Photograph your inverter's display (or a screenshot of its monitoring app). Sunarc reads the numbers on the phone, logs them and charts your daily output. No paid API, no trial API and no cloud: text recognition is Google ML Kit's bundled on-device model.

## Open and run

1. Android Studio Quail 4 (2026.1.4) → **Open** → this `SunarcInverterLogger` folder.
2. Gradle JDK 21. The wrapper uses Gradle 9.6.0, AGP 9.4.0, with AGP's built-in Kotlin (no `org.jetbrains.kotlin.android`).
3. compileSdk 37, targetSdk 36, minSdk 26. KSP 2.3.12 for Room (no kapt). `kotlin { compilerOptions { } }` is used.
4. Run the `app` configuration. Debug builds use Google's **test** ad units; release builds use your real AdMob IDs (see `app/build.gradle.kts`).

Library versions are in `gradle/libs.versions.toml`. They were pinned without access to Google Maven, so Android Studio may offer newer AndroidX/Compose/AdMob versions. Accepting them is safe.

## Features

| Area | What it does |
|---|---|
| Animated brand | Logo drawn live from nine layers (halo, orbit, trail, sun, turning rays, panel with moving glint, output bars, bolt). Animated splash with horizon rings, rising light and a letter-by-letter wordmark. Exit popup in which the sun sets behind the panel. Adaptive launcher icon with a themed (monochrome) layer. |
| Region-aware setup | North America / Europe, 23 countries (currency, decimal style, default tariff model, grid CO₂ factor) and 22 brands: Enphase, SolarEdge, Tesla, SMA, Fronius, Generac, Sol-Ark, APsystems, Hoymiles, Schneider, Huawei, Growatt, GoodWe, Sungrow, Kostal, Victron, SolaX, Solis, Deye, Sofar, FIMER/ABB, Delta, plus a generic option. |
| Scanner | CameraX preview with a guide frame and scan beam, live values while you aim, torch, and gallery import for app screenshots (Photo Picker, so no storage permission). Each photo gets several clean-up passes (contrast stretch, threshold, stroke thickening, inverted) to cope with LCD and seven-segment displays. |
| Parser | Labels in English, German, Dutch, French, Spanish, Italian, Polish and more (E-Today, Tagesertrag, E-Jour, Energía hoy…). Handles units (Wh/kWh/MWh, W/kW), `1.234` vs `1,234`, OCR look-alikes (O→0, B→8…), brand auto-detection and fault codes. |
| Checks before saving | Lifetime total never goes down, today can't exceed clear-sky output, power can't exceed system size. |
| Sun Arc home | Today's kWh over a clear-sky curve worked out offline (NOAA sun position + clear-sky model), sunrise/sunset, expected total by sunset, live power, lifetime, money saved, CO₂. |
| History | Week, month and year bar charts, month heatmap, best day, average, kWh per kWp, reading list with delete. Days without a scan are filled in from the lifetime counter. |
| Insights | System health versus clear-sky expectation, money split (self-used / exported) for net metering, net billing or feed-in, payback date, CO₂, yearly degradation, PDF monthly report. |
| Fault codes | Offline library: brand codes (Fronius, Huawei, SMA) plus keyword decoding (isolation, grid voltage, arc fault, temperature…) that works for any brand, with what to do. |
| Wi-Fi read | SunSpec Modbus TCP reader for SMA, Fronius, SolarEdge, Kostal, Sungrow and FIMER on the home network. Talks directly to the inverter; no cloud. |
| More | Several inverters, manual entry, sunset reminder and monthly summary (WorkManager, no exact alarms), home-screen widget (Glance), CSV export, PDF report, JSON backup/restore. |

## Ads (AdMob)

- **Consent first:** Google UMP runs on launch, and no ad is requested until UMP allows it. A "Ad privacy choices" entry appears in Settings when the user's region requires it. **In AdMob, set up the GDPR and US-states messages** under *Privacy & messaging*, otherwise EU/UK users get no consent form.
- **Banner** (anchored adaptive) under the main tabs. The first two requests go out right away; after two failures every retry waits 15 s (`BannerRetryPolicy`). The slot takes no space until an ad loads.
- **Interstitial** only after a finished major action: saving a reading, CSV/PDF export, or the monthly report. At most **one every 30 s** (`FrequencyCap`), never in the first 30 s of a session, never on launch or exit, and never mid-task.
- **Native** ads in free gaps: Home (below the quick actions), History (between heatmap and readings), Insights (between stats and the report), Faults (after the third entry). Each is marked "Ad", keeps AdChoices and collapses when no ad is available.
- **No ads** on the splash, scanner, confirm screen, settings or exit popup. AdMob does not allow ads in an exit dialog or next to camera controls.

## Google Play checklist (things only you can do)

1. **Privacy policy:** host `PRIVACY_POLICY.md` (for example on GitHub Pages), put its URL in Play Console, and set `PRIVACY_POLICY_URL` in `ui/screens/ScreenUtils.kt`.
2. **Data safety form:** the app itself collects nothing; readings, photos and location stay on the device. Declare what the **Google Mobile Ads SDK** collects (device/advertising ID, approximate location from IP, app interactions, diagnostics) for advertising/analytics, encrypted in transit, not optional. Google's SDK guidance lists the exact answers.
3. **Ads declaration:** "Yes, contains ads". **Target audience:** 18+ / not designed for children (the app tags requests as not under the age of consent).
4. **app-ads.txt:** add your AdMob line to `app-ads.txt` on the developer website listed in Play Console.
5. **Permissions:** Camera (asked when the scanner opens; the gallery works without it), Notifications (asked only when a reminder is turned on). No storage, location or exact-alarm permissions.
6. Test with your device registered as an AdMob test device, and never tap your own live ads.

## Checks run while building

The pure-Kotlin core (`domain/`: parser, number handling, sun model, aggregation, money, sanity checks, ad caps, SunSpec decoding, CSV, image passes) has 26 JUnit tests in `app/src/test`. All passed on Kotlin 2.4.21.

The shared Compose UI (theme, logo, splash and exit popup, charts, components) was also type-checked against the Compose APIs.

The full Android build (AGP, AndroidX, CameraX, ML Kit, AdMob) could not be compiled in the build environment because Google Maven was not reachable. Run **Build → Make Project** once in Android Studio. If anything fails, it will most likely be a library version in `libs.versions.toml`.

## Licences

Fonts: Space Grotesk and IBM Plex Sans/Mono, SIL Open Font License 1.1 (see `licenses/`).
