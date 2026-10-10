// ---------------------------------------------------------------------------
// AdMob setup
// These are Google's official TEST ad units, safe to use while developing.
// Before publishing, replace them with your own ad unit IDs from
// https://apps.admob.com and set `isTesting` to false.
// Also replace the APPLICATION_ID in android/app/src/main/AndroidManifest.xml.
// ---------------------------------------------------------------------------
export const ADS = {
  bannerId: 'ca-app-pub-3940256099942544/6300978111',
  interstitialId: 'ca-app-pub-3940256099942544/1033173712',
  appOpenId: 'ca-app-pub-3940256099942544/9257395921',
  isTesting: true,
  // This is a game for young children: ads must be child-directed (COPPA,
  // Google Play Families policy). Keep these on.
  childDirected: true,
  // Show the interstitial before every level (as requested). Raise this to
  // show it every N level opens instead.
  interstitialEveryNLevels: 1,
};

export const GOLDEN_ZIP_STARS = 150;
export const DAILY_GIFT_STARS = 5;
export const EGG_STARS = 5;
export const COLOR_IT_STARS = 1;
