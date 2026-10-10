import { register, go, setMusic } from '../app.js';
import { icons, starSVG, zipSVG } from '../art.js';
import { skyHTML, $, $$, onTap } from '../ui.js';
import { store } from '../store.js';
import { WORLDS } from '../pictures.js';
import { LEVELS } from '../levels.js';
import { pictureIcon } from '../render.js';
import { sfx } from '../audio.js';
import { stickerOverlay } from './overlays.js';
import { GOLDEN_ZIP_STARS } from '../config.js';

function grid(w) {
  return LEVELS.filter((lv) => lv.world === w).map((lv) => {
    if (store.isDone(lv.index)) {
      const tilt = ((lv.index * 37) % 15) - 7;
      return `<button class="sticker" data-index="${lv.index}" style="--tilt:${tilt}deg" aria-label="${lv.pic.name} sticker">
        <span class="sticker-disc">${pictureIcon(lv.pic, { size: 70, fill: store.data.colors[lv.pic.id] })}</span><span class="sticker-name">${lv.pic.name}</span></button>`;
    }
    return `<div class="sticker empty"><span class="sticker-disc"><b>?</b><i>${icons.lock({ size: 14 })}</i></span><span class="sticker-name">Level ${lv.number}</span></div>`;
  }).join('');
}

register('stickers', {
  render({ world: w = 0 }) {
    const total = store.totalStars();
    const gold = store.goldenZip();
    return `<div class="stickers">
      ${skyHTML(14)}
      <header class="topbar">
        <button class="icon-btn" data-back aria-label="Back home">${icons.back()}</button>
        <h1 class="map-title">Sticker Book</h1>
        <div class="pill"><b>${store.completedCount()}/60</b></div>
      </header>
      <div class="tabs" role="tablist">${WORLDS.map((wd, i) => `<button role="tab" class="tab ${i === w ? 'sel' : ''}" data-tab="${i}" aria-selected="${i === w}">${wd.short}</button>`).join('')}</div>
      <div class="book"><div class="book-grid">${grid(w)}</div></div>
      <div class="gold-card">
        ${zipSVG({ size: 54, gold: true })}
        <div class="gold-info"><b>${gold ? 'Golden Zip is yours!' : 'Unlock Golden Zip'}</b>
          <div class="meter"><i style="width:${Math.min(100, (total / GOLDEN_ZIP_STARS) * 100)}%"></i></div>
          <small>${Math.min(total, GOLDEN_ZIP_STARS)}/${GOLDEN_ZIP_STARS} ${starSVG(14)}</small></div>
      </div>
    </div>`;
  },
  mount(root, { world: w = 0 }) {
    setMusic(w);
    const bind = () => $$('.sticker[data-index]', root).forEach((s) => onTap(s, () => {
      sfx.pop();
      stickerOverlay(LEVELS[+s.dataset.index].pic);
    }));
    bind();
    $$('[data-tab]', root).forEach((t) => onTap(t, () => {
      const nw = +t.dataset.tab;
      sfx.tap();
      $$('[data-tab]', root).forEach((x) => {
        x.classList.toggle('sel', x === t);
        x.setAttribute('aria-selected', x === t);
      });
      $('.book-grid', root).innerHTML = grid(nw);
      bind();
    }));
    onTap($('[data-back]', root), () => { sfx.tap(); go('home'); });
    return null;
  },
  back() {
    go('home');
  },
});
