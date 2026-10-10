import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import { SplashScreen } from '@capacitor/splash-screen';
import { go, handleBack, app, setMusic } from './app.js';
import { initAds } from './ads.js';
import { unlockAudio, suspendAudio, resumeAudio, stopMusic } from './audio.js';
import './screens/splash.js';
import './screens/home.js';
import './screens/map.js';
import './screens/play.js';
import './screens/stickers.js';

const native = Capacitor.isNativePlatform();

// Keep room at the bottom for the banner so it never covers the game.
function setBannerHeight(px) {
  document.documentElement.style.setProperty('--banner-h', `${Math.round(px)}px`);
}

// Audio may only start after a tap.
let audioUnlocked = false;
document.addEventListener('pointerdown', () => {
  unlockAudio();
  if (!audioUnlocked) {
    audioUnlocked = true;
    if (app.musicWorld !== null) setMusic(app.musicWorld);
  }
}, { capture: true });

// No long-press menus, double-tap zoom or text selection while playing.
document.addEventListener('contextmenu', (e) => e.preventDefault());
document.addEventListener('dblclick', (e) => e.preventDefault());

initAds({
  onBannerHeight: setBannerHeight,
  onPause() {
    stopMusic();
    suspendAudio();
  },
  onResume() {
    resumeAudio();
    if (app.musicWorld !== null) setMusic(app.musicWorld);
  },
});

if (native) {
  App.addListener('backButton', handleBack);
  SplashScreen.hide({ fadeOutDuration: 200 }).catch(() => {});
} else {
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' || e.key === 'Backspace') handleBack();
  });
}

go('splash');
