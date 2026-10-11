// Robot builder, "workshop-real" style: painted & chipped metal, rivets, panel seams,
// camera-lens eyes with metal eyelids, LED mouths, hydraulic arms, real treads/tyres/thrusters.
// viewBox 0 -6 200 268, ground at y≈252. Every robot in the game is assembled from these parts.
//
// Options: head binocular|dome|crt|cyclops|bucket · mood happy|wow|focus|wink · body box|barrel|sphere
//          arms piston|bellows · hand claw|mitt · legs treads|wheels|legs|spider|hover|rocket
//          antenna ball|double|dish|prop|beacon|none · pose down|wave|cheer · color/accent/eye
//          only/hide/ghost (part lists) · sil (silhouette colour) · viewBox · style · cls · shadow
const K = '#1A1D2B';

function shade(hex, amt) {
  const n = parseInt(hex.slice(1), 16);
  let r = n >> 16, g = (n >> 8) & 255, b = n & 255;
  const t = amt < 0 ? 0 : 255, p = Math.abs(amt);
  r = Math.round((t - r) * p + r); g = Math.round((t - g) * p + g); b = Math.round((t - b) * p + b);
  return '#' + ((1 << 24) | (r << 16) | (g << 8) | b).toString(16).slice(1);
}
const f1 = n => +n.toFixed(1);

