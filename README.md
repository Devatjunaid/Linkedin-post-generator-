# Nestworth — 401(k) Retirement Planner (Android)

A React Native Android app that projects your 401(k) balance to retirement.
Built from the uploaded UI design: dark/gold theme, splash → onboarding →
calculator → animated results, plus persistent Saved plans and a functional
Settings screen.

## Features

- **Calculator** — current age, retirement age, salary, balance, and sliders
  for your contribution %, employer match %, and expected annual return.
- **Interstitial ad on Calculate** — tapping **Calculate my future** shows a
  full-screen (interstitial) ad; the results screen appears only after the ad
  is closed. If no ad is ready, the user is never blocked — results show
  immediately.
- **Results** — count-up balance, SVG area chart of growth over time, monthly
  retirement income estimate, and a contribution/match/growth breakdown.
- **AdMob banner ads** anchored on the Calculator, Results, Saved, and Settings
  screens.
- **Saved plans** — persisted on-device (AsyncStorage). Create a plan from your
  current inputs, reopen it, or delete it.
- **Settings** — working Default return rate picker (persisted, feeds the
  calculator), Rate app, Share app, Privacy policy, and About & disclaimer.

## Ads (important)

Ads currently use **Google's official AdMob TEST IDs**, so they render safely
during development. Before publishing, swap in your real IDs:

1. **App ID** — `android/app/src/main/AndroidManifest.xml`
   (the `com.google.android.gms.ads.APPLICATION_ID` meta-data).
2. **Banner + interstitial unit IDs** — `src/ads.ts` → `REAL_AD_UNITS`.

`src/ads.ts` automatically uses test ads in debug builds (`__DEV__`) and your
real IDs in release builds, so no code change is needed at build time once the
real IDs are filled in.

## Run it

Requires the React Native Android toolchain (Node 22+, JDK 17+, Android SDK).

```sh
npm install
npm start            # Metro bundler
npm run android      # build & launch on an emulator/device
```

Build a release APK:

```sh
cd android && ./gradlew assembleRelease
# output: android/app/build/outputs/apk/release/
```

## Project layout

```
App.tsx                 navigation state, ads init, persistence wiring
src/calc.ts             401(k) projection math
src/theme.ts            color palette + money formatting
src/storage.ts          AsyncStorage: saved plans + settings
src/ads.ts              AdMob unit IDs + interstitial hook
src/components/         Logo, banner slot, bottom nav, field, slider, chart
src/screens/            Splash, Welcome, Calc, Results, Saved, Settings
```

## Notes

- Figures are estimates for illustration only and are not financial advice.
- Calculations run entirely on-device; no account or network data required.
