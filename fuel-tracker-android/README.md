# Fuel & Trip Cost Tracker (Android · Kotlin · Jetpack Compose)

Package: `com.harrycodess.fueltracker` · min SDK 26 · target SDK 35

Estimate what a trip will cost in fuel, save it, and track your spending over time.
The UI follows the design in `../fuel-tracker-design/`.

## Features

- **Trip calculator**: distance, fuel price and efficiency, with the cost updating as you type.
  Shows fuel needed and cost per km/mi.
- **Units**: Metric (km, L, km/L **or** L/100km) and Imperial (mi, US gal, mpg).
  Switching units converts the values already entered.
- **Save trip**: add an optional note and pick a date (no future dates). The last fuel price and vehicle are
  remembered for next time.
- **History & stats**: trips grouped by month with monthly totals. Shows total spent, distance, average efficiency
  and average cost per distance. Filter by month and vehicle.
- **Edit & delete**: tap a trip to edit any field (the cost is recalculated). Swipe left to delete, with **Undo**.
- **Vehicles**: add, edit and delete vehicles, each with a default efficiency that fills in the calculator.
  Deleting a vehicle keeps its trips.
- **CSV export**: export all trips (Settings) or the filtered list (History share icon) through the share sheet.
- **Settings**: unit system, efficiency format, currency symbol, theme (System / Light / Dark).
- **AdMob**
  - Fixed 320×50 **banner** above the bottom navigation on every screen. The slot is always reserved, so
    nothing moves when an ad loads.
  - **Interstitial** after every *Save Trip*, at most once every 30 s
    (`AdsConfig.INTERSTITIAL_MIN_INTERVAL_MS`). The save confirmation appears after the ad closes. If no ad is
    ready, the user is never blocked.
  - **Google UMP consent** (GDPR) on launch, plus "Ad privacy options" in Settings when required.

## Tech

Kotlin 2.0, Jetpack Compose with Material 3, Navigation Compose, Room (KSP), DataStore Preferences,
MVVM with `StateFlow`, manual DI (`AppContainer`), Google Mobile Ads SDK and UMP.

```
app/src/main/java/com/harrycodess/fueltracker/
├── ads/            AdsManager (consent, SDK init, interstitial) + AdsConfig
├── data/           Room entities/DAOs/database, FuelRepository, DataStore settings
├── domain/         FuelMath (unit conversions + cost), formatters
├── export/         CsvExporter (FileProvider share)
└── ui/             theme, components, navigation, calculator, history, edit, settings, vehicles
```

## Build & run

1. Open the `fuel-tracker-android` folder in Android Studio (Ladybug or newer).
2. Let Gradle sync, then run the `app` configuration on a device or emulator.
3. Unit tests: `./gradlew test`.

Android Studio may offer to upgrade AGP and libraries. Accepting is fine. To publish on Google Play, also raise
`targetSdk` to whatever Play currently requires.

## Going live with your AdMob account

Debug builds **always** use Google's test ads. For release builds, put your real IDs in `gradle.properties`:

```properties
admob.appId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
admob.bannerId=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
admob.interstitialId=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
```

Then also:

- Create a GDPR consent message in AdMob → *Privacy & messaging* so the consent form can show.
- Replace the debug `signingConfig` in `app/build.gradle.kts` with your upload key.
- Add an `app-ads.txt` file to your developer website.
