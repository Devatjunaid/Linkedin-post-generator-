// Every sound in the game is synthesised live with the Web Audio API,
// so there are no audio files to ship.
import { store } from './store.js';

let ctx = null;
let master;
let sfxBus;
let musicBus;
let noiseBuf;

function ensure() {
  if (ctx) return ctx;
  const AC = window.AudioContext || window.webkitAudioContext;
  if (!AC) return null;
  ctx = new AC();
  master = ctx.createGain();
  master.gain.value = 0.9;
  const comp = ctx.createDynamicsCompressor();
  master.connect(comp).connect(ctx.destination);
  sfxBus = ctx.createGain();
  sfxBus.connect(master);
  musicBus = ctx.createGain();
  musicBus.gain.value = 0.32;
  musicBus.connect(master);
  noiseBuf = ctx.createBuffer(1, ctx.sampleRate * 2, ctx.sampleRate);
  const ch = noiseBuf.getChannelData(0);
  for (let i = 0; i < ch.length; i++) ch[i] = Math.random() * 2 - 1;
  return ctx;
}

// Browsers only allow audio after a tap: call this from the first pointerdown.
export function unlockAudio() {
  const c = ensure();
  if (c && c.state === 'suspended') c.resume();
}

export function suspendAudio() {
  if (ctx && ctx.state === 'running') ctx.suspend();
}

export function resumeAudio() {
  if (ctx && ctx.state === 'suspended') ctx.resume();
}

const sfxOn = () => store.data.sound && ensure();

// --- building blocks --------------------------------------------------------
function tone(freq, dur, { type = 'sine', vol = 0.3, when = 0, attack = 0.01, glide, glideTime, bus, vibrato = 0, vibratoRate = 6, filter } = {}) {
  const t0 = ctx.currentTime + when;
  const osc = ctx.createOscillator();
  osc.type = type;
  osc.frequency.setValueAtTime(freq, t0);
  if (glide) osc.frequency.exponentialRampToValueAtTime(glide, t0 + (glideTime || dur));
  if (vibrato) {
    const lfo = ctx.createOscillator();
    const lg = ctx.createGain();
    lfo.frequency.value = vibratoRate;
    lg.gain.value = vibrato;
    lfo.connect(lg).connect(osc.frequency);
    lfo.start(t0);
    lfo.stop(t0 + dur + 0.05);
  }
  const g = ctx.createGain();
  g.gain.setValueAtTime(0.0001, t0);
  g.gain.exponentialRampToValueAtTime(vol, t0 + attack);
  g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
  let node = osc;
  if (filter) {
    const f = ctx.createBiquadFilter();
    f.type = filter.type || 'lowpass';
    f.frequency.value = filter.freq;
    f.Q.value = filter.q || 1;
    node.connect(f);
    node = f;
  }
  node.connect(g).connect(bus || sfxBus);
  osc.start(t0);
  osc.stop(t0 + dur + 0.05);
}

function noise(dur, { when = 0, vol = 0.3, type = 'bandpass', freq = 1000, to, q = 1, attack = 0.01 } = {}) {
  const t0 = ctx.currentTime + when;
  const src = ctx.createBufferSource();
  src.buffer = noiseBuf;
  const f = ctx.createBiquadFilter();
  f.type = type;
  f.Q.value = q;
  f.frequency.setValueAtTime(freq, t0);
  if (to) f.frequency.exponentialRampToValueAtTime(to, t0 + dur);
  const g = ctx.createGain();
  g.gain.setValueAtTime(0.0001, t0);
  g.gain.exponentialRampToValueAtTime(vol, t0 + attack);
  g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
  src.connect(f).connect(g).connect(sfxBus);
  src.start(t0);
  src.stop(t0 + dur + 0.05);
}

const SCALE = [0, 2, 4, 7, 9]; // major pentatonic: every note sounds good together
const noteFreq = (semis, base = 392) => base * Math.pow(2, semis / 12);

