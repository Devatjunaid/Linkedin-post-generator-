/* ---------- AdMob ad units + interstitial hook ----------
 *
 * These use Google's OFFICIAL TEST ad unit IDs via `TestIds`, so ads render
 * safely during development without risking AdMob policy strikes.
 *
 * Before publishing, set USE_TEST_ADS = false and drop your real AdMob unit
 * IDs into REAL_AD_UNITS below (and replace the App ID in
 * android/app/src/main/AndroidManifest.xml).
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  AdEventType,
  InterstitialAd,
  TestIds,
} from 'react-native-google-mobile-ads';

const USE_TEST_ADS = __DEV__; // test ads in dev, real ads in release builds

const REAL_AD_UNITS = {
  banner: 'ca-app-pub-0000000000000000/0000000000', // TODO: your real banner unit
  interstitial: 'ca-app-pub-0000000000000000/0000000000', // TODO: your real interstitial unit
};

export const AD_UNITS = {
  banner: USE_TEST_ADS ? TestIds.ADAPTIVE_BANNER : REAL_AD_UNITS.banner,
  interstitial: USE_TEST_ADS
    ? TestIds.INTERSTITIAL
    : REAL_AD_UNITS.interstitial,
};

/**
 * Keeps one interstitial preloaded. Returns `showThen(cb)`:
 * if an ad is ready it is shown and `cb` runs after the user closes it;
 * otherwise `cb` runs immediately so the user is never blocked.
 */
export function useInterstitial() {
  const adRef = useRef(
    InterstitialAd.createForAdRequest(AD_UNITS.interstitial, {
      requestNonPersonalizedAdsOnly: true,
    }),
  );
  const [loaded, setLoaded] = useState(false);
  const pendingCb = useRef<(() => void) | null>(null);

  const runPending = useCallback(() => {
    const cb = pendingCb.current;
    pendingCb.current = null;
    cb?.();
  }, []);

  useEffect(() => {
    const ad = adRef.current;

    const unsubLoaded = ad.addAdEventListener(AdEventType.LOADED, () =>
      setLoaded(true),
    );
    const unsubClosed = ad.addAdEventListener(AdEventType.CLOSED, () => {
      setLoaded(false);
      runPending();
      ad.load(); // preload the next one
    });
    const unsubError = ad.addAdEventListener(AdEventType.ERROR, () => {
      setLoaded(false);
      runPending(); // never trap the user behind a failed ad
    });

    ad.load();

    return () => {
      unsubLoaded();
      unsubClosed();
      unsubError();
    };
  }, [runPending]);

  const showThen = useCallback(
    (cb: () => void) => {
      const ad = adRef.current;
      if (loaded) {
        pendingCb.current = cb;
        ad.show();
      } else {
        cb();
        ad.load(); // try to have one ready next time
      }
    },
    [loaded],
  );

  return showThen;
}
