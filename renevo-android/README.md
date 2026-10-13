# Renevo: private subscription tracker (Android, Kotlin)

Renevo tracks subscriptions, renewals and free trials without a bank link or an account.
Everything is stored on the phone. Built with Jetpack Compose and Material 3.

- **App name:** Renevo — Subscription Tracker
- **Package / applicationId:** `com.devatjunaid.renevo` (debug builds add `.debug`)
- **minSdk 26 · targetSdk 35 · compileSdk 35**

## Features

| Area | What it does |
|---|---|
| Splash | Android 12+ system splash with an animated vector logo, then an in-app animated splash (logo build-up, letter-by-letter wordmark, floating chips) while Google's consent check runs. |
| Onboarding | 3 illustrated pages + main-currency picker, then asks for notification permission. |
| Home | Monthly/yearly total (tap to switch), budget bar, due in 7 days, trial warning, next charges, category split. |
| Subscriptions | Search, filters (active, monthly, yearly, trials, paused, cancelled), sort by next due / cost / name. |
| Detail | Per-year cost, spent so far, price-history chart, cost per use, payment log, notes, pause / cancel / reactivate / delete. |
| Add / edit | 70 quick-add templates, any currency, weekly/monthly/quarterly/yearly/custom cycles, free trial with end date, reminder timing, categories (plus your own), payment label, notes. |
| Calendar | Month grid with coloured charge dots, paid vs still to pay, trial conversions, per-day list. |
| Insights | 6-month and yearly spend charts, rarely used, overlapping services, price increases, missing exchange rates, most expensive. |
| Trials & savings | Trial countdown, alert schedule, keep / I cancelled, money saved by cancelling. |
| Reminders | Daily WorkManager job (~09:00): renewal reminders, trial alerts (3 days / 1 day / morning of), optional Sunday summary. No exact-alarm permission. |
| Privacy | Fingerprint/face/PIN app lock, hide amounts, block screenshots, no Android cloud backup, delete all data. |
| Backup | AES-256-GCM encrypted backup file (password, PBKDF2), restore (replace or merge), CSV export/import, PDF report. All through the system file picker. |
| Widget | Home-screen widget with monthly total and the next charges. |
| Exit popup | Back on Home opens an animated "Leaving already?" dialog with the next charge, Stay / Exit and Rate. |

## Brand assets

- `design/generate_assets.py` generates every vector: adaptive launcher icon (with themed monochrome layer),
  notification icon, logo tile, animated splash icon (`avd_splash_logo.xml`) and four illustrations.
- `ui/components/Logo.kt` draws the same mark in Compose with an intro animation, twinkling spark and a light sweep.
- `store/play_icon_512.png` and `store/play_feature_1024x500.png` are ready for the Play listing.
- Fonts: DM Sans and Space Grotesk (SIL Open Font License), bundled in `res/font`.

## Ads (Google AdMob)

| Format | Where | Rules built in |
|---|---|---|
| Adaptive banner | Bottom of the main tabs, detail, trials and backup screens, below the navigation bar | Never on splash, onboarding, lock screen, exit dialog or while typing in the add/edit form. Never overlaps content. |
| Interstitial | After saving, cancelling or deleting a subscription, deciding on a trial, finishing a backup/export, opening Calendar, Insights or Trials | Shown only **after** the action completes. At most 1 every 90 s, none in the first 45 s of a session, max 6 per session. Never on launch, back or exit. Suppressed while the app is locked. |
| Native | Home, Subscriptions list (after the 4th row), Calendar, Insights, Trials | Clearly labelled "Ad", outlined style so it can't be mistaken for a subscription. Takes no space until an ad loads. |

Consent: Google's User Messaging Platform (UMP) runs on every launch. No ad is requested until it allows it.
Settings → *Ad privacy choices* lets users change consent (shown when required). Max ad content rating is PG.

**Why interstitials are not on literally every action:** AdMob and Play policy prohibit interstitials that
appear unexpectedly, on app launch or exit, or repeatedly after each user action. Accounts that do this get
limited or banned. The placements above cover all the main functions while staying within policy. To change
pacing, edit `COOLDOWN_MS`, `LAUNCH_GRACE_MS` and `MAX_PER_SESSION` in `ads/AdsManager.kt`.

### Put in your real AdMob IDs

Debug builds always use Google's test IDs. For release, add your IDs to `gradle.properties` or, better, to
`~/.gradle/gradle.properties` so they aren't committed:

```properties
RENEVO_ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
RENEVO_ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/1111111111
RENEVO_ADMOB_INTERSTITIAL_ID=ca-app-pub-XXXXXXXXXXXXXXXX/2222222222
RENEVO_ADMOB_NATIVE_ID=ca-app-pub-XXXXXXXXXXXXXXXX/3333333333
```

Never tap your own live ads. Add your phone as a test device in AdMob.

## Build

Open the `renevo-android` folder in Android Studio (Ladybug or newer), or:

```bash
./gradlew testDebugUnitTest   # billing math, CSV, encrypted backup
./gradlew assembleDebug       # test-ads APK
./gradlew bundleRelease       # .aab for Play
```

Release signing: create `keystore.properties` next to `settings.gradle.kts`:

```properties
storeFile=renevo-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

GitHub Actions (`.github/workflows/renevo-android.yml`) runs the tests and builds a debug APK on every push.

## Play Store checklist

1. **Privacy policy:** host `PRIVACY_POLICY.md` (e.g. GitHub Pages) and put the URL in
   `res/values/strings.xml` → `privacy_policy_url` and in Play Console.
2. **Ads:** declare "Contains ads".
3. **Data safety** (AdMob collects data, Renevo itself doesn't):
   - Device or other IDs (advertising ID): collected, shared, for advertising and analytics, by Google Mobile Ads.
   - App interactions and diagnostics: collected by the Google Mobile Ads SDK.
   - Approximate location (from IP address): collected by the Google Mobile Ads SDK.
   - Financial info: **not collected** (subscriptions never leave the device).
   - Data encrypted in transit: yes. Users can delete data: yes (Settings → Delete all data, or uninstall).
4. **Advertising ID declaration:** yes, used for advertising (the AD_ID permission comes from the Ads SDK).
5. **Target audience:** 18+ (a finance tool). Don't select children's age groups.
6. **Content rating questionnaire:** utility/productivity, no user-generated content.
7. **Financial features declaration:** choose personal budgeting/tracking only. The app doesn't move money.
8. **Store listing:** use generic service names in screenshots and graphics. Don't show other companies' logos.
   Listing text: `store/listing.md`.
9. **App name:** `Renevo — Subscription Tracker`. Search the Play Store before publishing to confirm nobody has claimed it.
