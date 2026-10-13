# SnapTensio: BP Camera Log

An Android blood pressure logbook written in Kotlin and Jetpack Compose. Point the camera at any digital blood pressure monitor and the systolic, diastolic and pulse numbers are read on the device. No paid APIs, no server, no account.

- **App name:** SnapTensio (store title suggestion: "SnapTensio: BP Camera Log")
- **Package:** `com.snaptensio.bplog` (debug builds install as `com.snaptensio.bplog.debug`)
- **Min / target SDK:** 24 / 35

## Features
| Area | What it does |
|---|---|
| Camera scan | CameraX + ML Kit on-device text recognition (bundled, works offline). Fixes common 7-segment misreads, ignores clocks and small print, and only accepts numbers that match on 3 of the last 6 frames. Torch, pick-from-gallery, and "type instead". |
| Reading form | Edit scanned values, date/time, arm, position, tags (incl. custom), note. Validates plausible ranges. |
| Guided session | Preparation checklist, 3 readings 1 minute apart with a rest timer, averaged (optionally ignoring the first). |
| Very high alert | Readings over 180/120 open a safety screen: re-measure timer, symptom checklist, one-tap dial for emergency number and doctor. No ads. |
| History | Grouped by day, filters (morning/evening/high), search by tag/note/value. |
| Trends | 7D/30D/90D/1Y animated chart with target lines and medication-start markers, range distribution, morning vs evening, highs/lows, change vs previous period. |
| Insights | Rule-based (no AI): trend change, medication effect, morning/evening gap, tag effects (e.g. coffee), arm difference, pulse pressure, consistency. |
| Doctor report | PDF with summary, chart, range breakdown, medications and full table, with an on-screen preview. CSV export. |
| Meds & reminders | Medications with dose times, mark taken, 7-day adherence ring, refill warning, daily measurement reminders (inexact alarms, survive reboot). |
| Settings | Several people per phone, each with their own target; AHA/ACC 2017 or ESC/ESH ranges; theme; CSV backup/import; ad privacy choices; privacy policy. |
| Brand | Animated logo (Compose), animated system splash icon (AVD), branded splash, adaptive + themed launcher icon, Play Store icon and feature graphic in `branding/`. |

## Ads (AdMob) and how they stay within policy
- **Consent first:** Google UMP consent form runs before any ad request. "Ad privacy choices" in Settings when required.
- **Banner:** anchored adaptive banner at the bottom of Home, History, Trends, Meds, Insights, Report and Settings, above the tab bar, with its height reserved so content never jumps.
- **Interstitial:** only at natural breaks *after* a task completes: saving a reading or session, saving a medication, opening Insights or the Report, finishing a CSV import. Frequency cap: at most one every 90 seconds, none in the first 45 seconds of a session. Never on launch, exit, splash, camera, forms or the high-reading screen.
- **Native:** small "Ad"-labelled card on Home, History (after the 4th reading), Insights, Meds and Settings. Collapses to nothing if no ad loads.
- **Exit popup:** Back on Home shows an animated "Leaving already?" dialog. It is deliberately ad-free (ads on app exit violate AdMob policy).
- Ad content capped at PG. Health data is never sent to ads.

### Putting in your real ad IDs
Debug builds always use Google's test IDs. For release, set these (preferably in `~/.gradle/gradle.properties` so they stay out of git):
```
SNAPTENSIO_ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
SNAPTENSIO_ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/1111111111
SNAPTENSIO_ADMOB_INTERSTITIAL_ID=ca-app-pub-XXXXXXXXXXXXXXXX/2222222222
SNAPTENSIO_ADMOB_NATIVE_ID=ca-app-pub-XXXXXXXXXXXXXXXX/3333333333
```
Never click your own live ads; add your phone as a test device in AdMob.

## Google Play checklist (health app)
1. **Privacy policy:** host `PRIVACY_POLICY.md` publicly (e.g. GitHub Pages), fill in the placeholders, and put the URL in Play Console and in `PRIVACY_POLICY_URL` (`ui/screens/SettingsScreen.kt`). The URL in the code is a placeholder.
2. **Health apps declaration:** declare it as a health and fitness / medical-adjacent logbook. The app shows a not-a-medical-device disclaimer at onboarding and in Settings, and does not claim to measure blood pressure itself.
3. **Data safety form:** health info and app activity are *stored on device only, not collected*. AdMob collects device or other IDs, approximate location, app interactions and diagnostics, which are shared for advertising. Data is encrypted in transit (AdMob). Users can delete data by uninstalling.
4. **Ads declaration:** "Yes, contains ads". **Target audience:** 18+ (not designed for children).
5. **Permissions:** camera (reading the monitor), notifications (reminders), boot completed (restore reminders). No exact-alarm, location, storage or contacts permissions.
6. **Signing:** create an upload keystore and add a `signingConfigs` block (or sign in Android Studio). Keystores are git-ignored.
7. **Store listing assets:** `branding/play-store-icon-512.png`, `branding/play-feature-graphic-1024x500.png`. Take phone screenshots from the running app.
8. **App name:** search Play before publishing to confirm "SnapTensio" isn't taken in your markets.

## Build
```
cd snaptensio
./gradlew testDebugUnitTest assembleDebug
```
Open the `snaptensio` folder in Android Studio (Ladybug or newer). CI (`.github/workflows/snaptensio-android.yml`) runs the unit tests, builds debug and release APKs, runs lint, and uploads the APKs as a downloadable artifact.

## Project layout
```
app/src/main/java/com/snaptensio/bplog/
  ads/         AdMob + UMP consent, banner and native composables
  data/        Room entities/DAOs, repository, DataStore settings
  domain/      BP classification (AHA/ESC), stats, rule-based insights
  export/      PDF report, CSV export/import, PDF preview
  ocr/         Pure-Kotlin monitor reading parser + multi-frame consensus (unit tested)
  reminders/   Notification channel, inexact daily alarms, boot receiver
  ui/          Navigation, theme, components (animated logo, chart), screens
branding/      SVG logos, Play Store icon and feature graphic
```

Fonts: Manrope (SIL Open Font License, see `FONT_LICENSE_OFL.txt`).
