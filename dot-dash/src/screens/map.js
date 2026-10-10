// Winding level map for one world. Level 1 sits at the bottom; the path climbs.
import { register, go, setMusic } from '../app.js';
import { zipSVG, starSVG, icons } from '../art.js';
import { skyHTML, $, $$, onTap } from '../ui.js';
import { store } from '../store.js';
import { WORLDS } from '../pictures.js';
import { LEVELS, LEVELS_PER_WORLD } from '../levels.js';
import { pictureIcon } from '../render.js';
import { sfx } from '../audio.js';
import { levelOpenAd } from '../ads.js';

const STEP = 108;
const BOTTOM = 150;
const TOP = 210;
const trackHeight = () => BOTTOM + TOP + (LEVELS_PER_WORLD - 1) * STEP;
const nodeX = (i) => 50 + 27 * Math.sin(i * 1.05 + 0.5);
const nodeY = (i) => trackHeight() - BOTTOM - i * STEP;

const EGG = (cls = '') => `<svg class="egg-ico ${cls}" viewBox="0 0 100 120" aria-hidden="true"><ellipse cx="50" cy="66" rx="40" ry="50" fill="#FFF1C9" stroke="#241C5C" stroke-width="6"/><circle cx="34" cy="48" r="8" fill="#3EE0B0"/><circle cx="64" cy="38" r="6" fill="#FF8A3D"/><circle cx="64" cy="80" r="10" fill="#9B6BFF"/></svg>`;

const DECOR = {
  animals: [
    '<svg viewBox="0 0 80 90"><rect x="35" y="50" width="10" height="36" rx="4" fill="#7A4A24"/><circle cx="40" cy="38" r="26" fill="#3FAE4A"/><circle cx="24" cy="48" r="16" fill="#3FAE4A"/><circle cx="56" cy="48" r="16" fill="#2F9442"/></svg>',
    '<svg viewBox="0 0 80 60"><circle cx="24" cy="38" r="18" fill="#2F9442"/><circle cx="44" cy="30" r="22" fill="#3FAE4A"/><circle cx="62" cy="40" r="15" fill="#2F9442"/><circle cx="40" cy="24" r="3" fill="#FF5D73"/><circle cx="58" cy="34" r="3" fill="#FFC93C"/></svg>',
    '<svg viewBox="0 0 60 70"><path d="M30 68 V30" stroke="#3FAE4A" stroke-width="4"/><circle cx="30" cy="24" r="8" fill="#FFC93C"/><circle cx="30" cy="12" r="8" fill="#FF5D73"/><circle cx="41" cy="24" r="8" fill="#FF5D73"/><circle cx="19" cy="24" r="8" fill="#FF5D73"/><circle cx="30" cy="36" r="8" fill="#FF5D73"/><circle cx="30" cy="24" r="6" fill="#FFC93C"/></svg>',
  ],
  vehicles: [
    '<svg viewBox="0 0 100 50"><circle cx="30" cy="30" r="16" fill="#E6EAFF"/><circle cx="52" cy="22" r="20" fill="#E6EAFF"/><circle cx="74" cy="32" r="14" fill="#E6EAFF"/><rect x="20" y="30" width="64" height="16" rx="8" fill="#E6EAFF"/></svg>',
    '<svg viewBox="0 0 70 100"><rect x="8" y="20" width="54" height="80" rx="4" fill="#2457E6"/><rect x="16" y="30" width="12" height="12" rx="2" fill="#FFE58A"/><rect x="42" y="30" width="12" height="12" rx="2" fill="#FFE58A"/><rect x="16" y="52" width="12" height="12" rx="2" fill="#FFE58A"/><rect x="42" y="52" width="12" height="12" rx="2" fill="#5C8DFF"/><rect x="28" y="76" width="14" height="24" fill="#16399E"/></svg>',
    '<svg viewBox="0 0 60 70"><path d="M30 6 L48 62 H12 Z" fill="#FF8A3D"/><path d="M22 34 H38 L41 44 H19 Z" fill="#FFFFFF"/><rect x="6" y="60" width="48" height="8" rx="3" fill="#FF8A3D"/></svg>',
  ],
  dinos: [
    '<svg viewBox="0 0 90 90"><path d="M45 88 Q40 50 10 30 M45 88 Q48 46 44 6 M45 88 Q52 50 82 28" fill="none" stroke="#1FA97A" stroke-width="7" stroke-linecap="round"/><path d="M45 88 Q30 70 16 66 M45 88 Q62 70 76 64" fill="none" stroke="#3FAE4A" stroke-width="6" stroke-linecap="round"/></svg>',
    '<svg viewBox="0 0 100 70"><path d="M50 4 L22 66 H80 Z" fill="#0B3A36"/><path d="M42 20 Q50 12 58 20 L54 30 Q50 24 46 30 Z" fill="#FF8A3D"/><circle cx="56" cy="6" r="5" fill="#9FB8B4" opacity=".6"/></svg>',
    '<svg viewBox="0 0 80 40"><rect x="14" y="16" width="52" height="9" rx="4.5" fill="#FFF1C9"/><circle cx="14" cy="14" r="7" fill="#FFF1C9"/><circle cx="14" cy="27" r="7" fill="#FFF1C9"/><circle cx="66" cy="14" r="7" fill="#FFF1C9"/><circle cx="66" cy="27" r="7" fill="#FFF1C9"/></svg>',
  ],
};

