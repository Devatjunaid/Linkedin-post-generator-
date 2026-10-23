# DriveReady — all-in-one driving test prep (Android, Kotlin + Jetpack Compose)

DriveReady prepares learners for **every part** of the driving test, adapted to their
country and — where licensing is regional — their **state, province or territory**:

| Area | What's in the app |
|---|---|
| Theory | Timed mock exams in the official format (question count, pass mark, time), practice by topic with explanations, review screen |
| Road signs | ~23 signs drawn natively in the country's sign family (Vienna triangles or MUTCD yellow diamonds, US/CA rectangular speed signs), mastery tracking, sign quiz |
| Hazard perception | 6 animated driver's-eye clips (child runs out, emerging car, braking lead car, cyclist, zebra crossing, animal). Scored 5→1 like the UK DVSA test, with anti-pattern-clicking |
| Oral test | Country-specific spoken questions (UK show me/tell me, US pre-drive checks, India RTO viva, Pakistan sign test, Gulf assessments, Germany Sicherheitskontrolle, France vérifications, Ireland hand signals, SA K53 pre-trip) with on-device examiner voice (TTS) and model answers |
| Practical test | Test stages in order, pass rules, common faults and 15 animated maneuver guides (parallel/bay/garage/angle parking, 3-point turn, reverse round a corner, hill start, emergency stop, figure-8, S-course, alley docking, roundabouts…) mirrored for left/right-hand traffic |
| Planning | Per-country syllabus with tick-off progress, readiness score, test-date countdown, test-day checklist, mock-score chart, study streak |

**Coverage:** 17 countries — United States (all 50 states + DC), United Kingdom, Canada
(ON, BC, AB, QC), Australia (NSW, VIC, QLD, WA), India, Pakistan, UAE, Saudi Arabia, Qatar,
Germany, France, Ireland, New Zealand, South Africa, Singapore, Malaysia, Philippines.

> Test formats were compiled from public sources in October 2026 and change often. The app
> shows a "confirm with your licensing authority" note wherever numbers appear.

## Tech stack (pinned)

| | Version |
|---|---|
| Android Gradle Plugin | 9.4.0 (built-in Kotlin — `org.jetbrains.kotlin.android` is **not** applied) |
| Gradle | 9.6.0 (wrapper included), Gradle JDK 21 |
| Kotlin (compiler via Compose plugin) | 2.4.21 · KSP 2.3.12 (no kapt) |
| SDK | compileSdk 37 · targetSdk 36 · minSdk 26 |
| Compose BOM | 2026.09.00 (Material 3) |
| AndroidX | core-ktx 1.19.1, activity-compose 1.13.0, fragment-ktx 1.9.1, lifecycle 2.11.0, navigation-compose 2.10.2, core-splashscreen 1.2.0, datastore 1.3.0, room 2.8.5 |
| Ads | play-services-ads 25.5.0 · UMP 4.0.0 |
| Images | Coil 3.6.3 (no manual `BitmapFactory`/`URLConnection` decoding anywhere) |

Kotlin options use `kotlin { compilerOptions { … } }` (never `kotlinOptions`).
Edge-to-edge uses `enableEdgeToEdge()` + Compose `WindowInsets`; no deprecated window APIs
(`setStatusBarColor`, `setNavigationBarColor`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`) are used.

## Open & build

1. Android Studio Quail 4 (2026.1.4) → **Open** → select the `DriveReady` folder.
2. Make sure *Settings → Build Tools → Gradle → Gradle JDK* is **21** and SDK Platform **37** is installed.
3. Run the `app` configuration, or from a terminal: `gradlew assembleDebug` / `gradlew testDebugUnitTest`.

## AdMob integration

| | |
|---|---|
| App ID | `ca-app-pub-1948347222012540~1473202990` (manifest placeholder in `app/build.gradle.kts`) |
| Banner | `…/5525670638` — anchored adaptive banner on every screen except splash/onboarding/hazard playback |
| Interstitial | `…/1586425623` — on major actions: start mock exam, finish a test, start full hazard test, open a maneuver guide, change country |
| Native | `…/7867306670` — in free slots: home, theory, syllabus, signs grid, country/state lists, results, oral, practical, exit dialog |

- **Debug builds use Google's public test ad units**; **release builds use your production units**
  (`BuildConfig.*_AD_UNIT_ID`). Never tap your own live ads.
- **Banner request cap:** attempts 1 and 2 are immediate; after two consecutive failures every
  further request waits **15 s** until one succeeds (`ads/AdPolicy.kt`, unit-tested).
- **Interstitial cap:** max **1 per 30 s**; the user's action always continues if no ad is ready.
- **Consent:** Google UMP runs during the splash; no ad is requested before consent. A
  "Privacy & ad choices" entry appears in Settings when the UMP SDK requires it.
- Native ad slots stay collapsed until an ad has loaded, so there are no empty gaps.

## App flow

System splash → animated brand splash (consent + SDK init) → 4-page guide → Get started →
country (→ state/province) → Home with bottom tabs **Home · Theory · Hazard · Oral · Practical**.
Pressing Back on Home opens the exit dialog with **Exit**, **Cancel** and **Rate us** (opens the Play Store).

## Project structure

```
app/src/main/java/com/driveready/testprep/
  ads/            AdsManager (UMP+init), AdPolicy, BannerAd, InterstitialAdManager, NativeAdManager, NativeAdCard
  data/content/   Countries, Regions (US states, CA provinces, AU states), QuestionBank, OralSets,
                  Maneuvers (animated diagrams), Signs, HazardClips
  data/local/     DataStore preferences, Room database of test attempts
  quiz/           Quiz engine, hazard scoring
  ui/             Compose screens (onboarding, country, home, theory, signs, syllabus, hazard,
                  oral, practical, progress) + components (logo, road signs, maneuver diagram, exit dialog)
app/src/main/res/ adaptive launcher icon (vector, with monochrome layer), splash theme, strings
```

## Before publishing

- Add a privacy policy URL and an `app-ads.txt` file on your developer website (required for AdMob).
- Configure release signing with your upload key.
- Review each country's numbers against the official authority (each syllabus screen shows the official source).
