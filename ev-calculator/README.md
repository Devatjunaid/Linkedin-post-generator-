# EV Calculator (Android · Kotlin · Jetpack Compose)

Package: `com.voltora.evcalculator`

Charging cost & trip calculator for electric cars, with typical prices for 38 countries and 81 EV models from 35 brands.

## Screens
- **Animated splash**: the system splash plays an animated vector logo, then a custom Compose splash. Speed-lines move across the screen, the logo draws itself (gauge ring, bolt, plug prongs, charge pulse), the wordmark appears letter by letter and a segmented battery bar fills up.
- **Home**: your car, a battery % slider, range, the cost to charge to 80% tonight, your country's rates, a cost-per-100-km comparison (EV at home vs DC fast vs petrol), a native ad and a trip shortcut.
- **Vehicle**: search plus brand chips. Each model shows usable kWh, official range (WLTP/EPA/CLTC/MIDC), peak DC power and its 10–80% charge time.
- **Charge**: a start → target range slider, charging location (home / public AC / DC fast) and charger power. Shows cost, time, kWh billed, range added and the same charge at the other locations.
- **Trip**: distance, battery when you leave, minimum battery level, driving style, how far to charge at stops and fast-charger power. Gives how much to charge before leaving, a timeline of stops, total cost, cost per 100 km, charging time and savings vs petrol.
- **Settings**: country, your own electricity/petrol prices, home charger power, km/miles, the petrol car to compare against, and privacy & ad choices.
- **Exit popup**: pressing back on Home asks "Leaving already?" and shows a native ad.

## Ads (AdMob)
| Format | Where |
|---|---|
| Anchored adaptive banner | Under the bottom nav, same dark background. It stays collapsed until an ad loads. |
| Interstitial | After *Continue* on Vehicle and after *Plan my trip*. At most once every 90 s, never in the first 30 s, and only every 2nd break. |
| Native | Home, Charge, Trip results and the exit popup. Styled like the app's own cards. |

Consent: Google UMP (GDPR/UK/US-state) runs before ads are requested. Settings shows "Privacy & ad choices" when that is required.

**Before publishing:**
1. Put your AdMob **App ID** in `app/src/main/res/values/strings.xml` (`admob_app_id`).
2. Put your **ad unit IDs** in `app/src/main/java/com/voltora/evcalculator/ads/AdIds.kt` (`RELEASE_*`). Debug builds always use Google's test IDs.
3. Create a GDPR message in AdMob → Privacy & messaging.
4. Add `app-ads.txt` to your developer website.

## Data and calculations
- Prices in `data/Countries.kt` are typical 2025 national averages in local currency (home electricity incl. taxes, public AC, DC fast, petrol per litre). Tariffs vary, so users can enter their own in Settings.
- EV specs in `data/EvDatabase.kt` are the manufacturer figures: usable battery, official range and test cycle, max AC/DC power, and 10–80% DC time.
- Real-world consumption = official consumption × test-cycle factor (WLTP 1.12, EPA 1.00, CLTC 1.30, MIDC 1.35) × driving style (City 0.88, Mixed 1.0, Highway 1.18, Winter 1.35).
- AC charging counts ~10% loss, DC ~5%. DC time follows a typical charge curve, scaled so each car hits its official 10–80% time and capped by the charger's power.

## Build
Open the folder in Android Studio (Ladybug or newer, JDK 17), let Gradle sync, then run. AGP 8.7.3 · Kotlin 2.1.0 · Compose BOM 2024.12.01 · minSdk 24 · targetSdk 35.

`playstore_icon_512.png` is the 512×512 Play Store icon.
