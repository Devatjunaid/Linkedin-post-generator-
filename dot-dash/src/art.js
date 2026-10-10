// Shared artwork: Zip the firefly, stars, icons and the DOT DASH! logo.
import { INK } from './pictures.js';

export function zipSVG({ size = 120, gold = false, cls = '', mood = 'happy' } = {}) {
  const body = gold ? '#FFD34D' : '#7B5CFF';
  const thorax = gold ? '#FFB020' : '#FF5D73';
  const tail = gold ? '#FFF3B0' : '#FFE14D';
  const mouth = mood === 'sad'
    ? '<path d="M53 48 Q60 43 67 48" fill="none" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round"/>'
    : '<path d="M53 45 Q60 51 67 45" fill="none" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round"/>';
  const brows = mood === 'sad'
    ? '<path d="M46 25 L56 28 M74 25 L64 28" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>' : '';
  return `<svg class="zip ${cls}" viewBox="0 0 120 120" width="${size}" height="${size}" aria-hidden="true">
    <g class="zip-wings">
      <ellipse cx="34" cy="50" rx="20" ry="11" fill="#FFFFFF" opacity=".8" transform="rotate(-30 34 50)"/>
      <ellipse cx="86" cy="50" rx="20" ry="11" fill="#FFFFFF" opacity=".8" transform="rotate(30 86 50)"/>
    </g>
    <ellipse cx="60" cy="84" rx="20" ry="19" fill="${tail}"/>
    <ellipse cx="60" cy="60" rx="21" ry="16" fill="${thorax}"/>
    <path d="M50 20 Q44 8 36 8 M70 20 Q76 8 84 8" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>
    <circle cx="36" cy="8" r="4.5" fill="#FFC93C"/><circle cx="84" cy="8" r="4.5" fill="#FFC93C"/>
    <circle cx="60" cy="36" r="20" fill="${body}"/>
    <circle cx="52" cy="34" r="7" fill="#FFFFFF"/><circle cx="68" cy="34" r="7" fill="#FFFFFF"/>
    <circle cx="53" cy="35" r="3.5" fill="${INK}"/><circle cx="69" cy="35" r="3.5" fill="${INK}"/>
    <circle cx="54.5" cy="33.5" r="1.2" fill="#FFFFFF"/><circle cx="70.5" cy="33.5" r="1.2" fill="#FFFFFF"/>
    ${brows}${mouth}
    ${gold ? '<path d="M48 18 L52 10 L56 16 L60 8 L64 16 L68 10 L72 18 Z" fill="#FFC93C" stroke="#E0A800" stroke-width="1.5" stroke-linejoin="round"/>' : ''}
  </svg>`;
}

const STAR_PTS = '12,1 14.94,7.95 22.46,8.6 16.76,13.55 18.47,20.9 12,17 5.53,20.9 7.24,13.55 1.54,8.6 9.06,7.95';
export function starSVG(size = 24, on = true, cls = '') {
  const f = on ? '#FFC93C' : '#4A43A0';
  return `<svg class="star ${cls}" viewBox="0 0 24 24" width="${size}" height="${size}" aria-hidden="true"><polygon points="${STAR_PTS}" fill="${f}" stroke="${f}" stroke-width="2" stroke-linejoin="round"/></svg>`;
}

const ic = (d, { size = 26, stroke = 'currentColor', w = 2.6, fill = 'none' } = {}) =>
  `<svg viewBox="0 0 24 24" width="${size}" height="${size}" fill="${fill}" stroke="${stroke}" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${d}</svg>`;