function smoothPath(points) {
  let d = `M${points[0][0]} ${points[0][1]}`;
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[i - 1] || points[i];
    const p1 = points[i];
    const p2 = points[i + 1];
    const p3 = points[i + 2] || p2;
    const c1 = [p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6];
    const c2 = [p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6];
    d += ` C${c1[0].toFixed(1)} ${c1[1].toFixed(1)}, ${c2[0].toFixed(1)} ${c2[1].toFixed(1)}, ${p2[0].toFixed(1)} ${p2[1].toFixed(1)}`;
  }
  return d;
}

function nodeHTML(lv, i, currentIndex) {
  const done = store.isDone(lv.index);
  const open = store.levelUnlocked(lv.index);
  const current = lv.index === currentIndex;
  const best = store.data.best[lv.index] || 0;
  const cls = done ? 'done' : open ? 'open' : 'locked';
  let inner;
  if (done) inner = `${pictureIcon(lv.pic, { size: 52, fill: store.data.colors[lv.pic.id] })}<i class="node-num">${lv.number}</i>`;
  else if (open) inner = `<b>${lv.number}</b>`;
  else inner = icons.lock({ size: 26 });
  const stars = done ? `<div class="node-stars">${[0, 1, 2].map((k) => starSVG(18, k < best)).join('')}</div>` : '';
  const egg = lv.egg ? `<span class="node-egg">${EGG(store.data.eggs[lv.index] ? 'hatched' : 'wobble')}</span>` : '';
  return `<div class="node-wrap ${current ? 'is-current' : ''}" style="left:${nodeX(i)}%;top:${nodeY(i)}px">
    ${current ? `<div class="node-ring"></div>${zipSVG({ size: 64, gold: store.goldenZip(), cls: 'node-zip bob' })}` : ''}
    <button class="node ${cls}" data-index="${lv.index}" aria-label="Level ${lv.number}${done ? `, ${best} stars` : open ? '' : ', locked'}">${inner}</button>
    ${stars}${egg}
  </div>`;
}

