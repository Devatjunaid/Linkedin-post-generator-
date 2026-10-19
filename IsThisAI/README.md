# Is This AI? — Image label checker (Android, Kotlin)

Reads the hidden provenance labels that AI tools and phone cameras write inside image files.
**Fully offline. No APIs, no servers, no credits.** The only network use is AdMob.

## What it detects

| Source | What it means | Strength |
|---|---|---|
| **C2PA / Content Credentials** (JPEG, PNG, WebP, HEIF/AVIF) | Signed history from Adobe, OpenAI, Google, Microsoft, Leica/Sony/Samsung cameras… The app verifies the COSE signature against the embedded certificate (ES256/384/512, PS256/384/512, Ed25519) and re-hashes the image to detect edits after signing. | Signed |
| **IPTC DigitalSourceType** in XMP | `trainedAlgorithmicMedia` (AI), `compositeWithTrainedAlgorithmicMedia` (AI-edited), `digitalCapture` (camera) | Label in file |
| **IPTC 2025 AI fields** | `AISystemUsed`, `AIPromptInformation` | Label in file |
| **China AIGC label** | `AIGC` metadata (`Label` code) | Label in file |
| **Generator text** in PNG | Stable Diffusion WebUI/Forge `parameters`, ComfyUI `prompt`/`workflow`, NovelAI, InvokeAI, Fooocus. The prompt is shown when present. | Label in file |
| **"Made with AI" phrases** | e.g. Google's "Made with Google AI" credit | Label in file |
| **Software / CreatorTool names** | Firefly, Midjourney, DALL·E, Imagen, Gemini… | Clue only |
| **EXIF camera data** | Make, model, lens, exposure | Clue only |

Verdicts: **AI-made · AI-edited · Camera · Labeled (no AI declared) · No label.** The app always says what the
file *claims*. "No label" never means "real", and the UI says so.

Limits: screenshots and chat apps strip labels. Invisible watermarks (SynthID etc.) need the vendor's own detector.
Certificate *trust* (whether the signer is on the C2PA trust list) is not checked offline. The app shows who signed.

## Features
- Photo picker (no storage permission), Files, Paste, **Share to app** and **Open with**
- Animated splash, lens logo, iris transition into scanning, stamp-drop verdict, animated provenance timeline
- Raw field inspector, history with previews (Room), share text report, copy/save JSON report
- Exit confirmation popup on Back from Home
- AdMob banner, interstitial and native ads with Google UMP consent

## Build
Android Studio Quail 4 (2026.1.4) · AGP 9.4.0 (built-in Kotlin) · Gradle 9.6.0 · JDK 21 · compileSdk 37 · targetSdk 36 · minSdk 26.
Kotlin 2.4.21 · KSP 2.3.12 · Compose BOM 2026.09.00 · Room 2.8.5 · play-services-ads 25.5.0 · UMP 4.0.0.

1. Open the `IsThisAI` folder in Android Studio and let Gradle sync.
2. Run the `app` configuration. **Debug builds use Google's test ad units**, so test freely.
3. Release: *Build → Generate Signed App Bundle*. Release builds use your real AdMob IDs (set in `app/build.gradle.kts`).

Unit tests (engine plus ad rules): `./gradlew :app:testDebugUnitTest`. The sample images in
`app/src/test/resources/samples` were made with the open-source c2pa-python SDK using throwaway test certificates.

## Ads (as requested)
- **Banner**: anchored adaptive banner at the bottom. The first 2 requests go out back to back; after the 2nd failure every new request waits **15 s** (`AdPolicy.bannerDelayAfterFailure`). Its space is reserved so content never jumps.
- **Interstitial**: shown at natural breaks only (scan finished → verdict, opening a saved check, saving a report). **Capped at 1 per 30 s** (`FrequencyCap`).
- **Native**: Home (between the actions and Recent), Verdict, Trail, History (after the 3rd item, then every 8), About, and the exit popup. Each is labelled "Ad" with AdChoices.

## Google Play policy checklist
- [x] Test ads in debug, live IDs only in release (avoids invalid traffic).
- [x] UMP consent form before ads; "Ad privacy choices" in About when required.
- [x] No interstitial on launch or exit, none over the splash, never back to back (30 s cap).
- [x] Ads never overlap content or buttons; banner space reserved; native ads clearly labelled.
- [x] Photo picker instead of `READ_MEDIA_IMAGES` (Photo & Video Permissions policy).
- [x] No misleading claims: results are worded as "the file says", with limits explained.
- [x] targetSdk 36, edge-to-edge, predictive back.
- [ ] **Host `PRIVACY_POLICY.md`** (e.g. GitHub Pages), fill in the email and date, and put the URL in Play Console.
- [ ] **Add `app-ads.txt`** to your developer website (AdMob → Apps → app-ads.txt).
- [ ] **Data safety form**: *Device or other IDs* (advertising ID), *App activity / interactions* and *Approximate location*, collected by the AdMob SDK for Advertising, Analytics and Fraud prevention; shared with Google; encrypted in transit. Images and history are **not collected** (they never leave the device).
- [ ] **Ads declaration**: "Yes, my app contains ads". Target audience 13+ (not designed for children).
- [x] Publisher (Super Dev Labs) and contact email set in `HistoryAndAbout.kt` and `PRIVACY_POLICY.md`.

Store graphics: `store/play_icon_512.png`, `store/feature_graphic_1024x500.png`.
Fonts: Bricolage Grotesque and JetBrains Mono (SIL OFL 1.1, see `licenses/`).
