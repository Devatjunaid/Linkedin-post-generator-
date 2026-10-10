// AdMob: footer banner, an interstitial each time a level opens, and an
// app-open ad ONLY when the app comes back from the background (never on the
// first launch). In a desktop browser the same flow runs with stand-in ads so
// it can be tested without a phone.
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import {
  AdMob,
  BannerAdSize,
  BannerAdPosition,
  BannerAdPluginEvents,
  InterstitialAdPluginEvents,
  AppOpenAdPluginEvents,
  MaxAdContentRating,
} from '@capacitor-community/admob';
import { ADS } from './config.js';

const native = Capacitor.isNativePlatform();
const APP_OPEN_TTL = 4 * 60 * 60 * 1000; // Google expires app-open ads after 4 hours
const WEB_BANNER_HEIGHT = 56;

const state = {
  ready: false,
  interstitialLoaded: false,
  interstitialLoading: false,
  appOpenLoadedAt: 0,
  appOpenLoading: false,
  fullscreen: false, // an interstitial or app-open ad is on screen
  ignoreUntil: 0, // ignore pause/resume caused by an ad closing
  backgrounded: false,
  bannerClicked: false,
  bannerShown: false,
  wantBanner: false,
  levelOpens: 0,
  afterInterstitial: null,
};

let hooks = { onPause() {}, onResume() {}, onBannerHeight() {} };

const adOpts = (adId) => ({ adId, isTesting: ADS.isTesting, npa: true });

export async function initAds(h) {
  hooks = { ...hooks, ...h };
  watchAppState();
  if (!native) return;
  try {
    await AdMob.initialize({
      initializeForTesting: ADS.isTesting,
      tagForChildDirectedTreatment: ADS.childDirected,
      tagForUnderAgeOfConsent: ADS.childDirected,
      maxAdContentRating: MaxAdContentRating.General,
    });
  } catch (err) {
    console.warn('AdMob init failed', err);
    return;
  }
  state.ready = true;

  AdMob.addListener(BannerAdPluginEvents.SizeChanged, (size) => hooks.onBannerHeight(size.height || 0));
  AdMob.addListener(BannerAdPluginEvents.Opened, () => { state.bannerClicked = true; });

  AdMob.addListener(InterstitialAdPluginEvents.Loaded, () => {
    state.interstitialLoaded = true;
    state.interstitialLoading = false;
  });
  AdMob.addListener(InterstitialAdPluginEvents.FailedToLoad, () => {
    state.interstitialLoading = false;
    setTimeout(loadInterstitial, 30000);
  });
  AdMob.addListener(InterstitialAdPluginEvents.Dismissed, finishInterstitial);
  AdMob.addListener(InterstitialAdPluginEvents.FailedToShow, finishInterstitial);

  AdMob.addListener(AppOpenAdPluginEvents.Loaded, () => {
    state.appOpenLoadedAt = Date.now();
    state.appOpenLoading = false;
  });
  AdMob.addListener(AppOpenAdPluginEvents.FailedToLoad, () => { state.appOpenLoading = false; });
  AdMob.addListener(AppOpenAdPluginEvents.Closed, finishAppOpen);
  AdMob.addListener(AppOpenAdPluginEvents.FailedToShow, finishAppOpen);

  loadInterstitial();
  loadAppOpen();
  if (state.wantBanner) showBanner();
}

// --- banner ------------------------------------------------------------------
export async function showBanner() {
  state.wantBanner = true;
  if (state.bannerShown) return;
  state.bannerShown = true;
  if (!native) {
    document.body.classList.add('web-banner');
    hooks.onBannerHeight(WEB_BANNER_HEIGHT);
    return;
  }
  if (!state.ready) {
    state.bannerShown = false;
    return;
  }
  try {
    await AdMob.showBanner({
      ...adOpts(ADS.bannerId),
      adSize: BannerAdSize.ADAPTIVE_BANNER,
      position: BannerAdPosition.BOTTOM_CENTER,
      margin: 0,
    });
  } catch (err) {
    state.bannerShown = false;
    console.warn('Banner failed', err);
  }
}

