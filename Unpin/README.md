# Unpin – Photo Location & Metadata Remover

Kotlin + Jetpack Compose app that removes GPS, device, time and other hidden
metadata from photos, entirely on the phone.

## Build setup

- Android Studio Quail 4 (2026.1.4), Gradle JDK 21
- AGP 9.4.0 with **built-in Kotlin** (no `org.jetbrains.kotlin.android`), Gradle 9.6.0 (wrapper included)
- compileSdk 37 · targetSdk 36 · minSdk 26
- Kotlin / Compose compiler plugin 2.4.21, Compose BOM 2026.09.00
- Google Mobile Ads 25.4.0, UMP 4.0.0, Coil 3.6.3
- No KSP or kapt is needed: the app has no annotation processors.

Open the `Unpin` folder in Android Studio, let Gradle sync, then run `app`.
The JVM unit tests for the scrubbing engine and the ad caps run with `./gradlew test`.

## Features

| Screen | What it does |
| --- | --- |
| Splash | Animated logo build (tile, radar rings, viewfinder brackets, photo, pin drop, scrubber slice, pixel scatter, verified badge), wordmark, tagline. Waits for the ads consent check (max 5 s). |
| Home | Exposure scan of the photos you picked (% with GPS, device model, timestamps), quick actions, auto-protect switches, recently cleaned copies, native ad. |
| Inspect | One photo: map preview of the exact GPS spot, Open in Maps, optional place-name lookup, every metadata group with a redaction-bar toggle, Scrub. |
| Batch | Up to 50 photos, select or deselect, presets (Full wipe, Keep date, Location only, Custom), anonymised names, keep rotation, keep colour profile, native ad. |
| Report | Fields removed, locations erased, size saved, lossless %, verification re-scan, Open and Share clean copies, native ad. |
| History / Settings | Past clean-ups, ad privacy choices (UMP), privacy policy, media-location permission, clear history. |
| Exit popup | Animated "pin flies away" dialog on Back from Home. |

**Clean when sharing:** share photos to Unpin from any app. With quick mode on,
they're fully wiped and handed straight back to the share sheet.

### How cleaning works
- JPEG, PNG and WebP are stripped **losslessly**: the image data is copied byte for byte. EXIF, XMP, IPTC, comments, text chunks, embedded thumbnails and hidden trailers (motion-photo video, gain maps) are dropped.
- HEIC/AVIF/other formats are decoded and re-saved as JPEG at quality 95.
- Kept fields (for example the date in *Keep date*) are written back with `ExifInterface`.
- Every copy is re-read from the gallery and checked. Originals are never modified. Copies go to `Pictures/Unpin`.

## Ads

| Format | Behaviour |
| --- | --- |
| Banner | Anchored adaptive banner at the bottom. If a request fails, the second goes out immediately; after 2 failures in a row, each retry waits 15 s. Pauses when the app is in the background. |
| Interstitial | Preloaded. Shown only after a clean-up finishes (a natural break), capped at **1 per 30 s**, never on launch or exit. |
| Native | In free space on Home, Batch, Report and History. Clearly labelled "Ad", with AdChoices. |

Debug builds use Google's **test** ad units; release builds use your real IDs
(see `app/build.gradle.kts`). The App ID is in `res/values/strings.xml`.

## Google Play policy checklist

- No `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`. All photos come from the system **Photo Picker** or the share sheet, as the Photo & Video Permissions policy requires.
- `ACCESS_MEDIA_LOCATION` is optional and requested with context; the app works without it.
- `WRITE_EXTERNAL_STORAGE` only on Android 8–9 (`maxSdkVersion=28`).
- UMP consent is collected before any ad request, and a privacy-options entry is in Settings.
- No ads on the splash, the exit popup, or over app controls.
- Backups are disabled, so scrub history stays on the device.

### Before you publish
1. **Replace `privacy_policy_url`** in `res/values/strings.xml` with your hosted privacy policy. Play and AdMob require one.
2. In Play Console › Data safety, declare the Google Mobile Ads SDK data (device or advertising ID, app interactions, diagnostics). Photos are processed on-device and are not collected.
3. Turn on GDPR / US-state messages in AdMob › Privacy & messaging so the UMP form is shown.
4. Add `app-ads.txt` to your developer website.
5. Never tap your own live ads. Test with the debug build.

Fonts: Bricolage Grotesque, Instrument Sans and JetBrains Mono (SIL OFL 1.1, see `licenses/`).