function bell(freq, when = 0, vol = 0.25) {
  tone(freq, 0.6, { type: 'sine', vol, when });
  tone(freq * 2, 0.35, { type: 'triangle', vol: vol * 0.35, when });
  tone(freq * 3.01, 0.2, { type: 'sine', vol: vol * 0.15, when });
}

// --- game sounds -----------------------------------------------------------
export const sfx = {
  // Each dot plays the next note up: the picture plays a little tune.
  dot(i) {
    if (!sfxOn()) return;
    const step = i % 11;
    const semis = SCALE[step % 5] + 12 * Math.floor(step / 5);
    bell(noteFreq(semis), 0, 0.28);
  },
  wrong() {
    if (!sfxOn()) return;
    tone(320, 0.32, { type: 'sine', vol: 0.3, glide: 140, vibrato: 30, vibratoRate: 18 });
  },
  pop() {
    if (!sfxOn()) return;
    tone(500, 0.09, { type: 'sine', vol: 0.25, glide: 900 });
  },
  tap() {
    if (!sfxOn()) return;
    tone(700, 0.07, { type: 'triangle', vol: 0.18, glide: 1100 });
  },
  whoosh() {
    if (!sfxOn()) return;
    noise(0.5, { vol: 0.25, freq: 300, to: 3000, q: 2 });
  },
  sparkle(when = 0) {
    if (!sfxOn()) return;
    [0, 4, 7, 12, 16].forEach((s, k) => bell(noteFreq(s + 12, 523), when + k * 0.05, 0.12));
  },
  star(k) {
    if (!sfxOn()) return;
    bell(noteFreq([0, 4, 7][k] + 12, 523), 0, 0.3);
  },
  fanfare() {
    if (!sfxOn()) return;
    [0, 4, 7, 12].forEach((s, k) => {
      tone(noteFreq(s, 523), 0.35, { type: 'triangle', vol: 0.2, when: k * 0.11 });
      tone(noteFreq(s, 262), 0.35, { type: 'sine', vol: 0.15, when: k * 0.11 });
    });
    tone(noteFreq(12, 523), 0.9, { type: 'triangle', vol: 0.22, when: 0.46, vibrato: 6 });
  },
  unlock() {
    if (!sfxOn()) return;
    tone(300, 0.12, { type: 'square', vol: 0.12, glide: 900, filter: { freq: 2000 } });
    noise(0.18, { vol: 0.15, when: 0.08, freq: 4000, type: 'highpass' });
    this.sparkle(0.15);
  },
  gift() {
    if (!sfxOn()) return;
    noise(0.3, { vol: 0.2, freq: 1500, to: 5000 });
    this.fanfare();
  },
  bye() {
    if (!sfxOn()) return;
    [7, 4, 0].forEach((s, k) => bell(noteFreq(s, 523), k * 0.14, 0.22));
  },
  // The creature's own voice when the picture comes alive.
  voice(spec) {
    if (!sfxOn()) return;
    const [name, p = '1'] = spec.split(':');
    const v = VOICES[name] || VOICES.happy;
    v.call(VOICES, parseFloat(p));
  },
};