// --- interstitial: shown each time a level opens ------------------------------
function loadInterstitial() {
  if (!state.ready || state.interstitialLoaded || state.interstitialLoading) return;
  state.interstitialLoading = true;
  AdMob.prepareInterstitial(adOpts(ADS.interstitialId)).catch(() => { state.interstitialLoading = false; });
}

function finishInterstitial() {
  state.fullscreen = false;
  state.ignoreUntil = Date.now() + 1500;
  const next = state.afterInterstitial;
  state.afterInterstitial = null;
  if (next) next();
  loadInterstitial();
}

// Shows the interstitial, then runs `then` (starting the level). If no ad is
// ready the level starts straight away — a child is never kept waiting.
export function levelOpenAd(then) {
  state.levelOpens++;
  if (state.levelOpens % Math.max(1, ADS.interstitialEveryNLevels) !== 0) return then();
  if (!native) return webStandIn('Interstitial ad', then);
  if (!state.ready || !state.interstitialLoaded) {
    loadInterstitial();
    return then();
  }
  state.interstitialLoaded = false;
  state.fullscreen = true;
  state.afterInterstitial = then;
  AdMob.showInterstitial().catch(finishInterstitial);
}

// --- app-open: only after returning from the background -----------------------
function loadAppOpen() {
  if (!state.ready || state.appOpenLoading) return;
  if (state.appOpenLoadedAt && Date.now() - state.appOpenLoadedAt < APP_OPEN_TTL) return;
  state.appOpenLoadedAt = 0;
  state.appOpenLoading = true;
  AdMob.loadAppOpen({ adId: ADS.appOpenId }).catch(() => { state.appOpenLoading = false; });
}

function showAppOpen() {
  if (!native) return webStandIn('App open ad', () => {});
  const fresh = state.appOpenLoadedAt && Date.now() - state.appOpenLoadedAt < APP_OPEN_TTL;
  if (!state.ready || !fresh) {
    loadAppOpen();
    return;
  }
  state.fullscreen = true;
  state.appOpenLoadedAt = 0;
  AdMob.showAppOpen().catch(finishAppOpen);
}

function finishAppOpen() {
  state.fullscreen = false;
  state.ignoreUntil = Date.now() + 1500;
  loadAppOpen();
}

function onBackground() {
  hooks.onPause();
  // Leaving because an ad is open (or an ad was tapped) doesn't count.
  if (state.fullscreen || state.bannerClicked || Date.now() < state.ignoreUntil) return;
  state.backgrounded = true;
}

function onForeground() {
  hooks.onResume();
  state.bannerClicked = false;
  if (!state.backgrounded) return;
  state.backgrounded = false;
  showAppOpen();
}

function watchAppState() {
  if (native) {
    App.addListener('appStateChange', ({ isActive }) => (isActive ? onForeground() : onBackground()));
  } else {
    document.addEventListener('visibilitychange', () => (document.hidden ? onBackground() : onForeground()));
  }
}

// --- browser stand-in so the ad flow can be seen without a phone --------------
function webStandIn(label, then) {
  state.fullscreen = true;
  const el = document.createElement('div');
  el.className = 'ad-standin';
  el.innerHTML = `<div class="ad-standin-box"><div class="ad-standin-tag">TEST AD</div><div class="ad-standin-title">${label}</div><div class="ad-standin-sub">AdMob shows the real ad here on a phone</div><button type="button" class="ad-standin-close" disabled>Close in 2</button></div>`;
  document.body.appendChild(el);
  const btn = el.querySelector('button');
  let n = 2;
  const timer = setInterval(() => {
    n--;
    if (n > 0) { btn.textContent = `Close in ${n}`; return; }
    clearInterval(timer);
    btn.disabled = false;
    btn.textContent = 'Close ✕';
  }, 1000);
  btn.addEventListener('click', () => {
    el.remove();
    state.fullscreen = false;
    state.ignoreUntil = Date.now() + 500;
    then();
  });
}

export const adsDebug = state;
