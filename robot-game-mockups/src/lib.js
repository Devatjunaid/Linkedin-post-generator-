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

let rid = 0;
// Robot builder. viewBox 0 0 200 260. Options pick parts so the same kit draws every robot in the game.
function robot(o = {}) {
  const id = 'rb' + rid++;
  const c = o.color || '#38B6FF', a = o.accent || '#FFC531', m = o.metal || '#C3CCE0', e = o.eye || '#5CF2FF';
  const hide = o.hide || [], ghost = o.ghost || [], only = o.only;
  const show = n => !hide.includes(n) && (!only || only.includes(n));
  const part = (n, s) => (show(n) ? (ghost.includes(n) ? `<g class="ghost">${s}</g>` : s) : '');
  const B = `url(#${id}b)`, M = `url(#${id}m)`, A = `url(#${id}a)`;
  const defs = `<defs>
    <linearGradient id="${id}b" x1="0" y1="0" x2="0.3" y2="1"><stop offset="0" stop-color="${shade(c, 0.4)}"/><stop offset=".5" stop-color="${c}"/><stop offset="1" stop-color="${shade(c, -0.3)}"/></linearGradient>
    <linearGradient id="${id}m" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${shade(m, 0.5)}"/><stop offset="1" stop-color="${shade(m, -0.25)}"/></linearGradient>
    <linearGradient id="${id}a" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${shade(a, 0.45)}"/><stop offset="1" stop-color="${shade(a, -0.2)}"/></linearGradient>
    <radialGradient id="${id}s" cx=".4" cy=".3" r=".9"><stop offset="0" stop-color="#34407A"/><stop offset="1" stop-color="#11152E"/></radialGradient>
    <radialGradient id="${id}g"><stop offset="0" stop-color="${e}" stop-opacity=".7"/><stop offset="1" stop-color="${e}" stop-opacity="0"/></radialGradient>
  </defs>`;

  // ---- antenna
  const ant = {
    ball: `<path d="M100 40 V16" stroke="${OL}" stroke-width="9"/><path d="M100 40 V16" stroke="${M}" stroke-width="4"/>
           <circle cx="100" cy="11" r="16" fill="url(#${id}g)" stroke="none"/><circle cx="100" cy="11" r="9" fill="${A}"/>`,
    double: `<path d="M80 42 L66 14 M120 42 L134 14" stroke="${OL}" stroke-width="9"/><path d="M80 42 L66 14 M120 42 L134 14" stroke="${M}" stroke-width="4"/>
           <circle cx="66" cy="11" r="8" fill="${A}"/><circle cx="134" cy="11" r="8" fill="${A}"/>`,
    bolt: `<path d="M104 4 L86 30 H100 L94 46 L116 18 H102 Z" fill="${A}"/>`,
    prop: `<path d="M100 40 V22" stroke="${OL}" stroke-width="9"/><path d="M100 40 V22" stroke="${M}" stroke-width="4"/>
           <path d="M100 18 C80 6 62 10 60 18 C62 26 80 28 100 18 C120 8 138 10 140 18 C138 26 120 30 100 18Z" fill="${A}"/><circle cx="100" cy="18" r="5" fill="${M}"/>`,
    none: ''
  }[o.antenna || 'ball'];

  // ---- legs
  const legs = {
    legs: `<rect x="68" y="204" width="20" height="34" rx="8" fill="${M}"/><rect x="112" y="204" width="20" height="34" rx="8" fill="${M}"/>
           <path d="M54 252 Q54 232 78 232 Q100 232 98 252Z" fill="${B}"/><path d="M146 252 Q146 232 122 232 Q100 232 102 252Z" fill="${B}"/>`,
    wheel: `<rect x="88" y="204" width="24" height="20" fill="${M}"/><circle cx="100" cy="228" r="25" fill="#2D2A4A"/>
           <circle cx="100" cy="228" r="12" fill="${A}"/><path d="M100 206 V216 M100 240 V250 M78 228 H88 M112 228 H122" stroke="#5B5880" stroke-width="4"/>`,
    treads: `<rect x="86" y="204" width="28" height="14" fill="${M}"/><rect x="46" y="212" width="108" height="40" rx="20" fill="#34305A"/>
           <circle cx="68" cy="232" r="10" fill="${M}"/><circle cx="100" cy="232" r="10" fill="${M}"/><circle cx="132" cy="232" r="10" fill="${M}"/>`,
    rocket: `<path d="M64 204 H92 L88 236 H68Z" fill="${M}"/><path d="M108 204 H136 L132 236 H112Z" fill="${M}"/>
           <path d="M70 238 Q78 268 86 238Z M114 238 Q122 268 130 238Z" fill="#FF8A1F" stroke="none"/>
           <path d="M74 238 Q78 256 82 238Z M118 238 Q122 256 126 238Z" fill="#FFE45C" stroke="none"/>`
  }[o.legs || 'legs'];

  // ---- arms (outline stroke then metal stroke gives a chunky tube)
  const tube = d => `<path d="${d}" stroke="${OL}" stroke-width="17" fill="none"/><path d="${d}" stroke="${M}" stroke-width="9" fill="none"/>`;
  const hand = (x, y) => (o.arms === 'claw'
    ? `<path d="M${x - 12} ${y - 2} Q${x - 16} ${y + 16} ${x - 2} ${y + 18} M${x + 12} ${y - 2} Q${x + 16} ${y + 16} ${x + 2} ${y + 18}" stroke="${OL}" stroke-width="10" fill="none"/>
       <path d="M${x - 12} ${y - 2} Q${x - 16} ${y + 16} ${x - 2} ${y + 18} M${x + 12} ${y - 2} Q${x + 16} ${y + 16} ${x + 2} ${y + 18}" stroke="${A}" stroke-width="4" fill="none"/><circle cx="${x}" cy="${y}" r="9" fill="${M}"/>`
    : `<circle cx="${x}" cy="${y}" r="13" fill="${B}"/>`);
  let L = 'M50 150 C26 160 20 182 26 204', LH = [26, 208];
  let R = 'M150 150 C174 160 180 182 174 204', RH = [174, 208];
  if (o.wave || o.cheer) { R = 'M150 150 C178 140 186 116 176 92'; RH = [176, 86]; }
  if (o.cheer) { L = 'M50 150 C22 140 14 116 24 92'; LH = [24, 86]; }
  const arms = tube(L) + hand(...LH) + tube(R) + hand(...RH) +
    `<circle cx="50" cy="150" r="10" fill="${A}"/><circle cx="150" cy="150" r="10" fill="${A}"/>`;

  // ---- body
  const bodyShape = {
    box: `<rect x="50" y="132" width="100" height="80" rx="24" fill="${B}"/>`,
    barrel: `<rect x="46" y="130" width="108" height="84" rx="42" fill="${B}"/>`,
    tri: `<path d="M64 132 H136 Q146 132 148 142 L158 200 Q160 212 148 212 H52 Q40 212 42 200 L52 142 Q54 132 64 132Z" fill="${B}"/>`
  }[o.body || 'box'];
  const chest = o.chest === 'bolt'
    ? `<circle cx="100" cy="172" r="18" fill="#141A33"/><path d="M103 158 L92 174 H100 L96 186 L109 168 H101Z" fill="${e}" stroke="none"/>`
    : `<circle cx="100" cy="172" r="18" fill="#141A33"/><path d="M100 182 C84 172 88 160 95 162 C98 163 100 166 100 166 C100 166 102 163 105 162 C112 160 116 172 100 182Z" fill="#FF5C8A" stroke="none"/>`;
  const body = bodyShape + chest +
    `<circle cx="70" cy="196" r="5" fill="#FF5C5C"/><circle cx="130" cy="196" r="5" fill="#7CF25A"/>
     <ellipse cx="74" cy="144" rx="14" ry="5" fill="#fff" opacity=".45" stroke="none" transform="rotate(-15 74 144)"/>`;

  // ---- head
  const headShape = {
    round: `<rect x="36" y="36" width="128" height="96" rx="46" fill="${B}"/>`,
    square: `<rect x="38" y="36" width="124" height="94" rx="18" fill="${B}"/>`,
    dome: `<path d="M38 130 V92 A62 58 0 0 1 162 92 V130 Q162 134 156 134 H44 Q38 134 38 130Z" fill="${B}"/>`
  }[o.head || 'round'];
  const ears = `<rect x="22" y="72" width="18" height="30" rx="8" fill="${A}"/><rect x="160" y="72" width="18" height="30" rx="8" fill="${A}"/>`;
  const head = ears + headShape +
    `<ellipse cx="66" cy="50" rx="16" ry="6" fill="#fff" opacity=".55" stroke="none" transform="rotate(-18 66 50)"/>`;

  // ---- face (screen + eyes + mouth)
  const eye = (x, r = 13) => `<circle cx="${x}" cy="84" r="${r + 9}" fill="url(#${id}g)" stroke="none"/><circle cx="${x}" cy="84" r="${r}" fill="${e}" stroke="none"/>
      <circle cx="${x - r * 0.35}" cy="${84 - r * 0.35}" r="${r * 0.38}" fill="#fff" stroke="none"/>`;
  const happy = x => `<path d="M${x - 12} 88 Q${x} 72 ${x + 12} 88" stroke="${e}" stroke-width="6" fill="none"/>`;
  const eyes = {
    big: eye(78) + eye(122),
    happy: happy(78) + happy(122),
    wink: happy(78) + eye(122),
    visor: `<rect x="62" y="72" width="76" height="24" rx="12" fill="${e}" stroke="none"/><rect x="70" y="76" width="26" height="6" rx="3" fill="#fff" opacity=".8" stroke="none"/>`,
    cyclops: eye(100, 19),
    hearts: [78, 122].map(x => `<path d="M${x} 94 C${x - 16} 84 ${x - 12} 72 ${x - 5} 74 C${x - 2} 75 ${x} 78 ${x} 78 C${x} 78 ${x + 2} 75 ${x + 5} 74 C${x + 12} 72 ${x + 16} 84 ${x} 94Z" fill="#FF6FB5" stroke="none"/>`).join('')
  }[o.eyes || 'big'];
  const mouth = {
    smile: `<path d="M86 102 Q100 114 114 102" stroke="${e}" stroke-width="5" fill="none"/>`,
    open: `<path d="M84 100 H116 Q114 116 100 116 Q86 116 84 100Z" fill="${e}" stroke="none"/><path d="M90 109 Q100 104 110 109 Q106 115 100 115 Q94 115 90 109Z" fill="#FF6F91" stroke="none"/>`,
    o: `<ellipse cx="100" cy="106" rx="7" ry="8" fill="${e}" stroke="none"/>`,
    grin: `<rect x="82" y="100" width="36" height="12" rx="6" fill="${e}" stroke="none"/><path d="M94 100 V112 M106 100 V112" stroke="#141A33" stroke-width="3"/>`
  }[o.mouth || 'smile'];
  const face = `<rect x="54" y="54" width="92" height="64" rx="22" fill="url(#${id}s)"/>` + eyes + mouth +
    `<circle cx="64" cy="108" r="6" fill="#FF7EB6" opacity=".7" stroke="none"/><circle cx="136" cy="108" r="6" fill="#FF7EB6" opacity=".7" stroke="none"/>`;

  const neck = `<rect x="86" y="124" width="28" height="14" rx="4" fill="${M}"/>`;
  const vb = o.viewBox || '0 -6 200 268';
  const shadow = o.shadow === false ? '' : `<ellipse cx="100" cy="254" rx="62" ry="9" fill="#000" opacity=".22"/>`;
  return `<svg viewBox="${vb}" class="${o.cls || ''}" style="${o.style || ''}" xmlns="http://www.w3.org/2000/svg">${defs}${shadow}
    <g stroke="${OL}" stroke-width="4" stroke-linejoin="round" stroke-linecap="round">
      ${part('antenna', ant)}${part('legs', legs)}${part('arms', arms)}${part('body', body)}${part('body', neck)}${part('head', head)}${show('face') && !ghost.includes('head') ? part('head', face) : ''}
    </g></svg>`;
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
.ghost *{fill:rgba(255,255,255,.14)!important;stroke:rgba(255,255,255,.95)!important;stroke-dasharray:8 7}
.glow{filter:drop-shadow(0 0 10px rgba(255,255,255,.9)) drop-shadow(0 0 22px rgba(120,240,255,.8))}
.panel{position:absolute;background:#FFF6E3;border:5px solid ${OL};border-radius:38px;box-shadow:inset 0 -10px 0 #F1DDB6,0 10px 0 ${OL},0 24px 40px rgba(0,0,0,.35);color:${OL}}
`;

module.exports = { OL, shade, starPath, gearPath, robot, I, sparkle, confetti, rnd, css };
