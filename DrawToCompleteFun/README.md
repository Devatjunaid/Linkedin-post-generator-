# Draw to Complete Fun

A drawing puzzle game for kids: every picture is missing one or more lines. Find the gap, draw it with
your finger, and win stars. It has 100 levels in 4 worlds that get cleverer as you go, with animations,
sounds, a sticker book and a daily gift.

- **Package name:** `com.drawtocompletefun.kids`
- **Toolchain:** AGP 9.4.0 (built-in Kotlin), Gradle 9.6.0, JDK 21, compileSdk 37, targetSdk 36, minSdk 26
- **Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Google Mobile Ads 25.5.0, UMP 3.2.0
- No KSP or kapt is needed: there is no annotation processing (no Room or Hilt).

## Open and run

1. Open the `DrawToCompleteFun` folder in Android Studio (File > Open).
2. Let Gradle sync. Studio uses the included wrapper (Gradle 9.6.0) and Gradle JDK 21.
3. Run the `app` configuration on a device or emulator.

**Debug builds show Google's test ads.** Release builds use your real AdMob units, so you never risk
your AdMob account by tapping your own ads while testing.

## The game

| World | Levels | What's in it |
|---|---|---|
| 1. Shapes | 1-25 | Squares, stars, houses, kites: usually 1-2 missing lines |
| 2. Animals | 26-50 | Kitty, fish, owl, crab, lion... 1-4 missing lines |
| 3. Things | 51-75 | Cars, rockets, trains, robots... 1-4 missing lines |
| 4. Brain Twisters | 76-100 | Mirror the other half, finish patterns, 3-6 missing lines |

- **Stars:** 3 for a neat drawing with no hints, 2 for a few mistakes or one hint, and 1 for finishing.
- **Stickers:** 20 stickers. A new one unlocks every 14 stars.
- **Daily gift:** +1 star each day, +3 on day 7 of a streak.
- **Treasure levels:** every 5th level (5, 10, 15 ... 100). See Ads below.
- Kids can draw in either direction. Matching is forgiving, but tracing a line that is already there
  does not count.

## Ads (AdMob)

All ad code is in `app/src/main/java/com/drawtocompletefun/kids/ads/`.

| Format | Where | Rule |
|---|---|---|
| Banner | Home, Level map and Rewards screens (never while drawing) | If the first 2 requests fail, every later request waits 15 seconds (`BannerController`) |
| Interstitial | Next level, Replay, Map, quitting a level, opening the sticker book | At most 1 per 30 seconds, and none in the first 30 seconds after launch (`AdsManager.INTERSTITIAL_GAP_MS`) |
| Rewarded | Treasure levels: after every 4 levels, the 5th opens after a video | Always started by the player tapping "Watch video". If no video can load, the level opens for free so a child is never stuck. |

Your IDs are set in `AdsManager.Ids` (ad units) and `res/values/strings.xml` (`admob_app_id`).

## Google Play Families policy checklist

These are built in already:
- Every ad request is tagged child-directed (COPPA) and under age of consent, with max ad rating **G**.
- The `AD_ID` permission is removed in the manifest (apps for kids must not collect the advertising ID).
- No analytics, accounts, chat or data collection. Progress stays on the device.
- UMP consent flow runs at start (tagged under age of consent).
- Interstitials only appear at natural breaks, never during drawing, never at launch and never on exit.
- Rewarded ads are opt-in. They are never forced, and the game continues if no ad loads.
- Leaving the app (Rate us, Privacy policy) sits behind a grown-up check (a simple sum).
- "Rate us" opens the store page (Google's in-app review sheet must not be behind a button).
- All art, icons and sounds were created for this project (vector drawings and synthesized audio).

You still need to do these in Play Console / AdMob:
1. **Host the privacy policy.** `privacy-policy.html` is ready (publisher Super Dev Labs). Enable GitHub Pages (or any host),
   then put its URL in `res/values/strings.xml` (`privacy_policy_url`) and in Play Console.
2. **Play Console > App content:**
   - Target audience: choose the children's age groups.
   - Ads: answer "Yes, my app contains ads".
   - Data safety: declare what the AdMob SDK collects (device or other IDs, app interactions, diagnostics; not shared for personalised ads).
3. **AdMob:** in App settings, mark the app as "Designed for families / child-directed".
   In Blocking controls, set the max ad content rating to G.
4. Sign the release build (Build > Generate Signed App Bundle) and upload the `.aab`.

Store graphics are in `store-assets/`: a 512x512 icon, a 1024x500 feature graphic and six 1080x1920 phone screenshots.

## Project map

```
app/src/main/java/com/drawtocompletefun/kids/
  MainActivity.kt          System splash -> animated splash, back handling, lifecycle
  AndroidPlatform.kt       Sounds, ads, Rate us, privacy link, exit
  ads/                     AdsManager (consent, interstitial, rewarded), BannerAd (retry cap)
  audio/SoundPlayer.kt     SoundPool effects and a looping music track
  data/PrefsStore.kt       On-device save
  game/                    Pictures (all drawings), Levels (100), StrokeMatcher, Progress
  ui/                      AppState (navigation), AppRoot, theme, components, screens
app/src/main/res/raw/      Synthesized sound effects and music
app/src/main/res/drawable/ Adaptive launcher icon (foreground, background, monochrome)
```