export const icons = {
  back: (o) => ic('<path d="M15 5l-7 7 7 7"/>', { w: 3, ...o }),
  next: (o) => ic('<path d="M9 5l7 7-7 7"/>', { w: 3.2, ...o }),
  close: (o) => ic('<path d="M6 6l12 12M18 6L6 18"/>', { w: 3, ...o }),
  play: (o) => ic('<path d="M7 4.5v15l12.5-7.5z" fill="currentColor"/>', { w: 2, ...o }),
  sound: (o) => ic('<path d="M4 9h4l5-4v14l-5-4H4z"/><path d="M16.5 8.5a5 5 0 0 1 0 7"/><path d="M19 6a8.5 8.5 0 0 1 0 12"/>', o),
  mute: (o) => ic('<path d="M4 9h4l5-4v14l-5-4H4z"/><path d="M17 9l5 6M22 9l-5 6"/>', o),
  music: (o) => ic('<path d="M9 18V5l11-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="17" cy="16" r="3"/>', o),
  noMusic: (o) => ic('<path d="M9 18V5l11-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="17" cy="16" r="3"/><path d="M3 3l18 18"/>', o),
  hint: (o) => ic('<path d="M9 18h6"/><path d="M10 21h4"/><path d="M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"/>', o),
  speak: (o) => ic('<path d="M4 9h4l5-4v14l-5-4H4z"/><path d="M16.5 8.5a5 5 0 0 1 0 7"/>', o),
  gift: (o) => ic('<rect x="3" y="9" width="18" height="12" rx="2"/><path d="M3 13h18"/><path d="M12 9v12"/><path d="M12 9C10 5 6 5 6.5 7.5S12 9 12 9z"/><path d="M12 9c2-4 6-4 5.5-1.5S12 9 12 9z"/>', o),
  book: (o) => ic('<path d="M4 19.5V5a2 2 0 0 1 2-2h14v16H6.5A2.5 2.5 0 0 0 4 21.5"/><path d="M12 7.5l1.2 2.4 2.6.4-1.9 1.8.5 2.6-2.4-1.3-2.4 1.3.5-2.6-1.9-1.8 2.6-.4z"/>', o),
  lock: (o) => ic('<rect x="5" y="11" width="14" height="10" rx="2.5"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>', o),
  replay: (o) => ic('<path d="M4 12a8 8 0 1 0 2.5-5.8"/><path d="M4 4v5h5"/>', o),
  paint: (o) => ic('<path d="M18.5 3.5l2 2L10 16l-3 1 1-3z"/><path d="M7 17c-2 0-3 1.5-3 3.5 2 0 3.5-1 3.5-3"/>', o),
  map: (o) => ic('<path d="M9 4L3 6v14l6-2 6 2 6-2V4l-6 2z"/><path d="M9 4v14M15 6v14"/>', o),
  home: (o) => ic('<path d="M3 11l9-7 9 7"/><path d="M5 10v10h14V10"/>', o),
  check: (o) => ic('<path d="M5 12.5l4.5 4.5L19 7.5"/>', { w: 3.2, ...o }),
  pause: (o) => ic('<path d="M9 6v12M15 6v12"/>', { w: 3.2, ...o }),
};

// The logo: chunky sticker letters, the "O" is a glowing numbered dot and a
// dotted trail runs underneath to Zip.
export function logoHTML(cls = '') {
  const L = (ch, color, r, i, extra = '') => `<span class="L ${extra}" style="--c:${color};--r:${r}deg;--i:${i}">${ch}</span>`;
  return `<div class="logo ${cls}" role="img" aria-label="Dot Dash!">
    <div class="logo-row">
      ${L('D', '#FFC93C', -6, 0)}
      <span class="L L-dot" style="--i:1;--r:0deg"><svg viewBox="0 0 100 100" aria-hidden="true"><circle class="L-dot-ring" cx="50" cy="50" r="38" fill="none" stroke="#3EE0B0" stroke-width="8"/><circle cx="50" cy="50" r="38" fill="#3EE0B0" stroke="${INK}" stroke-width="9"/><ellipse cx="36" cy="34" rx="11" ry="7" fill="#FFFFFF" opacity=".85" transform="rotate(-35 36 34)"/><circle cx="60" cy="62" r="5" fill="#FFFFFF" opacity=".5"/></svg></span>
      ${L('T', '#FF5D73', 5, 2)}
    </div>
    <div class="logo-row">
      ${L('D', '#5CC8FF', -4, 3)}${L('A', '#FFC93C', 4, 4)}${L('S', '#FF5D73', -5, 5)}${L('H', '#3EE0B0', 3, 6)}${L('!', '#C3A6FF', 8, 7)}
    </div>
    <svg class="logo-trail" viewBox="0 0 300 40" preserveAspectRatio="none" aria-hidden="true">
      <path d="M8 26 C 70 44, 130 4, 190 22 S 260 30, 292 12" fill="none" stroke="#FFC93C" stroke-width="10" stroke-linecap="round" opacity=".25"/>
      <path class="logo-trail-line" d="M8 26 C 70 44, 130 4, 190 22 S 260 30, 292 12" fill="none" stroke="#FFE58A" stroke-width="4" stroke-linecap="round" stroke-dasharray="1 12"/>
    </svg>
  </div>`;
}
