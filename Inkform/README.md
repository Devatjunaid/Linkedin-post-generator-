# Inkform — Offline PDF Form Filler & Signer (Android, Kotlin)

Package / application ID: **`com.inkformlabs.pdfsigner`**

Fill PDF forms, sign them and save the result — all on the phone. Documents never leave the device.

## Open & build

1. Android Studio Quail 4 (2026.1.4) → **Open** this `Inkform` folder.
2. Gradle JDK: **21** (Settings → Build Tools → Gradle). Android SDK **37** installed.
3. Let Gradle sync (Gradle 9.6.0 wrapper, AGP 9.4.0 with built-in Kotlin).
4. Run the `app` configuration on a device/emulator (API 26+).

Toolchain used:

| | Version |
|---|---|
| AGP | 9.4.0 (built-in Kotlin, new DSL — no `org.jetbrains.kotlin.android` plugin) |
| Gradle | 9.6.0 |
| Kotlin / Compose compiler | 2.4.21 (KGP pinned in root `build.gradle.kts` so both always match) |
| KSP | 2.3.12 (Room compiler, no kapt) |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| Compose BOM | 2026.09.00 (Material 3 1.4) |
| Google Mobile Ads / UMP | 25.5.0 / 4.0.0 |
| PDF engine | PdfBox-Android 2.0.27.0 (Apache 2.0) |

## Features

- **Library** — import PDFs (system file picker, "Open with" / "Share to" from other apps), password-protected PDFs supported, search, rename, share, print, delete, progress ring per document.
- **Scan** — take photos with the camera app (multi-page scan session) or pick photos from the gallery and turn them into a PDF (auto-rotation from EXIF, fitted to A4).
- **Fill forms** — real AcroForm support: text (single/multi-line, max length), check boxes, radio groups, drop-downs; guided *Next field* flow with progress; **auto-fill** from "My details".
- **Any PDF (no form fields)** — place text, today's date, ticks ✓ and crosses ✗ anywhere; drag to move, resize with the handle, black/blue ink.
- **Signatures & initials** — draw (smoothed strokes, 3 inks, 3 pen sizes, undo), type (handwriting styles) or import a photo (paper background removed on-device). Saved for one-tap reuse; tapping a signature field places the signature inside it.
- **Export** — flatten fields, remove hidden metadata, **AES-256 password protection**, save anywhere (Storage Access Framework), share, print. Revert to the original at any time.
- **4 themes** — Citrus Ink, Midnight Teal (dark), Coral Paper, Royal Violet, with animated switching.
- **Brand motion** — code-drawn animated logo (tile, stacked pages, form lines, pen writing a signature, check badge, orbiting ink dots, sparkles), animated splash, animated exit popup on Back, per-screen transitions (slide, rise, circular reveal).

## Ads (AdMob)

IDs are set in `app/build.gradle.kts`:

- **Debug builds use Google's official test ad units** — never tap your own live ads.
- **Release builds use your IDs** (App ID `ca-app-pub-1948347222012540~4764943685`, banner, interstitial, native).

Behaviour:

- **Consent first** — Google UMP shows the GDPR/US-states message where required; ads are requested only when allowed. "Ad privacy choices" appears in Settings when required.
- **Banner** — anchored adaptive banner at the bottom of the main tabs only (not in the editor, signature pad or splash). If a request fails it retries immediately once; after **2 failed requests it waits 15 s** before requesting again (`AppConfig.BANNER_*`). It takes no space until an ad loads.
- **Interstitial** — only at natural breaks after major tasks (finishing a document → export, creating a PDF from photos/scans, saving the final file). **At most one every 30 s** (`AppConfig.INTERSTITIAL_INTERVAL_MS`). Never on launch, never on exit, never blocks the user if no ad is ready.
- **Native** — in natural gaps: after the 2nd document in the library (or under the empty state), below the export options, below saved signatures and at the bottom of Settings. Clearly labelled "Ad", AdChoices shown, hidden until loaded.

## Before publishing on Google Play (checklist)

1. **Privacy policy URL** — required for apps with ads. Host one and set `AppConfig.PRIVACY_POLICY_URL`; also enter it in Play Console.
2. **Data safety form** — follow Google's "Mobile Ads SDK data disclosure" guide. Typically: *Device or other IDs* (advertising ID), *Approximate location* (from IP address), *App interactions* and *Diagnostics* — collected and shared with Google for advertising, analytics and fraud prevention, encrypted in transit. Documents, signatures and "My details" are **not collected**: they stay on the device and app backups are disabled.
3. **Ads declaration**: "Contains ads" = Yes. Target audience: 13+ / not designed for children.
4. **AdMob**: add the app in AdMob, link it to the Play listing, set up the GDPR & US-state messages in *Privacy & messaging* (UMP shows them).
5. **app-ads.txt** on your developer website.
6. **Signing** — create an upload key (Build → Generate Signed Bundle) and publish an `.aab`.
7. Store graphics are in `store-assets/` (512×512 icon, 1024×500 feature graphic).

Permissions: only `INTERNET` and `ACCESS_NETWORK_STATE` (for ads; the ads SDK also adds `AD_ID`). No storage, camera or media permissions — the app uses the system document picker, the photo picker and the camera app.

## Project layout

```
app/src/main/java/com/inkformlabs/pdfsigner/
  ads/        ConsentManager (UMP), AdsManager (init + interstitial cap), Banner & Native composables
  data/       Room (documents, signatures), DataStore (theme, my details), repositories
  pdf/        PdfEngine (fields, filling, overlays, flatten, encryption), renderer, photos→PDF, printing
  ui/         theme, icons, animated logo, splash, exit dialog, home tabs, editor, signature pad, export
```

Fonts: Bricolage Grotesque and Manrope (SIL Open Font License, see `licenses/`).
