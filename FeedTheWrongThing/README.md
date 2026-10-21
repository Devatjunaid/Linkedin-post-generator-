# Feed the Wrong Thing

A 3D toy-clay feeding game for kids aged 4–10, written in Kotlin with a custom OpenGL ES 3 renderer.
Players flick snacks (and very wrong things like socks, sneakers and alarm clocks) into hungry creatures' mouths and discover
silly reactions: giant cheeks, confetti sneezes, bubble burps, turning into a duck, blasting off like a rocket, and more.

Every model, texture, sound effect and music track is generated in code at runtime. The app ships with no image or audio files except the
vector app icon.

## Open and run

1. Open the `FeedTheWrongThing` folder in Android Studio (Quail 4 / 2026.1.4 or newer).
2. Let Gradle sync. The project uses:
   - AGP **9.4.0** with built-in Kotlin (no `org.jetbrains.kotlin.android` plugin)
   - Gradle **9.6.0** (wrapper included), JDK 21
   - `compileSdk 37`, `targetSdk 36`, `minSdk 26`
   - `kotlin { compilerOptions { … } }`, and no annotation processors (no kapt or KSP needed)
3. Run the `app` configuration on a device or emulator with OpenGL ES 3.0. Any phone from about 2015 onward has it.

Only one library dependency is used: `com.google.android.gms:play-services-ads`. If Android Studio suggests a newer stable
version in `gradle/libs.versions.toml`, it is safe to accept.

## What's in the game

| Area | Details |
| --- | --- |
| Gameplay | Drag a snack up and let go to throw it in a 3D arc. A dotted preview turns **gold** when the throw will be caught. Gentle aim assist, no fail states. |
| Content | 25 throwable items, 25 reactions, 10 creatures, 18 secret creature + item surprises, 14 hats to collect |
| Levels | 72 handcrafted levels on 9 floating islands, then the endless **Feast Forever** island with generated levels. Level numbers are island-local (for example 3-5), so the total is never shown. |
| Mechanics | Thought-bubble requests, "feed the WRONG thing", reaction hunts, discovery, combos, order lists, moving / peeking / flying / bobbing creatures, walls, trampolines, bumpers, fans, conveyor belts, wind, sleepyheads woken by noisy items, beat-timed mouths, a boss feast |
| Retention | 1–3 stars per level, a reaction journal (every creature × item), hat dress-up, daily gift, a bonus gift every 5 levels, endless best score |
| Screens | Animated splash, home with attract mode, island map, level picker, HUD with hints and tutorial hand, pause, results, collection, settings, exit dialog (Cancel / Exit / Rate us) |
| Audio | 39 synthesized sound effects and 3 looping music tracks, generated on first launch and cached. Music and sounds can be toggled separately, and the game is fully playable muted. |

## Ads (AdMob)

The real ad unit IDs are in `ads/AdsManager.kt` and the App ID is in `AndroidManifest.xml`.

- **Banner:** shown only on the Home, Map and Collection screens, never during gameplay or on top of popups, so children can't tap it by accident.
  If the first two requests fail, every later request waits 15 seconds.
- **Interstitial:** only at natural breaks (results → next level or map, quitting a level). At most one every 30 seconds, never
  in the first 30 seconds after launch, and never for a brand-new player's first two levels.
- **Rewarded:** after every 5th level a "Mystery Gift" is offered. The child must tap "Watch" to opt in, and "No thanks" is always
  available. If no video is available, the gift opens anyway.

**While developing, use test ads.** Register your phone as a test device in the AdMob console, or temporarily swap in
Google's sample ad unit IDs. Tapping your own live ads can get the AdMob account suspended.

## Google Play Families policy checklist

The code already does the following:

- Every ad request is tagged child-directed (`TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE`) and under the age of consent, with max ad rating **G**.
- The `AD_ID` permission and the Privacy Sandbox ad permissions are removed in the manifest.
- No analytics, no accounts, no location, no camera or microphone. Progress is stored only on the device.
- Links that leave the app ("Rate us") and "Reset progress" sit behind a grown-up check (a multiplication question).
- No ads on launch. Ads are clearly separate from game buttons.

In Play Console and AdMob you still need to:

1. Set **Target audience** to the kids' age groups you want (5 & under, 6–8, 9–12) and declare **Contains ads**.
2. Provide a **privacy policy URL** (required for apps aimed at children).
3. Fill in **Data safety**. The app itself collects nothing, but the Google Mobile Ads SDK does (see Google's
   "Data disclosure" page for the Mobile Ads SDK).
4. In AdMob, mark the app as **directed to children** and set the max ad content rating to **G** there too.

## Project layout

```
app/src/main/java/com/feedthewrongthing/game/
  MainActivity.kt        Activity, GL view + 2D overlay, frame loop, back button, ads/platform glue
  engine/                Mesh generation, mesh library, render frames, GLES3 renderer (+ icon thumbnails)
  audio/                 Synthesizer (SFX + music) and the playback engine
  models/                3D items, creatures, hats and island environments built from primitives
  game/                  Data, levels, physics world, creature AI and reactions, particles, saves, game state machine
  ui/                    2D interface kit and all screens / popups
  ads/                   AdMob integration
store/                   512 px icon, 1024×500 feature graphic and in-game 3D previews
```

## How it was verified

The build environment used to write this had no access to Google's Maven repository, so the Gradle build was **not** run there.
The following checks were run instead:

- All Kotlin sources were compiled with the Kotlin 2.4 compiler against the Android 16 (API 36) framework classes and against stubs of
  the Google Mobile Ads API.
- An automated player completed **all 72 levels and 40 endless levels** using only the in-game hint system. These runs found and fixed
  physics, hint and level-design bugs.
- Scenes were drawn with a software renderer that copies the GLES shader, to check framing and visuals (see `store/scene-preview-*.png`).

The first time you build, look at the Build output for any warnings about library versions.
