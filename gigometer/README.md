# Gigometer — free GPS mileage log for gig drivers (Android, Kotlin)

**App name:** Gigometer · **Package:** `app.gigometer.mileage` · **Tagline:** *Every mile. Every dollar.*

Gigometer records driving trips automatically with GPS and works out the IRS
standard-mileage deduction for Uber, Lyft, DoorDash, Instacart, Amazon Flex and
other gig drivers. The paid apps lock automatic tracking and reports behind a
subscription. Here everything is free, paid for by a bottom banner ad and
native ads. There are **no interstitial ads**.

## Screens

| | |
|---|---|
| **Splash** | The system splash hands off to an animated brand splash: the gauge arc sweeps in, the needle swings up, the road scrolls and the wordmark rises. |
| **1. Home** | Year-to-date deduction, today and this week, trips waiting for review, start/resume trip, native ad, recent trips. A setup card appears if automatic tracking isn't ready. |
| **2. Live trip** | Route drawn live, distance, deduction, timer, speed, stops, a "Working for" platform picker, pause/resume, End & save. |
| **3. Trips** | Grouped by day with daily totals. Filters: All, Needs review, Business, Personal. Classify unreviewed trips with one tap or a swipe. Tap a trip to edit or delete it. **+** adds a manual trip. A native ad appears after the 3rd trip. |
| **4. Reports** | Tax-year picker, monthly bar chart, the Schedule C line 9 deduction split by IRS rate period, total and business-use %, breakdown by platform, **PDF mileage log** and **CSV** export. |
| **5. Settings** | Auto-detect, battery saver, 9 PM review reminder, work hours (overnight shifts supported), IRS rate table, gig apps, name on reports, street addresses, backup and restore, ad privacy choices, privacy policy. |

## How tracking works

* **Automatic:** Google Play services *Activity Recognition* sends an
  `IN_VEHICLE` enter event, and a foreground service starts recording GPS. The
  trip ends after 4 min parked, or 75 s after the phone reports you left the
  vehicle. Trips under 0.1 mi are discarded.
* **Filtering:** GPS fixes worse than ±40 m are ignored, as are jumps faster
  than ~134 mph. Movement smaller than the GPS accuracy (parked drift) is not
  counted.
* **Battery saver:** a 5 s / 10 m update interval instead of 2 s.
* **Crash-safe:** the trip is written to the database every 30 s and recovered
  on the next launch.
* **Auto-classification:** a trip is saved as **Business** if you picked a
  platform, or if it was detected inside your work hours. Otherwise it waits in
  **Needs review**.
* **IRS rates are date-based.** 2026 has two business rates: **72.5¢ from Jan 1
  and 76¢ from Jul 1** (IRS Announcement 2026-11). Every trip uses the rate in
  force on its date, and you can add new rates in Settings.

## Ads (AdMob)

* **Banner:** an anchored adaptive banner at the bottom of every screen.
* **Native:** one on Home (between the stats and recent trips) and one in the
  Trips list. It takes no space until an ad loads.
* **Consent:** Google UMP (GDPR / US states) runs after the splash. Ads load
  only once consent allows it.
* **IDs:** debug builds always use Google's **test** ads. Put your real IDs in
  `gradle.properties` before building a release (see
  `docs/PLAY_CONSOLE_DATA_SAFETY.md` §3).

## Build

Requirements: Android Studio (Narwhal or newer) or JDK 17 with the Android SDK
(API 36).

```bash
cd gigometer
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew bundleRelease        # app/build/outputs/bundle/release/app-release.aab  (for Play)
```

Every push also builds on GitHub Actions (`.github/workflows/gigometer-android.yml`),
which uploads the debug APK and the release bundle as downloadable artifacts.

### Release signing

Create an upload key once and keep it safe (back it up — losing it means a key
reset request to Google):

```bash
keytool -genkeypair -v -keystore gigometer-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Then create `gigometer/keystore.properties` (it is git-ignored):

```
storeFile=gigometer-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

## Publishing checklist

1. Replace the AdMob IDs in `gradle.properties`.
2. Host `docs/privacy-policy.html` (GitHub Pages, Google Sites…). Fill in the
   date and contact email, then paste the URL into
   `res/values/strings.xml → privacy_policy_url` and into Play Console.
3. Fill in Play Console using **`docs/PLAY_CONSOLE_DATA_SAFETY.md`** (data
   safety, ads, advertising ID, background location, foreground service).
4. Store assets are in `store/`: `icon-512.png` (hi-res icon),
   `feature-graphic-1024x500.png`, and `icon.svg` (source). Take phone
   screenshots on a device or emulator.
5. Upload `app-release.aab` to an internal testing track first and test a real
   drive.

## Project layout

```
app/src/main/java/app/gigometer/mileage/
  data/        Room trip table, DataStore settings, IRS rate periods, polyline codec
  tracking/    TrackingService (GPS foreground service), AutoDetect (activity recognition), Geo
  ads/         UMP consent + AdMob init, banner and native ad composables
  export/      PDF / CSV report, JSON backup & restore
  reminder/    Daily review reminder (WorkManager)
  ui/          Theme, components, 5 screens, splash, permission flow, ViewModel
```
