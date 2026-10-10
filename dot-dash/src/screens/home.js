import { register, go, setMusic } from '../app.js';
import { logoHTML, zipSVG, starSVG, icons } from '../art.js';
import { skyHTML, $, $$, onTap } from '../ui.js';
import { store } from '../store.js';
import { WORLDS } from '../pictures.js';
import { LEVELS, LEVELS_PER_WORLD } from '../levels.js';
import { pictureIcon } from '../render.js';
import { sfx, stopMusic } from '../audio.js';
import { showBanner, levelOpenAd } from '../ads.js';
import { giftOverlay, exitOverlay } from './overlays.js';

const HERO_PICS = ['lion', 'rocket', 'trex'];
const TIPS = [
  'Tap the dots in order!',
  'Who is hiding in the dots?',
  'Every picture plays a song!',
  'Collect stars to open new worlds!',
  'Fill your sticker book!',
];

export function topbarToggles() {
  const d = store.data;
  return `<button class="icon-btn" data-toggle="sound" aria-label="Sound ${d.sound ? 'on' : 'off'}">${d.sound ? icons.sound() : icons.mute()}</button>
    <button class="icon-btn" data-toggle="music" aria-label="Music ${d.music ? 'on' : 'off'}">${d.music ? icons.music() : icons.noMusic()}</button>`;
}

export function bindToggles(root, musicWorld) {
  $$('[data-toggle]', root).forEach((b) => onTap(b, () => {
    const key = b.dataset.toggle;
    const on = store.toggle(key);
    if (key === 'sound') b.innerHTML = on ? icons.sound() : icons.mute();
    else {
      b.innerHTML = on ? icons.music() : icons.noMusic();
      if (on) setMusic(musicWorld);
      else stopMusic();
    }
    b.setAttribute('aria-label', `${key === 'sound' ? 'Sound' : 'Music'} ${on ? 'on' : 'off'}`);
    sfx.tap();
  }));
}

function worldTile(world, w) {
  const open = store.worldUnlocked(w);
  const pic = world.pictures.find((p) => p.id === HERO_PICS[w]);
  let done = 0;
  for (let i = 0; i < LEVELS_PER_WORLD; i++) if (store.isDone(w * LEVELS_PER_WORLD + i)) done++;
  return `<button class="world-tile ${open ? '' : 'locked'}" data-world="${w}" style="--wc:${world.color};--wd:${world.dark};--wi:${world.ink}">
    <span class="world-art">${pictureIcon(pic, { size: 64 })}${open ? '' : `<span class="world-lock">${icons.lock({ size: 22 })}</span>`}</span>
    <span class="world-name">${world.short}</span>
    <span class="world-sub">${open ? `${done}/${LEVELS_PER_WORLD}` : `${starSVG(14)} ${world.unlock}`}</span>
  </button>`;
}

register('home', {
  render() {
    const next = LEVELS[store.nextLevel()];
    const allDone = store.completedCount() === LEVELS.length;
    return `<div class="home">
      ${skyHTML()}
      <header class="topbar">
        <div class="pill">${starSVG(26)}<b>${store.totalStars()}</b></div>
        <div class="topbar-right">${topbarToggles()}</div>
      </header>
      ${logoHTML('logo-home')}
      <div class="home-hero">
        <div class="bubble bubble-left home-tip">${TIPS[Math.floor(Math.random() * TIPS.length)]}</div>
        ${zipSVG({ size: 92, gold: store.goldenZip(), cls: 'bob' })}
      </div>
      <button class="btn btn-play breathe">${icons.play({ size: 34 })}PLAY</button>
      <div class="home-next">${allDone ? 'Every picture found — play them again!' : `Level ${next.number} · ${WORLDS[next.world].name}`}</div>
      <div class="worlds">${WORLDS.map(worldTile).join('')}</div>
      <div class="home-bottom">
        <button class="btn-soft" data-go="stickers">${icons.book({ stroke: '#5CC8FF' })}<span>Stickers</span><small>${store.completedCount()}/60</small></button>
        <button class="btn-soft gift-btn" data-go="gift">${icons.gift({ stroke: '#FF5D73' })}<span>Daily gift</span>${store.giftReady() ? '<i class="badge">1</i>' : '<small>Tomorrow</small>'}</button>
      </div>
    </div>`;
  },
  mount(root, params) {
    setMusic(0);
    showBanner();
    bindToggles(root, 0);

    onTap($('.btn-play', root), () => {
      sfx.pop();
      const index = store.nextLevel();
      levelOpenAd(() => go('play', { index }));
    });

    $$('.world-tile', root).forEach((t) => onTap(t, () => {
      const w = +t.dataset.world;
      if (!store.worldUnlocked(w)) {
        sfx.wrong();
        t.classList.remove('shake');
        void t.offsetWidth;
        t.classList.add('shake');
        $('.home-tip', root).textContent = `Collect ${WORLDS[w].unlock} stars to open ${WORLDS[w].name}!`;
        return;
      }
      sfx.pop();
      go('map', { world: w });
    }));

    onTap($('[data-go="stickers"]', root), () => { sfx.pop(); go('stickers', { world: store.currentWorld() }); });
    onTap($('[data-go="gift"]', root), () => {
      sfx.pop();
      giftOverlay(() => go('home'));
    });

    if (params.fromSplash) root.classList.add('enter');
    return null;
  },
  back() {
    exitOverlay();
  },
});

