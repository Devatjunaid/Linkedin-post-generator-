// Shared art kit for the Bolt Buddies mockups: colours, robot builder, icons, CSS.
const OL = '#1E1B4B'; // outline navy used on every shape for a cartoon look

function shade(hex, amt) {
  const n = parseInt(hex.slice(1), 16);
  let r = n >> 16, g = (n >> 8) & 255, b = n & 255;
  const t = amt < 0 ? 0 : 255, p = Math.abs(amt);
  r = Math.round((t - r) * p + r); g = Math.round((t - g) * p + g); b = Math.round((t - b) * p + b);
  return '#' + ((1 << 24) | (r << 16) | (g << 8) | b).toString(16).slice(1);
}

function starPath(cx, cy, R, r) {
  let d = '';
  for (let i = 0; i < 10; i++) {
    const a = -Math.PI / 2 + i * Math.PI / 5, rad = i % 2 ? r : R;
    d += (i ? 'L' : 'M') + (cx + rad * Math.cos(a)).toFixed(1) + ' ' + (cy + rad * Math.sin(a)).toFixed(1);
  }
  return d + 'Z';
}

function gearPath(cx, cy, n, R, r) {
  const step = (2 * Math.PI) / n, pts = [];
  for (let i = 0; i < n; i++) {
    const b = i * step;
    [[r, -0.28], [R, -0.16], [R, 0.16], [r, 0.28]].forEach(([rad, f]) => {
      const a = b + f * step;
      pts.push((cx + rad * Math.cos(a)).toFixed(1) + ' ' + (cy + rad * Math.sin(a)).toFixed(1));
    });
  }
  return 'M' + pts.join('L') + 'Z';
}

