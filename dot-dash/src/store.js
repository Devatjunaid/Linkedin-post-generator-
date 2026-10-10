// Saved progress (localStorage persists inside the app's WebView).
import { LEVELS, LEVELS_PER_WORLD } from './levels.js';
import { WORLDS } from './pictures.js';
import { GOLDEN_ZIP_STARS } from './config.js';

const KEY = 'dotdash.save.v1';

const fresh = () => ({
  best: {}, // level index -> best stars (1..3)
  colors: {}, // picture id -> colour chosen in "Color it!"
  bonus: 0, // stars from eggs, daily gifts and colouring
  eggs: {}, // level index -> true once hatched
  colored: {}, // picture id -> true once coloured (one bonus star each)
  lastGift: '',
  sound: true,
  music: true,
});

let data = fresh();
try {
  const raw = localStorage.getItem(KEY);
  if (raw) data = { ...fresh(), ...JSON.parse(raw) };
} catch (_) {
  data = fresh();
}

function save() {
  try { localStorage.setItem(KEY, JSON.stringify(data)); } catch (_) { /* storage full or blocked */ }
}

export const store = {
  get data() { return data; },
  save,

  totalStars() {
    return Object.values(data.best).reduce((a, b) => a + b, 0) + data.bonus;
  },
  worldStars(w) {
    let n = 0;
    for (let i = 0; i < LEVELS_PER_WORLD; i++) n += data.best[w * LEVELS_PER_WORLD + i] || 0;
    return n;
  },
  completedCount() {
    return Object.keys(data.best).length;
  },
  isDone(index) {
    return !!data.best[index];
  },
  worldUnlocked(w) {
    return this.totalStars() >= WORLDS[w].unlock || w === 0 || this.isDone(w * LEVELS_PER_WORLD);
  },
  levelUnlocked(index) {
    const w = Math.floor(index / LEVELS_PER_WORLD);
    if (!this.worldUnlocked(w)) return false;
    const first = w * LEVELS_PER_WORLD;
    return index === first || this.isDone(index) || this.isDone(index - 1);
  },
  // The level the PLAY button should open: first unfinished level in an open world.
  nextLevel() {
    for (const lv of LEVELS) {
      if (!this.isDone(lv.index) && this.levelUnlocked(lv.index)) return lv.index;
    }
    return 0;
  },
  currentWorld() {
    return LEVELS[this.nextLevel()].world;
  },
  goldenZip() {
    return this.totalStars() >= GOLDEN_ZIP_STARS;
  },

  // Records a finished level and returns what the child earned.
  finishLevel(index, stars) {
    const before = data.best[index] || 0;
    const firstTime = !before;
    if (stars > before) data.best[index] = stars;
    let egg = false;
    if (firstTime && LEVELS[index].egg && !data.eggs[index]) {
      data.eggs[index] = true;
      egg = true;
    }
    save();
    return { gained: Math.max(0, stars - before), firstTime, egg };
  },
  addBonus(n) {
    data.bonus += n;
    save();
  },

  today() {
    const d = new Date();
    return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
  },
  giftReady() {
    return data.lastGift !== this.today();
  },
  claimGift(n) {
    data.lastGift = this.today();
    data.bonus += n;
    save();
  },

  setColor(picId, color) {
    data.colors[picId] = color;
    const first = !data.colored[picId];
    data.colored[picId] = true;
    save();
    return first;
  },
  toggle(key) {
    data[key] = !data[key];
    save();
    return data[key];
  },
};
