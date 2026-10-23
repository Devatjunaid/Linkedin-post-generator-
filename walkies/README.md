# Walkies — dog walk reminders & pee/poo log

Native Android app in Kotlin + Jetpack Compose.

## Open and run

Open **this `walkies/` folder** (not the repository root) in Android Studio Quail 4.

| | |
|---|---|
| AGP | 9.4.0, built-in Kotlin (no `org.jetbrains.kotlin.android`) |
| Gradle | 9.6.0 (wrapper included), Gradle JDK 21 |
| Kotlin | 2.4.21 via the Compose compiler plugin; `kotlin { compilerOptions { } }` |
| KSP | 2.3.12 (Room), no kapt |
| SDK | compileSdk 37, targetSdk 36, minSdk 26 |

All versions live in `gradle/libs.versions.toml`.

## What's inside

- **Animated splash**: the system launch screen hands over to a Compose animation of the
  logo (person walking a dog). They start walking on a scrolling path, the title rises in,
  and they settle back into the logo pose.
- **Home**: your dog's photo, a countdown to the next walk, time since the last pee,
  one-tap Pee / Poo / Meal / Start walk (with Undo), and today's timeline.
- **Walk**: stopwatch, goal progress, optional step count and estimated distance (hardware
  step counter, no GPS), "Pee here" / "Poo here", walk history.
- **Log**: 7-day pee/poo chart, filters, poo consistency (firm/soft/runny), add a missed
  entry, delete with Undo, and a 30-day **vet report** shared as text.
- **Reminders**: scheduled walks and potty breaks (days of the week, duration), a
  "no pee logged for N hours" alert, and a suggestion built from the dog's own pee intervals.
- **Exit popup** on Back from Home: Exit / Rate us / Cancel.

## Images

- The main dog image is **the user's own photo**, picked with the Android photo picker
  (no storage permission). Coil loads it (downsampling, caching, off-main-thread decoding).
- `app/src/main/res/drawable-nodpi/dog_default.webp` is the fallback shown until a
  photo is chosen. To use a stock dog photo instead, replace that file with a photo you
  have the rights to (Pexels / Unsplash licences allow app use). Keep the name, about
  1200×900 px, WebP or JPG, and delete the old file if the extension changes.
- `store/play_store_icon_512.png` is the Play listing icon. The launcher icon is the
  adaptive vector in `res/drawable/ic_launcher_foreground.xml`, with a themed-icon version.

## Ads (AdMob)

| Format | Where | Rule |
|---|---|---|
| Anchored adaptive banner | Bottom of every screen, under the nav bar | 2 back-to-back requests; if both fail, then every 15 s. Space reserved so content never jumps. |
| Interstitial | After *End & save walk*, before sharing the *vet report*, after adding a *new reminder* | At most 1 per 30 s. Never on launch or exit. |
| Native | Between sections on Home, Walk, Log and Reminders | Labelled "Ad" plus AdChoices. Hidden if nothing fills. |

- **Debug builds use Google's test ad units**; your real IDs are only in the `release`
  build type (`app/build.gradle.kts`). Don't tap live ads on your own phone.
- Consent: Google **UMP** runs on every launch, and ads wait for it. When required (EEA/UK),
  an "Ad privacy choices" entry appears in Settings.

## Before publishing on Google Play

1. **Privacy policy**: host one and put its URL in `privacy_policy_url`
   (`res/values/strings.xml`). Add the same URL in Play Console and AdMob.
2. **Data safety form**: declare *Advertising ID* and *app interactions/diagnostics*
   collected by the Google Mobile Ads SDK, shared for advertising. The log, photo and
   reminders stay on the device.
3. **Ads declaration**: "Contains ads" = Yes. Target audience: adults (not designed for children).
4. **app-ads.txt** on your developer website, as AdMob describes.
5. **Package name**: `com.walkies.dogwalk` is permanent once published. Change
   `applicationId` first if you want a different one.
6. Permissions used: notifications (asked from the Reminders screen), boot-completed (to
   re-arm reminders), physical activity (optional, for steps). No location, no exact
   alarms, no storage access.
