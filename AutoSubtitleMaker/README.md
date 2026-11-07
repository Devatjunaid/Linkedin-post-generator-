# Auto Subtitles & Captions — Android app (Kotlin)

Pick a video → the app listens to the speech in it → timed subtitles appear on the video → style them → export.
Everything runs **on the phone**. There is **no paid API, no API key and no credit limit**.

## Open & run

1. Android Studio Quail 4 → **File › Open** → select this `AutoSubtitleMaker` folder (not the repo root).
2. Gradle JDK: 21. SDK: API 37 installed.
3. Run the `app` configuration on a phone (Android 8.0+).

Toolchain (pinned in `gradle/libs.versions.toml`):

| Piece | Version |
|---|---|
| AGP | 9.4.0 — built-in Kotlin, no `org.jetbrains.kotlin.android` |
| Gradle | 9.6.0 (wrapper included) |
| Kotlin (compose / serialization plugins) | 2.4.21 |
| KSP | 2.3.12 (Room); no kapt |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| Compose BOM | 2026.09.00 |
| Media3 | 1.11.1 |
| Room | 2.8.5 |
| Google Mobile Ads | 25.5.0, UMP 4.0.0 |
| Vosk (offline speech) | 0.3.75 + JNA 5.18.1 |

## How subtitles are made without any paid API

| Step | Tech | Where |
|---|---|---|
| Read the video's sound track | `MediaExtractor` + `MediaCodec` → 16 kHz mono PCM | `speech/AudioPcmExtractor.kt`, `PcmResampler.kt` |
| Speech → words (Fast engine) | **Vosk** (Kaldi) offline recognizer, exact word timestamps | `speech/Transcriber.kt` |
| Speech → words (Accurate engine) | **Whisper** via **sherpa-onnx** + ONNX Runtime, Silero voice-activity detection, punctuation, 50+ languages, auto-detect | `speech/WhisperTranscriber.kt`, `WhisperText.kt` |
| Speech models | Vosk zips (31–133 MB) or Whisper int8 files (104/160/375 MB from Hugging Face, pinned revision), downloaded once | `speech/ModelManager.kt`, `ModelCatalog.kt`, `WhisperCatalog.kt` |
| Translation | **Google ML Kit** on-device translation (~59 languages, ~30 MB pack per language, downloaded when picked) | `translate/CaptionTranslator.kt`, `core/captions/TranslationPlanner.kt` |
| Words → readable captions | Groups by word count, characters, pauses, max duration | `core/captions/CaptionSegmenter.kt` |
| Styling + animation | One `Canvas` renderer for preview **and** export | `core/render/CaptionRenderer.kt` |
| Burn subtitles into video | Media3 Transformer + bitmap overlay, H.264, hardware encoder | `export/VideoExporter.kt`, `CaptionBitmapOverlay.kt` |
| Subtitle files | SRT, VTT, ASS (keeps font/colours/karaoke), TXT | `core/captions/SubtitleFormatter.kt` |

> Models are fetched from `alphacephei.com` (free, Apache 2.0). For production, upload the same
> zip files to your own storage (GitHub Releases, Firebase Storage, any CDN) and change
> `ModelCatalog.BASE_URL`, so you don't depend on someone else's server.

## What's new in 1.1

- **Accurate engine (Whisper):** punctuation, capitals, names, accents; auto-detects the language. Lite / Balanced / Pro models. Fast (Vosk) is still there.
- **Background processing:** subtitle creation and video export run as foreground jobs with a progress notification and a Cancel button, and keep going when the screen is off or the user leaves the app. "Ready" notification opens the project.
- **Viral styles:** auto emoji, keyword highlight colour, 5 new presets (Viral, Spotlight, Hype, Story, Podcast) with a NEW badge, TikTok/Reels/Shorts safe-zone guide.
- **Translate tab:** any of ~59 languages, as a second line under the original (bilingual) or replacing it. SRT/VTT/ASS include the translation.
- **In-app review** prompt after the 2nd, 5th, 10th… successful export (max once per 45 days).
- **Crashlytics + Analytics** (optional, see below). Users can switch it off in Settings.

## Firebase Crashlytics + Analytics (optional)