// ---- icons, all 24x24
const sv = (s, inner, vb = '0 0 24 24') => `<svg width="${s}" height="${s}" viewBox="${vb}" xmlns="http://www.w3.org/2000/svg">${inner}</svg>`;
const I = {
  star: (s = 24, f = '#FFD43B') => sv(s, `<path d="${starPath(12, 12.8, 11, 5.2)}" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><path d="M8 9 L10 8.6" stroke="#fff" stroke-width="1.6" stroke-linecap="round" opacity=".8"/>`),
  nut: (s = 24) => sv(s, `<path d="M12 1.5 L21.5 7 V17 L12 22.5 L2.5 17 V7Z" fill="#FFC531" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><path d="M12 3.5 L19.8 8 L12 12 L4.2 8Z" fill="#FFE27A"/><circle cx="12" cy="12.5" r="4.2" fill="#C98A12" stroke="${OL}" stroke-width="2"/>`),
  crystal: (s = 24) => sv(s, `<path d="M12 1.5 L21 8.5 L12 22.5 L3 8.5Z" fill="#B46BFF" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><path d="M12 1.5 L16 8.5 H8Z" fill="#E3C2FF"/><path d="M8 8.5 L12 22 L16 8.5" fill="none" stroke="#7B2FE0" stroke-width="1.3"/>`),
  gear: (s = 24, f = '#fff') => sv(s, `<path d="${gearPath(12, 12, 8, 11, 8)}" fill="${f}" stroke="${OL}" stroke-width="1.8" stroke-linejoin="round"/><circle cx="12" cy="12" r="3.4" fill="${OL}"/>`),
  lock: (s = 24) => sv(s, `<path d="M7.5 11 V7.5 a4.5 4.5 0 0 1 9 0 V11" fill="none" stroke="${OL}" stroke-width="3.2"/><path d="M7.5 11 V7.5 a4.5 4.5 0 0 1 9 0 V11" fill="none" stroke="#D7DCEB" stroke-width="1.4"/><rect x="4" y="10" width="16" height="12" rx="3.5" fill="#FFC531" stroke="${OL}" stroke-width="2"/><circle cx="12" cy="15" r="1.8" fill="${OL}"/><path d="M12 15 V18.5" stroke="${OL}" stroke-width="2" stroke-linecap="round"/>`),
  music: (s = 24, f = '#fff') => sv(s, `<path d="M9 17.5 V5 L20 2.5 V15" fill="none" stroke="${OL}" stroke-width="4.5" stroke-linejoin="round"/><path d="M9 17.5 V5 L20 2.5 V15" fill="none" stroke="${f}" stroke-width="1.8" stroke-linejoin="round"/><ellipse cx="6.5" cy="18" rx="3.6" ry="3" fill="${f}" stroke="${OL}" stroke-width="2"/><ellipse cx="17.5" cy="15.5" rx="3.6" ry="3" fill="${f}" stroke="${OL}" stroke-width="2"/>`),
  speaker: (s = 24, f = '#fff') => sv(s, `<path d="M3 9 H7 L12.5 4.5 V19.5 L7 15 H3Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><path d="M16 8.5 Q18.5 12 16 15.5 M18.8 6 Q23 12 18.8 18" fill="none" stroke="${OL}" stroke-width="2.4" stroke-linecap="round"/>`),
  vibrate: (s = 24, f = '#fff') => sv(s, `<rect x="7" y="3" width="10" height="18" rx="2.5" fill="${f}" stroke="${OL}" stroke-width="2"/><path d="M3.5 8 L2 10 L3.5 12 L2 14 L3.5 16 M20.5 8 L22 10 L20.5 12 L22 14 L20.5 16" fill="none" stroke="${OL}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>`),
  voice: (s = 24, f = '#fff') => sv(s, `<path d="M4 4 H20 Q22 4 22 6 V15 Q22 17 20 17 H11 L6 21 V17 H4 Q2 17 2 15 V6 Q2 4 4 4Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><circle cx="8" cy="10.5" r="1.5" fill="${OL}"/><circle cx="12" cy="10.5" r="1.5" fill="${OL}"/><circle cx="16" cy="10.5" r="1.5" fill="${OL}"/>`),
  heart: (s = 24, f = '#FF5C8A') => sv(s, `<path d="M12 21 C3 15 1.5 9.5 4.5 6 C7 3.5 10.5 4.5 12 7.5 C13.5 4.5 17 3.5 19.5 6 C22.5 9.5 21 15 12 21Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/>`),
  share: (s = 24, f = '#fff') => sv(s, `<path d="M17 5.5 L7 12 L17 18.5" fill="none" stroke="${OL}" stroke-width="2.6"/><circle cx="17.5" cy="5.5" r="3.5" fill="${f}" stroke="${OL}" stroke-width="2"/><circle cx="6.5" cy="12" r="3.5" fill="${f}" stroke="${OL}" stroke-width="2"/><circle cx="17.5" cy="18.5" r="3.5" fill="${f}" stroke="${OL}" stroke-width="2"/>`),
  home: (s = 24, f = '#fff') => sv(s, `<path d="M3 11.5 L12 3.5 L21 11.5 H18.5 V20.5 H14.5 V15 H9.5 V20.5 H5.5 V11.5Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/>`),
  back: (s = 24, f = '#fff') => sv(s, `<path d="M11 3.5 L2.5 12 L11 20.5 V15 H21 V9 H11Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/>`),
  play: (s = 24, f = '#fff') => sv(s, `<path d="M6 3.5 Q6 2 7.4 2.8 L20.5 11 Q21.8 12 20.5 13 L7.4 21.2 Q6 22 6 20.5Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/>`),
  replay: (s = 24, f = '#fff') => sv(s, `<path d="M19.5 12 A7.5 7.5 0 1 1 15.5 5.4" fill="none" stroke="${OL}" stroke-width="5.4" stroke-linecap="round"/><path d="M19.5 12 A7.5 7.5 0 1 1 15.5 5.4" fill="none" stroke="${f}" stroke-width="2.4" stroke-linecap="round"/><path d="M13 1.5 L20 4.5 L14.5 9.5Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/>`),
  close: (s = 24, f = '#fff') => sv(s, `<path d="M5 5 L19 19 M19 5 L5 19" stroke="${OL}" stroke-width="7" stroke-linecap="round"/><path d="M5 5 L19 19 M19 5 L5 19" stroke="${f}" stroke-width="3.4" stroke-linecap="round"/>`),
  pause: (s = 24, f = '#fff') => sv(s, `<rect x="5" y="4" width="5" height="16" rx="2" fill="${f}" stroke="${OL}" stroke-width="2"/><rect x="14" y="4" width="5" height="16" rx="2" fill="${f}" stroke="${OL}" stroke-width="2"/>`),
  bulb: (s = 24) => sv(s, `<path d="M12 2.5 A7 7 0 0 1 16 15 V17 H8 V15 A7 7 0 0 1 12 2.5Z" fill="#FFE45C" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><rect x="8.5" y="17" width="7" height="4" rx="1.5" fill="#C3CCE0" stroke="${OL}" stroke-width="2"/><path d="M9.5 7 Q10 5 12 4.8" stroke="#fff" stroke-width="1.6" fill="none" stroke-linecap="round"/>`),
  paint: (s = 24) => sv(s, `<path d="M4 9 L13 3 L20 13 L11 19Z" fill="#E8ECF7" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><path d="M4 9 Q2 16 5 19 Q8 21 9 17 L4 9Z" fill="#FF5C8A" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><circle cx="20" cy="19" r="2.6" fill="#FF5C8A" stroke="${OL}" stroke-width="1.6"/>`),
  gift: (s = 24) => sv(s, `<rect x="3" y="9" width="18" height="12" rx="2" fill="#FF5C8A" stroke="${OL}" stroke-width="2"/><rect x="2" y="6.5" width="20" height="4.5" rx="1.6" fill="#FF8FB1" stroke="${OL}" stroke-width="2"/><path d="M12 6.5 V21" stroke="#FFD43B" stroke-width="3.6"/><path d="M12 6.5 C8 1 4 4 7 6.5 M12 6.5 C16 1 20 4 17 6.5" fill="#FFD43B" stroke="${OL}" stroke-width="1.8"/>`),
  chest: (s = 24) => sv(s, `<path d="M3 11 V8 Q3 3.5 12 3.5 Q21 3.5 21 8 V11Z" fill="#C9772B" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><rect x="3" y="11" width="18" height="10" rx="1.5" fill="#E08A3C" stroke="${OL}" stroke-width="2"/><path d="M8 4.5 V21 M16 4.5 V21" stroke="#FFC531" stroke-width="2.6"/><rect x="10" y="9.5" width="4" height="5" rx="1" fill="#FFE27A" stroke="${OL}" stroke-width="1.6"/>`),
  globe: (s = 24, f = '#fff') => sv(s, `<circle cx="12" cy="12" r="9.5" fill="${f}" stroke="${OL}" stroke-width="2"/><path d="M2.5 12 H21.5 M12 2.5 Q6 12 12 21.5 M12 2.5 Q18 12 12 21.5" fill="none" stroke="${OL}" stroke-width="1.8"/>`),
  shield: (s = 24, f = '#fff') => sv(s, `<path d="M12 2.5 L20 5.5 V11 Q20 18 12 21.5 Q4 18 4 11 V5.5Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><path d="M8.5 12 L11 14.5 L15.8 9.5" fill="none" stroke="${OL}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>`),
  pad: (s = 24, f = '#fff') => sv(s, `<path d="M6 7 H18 Q22.5 7 22.5 13 Q22.5 19 19 19 Q17 19 15.5 16 H8.5 Q7 19 5 19 Q1.5 19 1.5 13 Q1.5 7 6 7Z" fill="${f}" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><path d="M7 10.5 V14.5 M5 12.5 H9" stroke="${OL}" stroke-width="2" stroke-linecap="round"/><circle cx="16" cy="11.5" r="1.4" fill="${OL}"/><circle cx="18.5" cy="14" r="1.4" fill="${OL}"/>`),
  hand: (s = 24) => sv(s, `<path d="M9 13 V4.5 Q9 2.5 11 2.5 Q13 2.5 13 4.5 V10 Q15 9 16.5 10.5 Q18.5 10 19.5 12 Q21.5 12 21.5 14.5 V17 Q21.5 22 16 22 H12.5 Q10 22 8.5 20 L4.5 15 Q3.5 13 5.5 12.5 Q7 12.3 9 15Z" fill="#FFE1C7" stroke="${OL}" stroke-width="1.8" stroke-linejoin="round"/>`),
  check: (s = 24, f = '#fff') => sv(s, `<path d="M4 12.5 L9.5 18 L20 6.5" fill="none" stroke="${OL}" stroke-width="6.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M4 12.5 L9.5 18 L20 6.5" fill="none" stroke="${f}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`)
};

