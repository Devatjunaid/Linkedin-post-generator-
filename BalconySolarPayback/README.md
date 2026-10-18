# Balcony Solar Payback (Android, Kotlin + Jetpack Compose)

Estimates the yearly output of a balcony / plug-in solar system from sun-position maths and
built-in climate tables, then calculates self-consumption and payback.

Package: `com.devatjunaid.balconysolar`

## Open and run

1. Android Studio → **Open** → select this `BalconySolarPayback` folder (not the repository root).
2. Gradle JDK 21, Android SDK 37 installed. Let Gradle sync (wrapper: Gradle 9.6.0).
3. Run the `app` configuration. Debug builds use **Google's test ad units**, so test ads appear; this is intended.
4. Unit tests: `./gradlew :app:testDebugUnitTest`.

Toolchain: AGP 9.4.0 with built-in Kotlin (no `org.jetbrains.kotlin.android` plugin), Kotlin/Compose
compiler 2.4.20, KSP 2.3.12 (Room), `compileSdk 37`, `targetSdk 36`, `minSdk 26`.
All versions are in `gradle/libs.versions.toml`.

## Screens

Splash (animated) → Location → Setup → Sun path → Yield → Payback, plus Saved setups, How it works,
and an animated exit popup on Back from the first screen.

## How the numbers are calculated

| Step | Model |
| --- | --- |
| Sun position, sunrise/sunset | NOAA solar calculator (Meeus), refraction-corrected |
| Daily to 15-minute irradiance | Monthly means split into sunny/average/cloudy days with the same total; Collares-Pereira & Rabl / Liu & Jordan shapes; Erbs diffuse split |
| Panel plane | Perez 1990 sky diffuse + beam geometry + ground reflection |
| Optical / thermal | Martin & Ruiz IAM, PVsyst heat-loss cell temperature per mounting, temperature coefficient, low-light |
| Inverter | PVWatts efficiency curve with AC limit (clipping) |
| Self-use | 15-minute PV vs. hourly household profile with load fluctuation; optional battery simulation |
| Money | Year-by-year cash flow: price change, degradation, feed-in, maintenance, discounting, LCOE |

`app/src/test` checks the engine against NOAA reference times and PVGIS-range yields.

Built-in climate tables (169 cities, 59 countries) are long-term approximations. **Refine with
PVGIS** fetches satellite data for the exact coordinates from the EU JRC API (only coordinates are sent).
Tariffs, kit prices, CO₂ and plug-in notes are typical values; users edit them.

## Ads (AdMob)

| | Debug | Release |
| --- | --- | --- |
| App ID | Google test | `ca-app-pub-1948347222012540~4972652239` |
| Banner | test | `…/2103121327` |
| Interstitial | test | `…/1984446687` |
| Native | test | `…/7375454298` |

IDs are set per build type in `app/build.gradle.kts` (`manifestPlaceholders` + `BuildConfig`).

* **Consent first:** Google UMP runs on start; no ad is requested until `canRequestAds()` is true.
  "Ad privacy choices" appears in *How it works* where required (EEA/UK).
* **Banner** (bottom, anchored adaptive): first failure → retry at once; after 2 failed calls each
  retry waits 15 s; success resets (`ads/BannerRetryPolicy.kt`, unit-tested).
* **Interstitial**: only after the user starts a major action (Calculate yield, See payback, Save,
  Share), max 1 per 30 s, and none in the first 30 s after launch. None at app start or exit.
* **Native**: in natural gaps (Yield, Payback, Saved list), labelled "Ad", with AdChoices.

## Before publishing on Google Play

1. Host a privacy policy and put its URL in `app/src/main/res/values/strings.xml` (`privacy_policy_url`).
2. In AdMob: create the GDPR/US-states consent messages (Privacy & messaging), and add `app-ads.txt` to your developer website.
3. Play Console → Data safety: approximate location (optional, app functionality, not shared);
   device/advertising ID and ad interactions collected by the AdMob SDK (advertising, shared with Google).
   Declare **Contains ads** and set the target audience to adults (18+); the app is not for children.
4. Create a release keystore and sign the release build. Never click your own live ads.
5. Check the ad placements with AdMob's Ad Inspector on a test device before release.

## Notes

* Location uses only `ACCESS_COARSE_LOCATION`, asked after an in-app explanation, and only on tap.
* Fonts: Bricolage Grotesque and IBM Plex (SIL OFL 1.1, licences in `app/src/main/assets/licenses`).
