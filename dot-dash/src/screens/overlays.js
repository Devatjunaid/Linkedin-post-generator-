// Pop-up screens: daily gift, exit, colouring and sticker close-up.
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import { openOverlay, closeOverlay } from '../app.js';
import { zipSVG, starSVG, icons } from '../art.js';
import { $, $$, onTap, confetti, wait, article } from '../ui.js';
import { store } from '../store.js';
import { LEVELS } from '../levels.js';
import { pictureIcon, silhouetteIcon } from '../render.js';
import { sfx, stopMusic, suspendAudio } from '../audio.js';
import { say } from '../speech.js';
import { DAILY_GIFT_STARS, COLOR_IT_STARS } from '../config.js';

const GIFT_SVG = `<svg class="gift-box" viewBox="0 0 160 160" aria-hidden="true">
  <g class="gift-lid"><rect x="18" y="44" width="124" height="30" rx="8" fill="#FF5D73" stroke="#241C5C" stroke-width="5"/><rect x="70" y="44" width="20" height="30" fill="#FFC93C" stroke="#241C5C" stroke-width="5"/>
  <path d="M80 44 C 60 10, 30 22, 46 40 Z M80 44 C 100 10, 130 22, 114 40 Z" fill="#FFC93C" stroke="#241C5C" stroke-width="5" stroke-linejoin="round"/></g>
  <rect x="28" y="72" width="104" height="72" rx="8" fill="#FF7A8C" stroke="#241C5C" stroke-width="5"/>
  <rect x="70" y="72" width="20" height="72" fill="#FFC93C" stroke="#241C5C" stroke-width="5"/>
</svg>`;