1. In the [Firebase console](https://console.firebase.google.com) create a project → *Add app* → Android → package `com.superdevlabs.autosubtitles`.
2. Download **`google-services.json`** and put it in `AutoSubtitleMaker/app/`.
3. Rebuild. That's it: the Gradle plugins switch on automatically when the file exists. Release builds upload the R8 mapping file so Crashlytics shows readable stack traces.

Without the file the app builds and runs normally, with Firebase switched off.

## Features

- Pick any video with the system Photo Picker (no storage permission).
- 18 language models (English US/India, Spanish, Portuguese, French, German, Italian, Dutch, Russian, Ukrainian, Turkish, Hindi, Persian, Vietnamese, Japanese, Korean, Chinese).
- Live transcript while it listens; word-level timing.
- Editor: video preview with the real subtitles, drag subtitles up/down on the video, scrubbable timeline, tap a caption to jump, edit text, nudge timing ±0.1 s, delete, undo.
- 8 style presets: Pop, Karaoke, Boxed, Neon, Minimal, Comic, Typewriter, Marker.
- 12 bundled caption fonts + **import your own .ttf/.otf**.
- Text colour, highlight the spoken word (colour or box), outline, per-line/block background, drop shadow or glow, letter spacing, case.
- Motion: Pop, Bounce, Slide up, Fade, Typewriter, Karaoke, Still. Animations are timed per word.
- Words per caption 1–6, re-grouping keeps your text edits.
- Export: burned-in MP4 (720p / 1080p / original), subtitle file, or both. Keep/remove audio. Saved to `Movies/AutoSubtitles`, share sheet.
- Animated splash, animated multi-part logo, animated exit popup on Back.

## Ads (AdMob)

| Format | Where | Rule implemented |
|---|---|---|
| Banner (anchored adaptive) | Above the bottom navigation on Home, Projects, Languages, Settings; Export (bottom) | Request 1 fails → request 2 right away; request 2 fails → wait **15 s** and request again; keeps retrying every 15 s, max 10 requests per screen visit. `ads/BannerAd.kt` |
| Interstitial | Before **starting a new project** and before **Export/Save** | Max **1 per 30 s** (measured from when the last one was shown). If none is ready, the action continues instantly. `ads/AdsManager.kt` |
| Native | Home (under recent projects), Projects grid (after every 6th project), processing screen, export screen (while rendering / when done) | Takes no space until an ad has loaded. `ads/NativeAdCard.kt` |

- **Debug builds always use Google's test ad units.** Release builds use your real IDs (set in `app/build.gradle.kts`). Never tap your own live ads.
- Consent: Google UMP shows the GDPR / US-state message when required and ads only load after it allows them. "Ad privacy choices" appears in Settings when required.

## Google Play policy checklist

Already handled in code:
- [x] targetSdk 36; edge-to-edge.
- [x] 16 KB page-size: Vosk, JNA and sherpa-onnx `.so` files checked (LOAD alignment 0x4000), uncompressed packaging.
- [x] Foreground service types declared: `mediaProcessing` (Android 15+) and `dataSync` (older) for WorkManager jobs; notification permission asked once, before the first job.
- [x] Database migration v1 → v2 keeps every saved project (tested in `DatabaseMigrationTest`).
- [x] Photo Picker; no `READ_MEDIA_*` / `READ_EXTERNAL_STORAGE`. `WRITE_EXTERNAL_STORAGE` only on Android 8–9 (`maxSdkVersion=28`).
- [x] Subtitle files saved through the system "Save as" picker (no permission).
- [x] UMP consent before ads; privacy options entry.
- [x] No ads on splash or exit popup; interstitials only at natural breaks before an action, never on launch/exit; frequency cap.
- [x] Native ads show an "Ad" badge, AdChoices, all assets inside `NativeAdView`, MediaView ≥ 120 dp.
- [x] Test ad units in debug.
- [x] Downloaded models are data files, not code.
- [x] Open-source licenses screen (Settings) + texts in `assets/licenses`.

You still need to do in Play Console / AdMob:
- [ ] Replace `privacy_policy_url` in `res/values/strings.xml` with your real privacy policy, and add it in Play Console.
- [ ] Data safety form: the Google Mobile Ads SDK collects device/advertising ID, approximate location (IP), app interactions, diagnostics — for advertising and analytics. Firebase (if enabled) collects crash logs, diagnostics, app interactions and a Firebase installation ID — for analytics and app functionality; optional for the user (Settings switch). Data is encrypted in transit.
- [ ] App content → **Foreground service permissions**: declare *Media processing* and *Data sync* — "Creates subtitles from the user's video and exports the subtitled video; the user starts it and sees progress in a notification." Attach a short screen recording.
- [ ] Declare "Contains ads" and "Advertising ID: yes" (the Ads SDK adds the `AD_ID` permission).
- [ ] Target audience: 13+ / not designed for children (otherwise Families ad rules apply).
- [ ] In AdMob, create and publish the GDPR + US state privacy messages (UMP shows nothing until you do).
- [ ] Add your `app-ads.txt` to your developer website.

## Project layout

```
app/src/main/java/com/superdevlabs/autosubtitles/
  SubtitleApp.kt, MainActivity.kt          app start, manual DI container
  ads/                                  consent, banner retry policy, interstitial cap, native card
  core/model, core/captions             data model, segmenter, editing, SRT/VTT/ASS, presets (unit tested)
  core/render                           caption renderer + font registry
  speech/                               audio decoding, resampler, Vosk + Whisper transcribers, model downloads
  work/                                 WorkManager jobs (subtitles, export), notifications, live job state
  translate/                            ML Kit on-device translation
  telemetry/, review/                   Firebase wrapper, Play in-app review
app/src/main/java/com/k2fsa/sherpa/onnx/   sherpa-onnx Kotlin API (v2.0.1, Apache 2.0), matches jniLibs/
  data/                                 Room database, project repository, DataStore settings
  export/                               Media3 export, overlay, gallery saving, sharing
  ui/                                   Compose screens: splash, home, projects, languages, settings,
                                        processing, editor (+panels), export; theme; components
```

Unit tests: `./gradlew :app:testDebugUnitTest` (caption grouping, editing, subtitle formats, resampler,
Whisper timing/cleaning, keyword/emoji rules, translation planning, database migration).
