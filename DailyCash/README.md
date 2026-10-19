# Daily Cash — cash expense tracker by currency and country

Native Android app (Kotlin, Jetpack Compose, Room, AdMob). Open the `DailyCash` folder in Android Studio.

## Toolchain
- AGP 9.4.0 with built-in Kotlin (no `org.jetbrains.kotlin.android`), Gradle 9.6.0, JDK 21
- compileSdk 37, targetSdk 36, minSdk 26
- Kotlin 2.4.21 (Compose compiler plugin), KSP 2.3.12 for Room — no kapt
- All versions live in `gradle/libs.versions.toml`. If Gradle reports a Kotlin version mismatch,
  set `kotlin` there to the version it names.

## Features
- **Wallets per currency** drawn as animated banknotes; balance, home-currency value and "cash runway"
  (days your cash lasts at your recent pace).
- **Count it like cash**: tap the real notes and coins of 36 currencies; "Got change?" mode subtracts
  the change you get back. A keypad is there for exact amounts.
- **60 everyday categories** with icons in 8 groups, a search, and a "Something else?" box to save your own.
- **Auto country**: detected from the mobile network (no location permission). Every entry is stamped with
  the country and the exchange rate of that day.
- **Spend Atlas**: animated globe with your route, passport stamps per country, and "a day there vs a day at home".
- **Insights**: cash calendar heat map, 7-day and monthly totals, categories, currency mix, leftover-coin alert.
- **History** with delete and undo, **CSV export**, any ISO currency as home currency.
- Live rates from open.er-api.com, cached; offline estimate until the first refresh.
- Animated logo and splash, animated exit confirmation on back.

## Ads (AdMob)
| Format | Where | Rules |
|---|---|---|
| Anchored adaptive banner | Under the bottom dock on Wallet, Atlas, Insights, History | 2 immediate attempts, then every retry waits 15 s |
| Interstitial | After saving an entry; when opening Atlas or Insights | At most 1 per 30 s, never on launch or exit |
| Native | In the gaps between sections on Wallet, Atlas and Insights | Labelled "Ad", AdChoices shown, hidden until loaded |

Debug builds use Google's **test** ad units; release builds use your units (`app/build.gradle.kts`).
Consent is gathered with Google UMP before any ad request; "Ad privacy choices" appears in Settings when required.

## Before publishing on Google Play
1. Host a privacy policy and set `PRIVACY_POLICY_URL` in `ui/screens/SettingsScreen.kt`. Add the same URL in Play Console.
2. Publish `app-ads.txt` on your developer website (AdMob → Apps → app-ads.txt).
3. Data safety form: the app shows ads (AdMob collects device/advertising ID); entries stay on the device; no account.
4. Target audience: 18+ / not designed for children (AdMob is configured for general audiences).
5. Ads declaration: **Yes, contains ads**.
6. Create an upload key and sign the release build (`Build → Generate Signed App Bundle`).