register('map', {
  render({ world: w = 0 }) {
    const world = WORLDS[w];
    const levels = LEVELS.filter((lv) => lv.world === w);
    const nextIdx = store.nextLevel();
    const currentIndex = LEVELS[nextIdx].world === w ? nextIdx : -1;
    const decor = DECOR[world.id];
    let deco = '';
    for (let i = 0; i < LEVELS_PER_WORLD; i++) {
      const side = nodeX(i) > 50 ? 'left' : 'right';
      deco += `<div class="decor decor-${side}" style="top:${nodeY(i) - 30}px">${decor[i % decor.length]}</div>`;
    }
    const nextWorld = WORLDS[w + 1];
    const nextOpen = nextWorld && store.worldUnlocked(w + 1);
    const sign = nextWorld
      ? `<button class="map-sign ${nextOpen ? '' : 'locked'}" data-next="${w + 1}" style="--wc:${nextWorld.color}">${nextOpen ? icons.next({ size: 22 }) : icons.lock({ size: 20 })}<span>${nextWorld.name}</span>${nextOpen ? '' : `<small>${starSVG(14)} ${nextWorld.unlock}</small>`}</button>`
      : `<div class="map-sign end">${starSVG(22)}<span>You found them all!</span></div>`;
    return `<div class="map" style="--bg:${world.bg};--bg2:${world.bg2};--wc:${world.color}">
      ${skyHTML(18)}
      <header class="topbar map-top">
        <button class="icon-btn" data-back aria-label="Back home">${icons.back()}</button>
        <h1 class="map-title">${world.name}</h1>
        <div class="pill">${starSVG(22)}<b>${store.worldStars(w)}/${LEVELS_PER_WORLD * 3}</b></div>
      </header>
      <div class="map-scroll">
        <div class="map-track" style="height:${trackHeight()}px">
          <div class="hill hill-a"></div><div class="hill hill-b"></div>
          ${deco}
          <svg class="map-path" aria-hidden="true"></svg>
          <div class="map-sign-wrap" style="top:${nodeY(LEVELS_PER_WORLD - 1) - 150}px">${sign}</div>
          ${levels.map((lv, i) => nodeHTML(lv, i, currentIndex)).join('')}
        </div>
      </div>
      <div class="map-say bubble" hidden></div>
    </div>`;
  },
  mount(root, { world: w = 0 }) {
    setMusic(w);
    const track = $('.map-track', root);
    const scroll = $('.map-scroll', root);
    const svg = $('.map-path', root);

    const draw = () => {
      const W = track.clientWidth;
      const H = trackHeight();
      svg.setAttribute('viewBox', `0 0 ${W} ${H}`);
      svg.setAttribute('width', W);
      svg.setAttribute('height', H);
      const pts = [];
      for (let i = 0; i < LEVELS_PER_WORLD; i++) pts.push([(nodeX(i) / 100) * W, nodeY(i)]);
      let doneTo = 0;
      for (let i = 0; i < LEVELS_PER_WORLD; i++) if (store.isDone(w * LEVELS_PER_WORLD + i)) doneTo = i + 1;
      const full = smoothPath(pts);
      const done = doneTo > 0 ? smoothPath(pts.slice(0, Math.min(LEVELS_PER_WORLD, doneTo + 1))) : '';
      svg.innerHTML = `<path d="${full}" class="path-all"/>${done ? `<path d="${done}" class="path-done"/>` : ''}`;
    };
    draw();
    window.addEventListener('resize', draw);

    // Scroll so the current level is in view.
    const cur = $('.is-current', root) || $$('.node.done', root).pop()?.parentElement;
    const y = cur ? parseFloat(cur.style.top) : trackHeight();
    scroll.scrollTop = Math.max(0, y - scroll.clientHeight * 0.6);

    const sayBox = $('.map-say', root);
    let sayTimer = 0;
    const say = (text) => {
      sayBox.textContent = text;
      sayBox.hidden = false;
      clearTimeout(sayTimer);
      sayTimer = setTimeout(() => { sayBox.hidden = true; }, 2600);
    };

    $$('.node', root).forEach((n) => onTap(n, () => {
      const index = +n.dataset.index;
      if (!store.levelUnlocked(index)) {
        sfx.wrong();
        n.classList.remove('shake');
        void n.offsetWidth;
        n.classList.add('shake');
        say(`Finish level ${LEVELS[index].number - 1} first!`);
        return;
      }
      sfx.pop();
      levelOpenAd(() => go('play', { index }));
    }));

    const sign = $('[data-next]', root);
    onTap(sign, () => {
      const nw = +sign.dataset.next;
      if (!store.worldUnlocked(nw)) {
        sfx.wrong();
        say(`Collect ${WORLDS[nw].unlock} stars to open ${WORLDS[nw].name}!`);
        return;
      }
      sfx.pop();
      go('map', { world: nw });
    });

    onTap($('[data-back]', root), () => { sfx.tap(); go('home'); });

    return () => {
      window.removeEventListener('resize', draw);
      clearTimeout(sayTimer);
    };
  },
  back() {
    go('home');
  },
});
