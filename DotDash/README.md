# Dot Dash! (native Android, Kotlin + Jetpack Compose)

A connect-the-dots game for ages 3–8.

- 60 levels across 3 worlds: Animal Park, Turbo Town and Dino Valley.
- All pictures are drawn in code, and all sounds are synthesised in code.
- Zip the firefly draws the line between the dots.
- Rewards: stars, stickers, mystery eggs, a daily gift, "Color it!" and Golden Zip.
- Animated splash screen, and an exit screen on the back button.

## Run it
Open this `DotDash` folder in Android Studio, let Gradle sync, then press **Run**.

## Ads (AdMob): `platform/AdsManager.kt`
- **Banner** sits under the game on every screen after the splash.
- **Interstitial** shows every time a level opens.
- **App open** shows only when the app returns from the background. It never shows on first launch.

The IDs in the code are Google's **test** IDs. Before publishing:
1. Put your ad unit IDs in `AdConfig` in `AdsManager.kt`.
2. Put your AdMob app ID in `AndroidManifest.xml`.

Ads are set to child-directed and G-rated, and the `AD_ID` permission is removed, as Google Play's Families policy requires.

## Code map
- `game/` contains the pictures, levels, saved progress and the sound synthesiser.
- `ui/` contains all the screens, the logo and the artwork.
- `platform/` contains the AdMob, audio playback, text-to-speech and storage code.
