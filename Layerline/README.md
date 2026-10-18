# Layerline — 3D Print Toolkit (Android, Kotlin)

Package: `app.layerline.toolkit3d` · Kotlin + Jetpack Compose · min SDK 26 · target SDK 36 · compile SDK 37

## What's inside

| Screen | What it does |
| --- | --- |
| **Splash** | Animated logo: the nozzle prints the "L" layer by layer, the spool ring fills, the wordmark is printed letter by letter while a G-code boot log types out. |
| **Workbench** | Live progress of the print that is running (layer-stack animation, time left, Done / Failed), tool tiles, machine garage shortcut, this month's filament / spend / hours. |
| **Print cost** | 20 regions (currency, electricity, filament and labour rates fill in automatically), 18 printer presets plus your own printers, 10 materials or price from a spool in stock, weight / time / pieces / margin steppers, extras (supports %, heat inserts, paint, labour, packaging, failure buffer, tax), receipt-style breakdown, save and share quotes. |
| **Saved quotes** | Every saved quote with share and delete. |
| **Filament stock** | Spools drawn as rings showing what's left, low-stock warnings, value on the shelf, material filters, and a weigh tool (gross − empty spool = grams left, plus length in metres) that updates a spool in one tap. |
| **Calibration** | 8 calculators: E-steps, Klipper rotation distance, flow, shrinkage / XY scale, pressure advance, retraction tower, temperature tower, max volumetric flow. Each shows the result, a ruler with OLD/NEW markers, the formula, ready-to-send G-code with copy, and a history you can roll back from. |
| **Print log** | Timeline with a success-rate ring, filters, failure reasons and how far the print got. Logging a finished or failed print takes the filament off the linked spool and adds the hours to the printer automatically. |
| **Machine garage** | Your printers with running cost per hour, hours run, payback %, and maintenance counters (nozzle, belts, rods, plate…) that turn orange when service is due. |
| **Settings** | Region, all rates, default margin and failure buffer, ad privacy choices (shown when the user's region requires it), privacy policy link. |
| **Exit popup** | Back on the Workbench opens an animated "cool-down" dialog (Stay / Exit). |

Everything is stored on the device (Room database + DataStore). There's no account and no server.

## Open and run

1. Android Studio → **Open** → pick this `Layerline` folder (not the repository root).
2. Let Gradle sync. Versions used: AGP 9.4.0 with built-in Kotlin (no `org.jetbrains.kotlin.android`), Gradle 9.6.0, Kotlin 2.4.20, KSP 2.3.12, Compose BOM 2026.09.00, Room 2.8.5, Google Mobile Ads 25.5.0, UMP 4.0.0. They're all in `gradle/libs.versions.toml`.
3. Run the `app` configuration. Debug builds use **Google's test ad units**, so you can tap ads safely.

If Gradle can't find a version, bump that line in `gradle/libs.versions.toml`. Keep `kotlin` and the KGP classpath line in the root `build.gradle.kts` the same.

## Ads

| Format | Where | Rule |
| --- | --- | --- |
| Banner (anchored adaptive) | Bottom of every screen, under the navigation bar | If the first request fails, a second goes out right away. After 2 failed calls in a row, each retry waits 15 s. No requests are made while the app is in the background. The slot keeps its height, so content never jumps. |
| Interstitial | After a major action: saving a quote, logging a print, adding a spool or printer, updating a spool by weight, saving a calibration, finishing a running print | At most 1 per 30 s, counted from when the last one closed (and from app start, so none at launch). Never on exit or in the middle of a task. |
| Native | In free space between blocks on Workbench, Stock (after 2 rows), Log (after 3 entries), Calibration, Machines, Saved quotes | Labelled "Ad" with AdChoices. Hidden completely when no ad is loaded. Cached per placement and refreshed after ~50 min. |

The live IDs are set for **release** builds only, in `app/build.gradle.kts`. The live AdMob App ID goes into the manifest through `manifestPlaceholders`.

## Before publishing on Google Play

1. **Privacy policy**: host one and put its URL in `app/src/main/res/values/strings.xml` (`privacy_policy_url`) and in Play Console. It must cover the data the AdMob SDK collects (advertising ID, IP address, device info, ad interactions).
2. **Consent**: in AdMob → Privacy & messaging, create a GDPR message (and a US states message if you serve there). The app already runs the UMP flow before requesting any ad and shows "Ad privacy choices" in Settings when required.
3. **Data safety form**: declare "Contains ads", and that device or other IDs, app interactions and diagnostics are collected and shared by the Google Mobile Ads SDK for advertising and analytics. The app itself sends nothing anywhere.
4. **app-ads.txt**: add it to the website listed on your Play store page.
5. **Target audience**: general / adults (not "Designed for Families").
6. **Signing**: create a release keystore (Build → Generate Signed App Bundle). Keystore files are git-ignored.
7. **Don't click your own live ads.** Test with debug builds, or add your device as a test device in AdMob.

## Branding

- `branding/layerline-icon.svg` and `branding/layerline-icon-512.png`: the store icon (512 × 512).
- The adaptive launcher icon (`res/drawable/ic_launcher_*.xml`) has background, foreground and monochrome (themed icon) layers.
- Fonts: Chakra Petch, IBM Plex Mono and Familjen Grotesk, all SIL Open Font License (see `licenses/`).

## Tests

`app/src/test` holds JVM unit tests for the cost maths, every calibration formula, filament length and weight, and the ad retry and frequency rules. Run `./gradlew testDebugUnitTest`.