const VOICES = {
  roar(p) {
    tone(140 * p, 1.1, { type: 'sawtooth', vol: 0.35, glide: 55 * p, attack: 0.08, vibrato: 12, vibratoRate: 22, filter: { freq: 900 } });
    tone(147 * p, 1.1, { type: 'sawtooth', vol: 0.25, glide: 60 * p, attack: 0.08, filter: { freq: 700 } });
    noise(1.0, { vol: 0.3, freq: 500 * p, to: 200, attack: 0.08, q: 0.7 });
  },
  growl(p) {
    tone(110 * p, 0.7, { type: 'sawtooth', vol: 0.3, glide: 80 * p, attack: 0.05, vibrato: 15, vibratoRate: 28, filter: { freq: 600 } });
    noise(0.6, { vol: 0.2, freq: 300 * p, attack: 0.05 });
  },
  trumpet(p) {
    tone(300 * p, 0.9, { type: 'sawtooth', vol: 0.28, glide: 560 * p, glideTime: 0.3, attack: 0.04, vibrato: 18, vibratoRate: 7, filter: { type: 'bandpass', freq: 1400 * p, q: 1.5 } });
  },
  meow() {
    tone(500, 0.6, { type: 'triangle', vol: 0.3, glide: 850, glideTime: 0.25, filter: { type: 'bandpass', freq: 1200, q: 2 } });
    tone(850, 0.35, { type: 'triangle', vol: 0.2, when: 0.25, glide: 450, filter: { type: 'bandpass', freq: 1000, q: 2 } });
  },
  woof() {
    [0, 0.28].forEach((w) => {
      tone(230, 0.16, { type: 'square', vol: 0.22, when: w, glide: 140, filter: { freq: 900 } });
      noise(0.12, { vol: 0.2, when: w, freq: 600 });
    });
  },
  quack() {
    [0, 0.25].forEach((w) => tone(320, 0.18, { type: 'sawtooth', vol: 0.22, when: w, glide: 220, filter: { type: 'bandpass', freq: 1300, q: 3 } }));
  },
  oink() {
    [0, 0.22].forEach((w) => tone(180, 0.16, { type: 'square', vol: 0.2, when: w, glide: 130, vibrato: 25, vibratoRate: 30, filter: { freq: 700 } }));
  },
  hoot() {
    [0, 0.4].forEach((w) => tone(430, 0.32, { type: 'sine', vol: 0.3, when: w, glide: 370, attack: 0.05 }));
  },
  squeak() {
    [0, 0.15, 0.3].forEach((w) => tone(1300, 0.1, { type: 'sine', vol: 0.2, when: w, glide: 1900 }));
  },
  neigh() {
    tone(520, 0.8, { type: 'sawtooth', vol: 0.2, glide: 380, vibrato: 60, vibratoRate: 16, filter: { type: 'bandpass', freq: 1200, q: 2 } });
  },
  ooh() {
    [0, 0.18, 0.36, 0.5].forEach((w, k) => tone(k < 2 ? 320 : 520, 0.16, { type: 'triangle', vol: 0.22, when: w, glide: k < 2 ? 600 : 800 }));
  },
  bubble() {
    for (let k = 0; k < 6; k++) tone(300 + Math.random() * 300, 0.1, { type: 'sine', vol: 0.22, when: k * 0.09, glide: 900 + Math.random() * 600 });
  },
  hiss() {
    noise(0.9, { vol: 0.2, type: 'highpass', freq: 4000, attack: 0.1 });
  },
  twinkle() {
    [12, 16, 19, 24, 19, 24].forEach((s, k) => bell(noteFreq(s, 523), k * 0.08, 0.12));
  },
  happy() {
    [0, 4, 7, 12].forEach((s, k) => tone(noteFreq(s, 523), 0.15, { type: 'triangle', vol: 0.2, when: k * 0.08 }));
  },
  vroom(p) {
    tone(70 * p, 1.0, { type: 'sawtooth', vol: 0.3, glide: 260 * p, glideTime: 0.7, vibrato: 10, vibratoRate: 30, filter: { freq: 1200 } });
    noise(0.8, { vol: 0.12, freq: 400, to: 1500 });
  },
  honk(p) {
    [0, 0.3].forEach((w) => {
      tone(350 * p, 0.22, { type: 'square', vol: 0.14, when: w, filter: { freq: 1800 } });
      tone(440 * p, 0.22, { type: 'square', vol: 0.12, when: w, filter: { freq: 1800 } });
    });
  },
  choo() {
    [0, 0.45].forEach((w) => {
      tone(620, 0.35, { type: 'sine', vol: 0.22, when: w, attack: 0.04 });
      tone(780, 0.35, { type: 'sine', vol: 0.18, when: w, attack: 0.04 });
      noise(0.3, { vol: 0.1, when: w, freq: 2500 });
    });
  },
  whoosh() {
    noise(0.9, { vol: 0.28, freq: 250, to: 3500, q: 1.5, attack: 0.2 });
  },
  blastoff() {
    noise(1.3, { vol: 0.3, type: 'lowpass', freq: 200, to: 3000, attack: 0.2 });
    tone(150, 1.2, { type: 'sawtooth', vol: 0.12, glide: 1200, filter: { freq: 1500 } });
  },
  whirr() {
    for (let k = 0; k < 10; k++) noise(0.07, { vol: 0.22, when: k * 0.08, type: 'lowpass', freq: 500 });
  },
  siren(p) {
    for (let k = 0; k < 4; k++) tone((k % 2 ? 900 : 650) * p, 0.28, { type: 'triangle', vol: 0.2, when: k * 0.28, attack: 0.03 });
  },
  beam() {
    tone(300, 1.1, { type: 'sine', vol: 0.25, glide: 1500, vibrato: 40, vibratoRate: 9 });
  },
  horn() {
    tone(140, 1.0, { type: 'sawtooth', vol: 0.25, attack: 0.1, filter: { freq: 700 } });
    tone(176, 1.0, { type: 'sawtooth', vol: 0.15, attack: 0.1, filter: { freq: 700 } });
  },
  jingle() {
    [7, 12, 11, 12, 16, 12].forEach((s, k) => bell(noteFreq(s, 523), k * 0.13, 0.15));
  },
  ding() {
    bell(noteFreq(12, 523), 0, 0.3);
    bell(noteFreq(7, 523), 0.3, 0.3);
  },
  splash() {
    noise(0.5, { vol: 0.25, type: 'highpass', freq: 1500, attack: 0.02 });
    this.bubble();
  },
  peep() {
    [0, 0.14, 0.28].forEach((w) => tone(1500, 0.1, { type: 'sine', vol: 0.2, when: w, glide: 2200 }));
  },
  rumble() {
    noise(1.4, { vol: 0.4, type: 'lowpass', freq: 160, attack: 0.15 });
    tone(55, 1.3, { type: 'sawtooth', vol: 0.15, vibrato: 4, vibratoRate: 9, filter: { freq: 300 } });
  },
  screech(p) {
    tone(900 * p, 0.6, { type: 'sawtooth', vol: 0.16, glide: 1500 * p, glideTime: 0.2, vibrato: 50, vibratoRate: 20, filter: { type: 'bandpass', freq: 1800, q: 2 } });
  },
  tweet() {
    [0, 0.13, 0.26].forEach((w) => tone(2400, 0.1, { type: 'sine', vol: 0.15, when: w, glide: 3400 }));
  },
};

