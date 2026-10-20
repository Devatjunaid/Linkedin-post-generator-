# Cold Plunge & Sauna Timer — Contrast Therapy & Ice Bath Tracker

Publisher: Super Dev Labs

Native Android app in Kotlin + Jetpack Compose. Dark "thermal" design (ice ↔ ember), custom
animated logo, animated splash, exit popup, AdMob banner / interstitial / native ads with
Google UMP consent.

## Open & run

1. Unzip, then **File › Open** the `PlungeApp` folder in Android Studio.
2. Gradle JDK: 21. Let the sync finish (Gradle 9.6.0 wrapper, AGP 9.4.0, built-in Kotlin).
3. Run the `app` configuration on a device or emulator (API 26+).

Debug builds use Google's **test ad units**, so you can tap ads safely. Release builds use your
real unit IDs (see `app/build.gradle.kts`).

## Toolchain

| | Version |
|---|---|
| AGP | 9.4.0 (built-in Kotlin — `org.jetbrains.kotlin.android` is not applied) |
| Gradle | 9.6.0 |
| Kotlin / Compose compiler plugin | 2.4.21 |
| KSP | 2.3.12 (Room; no kapt) |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| JVM target | 17 (`kotlin { compilerOptions { } }`) |

AndroidX / AdMob versions in `gradle/libs.versions.toml` are known-good stable releases. Android
Studio will flag newer ones; you can accept those upgrades.

## Features

**Timers**
- Cold plunge and sauna quick start, with duration and temperature (°C/°F) presets. The app
  remembers your last values.
- Contrast protocols that run heat → cold → rest automatically, round by round, with optional
  "finish on cold".
- Accurate timing from the monotonic clock: the timer stays exact while paused, backgrounded or
  with the screen off, and catches up when you return.
- +30 s, pause/resume and skip phase; end early and still log the time you did.
- 3-2-1 countdown beeps, phase-change sound and vibration, a vibration tap each cold minute, and
  mute in-session.
- Cold-water breathing guide (inhale 4 s · exhale 6 s) after a 30 s "settle in" period.
- Screen stays on during a session. Full-screen flash and label on every phase change.

**Protocols**
- 5 built-in protocols (Nordic Classic, Quick Reset, Deep Recovery, First Timer, Warm Finish).
- Builder with a live "thermal ribbon" timeline: heat/cold/rest durations, rounds, both
  temperatures, and finish on cold or warm.
- Save, edit, delete, and star a protocol to feature it on Today.

**Log & insights**
- Session summary after every session: totals, mood (5 levels), actual temperatures, and a note.
- Month calendar heatmap (cold / heat / contrast days). Tap a day to filter.
- Weekly dose rings against editable goals (default 11 min cold / 57 min heat).
- Current and best streak, total sessions, most cold in one session, coldest water, total heat.
- Manual entry for sessions done without the timer.
- CSV export through the share sheet (no storage permission needed).

**Safety & habits**
- First-launch safety briefing, safety tips on Today, and duration/temperature warnings in the
  setup screens (wellness guidance only; no medical claims).
- Daily reminder at a chosen time. It only fires if nothing has been logged that day, and it
  survives reboots.

**Brand & motion**
- Animated logo with several drawn parts: rotating thermal ring, tick dial, ice/ember split core,
  snowflake, flickering flame, orbiting particles, steam, water ripples and sparkles.
- Animated splash: frost falls and embers rise, the logo assembles, a shock-ring expands, the
  wordmark flips up letter by letter, then the scene zooms through to the app.
- Exit popup on Back from Today, with the animated logo and your streak.
- Liquid-glow bottom bar, a spinning thermal start button, and custom screen transitions.
  Sessions rise up "into the water".
- Adaptive launcher icon with a themed (monochrome) variant.

## Ads (AdMob) — how it works

| Format | Where | Rules |
|---|---|---|
| Banner (anchored adaptive) | Bottom of Today, Protocols, Log, Settings, protocol editor | First 2 requests go out back to back; if both fail, each further request waits **15 s**. The counter resets on success (`BannerRetryPolicy`). Takes no space until loaded. |
| Interstitial | After a major action: starting a session, saving a session, saving a protocol, exporting the log | At most **1 per 30 s** (`FrequencyCap`), preloaded, never at launch, on exit, or while a timer runs. |
| Native | Between cards on Today, Protocols and Log (one ad per tab) | Labelled "Ad" plus AdChoices; collapses to nothing if it fails to load. |

Consent: Google UMP runs after the splash. The Mobile Ads SDK is initialized (on a background
thread) only when `canRequestAds()` is true. Settings shows **Ad privacy options** whenever UMP
requires it (EEA/UK).

## Google Play policy checklist (things you must do in Play Console)

The code is built to be policy-compliant: test ads in debug, consent first, no exit or launch
interstitials, labelled native ads, banners off the timer, no exact-alarm or foreground-service
permissions, and no health claims. Before you publish, you still need to:

1. **Privacy policy**: host one and set `PRIVACY_POLICY_URL` in
   `ui/settings/SettingsScreen.kt`. Also add the URL in Play Console.
2. **Data safety**: declare what the AdMob SDK collects (device / advertising ID, app
   interactions, diagnostics) and that it is shared for advertising. Session data stays on the
   device.
3. **Advertising ID**: answer "Yes" (play-services-ads adds `AD_ID`).
4. **Contains ads**: Yes.
5. **Target audience**: 18+ (or 13+). Do not target children.
6. **Health apps declaration**: it's a fitness/wellness timer, not a medical device.
7. **app-ads.txt** on your developer website, as AdMob asks.
8. In AdMob, create a **GDPR message** and a **US state regulations message** (Privacy &
   messaging), otherwise UMP has nothing to show.
9. Change `applicationId` if `com.superdevlabs.coldplunge` isn't what you want. It can't be changed
   after the first upload.
10. Sign the release build (Build › Generate Signed App Bundle) and upload the `.aab`.

## Project layout

```
app/src/main/java/com/superdevlabs/coldplunge/
  domain/      Pure Kotlin: timer engine, protocols, stats, units, safety (unit-tested)
  data/        Room (sessions, protocols) + DataStore settings
  ads/         Consent, banner retry, interstitial cap, native ads
  reminders/   Daily reminder alarm, receiver, boot re-arm
  ui/          Compose screens, components, theme, navigation
app/src/test/  JUnit tests for the timer engine, protocols, stats and ad policies
```

## Troubleshooting

- If KSP complains about Kotlin source sets with built-in Kotlin, add
  `android.disallowKotlinSourceSets=false` to `gradle.properties` (only needed with older KSP
  releases).
- Real ads on your own device: add it as a test device in AdMob, or keep using the debug build.