let rid = 0;
function robot(o = {}) {
  const id = 'rr' + rid++;
  const c = o.color || '#E8692A', a = o.accent || '#F2B705', e = o.eye || '#4FE3FF';
  const mood = o.mood || 'happy', pose = o.pose || 'down';
  const hide = o.hide || [], ghost = o.ghost || [], only = o.only;
  const show = n => !hide.includes(n) && (!only || only.includes(n));
  const U = s => `url(#${id}${s})`;
  const P = U('p'), PC = U('pc'), S = U('s'), SC = U('sc'), A = U('a'), RV = U('rv'), AO = U('ao'), HZ = U('hz'), GL = U('g');
  const STEEL = '#B9C0CC';

  const defs = `<defs>
    <linearGradient id="${id}p" x1="0" y1="0" x2=".25" y2="1"><stop offset="0" stop-color="${shade(c, 0.5)}"/><stop offset=".1" stop-color="${shade(c, 0.25)}"/><stop offset=".5" stop-color="${c}"/><stop offset="1" stop-color="${shade(c, -0.42)}"/></linearGradient>
    <linearGradient id="${id}pc" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="${shade(c, -0.45)}"/><stop offset=".2" stop-color="${shade(c, 0.35)}"/><stop offset=".4" stop-color="${c}"/><stop offset=".82" stop-color="${shade(c, -0.22)}"/><stop offset="1" stop-color="${shade(c, -0.55)}"/></linearGradient>
    <linearGradient id="${id}a" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${shade(a, 0.45)}"/><stop offset=".5" stop-color="${a}"/><stop offset="1" stop-color="${shade(a, -0.35)}"/></linearGradient>
    <linearGradient id="${id}s" x1="0" y1="0" x2=".2" y2="1"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".15" stop-color="#DDE2EA"/><stop offset=".55" stop-color="#A7AFBD"/><stop offset="1" stop-color="#5E6576"/></linearGradient>
    <linearGradient id="${id}sc" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#4E5463"/><stop offset=".22" stop-color="#F2F4F8"/><stop offset=".45" stop-color="#B9C0CC"/><stop offset=".8" stop-color="#7D8596"/><stop offset="1" stop-color="#474C5A"/></linearGradient>
    <linearGradient id="${id}r" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#4A4E5C"/><stop offset="1" stop-color="#1B1D25"/></linearGradient>
    <radialGradient id="${id}rv" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".35" stop-color="#C9CED8"/><stop offset="1" stop-color="#4E5463"/></radialGradient>
    <linearGradient id="${id}ao" x1="0" y1="0" x2="0" y2="1"><stop offset=".5" stop-color="#2A1A08" stop-opacity="0"/><stop offset="1" stop-color="#2A1A08" stop-opacity=".38"/></linearGradient>
    <pattern id="${id}hz" width="10" height="10" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><rect width="10" height="10" fill="#F2B705"/><rect width="5" height="10" fill="${K}"/></pattern>
    <pattern id="${id}scan" width="4" height="3" patternUnits="userSpaceOnUse"><rect width="4" height="1.2" fill="#000" opacity=".28"/></pattern>
    <radialGradient id="${id}g"><stop offset="0" stop-color="${e}" stop-opacity=".75"/><stop offset="1" stop-color="${e}" stop-opacity="0"/></radialGradient>
    <radialGradient id="${id}l" cx=".42" cy=".38" r=".7"><stop offset="0" stop-color="#2A5378"/><stop offset=".5" stop-color="#0D1A2A"/><stop offset="1" stop-color="#020409"/></radialGradient>
    <radialGradient id="${id}crt" cx=".45" cy=".4" r=".75"><stop offset="0" stop-color="${shade(e, -0.55)}"/><stop offset="1" stop-color="#04080B"/></radialGradient>
    <radialGradient id="${id}core" cx=".45" cy=".42" r=".6"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".3" stop-color="${e}"/><stop offset="1" stop-color="${shade(e, -0.65)}"/></radialGradient>
    <radialGradient id="${id}bulb" cx=".38" cy=".32" r=".7"><stop offset="0" stop-color="#FFE0E0"/><stop offset=".35" stop-color="#FF4040"/><stop offset="1" stop-color="#8A0D16"/></radialGradient>
    <radialGradient id="${id}rg"><stop offset="0" stop-color="#FF4040" stop-opacity=".6"/><stop offset="1" stop-color="#FF4040" stop-opacity="0"/></radialGradient>
    <radialGradient id="${id}bc" cx=".4" cy=".35" r=".75"><stop offset="0" stop-color="#FFF1C2"/><stop offset=".35" stop-color="#FFA21F"/><stop offset="1" stop-color="#B34A00"/></radialGradient>
    <radialGradient id="${id}og"><stop offset="0" stop-color="#FFA21F" stop-opacity=".55"/><stop offset="1" stop-color="#FFA21F" stop-opacity="0"/></radialGradient>
    <radialGradient id="${id}blur"><stop offset="0" stop-color="#fff" stop-opacity=".55"/><stop offset=".8" stop-color="#E6EAF2" stop-opacity=".35"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
    <linearGradient id="${id}fl" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFF7D1"/><stop offset=".3" stop-color="#FFC23D"/><stop offset=".7" stop-color="#FF6A1A"/><stop offset="1" stop-color="#FF5000" stop-opacity="0"/></linearGradient>
    <linearGradient id="${id}th" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFFFFF" stop-opacity=".95"/><stop offset=".3" stop-color="${e}" stop-opacity=".8"/><stop offset="1" stop-color="${e}" stop-opacity="0"/></linearGradient>
    <linearGradient id="${id}heat" x1="0" y1="0" x2="0" y2="1"><stop offset=".4" stop-color="#FF8A1F" stop-opacity="0"/><stop offset=".75" stop-color="#FF8A1F" stop-opacity=".45"/><stop offset="1" stop-color="#5A6BFF" stop-opacity=".5"/></linearGradient>
  </defs>`;

  // ---------- detail helpers
  const rivet = (x, y, r = 2.4) => `<circle cx="${f1(x)}" cy="${f1(y)}" r="${r}" fill="${RV}" stroke="${K}" stroke-width=".8"/>`;
  const seam = d => `<path d="${d}" fill="none" stroke="#000" stroke-opacity=".38" stroke-width="1.4"/><path d="${d}" transform="translate(.6 1.1)" fill="none" stroke="#fff" stroke-opacity=".3" stroke-width="1"/>`;
  const vent = (x, y, w, n, g = 5) => Array.from({ length: n }, (_, i) => `<rect x="${x}" y="${y + i * g}" width="${w}" height="2.8" rx="1.4" fill="#12141B" stroke="none"/><path d="M${x + 1} ${y + i * g + 3.7} H${x + w - 1}" stroke="#fff" stroke-opacity=".3" stroke-width=".9"/>`).join('');
  const scratch = (x, y, l, ang) => `<path d="M${x} ${y} l${f1(l * Math.cos(ang))} ${f1(l * Math.sin(ang))}" stroke="#fff" stroke-opacity=".5" stroke-width=".9" stroke-linecap="round"/>`;
  const chip = (x, y, s = 1) => `<path d="M${x} ${y} l${3 * s} ${-1.5 * s} l${2 * s} ${2 * s} l${-1 * s} ${2.5 * s} l${-3 * s} ${0.5 * s}Z" fill="#C7CDD6" stroke="#000" stroke-opacity=".35" stroke-width=".7"/>`;
  const tube = (d, w, col) => `<path d="${d}" fill="none" stroke="${K}" stroke-width="${w + 4}"/><path d="${d}" fill="none" stroke="${col}" stroke-width="${w}"/>
     <path d="${d}" fill="none" stroke="#000" stroke-opacity=".22" stroke-width="${f1(w * 0.35)}" transform="translate(${f1(w * 0.25)} ${f1(w * 0.2)})"/>
     <path d="${d}" fill="none" stroke="#fff" stroke-opacity=".6" stroke-width="${f1(w * 0.2)}" transform="translate(${f1(-w * 0.24)} ${f1(-w * 0.2)})"/>`;
  const glowDot = (x, y, r, col) => `<circle cx="${x}" cy="${y}" r="${r * 2.6}" fill="${col}" opacity=".25" stroke="none"/><circle cx="${x}" cy="${y}" r="${r}" fill="${col}" stroke="${K}" stroke-width="1"/><circle cx="${x - r * 0.3}" cy="${y - r * 0.3}" r="${r * 0.35}" fill="#fff" opacity=".8" stroke="none"/>`;

  // camera-lens eye with an eyelid plate for expression
  let eyeN = 0;
  const lens = (cx, cy, r, lid = 'none') => {
    const k = `${id}e${eyeN++}`;
    let s = `<clipPath id="${k}"><circle cx="${cx}" cy="${cy}" r="${r + 1}"/></clipPath>
      <circle cx="${cx}" cy="${cy}" r="${r + 4.5}" fill="${RV}" stroke="${K}" stroke-width="2.2"/>
      <circle cx="${cx}" cy="${cy}" r="${r + 1}" fill="#07090E" stroke="none"/>
      <circle cx="${cx}" cy="${cy}" r="${f1(r * 0.88)}" fill="${U('l')}" stroke="none"/>
      <circle cx="${cx}" cy="${cy}" r="${f1(r * 0.62)}" fill="${GL}" stroke="none" opacity=".55"/>
      <circle cx="${cx}" cy="${cy}" r="${f1(r * 0.52)}" fill="none" stroke="${e}" stroke-width="${f1(r * 0.15)}"/>
      <circle cx="${cx}" cy="${cy}" r="${f1(r * 0.26)}" fill="#000" stroke="none"/>
      <ellipse cx="${f1(cx - r * 0.38)}" cy="${f1(cy - r * 0.4)}" rx="${f1(r * 0.3)}" ry="${f1(r * 0.17)}" transform="rotate(-35 ${f1(cx - r * 0.38)} ${f1(cy - r * 0.4)})" fill="#fff" opacity=".9" stroke="none"/>
      <circle cx="${f1(cx + r * 0.35)}" cy="${f1(cy + r * 0.32)}" r="${f1(r * 0.09)}" fill="#fff" opacity=".7" stroke="none"/>`;
    const L = cx - r - 3, R = cx + r + 3;
    if (lid === 'happy') s += `<g clip-path="url(#${k})"><path d="M${L} ${f1(cy + r * 0.62)} Q${cx} ${f1(cy + r * 0.02)} ${R} ${f1(cy + r * 0.62)} V${cy + r + 3} H${L}Z" fill="${P}" stroke="${K}" stroke-width="2"/></g>`;
    if (lid === 'focus') s += `<g clip-path="url(#${k})"><path d="M${L} ${cy - r - 3} H${R} V${f1(cy - r * 0.2)} L${L} ${f1(cy - r * 0.5)}Z" fill="${P}" stroke="${K}" stroke-width="2"/></g>`;
    if (lid === 'closed') s += `<g clip-path="url(#${k})"><rect x="${L}" y="${cy - r - 3}" width="${2 * r + 6}" height="${2 * r + 6}" fill="${P}" stroke="none"/></g>
      <path d="M${f1(cx - r * 0.7)} ${cy} Q${cx} ${f1(cy + r * 0.55)} ${f1(cx + r * 0.7)} ${cy}" fill="none" stroke="${K}" stroke-width="2.6"/>`;
    return s;
  };
  const lids = { happy: ['happy', 'happy'], wow: ['none', 'none'], focus: ['focus', 'focus'], wink: ['closed', 'happy'] }[mood];

  // LED-grid mouth
  const ledMouth = (cx, y, cols = 9) => {
    const w = cols * 5.2 + 3, x = cx - w / 2, c0 = (cols - 1) / 2;
    let s = `<rect x="${f1(x)}" y="${y}" width="${f1(w)}" height="17" rx="4" fill="#11141B" stroke="${K}" stroke-width="2"/>`;
    for (let i = 0; i < cols; i++) for (let r = 0; r < 3; r++) {
      const t = Math.abs(i - c0) / c0, d = Math.abs(i - c0);
      const lit = mood === 'happy' ? r === (t > 0.7 ? 0 : t > 0.3 ? 1 : 2)
        : mood === 'wow' ? d <= 1 && (r !== 1 || d === 1)
        : mood === 'focus' ? r === 1 && t < 0.6
        : r === (t > 0.6 ? 1 : 2) && t < 0.85;
      const cx2 = f1(x + 2 + i * 5.2), cy2 = f1(y + 2 + r * 4.6);
      s += lit ? `<rect x="${cx2 - 1}" y="${cy2 - 1}" width="6.2" height="5.6" rx="2" fill="${e}" opacity=".35" stroke="none"/><rect x="${cx2}" y="${cy2}" width="4.2" height="3.6" rx="1" fill="${e}" stroke="none"/>`
        : `<rect x="${cx2}" y="${cy2}" width="4.2" height="3.6" rx="1" fill="#252A36" stroke="none"/>`;
    }
    return s;
  };

  // LED eyes behind a visor (dome head)
  const ledEye = (cx, cy, kind) => {
    const g = (d, w) => `<path d="${d}" fill="none" stroke="${e}" stroke-opacity=".3" stroke-width="${w + 7}" stroke-linecap="round"/><path d="${d}" fill="none" stroke="${e}" stroke-width="${w}" stroke-linecap="round"/>`;
    if (kind === 'happy') return g(`M${cx - 9} ${cy + 4} Q${cx} ${cy - 9} ${cx + 9} ${cy + 4}`, 4.5);
    if (kind === 'focus') return g(`M${cx - 8} ${cy} H${cx + 8}`, 6);
    if (kind === 'closed') return g(`M${cx - 8} ${cy - 2} Q${cx} ${cy + 6} ${cx + 8} ${cy - 2}`, 4);
    return `<circle cx="${cx}" cy="${cy}" r="13" fill="${e}" opacity=".25" stroke="none"/><circle cx="${cx}" cy="${cy}" r="7.5" fill="${e}" stroke="none"/><circle cx="${cx - 2.5}" cy="${cy - 2.5}" r="2.4" fill="#fff" stroke="none"/>`;
  };

  // pixel art face on a CRT screen
  const px = (x0, y0, cells, sz = 4) => cells.map(([i, j]) => `<rect x="${x0 + i * sz}" y="${y0 + j * sz}" width="${sz - 0.6}" height="${sz - 0.6}" fill="${e}" stroke="none"/>`).join('');
  const pxEye = (x0, y0, kind) => ({
    happy: [[0, 2], [1, 1], [2, 0], [3, 1], [4, 2]],
    wow: [[1, 0], [2, 0], [3, 0], [0, 1], [1, 1], [2, 1], [3, 1], [4, 1], [0, 2], [1, 2], [2, 2], [3, 2], [4, 2], [1, 3], [2, 3], [3, 3]],
    focus: [[0, 1], [1, 1], [2, 1], [3, 1], [4, 1]],
    closed: [[0, 1], [1, 2], [2, 2], [3, 2], [4, 1]]
  }[kind]);

  // ---------- HEADS  (each: base silhouette, full art, antenna anchor)
  const lidOf = i => lids[i];
  const heads = {
    binocular: () => {
      const housing = (cx, cy, w, h, rot) => `<rect x="${cx - w / 2}" y="${cy - h / 2}" width="${w}" height="${h}" rx="18" fill="${P}" transform="rotate(${rot} ${cx} ${cy})"/>`;
      const base = `<rect x="93" y="96" width="14" height="40" rx="4" fill="${SC}"/><rect x="88" y="58" width="24" height="18" rx="6" fill="${SC}"/>` + housing(69, 66, 58, 52, -7) + housing(131, 66, 60, 56, 7);
      const full = `<rect x="93" y="96" width="14" height="40" rx="4" fill="${SC}"/>${seam('M94 106 H106 M94 116 H106 M94 126 H106')}
        <rect x="88" y="58" width="24" height="18" rx="6" fill="${SC}"/>
        <g transform="rotate(-7 69 66)"><rect x="40" y="40" width="58" height="52" rx="18" fill="${P}"/><rect x="40" y="40" width="58" height="52" rx="18" fill="${AO}" stroke="none"/>
          <rect x="47" y="38" width="44" height="8" rx="4" fill="${A}" stroke-width="1.6"/>${rivet(46, 84)}${rivet(92, 84)}${lens(69, 68, 15, lidOf(0))}${chip(43, 60)}</g>
        <g transform="rotate(7 131 66)"><rect x="101" y="38" width="60" height="56" rx="18" fill="${P}"/><rect x="101" y="38" width="60" height="56" rx="18" fill="${AO}" stroke="none"/>
          <rect x="108" y="36" width="46" height="8" rx="4" fill="${A}" stroke-width="1.6"/>${rivet(107, 87)}${rivet(155, 87)}${lens(131, 67, 18, lidOf(1))}${scratch(150, 52, 7, 1.2)}</g>
        ${glowDot(100, 67, 2.4, '#FF4D4D')}`;
      return { base, full, ant: [131, 36] };
    },
    dome: () => {
      const shell = 'M44 126 V86 C44 52 70 34 100 34 C130 34 156 52 156 86 V126 Q156 132 150 132 H50 Q44 132 44 126Z';
      const base = `<rect x="32" y="76" width="16" height="36" rx="6" fill="${SC}"/><rect x="152" y="76" width="16" height="36" rx="6" fill="${SC}"/><path d="${shell}" fill="${P}"/>`;
      const full = `<rect x="32" y="76" width="16" height="36" rx="6" fill="${SC}"/><rect x="152" y="76" width="16" height="36" rx="6" fill="${SC}"/>
        ${seam('M35 86 H45 M35 94 H45 M35 102 H45 M155 86 H165 M155 94 H165 M155 102 H165')}
        <path d="${shell}" fill="${P}"/><path d="${shell}" fill="${AO}" stroke="none"/>
        ${seam('M100 36 V58 M48 118 H152')}
        <path d="M54 68 Q100 56 146 68 V96 Q100 106 54 96Z" fill="#0C1424" stroke="${K}" stroke-width="2.6"/>
        ${ledEye(80, 82, lidOf(0))}${ledEye(120, 82, lidOf(1))}
        <path d="M60 70 Q100 60 140 70 L136 76 Q100 67 64 77Z" fill="#fff" opacity=".22" stroke="none"/>
        ${ledMouth(100, 106, 9)}${rivet(52, 125)}${rivet(148, 125)}${rivet(60, 50)}${rivet(140, 50)}${chip(128, 40)}${scratch(64, 60, 8, -0.6)}`;
      return { base, full, ant: [100, 36] };
    },
    crt: () => {
      const base = `<rect x="38" y="38" width="124" height="94" rx="14" fill="${P}"/>`;
      const eyes = { happy: ['happy', 'happy'], wow: ['wow', 'wow'], focus: ['focus', 'focus'], wink: ['closed', 'happy'] }[mood];
      const mouthPx = { happy: [[0, 0], [1, 1], [2, 1], [3, 1], [4, 1], [5, 0]], wow: [[1, 0], [2, 0], [0, 1], [3, 1], [1, 2], [2, 2]], focus: [[0, 1], [1, 1], [2, 1], [3, 1]], wink: [[0, 0], [1, 1], [2, 1], [3, 1], [4, 0]] }[mood];
      const face = px(64, 66, pxEye(0, 0, eyes[0])) + px(98, 66, pxEye(0, 0, eyes[1])) + px(mood === 'wow' ? 85 : 80, 94, mouthPx);
      const full = `${base}<rect x="38" y="38" width="124" height="94" rx="14" fill="${AO}" stroke="none"/>${vent(124, 42, 30, 2, 4)}
        <rect x="47" y="47" width="88" height="74" rx="16" fill="#2A2E38" stroke="${K}" stroke-width="2.4"/>
        <rect x="53" y="53" width="76" height="62" rx="13" fill="${U('crt')}" stroke="none"/>
        <g opacity=".45" transform="translate(-1 -1) scale(1)">${face.replace(/fill="[^"]+"/g, `fill="${e}"`)}</g>
        <g style="filter:drop-shadow(0 0 3px ${e})">${face}</g>
        <rect x="53" y="53" width="76" height="62" rx="13" fill="${U('scan')}" stroke="none"/>
        <path d="M58 62 Q59 57 66 57 H102 Q74 62 62 92Z" fill="#fff" opacity=".2" stroke="none"/>
        <circle cx="148" cy="66" r="7" fill="${RV}" stroke="${K}" stroke-width="1.8"/><path d="M148 66 L152 61" stroke="${K}" stroke-width="2"/>
        <circle cx="148" cy="86" r="5" fill="${RV}" stroke="${K}" stroke-width="1.6"/>
        ${[0, 1, 2].map(r => [0, 1, 2].map(q => `<circle cx="${143 + q * 5}" cy="${102 + r * 6}" r="1.5" fill="#12141B" stroke="none"/>`).join('')).join('')}
        ${rivet(44, 44, 2)}${rivet(156, 44, 2)}${rivet(44, 126, 2)}${rivet(156, 126, 2)}${chip(150, 120)}`;
      return { base, full, ant: [100, 38] };
    },
    cyclops: () => {
      const shell = 'M52 130 L44 76 Q44 40 100 38 Q156 40 156 76 L148 130Z';
      const base = `<rect x="34" y="70" width="14" height="28" rx="5" fill="${SC}"/><rect x="152" y="70" width="14" height="28" rx="5" fill="${SC}"/><path d="${shell}" fill="${P}"/>`;
      const full = `${base.replace(`<path d="${shell}" fill="${P}"/>`, '')}<path d="${shell}" fill="${P}"/><path d="${shell}" fill="${AO}" stroke="none"/>
        <path d="M50 116 H150 L148 130 H52Z" fill="${HZ}" stroke="${K}" stroke-width="2"/>
        ${seam('M56 104 H144 M100 40 V54')}${lens(100, 80, 22, lidOf(1))}${vent(86, 106, 28, 1, 4)}
        ${glowDot(66, 56, 3, '#FF4D4D')}${glowDot(134, 56, 3, '#3FE07A')}${rivet(54, 92)}${rivet(146, 92)}${chip(140, 70)}${scratch(58, 70, 9, 1.3)}`;
      return { base, full, ant: [100, 38] };
    },
    bucket: () => {
      const base = `<rect x="46" y="40" width="108" height="92" rx="10" fill="${PC}"/><ellipse cx="100" cy="42" rx="54" ry="8" fill="${S}"/>`;
      const full = `${base}<ellipse cx="100" cy="42" rx="46" ry="4.6" fill="#2A2E38" stroke="none"/>
        <rect x="46" y="56" width="108" height="7" fill="${SC}" stroke="${K}" stroke-width="1.6"/><rect x="46" y="120" width="108" height="7" fill="${SC}" stroke="${K}" stroke-width="1.6"/>
        ${[56, 78, 100, 122, 144].map(x => rivet(x, 59.5, 1.8) + rivet(x, 123.5, 1.8)).join('')}
        ${lens(78, 86, 13, lidOf(0))}${lens(122, 85, 15, lidOf(1))}${ledMouth(100, 102, 7)}${scratch(60, 70, 10, 0.9)}${chip(140, 108)}`;
      return { base, full, ant: [100, 36] };
    }
  };

  // ---------- BODIES
  const bodies = {
    box: () => {
      const base = `<rect x="48" y="128" width="104" height="84" rx="14" fill="${P}"/>`;
      const ticks = Array.from({ length: 7 }, (_, i) => { const t = (-210 + i * 40) * Math.PI / 180; return `<path d="M${f1(80 + 8 * Math.cos(t))} ${f1(158 + 8 * Math.sin(t))} L${f1(80 + 10 * Math.cos(t))} ${f1(158 + 10 * Math.sin(t))}" stroke="${K}" stroke-width="1"/>`; }).join('');
      const full = `${base}<rect x="48" y="128" width="104" height="84" rx="14" fill="${AO}" stroke="none"/>
        <path d="M50 198 H150 V200 Q150 210 140 210 H60 Q50 210 50 200Z" fill="${HZ}" stroke="none"/><path d="M50 197.5 H150" stroke="${K}" stroke-width="2"/>
        <rect x="60" y="137" width="80" height="54" rx="8" fill="none" stroke="#000" stroke-opacity=".35" stroke-width="1.5"/><rect x="61" y="138.5" width="80" height="54" rx="8" fill="none" stroke="#fff" stroke-opacity=".25" stroke-width="1"/>
        <circle cx="80" cy="158" r="12" fill="#F3EFE4" stroke="${K}" stroke-width="2.2"/>${ticks}
        <path d="M80 158 L${f1(80 + 9 * Math.cos(-0.7))} ${f1(158 + 9 * Math.sin(-0.7))}" stroke="#E5484D" stroke-width="1.8"/><circle cx="80" cy="158" r="1.8" fill="${K}" stroke="none"/>
        <path d="M71 152 Q74 148 79 147" stroke="#fff" stroke-width="1.4" fill="none" opacity=".8"/>
        ${glowDot(106, 151, 3.4, '#E5484D').replace(/opacity="\.25"/, 'opacity="0"')}${glowDot(118, 151, 3.4, '#3FCF6E').replace(/opacity="\.25"/, 'opacity="0"')}${glowDot(130, 151, 3.4, '#3D8BFF').replace(/opacity="\.25"/, 'opacity="0"')}
        <rect x="102" y="160" width="32" height="10" rx="2" fill="#0D1F1A" stroke="${K}" stroke-width="1.5"/><rect x="105" y="163" width="7" height="4" fill="${e}" stroke="none"/><rect x="114" y="163" width="11" height="4" fill="${e}" opacity=".7" stroke="none"/><rect x="127" y="163" width="4" height="4" fill="${e}" opacity=".45" stroke="none"/>
        ${vent(104, 176, 30, 3, 4.5)}<text x="64" y="186" font-family="Lilita" font-size="11" fill="#000" fill-opacity=".38" stroke="none">B-07</text>
        ${rivet(55, 135)}${rivet(145, 135)}${rivet(55, 191)}${rivet(145, 191)}${chip(138, 132)}${chip(51, 170, 0.8)}${scratch(64, 146, 10, -0.3)}${scratch(120, 186, 8, 0.4)}`;
      return { base, full, sh: [[48, 146], [152, 146]] };
    },
    barrel: () => {
      const base = `<rect x="50" y="126" width="100" height="88" rx="12" fill="${PC}"/>`;
      const bolts = Array.from({ length: 8 }, (_, i) => { const t = i * Math.PI / 4; return rivet(100 + 17 * Math.cos(t), 170 + 17 * Math.sin(t), 1.6); }).join('');
      const full = `${base}<rect x="50" y="126" width="100" height="88" rx="12" fill="${AO}" stroke="none"/>
        <rect x="50" y="138" width="100" height="7" fill="${SC}" stroke="${K}" stroke-width="1.6"/><rect x="50" y="196" width="100" height="7" fill="${SC}" stroke="${K}" stroke-width="1.6"/>
        ${[60, 80, 120, 140].map(x => rivet(x, 141.5, 1.6) + rivet(x, 199.5, 1.6)).join('')}
        <circle cx="100" cy="170" r="30" fill="${GL}" stroke="none" opacity=".55"/>
        <circle cx="100" cy="170" r="21" fill="${RV}" stroke="${K}" stroke-width="2.4"/>${bolts}
        <circle cx="100" cy="170" r="13.5" fill="${U('core')}" stroke="${K}" stroke-width="2"/>
        <ellipse cx="95" cy="164" rx="6" ry="3.2" transform="rotate(-30 95 164)" fill="#fff" opacity=".75" stroke="none"/>
        ${seam('M66 150 V190 M134 150 V190')}${chip(140, 184)}${scratch(58, 160, 12, 1.4)}`;
      return { base, full, sh: [[50, 148], [150, 148]] };
    },
    sphere: () => {
      const base = `<circle cx="100" cy="168" r="46" fill="${P}"/>`;
      const full = `${base}<circle cx="100" cy="168" r="46" fill="${AO}" stroke="none"/>
        ${seam('M55 172 Q100 190 145 172')}${[66, 84, 100, 116, 134].map((x, i) => rivet(x, [178, 183, 184.5, 183, 178][i], 1.7)).join('')}
        <circle cx="100" cy="150" r="22" fill="${GL}" stroke="none" opacity=".5"/>
        <circle cx="100" cy="150" r="14" fill="#11141B" stroke="${K}" stroke-width="2.2"/><circle cx="100" cy="150" r="9" fill="none" stroke="${e}" stroke-width="3.5"/><circle cx="100" cy="150" r="3" fill="${e}" stroke="none"/>
        ${vent(84, 194, 32, 2, 4.5)}<path d="M70 132 Q84 124 100 123" stroke="#fff" stroke-width="3" fill="none" opacity=".45" stroke-linecap="round"/>${chip(136, 150)}${scratch(66, 156, 9, 1.1)}`;
      return { base, full, sh: [[56, 150], [144, 150]] };
    }
  };

  const B = bodies[o.body || 'box']();
  const H = heads[o.head || 'binocular']();

  // ---------- ARMS
  const [SL, SR] = B.sh;
  const armPts = side => {
    const s = side < 0 ? SL : SR, up = pose === 'cheer' || (pose === 'wave' && side > 0);
    return up ? [s, [s[0] + side * 26, s[1] - 20], [s[0] + side * 30, s[1] - 52]] : [s, [s[0] + side * 18, s[1] + 30], [s[0] + side * 14, s[1] + 58]];
  };
  const gripper = (h, ang, kind) => `<g transform="translate(${f1(h[0])} ${f1(h[1])}) rotate(${f1(ang)})">` + (kind === 'mitt'
    ? `<rect x="-9" y="-4" width="18" height="15" rx="6" fill="${P}" stroke="${K}" stroke-width="2"/>${[-7, -1.5, 4].map(x => `<rect x="${x}" y="9" width="5" height="9" rx="2.5" fill="${S}" stroke="${K}" stroke-width="1.6"/>`).join('')}`
    : `<rect x="-7" y="-6" width="14" height="10" rx="3" fill="${SC}" stroke="${K}" stroke-width="2"/>
       <path d="M-6 3 Q-15 12 -9 24 Q-7 27 -4 24 Q-8 15 -1 6Z" fill="${S}" stroke="${K}" stroke-width="2"/><path d="M6 3 Q15 12 9 24 Q7 27 4 24 Q8 15 1 6Z" fill="${S}" stroke="${K}" stroke-width="2"/>
       <circle cx="-6.5" cy="25" r="2.6" fill="#2A2E38" stroke="${K}" stroke-width="1"/><circle cx="6.5" cy="25" r="2.6" fill="#2A2E38" stroke="${K}" stroke-width="1"/>`) + `</g>`;
  const arm = side => {
    const [s, el, h] = armPts(side), dx = h[0] - el[0], dy = h[1] - el[1], len = Math.hypot(dx, dy);
    const ang = Math.atan2(dy, dx) * 180 / Math.PI - 90, ux = dx / len, uy = dy / len, px2 = -uy * 6 * side, py2 = ux * 6 * side;
    const hand = gripper(h, ang, o.hand || (o.arms === 'bellows' ? 'mitt' : 'claw'));
    if (o.arms === 'bellows') {
      let rings = '';
      for (let i = 0; i <= 10; i++) {
        const t = i / 10, x = (1 - t) ** 2 * s[0] + 2 * (1 - t) * t * el[0] + t * t * h[0], y = (1 - t) ** 2 * s[1] + 2 * (1 - t) * t * el[1] + t * t * h[1];
        const tx = 2 * (1 - t) * (el[0] - s[0]) + 2 * t * (h[0] - el[0]), ty = 2 * (1 - t) * (el[1] - s[1]) + 2 * t * (h[1] - el[1]);
        rings += `<ellipse cx="${f1(x)}" cy="${f1(y)}" rx="3.6" ry="8.5" transform="rotate(${f1(Math.atan2(ty, tx) * 180 / Math.PI)} ${f1(x)} ${f1(y)})" fill="${i % 2 ? '#AEB6C3' : '#D5DAE3'}" stroke="${K}" stroke-width="1.3"/>`;
      }
      return `<path d="M${s} Q${el} ${h}" fill="none" stroke="#3A3F4C" stroke-width="10"/>${rings}${hand}${`<circle cx="${s[0]}" cy="${s[1]}" r="10" fill="${RV}" stroke="${K}" stroke-width="2"/><circle cx="${s[0]}" cy="${s[1]}" r="4.5" fill="#5E6576" stroke="none"/>`}`;
    }
    const a1 = [el[0] + px2 + ux * 4, el[1] + py2 + uy * 4], a2 = [el[0] + px2 + ux * len * 0.42, el[1] + py2 + uy * len * 0.42], a3 = [el[0] + px2 + ux * len * 0.78, el[1] + py2 + uy * len * 0.78];
    return tube(`M${s} L${el}`, 13, shade(c, -0.05)) + tube(`M${el} L${h}`, 9, STEEL) +
      tube(`M${a1.map(f1)} L${a3.map(f1)}`, 2.4, '#EEF1F6') + tube(`M${a1.map(f1)} L${a2.map(f1)}`, 4.6, '#5E6576') +
      `<circle cx="${el[0]}" cy="${el[1]}" r="7.5" fill="${RV}" stroke="${K}" stroke-width="2"/><circle cx="${el[0]}" cy="${el[1]}" r="2.6" fill="#4E5463" stroke="none"/>` + hand +
      `<circle cx="${s[0]}" cy="${s[1]}" r="10.5" fill="${RV}" stroke="${K}" stroke-width="2"/><circle cx="${s[0]}" cy="${s[1]}" r="5" fill="${A}" stroke="${K}" stroke-width="1.4"/>`;
  };
  const armBase = side => { const [s, el, h] = armPts(side); return `<path class="ln" d="M${s} L${el} L${h}" fill="none" stroke-width="12"/><circle cx="${h[0]}" cy="${h[1]}" r="8"/><circle cx="${s[0]}" cy="${s[1]}" r="10"/>`; };
  const ARMS = { base: armBase(-1) + armBase(1), full: arm(-1) + arm(1) };

  // ---------- LOCOMOTION
  const legsLib = {
    treads: () => ({
      base: `<rect x="88" y="204" width="24" height="12" fill="${SC}"/><rect x="38" y="212" width="124" height="40" rx="20" fill="${U('r')}"/>`,
      full: `<rect x="88" y="204" width="24" height="12" fill="${SC}"/><rect x="38" y="212" width="124" height="40" rx="20" fill="${U('r')}"/>
        <rect x="42" y="215.5" width="116" height="33" rx="16.5" fill="none" stroke="#5A5F6E" stroke-width="3.2" stroke-dasharray="3 4" stroke-linecap="butt"/>
        <rect x="52" y="219" width="96" height="26" rx="13" fill="${P}"/><rect x="52" y="219" width="96" height="26" rx="13" fill="${AO}" stroke="none"/>
        ${[[64, 232, 10], [136, 232, 10], [89, 234, 6.5], [111, 234, 6.5]].map(([x, y, r]) => `<circle cx="${x}" cy="${y}" r="${r}" fill="${RV}" stroke="${K}" stroke-width="1.8"/><circle cx="${x}" cy="${y}" r="${f1(r * 0.32)}" fill="#3A3F4C" stroke="none"/>`).join('')}
        ${[0, 1, 2, 3].map(i => { const t = i * Math.PI / 2 + 0.6; return `<circle cx="${f1(64 + 6 * Math.cos(t))}" cy="${f1(232 + 6 * Math.sin(t))}" r="1" fill="#3A3F4C" stroke="none"/><circle cx="${f1(136 + 6 * Math.cos(t))}" cy="${f1(232 + 6 * Math.sin(t))}" r="1" fill="#3A3F4C" stroke="none"/>`; }).join('')}
        <path d="M46 246 Q70 252 96 249" stroke="#6B4A2A" stroke-width="3" opacity=".45" fill="none"/>`
    }),
    wheels: () => {
      const tyre = x => `<circle cx="${x}" cy="231" r="21" fill="${U('r')}"/><circle cx="${x}" cy="231" r="18.2" fill="none" stroke="#5A5F6E" stroke-width="3" stroke-dasharray="3 3.2"/>
        <circle cx="${x}" cy="231" r="12" fill="${RV}" stroke="${K}" stroke-width="1.8"/><circle cx="${x}" cy="231" r="5" fill="${A}" stroke="${K}" stroke-width="1.5"/>
        ${[0, 1, 2, 3, 4].map(i => { const t = i * 2 * Math.PI / 5; return `<circle cx="${f1(x + 8.5 * Math.cos(t))}" cy="${f1(231 + 8.5 * Math.sin(t))}" r="1.3" fill="#3A3F4C" stroke="none"/>`; }).join('')}
        <path d="M${x - 15} 220 Q${x - 8} 212 ${x + 2} 211" stroke="#fff" stroke-opacity=".3" stroke-width="2" fill="none"/>`;
      return {
        base: `<rect x="70" y="204" width="60" height="18" rx="8" fill="${P}"/><circle cx="64" cy="231" r="21"/><circle cx="136" cy="231" r="21"/>`,
        full: `<rect x="70" y="204" width="60" height="18" rx="8" fill="${P}"/>${vent(86, 209, 28, 2, 4.5)}${tyre(64)}${tyre(136)}`
      };
    },
    legs: () => {
      const leg = x => `<rect x="${x - 5}" y="206" width="10" height="24" rx="3" fill="${SC}"/>${tube(`M${x + 9} 212 L${x + 9} 236`, 2.2, '#EEF1F6')}
        <circle cx="${x}" cy="208" r="7" fill="${RV}" stroke="${K}" stroke-width="1.8"/><rect x="${x - 9}" y="226" width="18" height="18" rx="5" fill="${P}"/>${rivet(x, 233, 1.8)}
        <path d="M${x - 17} 251 Q${x - 17} 241 ${x - 8} 241 H${x + 11} Q${x + 19} 241 ${x + 19} 249 V251Z" fill="${P}"/><rect x="${x - 18}" y="248" width="38" height="5" rx="2" fill="#1F222B"/>`;
      return {
        base: `<rect x="71" y="206" width="10" height="38" rx="3"/><rect x="119" y="206" width="10" height="38" rx="3"/><path d="M59 251 V241 H95 V251Z"/><path d="M107 251 V241 H143 V251Z"/>`,
        full: leg(76) + leg(124)
      };
    },
    spider: () => {
      const leg = (hip, knee, foot, w1 = 8) => tube(`M${hip} L${knee}`, w1, shade(c, -0.05)) + tube(`M${knee} L${foot}`, 5.5, STEEL) +
        `<circle cx="${knee[0]}" cy="${knee[1]}" r="5" fill="${RV}" stroke="${K}" stroke-width="1.6"/><path d="M${foot[0] - 4} ${foot[1] - 4} L${foot[0]} ${foot[1] + 2} L${foot[0] + 4} ${foot[1] - 4}Z" fill="#2A2E38" stroke="${K}" stroke-width="1.4"/>`;
      return {
        base: `<path class="ln" d="M72 210 L34 196 L22 248 M128 210 L166 196 L178 248 M86 214 L62 224 L58 250 M114 214 L138 224 L142 250" fill="none" stroke-width="8"/><ellipse cx="100" cy="212" rx="34" ry="10"/>`,
        full: leg([72, 210], [34, 196], [22, 248]) + leg([128, 210], [166, 196], [178, 248]) +
          `<ellipse cx="100" cy="212" rx="34" ry="10" fill="${SC}"/>${rivet(78, 212, 1.8)}${rivet(122, 212, 1.8)}` +
          leg([86, 214], [62, 224], [58, 250], 7) + leg([114, 214], [138, 224], [142, 250], 7)
      };
    },
    hover: () => ({
      base: `<path d="M56 206 H144 L136 226 Q100 234 64 226Z"/>`,
      full: `<ellipse cx="100" cy="248" rx="58" ry="12" fill="${GL}" stroke="none" opacity=".9"/><path d="M76 226 Q100 268 124 226Z" fill="${U('th')}" stroke="none"/>
        <path d="M56 206 H144 L136 226 Q100 234 64 226Z" fill="${P}"/>${vent(78, 211, 44, 2, 5)}
        <ellipse cx="100" cy="227" rx="34" ry="6.5" fill="#2A2E38" stroke="${K}" stroke-width="2"/><ellipse cx="100" cy="227.5" rx="26" ry="4" fill="${e}" stroke="none"/><ellipse cx="100" cy="227.5" rx="14" ry="2" fill="#fff" stroke="none"/>
        ${rivet(62, 211, 1.8)}${rivet(138, 211, 1.8)}`
    }),
    rocket: () => {
      const jet = x => `<path d="M${x - 9} 234 Q${x - 13} 254 ${x} 270 Q${x + 13} 254 ${x + 9} 234Z" fill="${U('fl')}" stroke="none"/><path d="M${x - 4} 234 Q${x - 5} 246 ${x} 254 Q${x + 5} 246 ${x + 4} 234Z" fill="#FFFBEA" stroke="none"/>
        <path d="M${x - 12} 204 H${x + 12} L${x + 9} 232 H${x - 9}Z" fill="${SC}"/><path d="M${x - 12} 204 H${x + 12} L${x + 9} 232 H${x - 9}Z" fill="${U('heat')}" stroke="none"/><rect x="${x - 10.5}" y="228" width="21" height="6" rx="2" fill="#5E6576"/>`;
      return { base: `<path d="M66 204 H90 L87 232 H69Z"/><path d="M110 204 H134 L131 232 H113Z"/>`, full: jet(78) + jet(122) };
    }
  };
  const LEGS = legsLib[o.legs || 'treads']();

  // ---------- TOPPERS (drawn around x=100,y=40 then moved to the head's anchor)
  const antLib = {
    ball: `<rect x="98.5" y="16" width="3" height="24" fill="${SC}" stroke="${K}" stroke-width="1.3"/><rect x="95.5" y="26" width="9" height="4" rx="1.5" fill="#5E6576" stroke="${K}" stroke-width="1.2"/><rect x="92" y="35" width="16" height="6" rx="2" fill="${SC}" stroke="${K}" stroke-width="1.6"/>
      <circle cx="100" cy="12" r="13" fill="${U('rg')}" stroke="none"/><circle cx="100" cy="12" r="6" fill="${U('bulb')}" stroke="${K}" stroke-width="1.6"/><circle cx="98" cy="10" r="1.6" fill="#fff" stroke="none"/>`,
    double: tube('M90 40 L74 12', 2.5, '#D5DAE3') + tube('M110 40 L126 12', 2.5, '#D5DAE3') +
      `<circle cx="74" cy="11" r="4.5" fill="${RV}" stroke="${K}" stroke-width="1.5"/><circle cx="126" cy="11" r="4.5" fill="${RV}" stroke="${K}" stroke-width="1.5"/><rect x="84" y="35" width="32" height="6" rx="2" fill="${SC}" stroke="${K}" stroke-width="1.6"/>`,
    dish: `<rect x="97.5" y="20" width="5" height="20" fill="${SC}" stroke="${K}" stroke-width="1.4"/><ellipse cx="100" cy="17" rx="21" ry="8" transform="rotate(-16 100 17)" fill="${U('s')}" stroke="${K}" stroke-width="2"/>
      <ellipse cx="100" cy="16" rx="15" ry="4.6" transform="rotate(-16 100 16)" fill="#8E96A6" stroke="none"/><path d="M100 16 L108 2" stroke="${K}" stroke-width="1.6"/><circle cx="108" cy="2" r="2.6" fill="${A}" stroke="${K}" stroke-width="1.2"/>`,
    prop: `<rect x="97.5" y="18" width="5" height="22" fill="${SC}" stroke="${K}" stroke-width="1.4"/><ellipse cx="100" cy="15" rx="46" ry="6.5" fill="${U('blur')}" stroke="none"/>
      <path d="M100 15 L58 12.5 Q55 15 58 17.5Z" fill="${A}" opacity=".7" stroke="none"/><path d="M100 15 L142 17.5 Q145 15 142 12.5Z" fill="${A}" opacity=".45" stroke="none"/><circle cx="100" cy="15" r="5" fill="${RV}" stroke="${K}" stroke-width="1.6"/>`,
    beacon: `<circle cx="100" cy="22" r="20" fill="${U('og')}" stroke="none"/><rect x="88" y="32" width="24" height="9" rx="2" fill="${SC}" stroke="${K}" stroke-width="1.6"/>
      <path d="M91 32 V22 Q91 11 100 11 Q109 11 109 22 V32Z" fill="${U('bc')}" stroke="${K}" stroke-width="1.8"/><path d="M95 13 V32 M100 11 V32 M105 13 V32" stroke="${K}" stroke-opacity=".5" stroke-width="1"/><path d="M94 18 Q95 14 98 13" stroke="#fff" stroke-width="1.6" fill="none"/>`,
    none: ''
  };
  const antName = o.antenna === 'bolt' ? 'beacon' : (o.antenna || 'ball');
  const antArt = antLib[antName] || '';
  const ANT = { base: antArt ? `<g transform="translate(${H.ant[0] - 100} ${H.ant[1] - 40})"><rect x="96" y="14" width="8" height="26"/><circle cx="100" cy="12" r="7"/></g>` : '', full: `<g transform="translate(${H.ant[0] - 100} ${H.ant[1] - 40})">${antArt}</g>` };

  const parts = { antenna: ANT, legs: LEGS, arms: ARMS, body: B, head: H };
  const render = n => {
    if (!show(n)) return '';
    const p = parts[n];
    if (o.sil) return `<g class="sil">${p.base}</g>`;
    if (ghost.includes(n)) return `<g class="ghost">${p.base}</g>`;
    return p.full;
  };
  const vb = o.viewBox || '0 -6 200 268';
  const shadow = o.shadow === false ? '' : `<ellipse cx="100" cy="254" rx="66" ry="8" fill="#000" opacity=".22"/>`;
  return `<svg viewBox="${vb}" class="${o.cls || ''}" style="${o.sil ? `--sil:${o.sil};` : ''}${o.style || ''}" xmlns="http://www.w3.org/2000/svg">${defs}${shadow}
    <g stroke="${K}" stroke-width="2.2" stroke-linejoin="round" stroke-linecap="round">${render('antenna')}${render('legs')}${render('body')}${render('arms')}${render('head')}</g></svg>`;
}

