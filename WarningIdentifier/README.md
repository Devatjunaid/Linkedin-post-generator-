# Warning Identifier (Android · Kotlin · Jetpack Compose)

Identifies car dashboard warning lights with **on-device AI**. No paid API, no
usage limits, works offline. Photos never leave the phone.

- **Package:** `com.glowdash.warningidentifier` (debug builds add `.debug`)
- **Min SDK:** 26 · **Target/Compile SDK:** 35
- **Stack:** Kotlin, Jetpack Compose, CameraX, AdMob and UMP consent

## Screens
| Screen | What it does |
|---|---|
| Splash | Animated logo with spinning rings, symbols lighting up in turn, and a progress bar |
| Home | Animated hero orb, Scan / From Photo / Library, native ad, recent scans, floating nav. Pressing back opens the exit pop-up |
| Scanner | Live camera with a sweeping scan line, corner brackets, live "light spotted" hints, flash, gallery and shutter |
| Result | Glowing symbol, animated severity meter, what it means / what to do / can I drive, read-aloud (TTS), find a mechanic nearby |
| Candidate picker | Used when the AI isn't sure: shows symbols matching the colours it detected so the user can tap the right one |
| Light Library | 32 symbols with search, severity filters, animated cards and native ads in the grid |
| History | Scans saved locally, with a clear button |

## How identification works (free and unlimited)
On-device **colour analysis** finds the lit red/amber/green/blue symbols in the frame, then the
app shows the matching symbols so the user can tap the exact one. Everything runs on the phone.

> The TensorFlow Lite / LiteRT runtime was removed in v1.0.1: its native library (`libtensorflowlite_jni.so`)
> is aligned to 4 KB, and Google Play requires 16 KB page-size support. To add an image model later, use a
> 16 KB-compatible runtime version and return its results from `ai/LightClassifier.kt`.

## Ads (AdMob)
| Format | Where | Behaviour |
|---|---|---|
| Banner | Bottom of every screen except splash and camera | One shared anchored adaptive banner (not reloaded per screen), takes **no space until loaded**, then slides in; hairline divider, matches app colours, no collapsible format |
| Interstitial | Main actions: scan result, photo result, opening a library symbol, "Find mechanic" | Preloaded; **40 s cap** between ads (also counted from app launch); the action still runs right away if no ad is ready |
| Native | Home (between actions and recent scans), Result, Library grid (2 slots), History, scan candidate picker, Exit pop-up | Styled like the app's cards, cached per placement, invisible until loaded |

- Consent: Google UMP (GDPR / US states) runs before any ad request. "Privacy & ad choices" appears in Settings when required.
- **Debug builds always use Google's test ad IDs.**
- For release, put your real IDs in `gradle.properties` (`ADMOB_APP_ID`, `ADMOB_BANNER_ID`,
  `ADMOB_INTERSTITIAL_ID`, `ADMOB_NATIVE_ID`). If they are missing, release falls back to test IDs.
- Change the cap in `ads/AdsManager.kt` → `INTERSTITIAL_CAP_MS`.
- Add an `app-ads.txt` file to your developer website, as AdMob requires.

## Build
Open the `WarningIdentifier` folder in Android Studio (Ladybug or newer) and press Run, or:
```
./gradlew assembleDebug
```
Before publishing, check Google Play's current target-API requirement and raise `compileSdk`/`targetSdk`
(and AGP/library versions) if Android Studio suggests it.

## Project layout
```
app/src/main/java/com/glowdash/warningidentifier/
  MainActivity.kt          splash handoff, edge-to-edge, consent + ads init
  ads/                     AdsManager (consent, interstitial cap, native cache), banner, native card
  ai/                      LightClassifier, ColorAnalyzer, bitmap helpers
  data/                    WarningLight model, 32-symbol catalog, local history
  ui/                      navigation, theme, components, and one package per screen
```

Fonts: Space Grotesk and Manrope (SIL Open Font License).
Safety: the in-app information is general guidance. Users should always check their owner's manual.
