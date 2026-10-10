// Tiny screen router + overlay stack + back-button handling.
import { $, el } from './ui.js';
import { playMusic, stopMusic } from './audio.js';
import { store } from './store.js';

const routes = {};
export const app = { screen: null, params: {}, cleanup: null, overlays: [], musicWorld: null };

export function register(name, def) {
  routes[name] = def;
}

export function go(name, params = {}) {
  closeAllOverlays();
  if (app.cleanup) app.cleanup();
  const root = $('#screen');
  root.className = `screen screen-${name}`;
  root.innerHTML = routes[name].render(params);
  app.screen = name;
  app.params = params;
  app.cleanup = routes[name].mount(root, params) || null;
}

// Music follows the screen: null = silence.
export function setMusic(world) {
  app.musicWorld = world;
  if (world === null || !store.data.music) stopMusic();
  else playMusic(world);
}

export function openOverlay(html, { closable = true, onClose } = {}) {
  const node = el(`<div class="overlay">${html}</div>`);
  $('#overlays').appendChild(node);
  requestAnimationFrame(() => node.classList.add('show'));
  const entry = { node, closable, onClose };
  app.overlays.push(entry);
  node.addEventListener('click', (ev) => {
    if (ev.target === node && closable) closeOverlay(entry);
  });
  return entry;
}

export function closeOverlay(entry = app.overlays[app.overlays.length - 1]) {
  if (!entry) return;
  app.overlays = app.overlays.filter((o) => o !== entry);
  entry.node.classList.remove('show');
  setTimeout(() => entry.node.remove(), 220);
  if (entry.onClose) entry.onClose();
}

function closeAllOverlays() {
  for (const o of [...app.overlays]) {
    app.overlays = app.overlays.filter((x) => x !== o);
    o.node.remove();
  }
}

// Android back button (and Escape in a browser).
export function handleBack() {
  if (document.querySelector('.ad-standin')) return;
  const top = app.overlays[app.overlays.length - 1];
  if (top) {
    if (top.closable) closeOverlay(top);
    return;
  }
  const r = routes[app.screen];
  if (r && r.back) r.back(app.params);
}