export function giftOverlay(onDone) {
  const ready = store.giftReady();
  const o = openOverlay(`<div class="card gift-card">
    <h2>Daily Gift</h2>
    ${ready ? `<button class="gift-tap wobble" aria-label="Open your gift">${GIFT_SVG}</button><p class="gift-msg">Tap the present!</p>`
    : `${zipSVG({ size: 110, cls: 'bob', gold: store.goldenZip() })}<p class="gift-msg">You opened today's gift.<br>Come back tomorrow for another one!</p><button class="btn btn-sun" data-close>OK</button>`}
  </div>`, { onClose: onDone });
  const node = o.node;
  onTap($('[data-close]', node), () => closeOverlay(o));
  const tap = $('.gift-tap', node);
  onTap(tap, async () => {
    if (tap.classList.contains('opened')) return;
    tap.classList.remove('wobble');
    tap.classList.add('opened');
    store.claimGift(DAILY_GIFT_STARS);
    sfx.gift();
    confetti(node);
    $('.gift-msg', node).innerHTML = `<span class="gift-win">+${DAILY_GIFT_STARS} ${starSVG(30)}</span>`;
    say(`You got ${DAILY_GIFT_STARS} stars!`);
    await wait(700);
    const btn = document.createElement('button');
    btn.className = 'btn btn-sun';
    btn.textContent = 'YAY!';
    $('.gift-card', node).appendChild(btn);
    onTap(btn, () => closeOverlay(o));
  });
}

export function exitOverlay() {
  sfx.pop();
  const hiding = LEVELS.filter((lv) => !store.isDone(lv.index)).slice(0, 3);
  const peek = hiding.length
    ? `<div class="exit-peek"><p>Still hiding in the dots…</p><div class="exit-sils">${hiding.map((lv) => `<span class="sil">${silhouetteIcon(lv.pic, 54)}<b>?</b></span>`).join('')}</div></div>`
    : '';
  const o = openOverlay(`<div class="exit">
    <div class="exit-stars" aria-hidden="true">${Array.from({ length: 14 }, (_, i) => `<i style="left:${(i * 7.3) % 100}%;animation-delay:${(i * 0.37) % 3}s">${starSVG(14 + (i % 3) * 6)}</i>`).join('')}</div>
    <div class="exit-zip">${zipSVG({ size: 130, mood: 'sad', gold: store.goldenZip(), cls: 'exit-wave' })}</div>
    <h2 class="exit-title">Leaving so soon?</h2>
    <p class="exit-sub">Zip will miss you! You have <b>${store.totalStars()}</b> ${starSVG(20)}</p>
    ${peek}
    <button class="btn btn-mint btn-keep" data-keep>${icons.play({ size: 26 })}Keep Playing</button>
    <button class="btn-exit" data-exit>Exit</button>
  </div>`, { closable: true });
  const node = o.node;
  node.classList.add('overlay-exit');
  onTap($('[data-keep]', node), () => { sfx.pop(); closeOverlay(o); });
  onTap($('[data-exit]', node), async () => {
    sfx.bye();
    say('Bye bye! See you soon!');
    node.classList.add('leaving');
    $('.exit-title', node).textContent = 'Bye bye!';
    $('.exit-sub', node).textContent = 'See you soon!';
    const z = $('.exit-zip', node);
    z.innerHTML = zipSVG({ size: 130, gold: store.goldenZip(), cls: 'exit-fly' });
    await wait(1400);
    stopMusic();
    if (Capacitor.isNativePlatform()) {
      suspendAudio();
      App.exitApp();
    } else {
      $('.exit-sub', node).textContent = '(The app closes here on a phone.)';
      node.classList.remove('leaving');
      $('[data-exit]', node).hidden = true;
    }
  });
}

const PALETTE = ['#FF5D73', '#FF8A3D', '#FFC93C', '#8BD346', '#3EE0B0', '#5CC8FF', '#2F7BFF', '#9B6BFF', '#FFB3C7', '#A0663A', '#F4F6FF', '#3B4A7A'];

export function colorOverlay(pic, onPick) {
  const current = store.data.colors[pic.id] || pic.fill;
  const o = openOverlay(`<div class="card color-card">
    <h2>Color it!</h2>
    <div class="color-pic">${pictureIcon(pic, { size: 220, fill: current })}</div>
    <div class="palette">${[pic.fill, ...PALETTE.filter((c) => c !== pic.fill)].map((c) => `<button class="swatch ${c === current ? 'sel' : ''}" style="background:${c}" data-c="${c}" aria-label="Color ${c}"></button>`).join('')}</div>
    <button class="btn btn-sun" data-done>${icons.check()}Done</button>
  </div>`);
  const node = o.node;
  let chosen = current;
  $$('.swatch', node).forEach((s) => onTap(s, () => {
    chosen = s.dataset.c;
    $$('.swatch', node).forEach((x) => x.classList.toggle('sel', x === s));
    $('.color-pic .pic-body', node).setAttribute('fill', chosen);
    sfx.dot(Math.floor(Math.random() * 10));
  }));
  onTap($('[data-done]', node), () => {
    const first = store.setColor(pic.id, chosen);
    if (first) {
      store.addBonus(COLOR_IT_STARS);
      sfx.fanfare();
      say('Beautiful! Here is a bonus star!');
    } else sfx.pop();
    onPick(chosen);
    closeOverlay(o);
  });
}

export function stickerOverlay(pic) {
  const o = openOverlay(`<div class="card sticker-card">
    <div class="sticker-big">${pictureIcon(pic, { size: 230, fill: store.data.colors[pic.id] })}</div>
    <h2>${pic.name}</h2>
    <div class="sticker-actions">
      <button class="round-btn" data-voice aria-label="Hear it">${icons.speak()}</button>
      <button class="round-btn coral" data-paint aria-label="Color it in">${icons.paint()}</button>
      <button class="btn btn-sun" data-close>OK</button>
    </div>
  </div>`);
  const node = o.node;
  const play = () => {
    $('.sticker-big .pic', node).classList.remove('alive');
    void node.offsetWidth;
    $('.sticker-big .pic', node).classList.add('alive');
    sfx.voice(pic.voice);
    setTimeout(() => say(`It's ${article(pic.name)} ${pic.name}!`), 700);
  };
  play();
  onTap($('[data-voice]', node), play);
  onTap($('[data-paint]', node), () => colorOverlay(pic, (c) => $('.sticker-big .pic-body', node).setAttribute('fill', c)));
  onTap($('[data-close]', node), () => closeOverlay(o));
  return o;
}
