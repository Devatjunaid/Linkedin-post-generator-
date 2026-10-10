# Dot Dash! — connect-the-dots for kids

A connect-the-dots game for ages 3–8. Kids tap (or drag through) numbered dots, Zip
the firefly draws a glowing line, and the hidden picture comes to life with its own
sound. Android app built with [Capacitor](https://capacitorjs.com), monetised with
AdMob.

## What's inside

- **60 levels in 3 worlds:** Animal Park (20 animals), Turbo Town (20 vehicles) and
  Dino Valley (20 dinosaurs). Pictures get more dots as you go (10 → 25).
- **All artwork is code:** every picture, Zip, the logo, the icon and the splash are
  drawn as SVG, so there are no image files to manage.
- **All sound is synthesised** (Web Audio): each dot plays the next note of a tune, plus a
  soft "boing" for a wrong tap, sparkles, star chimes, a voice for every creature (roar,
  trumpet, vroom, quack…) and a gentle music loop for each world. "Say it" reads the
  number aloud (text-to-speech).
- **Rewards:**
  - 3 stars per level: finish it; make 2 or fewer wrong taps; use no hint.
  - Every picture becomes a sticker in the Sticker Book.
  - A Mystery Egg every 5 levels gives +5 stars.
  - A daily gift gives +5 stars.
  - "Color it!" gives +1 star.
  - Golden Zip (gold firefly, rainbow trail) unlocks at 150 stars.
  - Turbo Town opens at 20 stars and Dino Valley at 40.
- **Screens:**
  - Animated splash: Zip draws a line through 5 dots, then the logo drops in.
  - Home, world maps, game board, reveal and results.
  - Sticker book, colouring, daily gift.
  - Exit screen on the Android back button ("Leaving so soon?").

## Ads (AdMob) — `src/ads.js`, `src/config.js`

| Ad | When |
| --- | --- |
| Banner | Always at the bottom of the screen after the splash. The game leaves room for it so it never covers anything. |
| Interstitial | Each time a level opens (Play, a map level, Next, Replay). If no ad is loaded, the level starts straight away. |
| App open | Only when the app comes back from the background, never on first launch. It is not shown when the app was left because an ad was open or a banner was tapped. |

The IDs in `src/config.js` and `android/app/src/main/AndroidManifest.xml` are
**Google's test IDs**. Before publishing:

1. Create the app and its 3 ad units (Banner, Interstitial, App open) in AdMob.
2. Put the ad unit IDs in `src/config.js` and set `isTesting: false`.
3. Put your AdMob **app ID** in `AndroidManifest.xml` (`com.google.android.gms.ads.APPLICATION_ID`).

Ads are set up as child-directed (`tagForChildDirectedTreatment`, G-rated content
only, non-personalised), and the `AD_ID` permission is removed. Google Play's
Families policy requires this for an app aimed at children. Google's policies
also restrict ads that interrupt play for young children. If review flags the
interstitial frequency, raise `interstitialEveryNLevels` in `src/config.js`
(for example to 3).

## Build and run

Requirements: Node 20+, and Android Studio for the Android build.

```bash
cd dot-dash
npm install
npm run dev        # play in a browser at http://localhost:8080 (ads show as stand-ins)
npm run android    # build, sync into android/ and open Android Studio, then press Run
```

In the browser, Esc acts as the Android back button. Switching tabs and back
shows the app-open stand-in.

Other scripts:

- `npm run build`: bundles `src/` into `www/app.js`.
- `npm run sync`: build + `npx cap sync android`.
- `npm run icons`: re-renders the icon and splash into `resources/`, then generates
  the Android resources. Needs Playwright (`npm i -D playwright`).

## Project layout

```
src/
  pictures.js   the 60 pictures (outline = dots, plus detail art and voice)
  levels.js     orders pictures easy→hard and adds dots for later levels
  render.js     picture → SVG
  art.js        Zip, stars, icons, logo
  audio.js      synthesised sound effects + music
  speech.js     reads numbers/names aloud
  ads.js        AdMob banner / interstitial / app-open logic
  store.js      saved progress (stars, stickers, colours, settings)
  app.js        screen router, overlays, back button
  screens/      splash, home, map, play, stickers, overlays (gift, exit, colour)
www/            index.html, style.css, fonts (bundle goes here)
android/        Capacitor Android project
resources/      icon + splash source images
```