// a sparkle (4-point twinkle)
const sparkle = (x, y, s, col = '#fff', rot = 0) =>
  `<svg class="abs" style="left:${x}px;top:${y}px;width:${s}px;height:${s}px;transform:rotate(${rot}deg)" viewBox="0 0 24 24"><path d="M12 0 Q13.5 10.5 24 12 Q13.5 13.5 12 24 Q10.5 13.5 0 12 Q10.5 10.5 12 0Z" fill="${col}"/></svg>`;

// deterministic pseudo random so renders are stable
let seed = 7;
const rnd = () => ((seed = (seed * 16807) % 2147483647) / 2147483647);

function confetti(n, w, h, cols = ['#FFD43B', '#FF5C8A', '#5CF2FF', '#7CF25A', '#B46BFF', '#FF8A1F']) {
  let s = '';
  for (let i = 0; i < n; i++) {
    const x = rnd() * w, y = rnd() * h, c = cols[i % cols.length], r = rnd() * 360, k = rnd();
    s += k < 0.5
      ? `<div class="abs" style="left:${x}px;top:${y}px;width:${8 + k * 8}px;height:${14 + k * 6}px;background:${c};border-radius:3px;transform:rotate(${r}deg)"></div>`
      : `<div class="abs" style="left:${x}px;top:${y}px;width:12px;height:12px;background:${c};border-radius:50%;"></div>`;
  }
  return s;
}