// --- background music: a gentle plucked loop, one key per world ------------
const PROG = [[0, 4, 7], [7, 11, 14], [9, 12, 16], [5, 9, 12]]; // I V vi IV
const KEYS = [0, 2, -3];
let music = { timer: null, step: 0, next: 0, world: 0 };

function pluck(freq, when, vol) {
  tone(freq, 0.5, { type: 'triangle', vol, when, attack: 0.005, bus: musicBus });
  tone(freq * 2, 0.25, { type: 'sine', vol: vol * 0.3, when, attack: 0.005, bus: musicBus });
}

function tick() {
  const spb = 60 / 100 / 2; // eighth notes at 100 bpm
  while (music.next < ctx.currentTime + 0.15) {
    const bar = Math.floor(music.step / 8) % 4;
    const beat = music.step % 8;
    const chord = PROG[bar];
    const key = KEYS[music.world] || 0;
    const when = music.next - ctx.currentTime;
    const pattern = [0, 1, 2, 1, 0, 1, 2, 1];
    pluck(noteFreq(chord[pattern[beat]] + key, 262), when, 0.09);
    if (beat === 0 || beat === 4) tone(noteFreq(chord[0] + key - 12, 131), 0.6, { type: 'sine', vol: 0.12, when, bus: musicBus });
    music.step++;
    music.next += spb;
  }
}

export function playMusic(world = 0) {
  if (!store.data.music || !ensure()) return;
  if (music.timer && music.world === world) return;
  stopMusic();
  music = { timer: null, step: 0, next: ctx.currentTime + 0.1, world };
  music.timer = setInterval(tick, 40);
}

export function stopMusic() {
  if (music.timer) clearInterval(music.timer);
  music.timer = null;
}