// The robot cast used across the mockups (real paint colours: safety yellow, rust orange, teal, enamel white...)
const CAST = {
  sparky: { head: 'binocular', body: 'box', legs: 'legs', color: '#E8692A', accent: '#F2B705', antenna: 'ball' },
  nimbus: { head: 'dome', body: 'sphere', legs: 'hover', color: '#E9EDF2', accent: '#3D8BFF', arms: 'bellows', antenna: 'dish' },
  telly: { head: 'crt', body: 'box', legs: 'treads', color: '#2BA3A0', accent: '#F2B705', antenna: 'double' },
  rex: { head: 'cyclops', body: 'barrel', legs: 'spider', color: '#D9433B', accent: '#F2B705', antenna: 'beacon', eye: '#FF9A3C', mood: 'focus' },
  canny: { head: 'bucket', body: 'barrel', legs: 'wheels', color: '#3567B5', accent: '#E8692A', antenna: 'prop', mood: 'wink' },
  zippy: { head: 'binocular', body: 'sphere', legs: 'rocket', color: '#7C4DDB', accent: '#4FE3FF', arms: 'bellows' },
  bolt: { head: 'binocular', body: 'barrel', legs: 'wheels', color: '#2BA3A0', accent: '#F2B705', antenna: 'ball' }
};

module.exports = { robot, CAST };
