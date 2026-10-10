# Twilio Kids Drawing

A kids' drawing and coloring app for Android, written in Kotlin with Jetpack Compose.
Every graphic in the app (39 animals and birds, the logo, icons, brush textures) and every
sound effect is generated in code. There are no image or audio files.

- **Package:** `com.harrycodess.drawingkids`
- **Min SDK:** 26 (Android 8.0) · **Target SDK:** 35

## Features

| Screen | What it does |
| --- | --- |
| Splash | A crayon draws a rainbow, the logo pops in letter by letter and animals bounce in. Then the app open ad shows (if loaded). |
| Home | Three big cards: Free Draw, Color Animals, My Gallery. Sound on/off toggle. |
| Free Draw | Pencil (grainy), marker (see-through), crayon (waxy), eraser, stamps (star, heart, flower, smiley). 20 colors, 4 sizes, undo/redo, clear, save. |
| Pick an Animal | 39 coloring pages in 6 categories (Pets, Farm, Wild, Ocean, Birds, Bugs). A star marks pages a kid has started. |
| Coloring | Tap-to-fill bucket, plus crayon/marker/pencil/eraser. The outlines always stay on top, so lines stay crisp. Progress is saved automatically. |
| My Gallery | Saved artworks shown as taped-up photos. Tap to view or delete one. |

Landscape (tablet) shows side panels like the mockups. Portrait (phone) stacks the tool
bars above and below the paper.

## Ads (AdMob)

| Format | When |
| --- | --- |
| Banner (adaptive) | Bottom of every screen except the splash |
| Interstitial | When opening Free Draw, and when opening any animal to color |
| App open | After the splash on a cold start, and whenever the app returns from the background |

**The project ships with Google's public test IDs.** Before publishing, replace them with your own:

1. `app/build.gradle.kts` → `manifestPlaceholders["admobAppId"]` (your AdMob **App ID**)
2. `app/src/main/java/com/harrycodess/drawingkids/ads/Ads.kt` → `AdUnits` (banner, interstitial, app open **unit IDs**)

### Kids / Google Play Families settings (already applied)

- All ad requests are tagged child-directed (`TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE`), under age of consent, max content rating **G**.
- The `AD_ID` permission is removed from the manifest.

In the Play Console, set the target audience to children and complete the Families declarations.
In AdMob, mark the app as child-directed too.

## Build

Open the `TwilioKidsDrawing` folder in Android Studio (Ladybug or newer) and run, or:

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew bundleRelease        # Play Store .aab (set up signing first)
```

`store/play-store-icon-512.png` is the 512×512 icon for the Play Store listing.

## Code map

```
art/        Animal DSL + all 39 coloring pages (Animals.kt), hit-testing for the fill bucket
drawing/    Brushes, CanvasDocument (undo/redo), DrawingSurface (touch + layers), ArtworkRenderer
ads/        AdMob setup, interstitial + app open managers, adaptive banner
audio/      Synthesized sound effects
data/       Saved coloring progress and the gallery
ui/         Theme, components (buttons, icons, logo), and each screen
```

To add a coloring page, add a function in `art/Animals.kt` built from `fill`, `ink`,
`line`, `white` and `eye` calls in a 200×200 box, then list it in `Animals.all`.