const css = (fontDir) => `
@font-face{font-family:Lilita;src:url(${fontDir}/lilita.ttf)}
@font-face{font-family:Fredoka;font-weight:500;src:url(${fontDir}/fredoka500.ttf)}
@font-face{font-family:Fredoka;font-weight:700;src:url(${fontDir}/fredoka700.ttf)}
*{box-sizing:border-box;margin:0;padding:0}
body{width:540px;height:960px;overflow:hidden;font-family:Fredoka,sans-serif;font-weight:700;color:#fff;background:#222}
.screen{position:relative;width:540px;height:960px;overflow:hidden}
.abs{position:absolute}
.lil{font-family:Lilita,Fredoka,sans-serif;font-weight:400;letter-spacing:.5px}
.ol{text-shadow:0 3px 0 ${OL},3px 0 0 ${OL},-3px 0 0 ${OL},0 -3px 0 ${OL},2px 2px 0 ${OL},-2px 2px 0 ${OL},2px -2px 0 ${OL},-2px -2px 0 ${OL},0 6px 0 ${OL},3px 5px 0 ${OL},-3px 5px 0 ${OL}}
.ol-s{text-shadow:0 2px 0 ${OL},2px 0 0 ${OL},-2px 0 0 ${OL},0 -2px 0 ${OL},1.5px 1.5px 0 ${OL},-1.5px 1.5px 0 ${OL},1.5px -1.5px 0 ${OL},-1.5px -1.5px 0 ${OL},0 4px 0 ${OL}}
.btn{position:absolute;display:flex;align-items:center;justify-content:center;gap:12px;font-family:Lilita;font-weight:400;color:#fff;
  border:4px solid ${OL};border-radius:30px;box-shadow:inset 0 -9px 0 rgba(0,0,0,.18),inset 0 7px 0 rgba(255,255,255,.45),0 8px 0 ${OL},0 14px 20px rgba(0,0,0,.25)}
.green{background:linear-gradient(#8DF56A,#2EC24A)}
.yellow{background:linear-gradient(#FFE66B,#FFB21E)}
.blue{background:linear-gradient(#6FD6FF,#2D86FF)}
.pink{background:linear-gradient(#FF9BD0,#FF4F9A)}
.purple{background:linear-gradient(#C99BFF,#7B45F0)}
.red{background:linear-gradient(#FF8C7A,#F0383F)}
.grey{background:linear-gradient(#D9DEEC,#9AA3BD)}
.orange{background:linear-gradient(#FFC067,#FF7A1F)}
.rbtn{position:absolute;width:68px;height:68px;border-radius:24px;border:4px solid ${OL};display:flex;align-items:center;justify-content:center;
  box-shadow:inset 0 -7px 0 rgba(0,0,0,.18),inset 0 5px 0 rgba(255,255,255,.45),0 6px 0 ${OL}}
.pill{position:absolute;height:40px;border-radius:20px;background:rgba(20,16,60,.55);border:3px solid ${OL};display:flex;align-items:center;
  padding:0 34px 0 40px;font-size:20px;box-shadow:inset 0 3px 0 rgba(255,255,255,.12)}
.pill .ic{position:absolute;left:-10px;top:-8px}
.pill .plus{position:absolute;right:-6px;top:5px;width:28px;height:28px;border-radius:10px;border:3px solid ${OL};background:linear-gradient(#8DF56A,#2EC24A);
  display:flex;align-items:center;justify-content:center;font-size:22px;line-height:1;padding-bottom:3px}
.badge{position:absolute;min-width:30px;height:30px;border-radius:15px;background:#FF3B5C;border:3px solid ${OL};font-size:17px;display:flex;align-items:center;justify-content:center;padding:0 6px}
.ghost *{fill:rgba(255,255,255,.14)!important;stroke:rgba(255,255,255,.95)!important;stroke-dasharray:8 7;stroke-width:2.5px}
.sil *{fill:var(--sil)!important;stroke:var(--sil)!important}
.ghost .ln,.sil .ln{fill:none!important}
.ghost .ln{stroke-width:12px}
.glow{filter:drop-shadow(0 0 10px rgba(255,255,255,.9)) drop-shadow(0 0 22px rgba(120,240,255,.8))}
.panel{position:absolute;background:#FFF6E3;border:5px solid ${OL};border-radius:38px;box-shadow:inset 0 -10px 0 #F1DDB6,0 10px 0 ${OL},0 24px 40px rgba(0,0,0,.35);color:${OL}}
`;

const { robot, CAST } = require('./robot');
module.exports = { OL, shade, starPath, gearPath, robot, CAST, I, sparkle, confetti, rnd, css };
