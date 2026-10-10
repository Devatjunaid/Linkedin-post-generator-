// Reads numbers and names aloud so pre-readers can play on their own.
import { Capacitor } from '@capacitor/core';
import { TextToSpeech } from '@capacitor-community/text-to-speech';
import { store } from './store.js';

export function say(text) {
  if (!store.data.sound) return;
  if (Capacitor.isNativePlatform()) {
    TextToSpeech.stop().catch(() => {});
    TextToSpeech.speak({ text, lang: 'en-US', rate: 0.95, pitch: 1.25, volume: 1, category: 'ambient' }).catch(() => {});
    return;
  }
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = 'en-US';
  u.rate = 0.95;
  u.pitch = 1.25;
  window.speechSynthesis.speak(u);
}
