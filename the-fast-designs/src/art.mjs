// Original, code-generated artwork for The Fast Designs.
// Every portfolio/case-study image is drawn here as SVG — no stock imagery.
import { PROJECTS } from './projects.mjs';

const FONT = `font-family="Helvetica Neue, Helvetica, Arial, sans-serif"`;
const SERIF = `font-family="Georgia, 'Times New Roman', serif"`;

// Deterministic pseudo-random so builds are stable
function rng(seed) {
  let s = 0;
  for (const c of seed) s = (s * 31 + c.charCodeAt(0)) >>> 0;
  return () => ((s = (s * 1664525 + 1013904223) >>> 0) / 4294967296);
}

const rect = (x, y, w, h, fill, rx = 6, extra = '') => `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="${fill}" ${extra}/>`;
const xml = (v) => String(v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
const text = (x, y, str, size, fill, weight = 700, extra = '') =>
  `<text x="${x}" y="${y}" font-size="${size}" font-weight="${weight}" fill="${fill}" ${FONT} ${extra}>${xml(str)}</text>`;

function linePath(r, x, y, w, h, n = 12, trend = 0.6) {
  const pts = [];
  for (let i = 0; i < n; i++) {
    const t = i / (n - 1);
    const v = 0.15 + t * trend + (r() - 0.5) * 0.22;
    pts.push([x + t * w, y + h - Math.max(0.05, Math.min(0.95, v)) * h]);
  }
  let d = `M${pts[0][0].toFixed(1)},${pts[0][1].toFixed(1)}`;
  for (let i = 1; i < pts.length; i++) {
    const [px, py] = pts[i - 1];
    const [cx, cy] = pts[i];
    const mx = (px + cx) / 2;
    d += ` C${mx.toFixed(1)},${py.toFixed(1)} ${mx.toFixed(1)},${cy.toFixed(1)} ${cx.toFixed(1)},${cy.toFixed(1)}`;
  }
  return { d, area: `${d} L${x + w},${y + h} L${x},${y + h} Z`, last: pts[pts.length - 1] };
}

function frame(p, inner, { w = 1200, h = 800 } = {}) {
  const id = p.id;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${w} ${h}" width="${w}" height="${h}" role="img" aria-label="${xml(p.name)} — ${xml(p.type)} design by The Fast Designs">
<defs>
  <linearGradient id="${id}-bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${p.bg2}"/><stop offset="1" stop-color="${p.bg}"/></linearGradient>
  <linearGradient id="${id}-a" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${p.a}"/><stop offset="1" stop-color="${p.b}"/></linearGradient>
  <linearGradient id="${id}-fade" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${p.a}" stop-opacity=".45"/><stop offset="1" stop-color="${p.a}" stop-opacity="0"/></linearGradient>
  <filter id="${id}-blur" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="70"/></filter>
  <filter id="${id}-shadow" x="-20%" y="-20%" width="140%" height="160%"><feDropShadow dx="0" dy="30" stdDeviation="30" flood-color="#000" flood-opacity=".55"/></filter>
  <pattern id="${id}-grid" width="40" height="40" patternUnits="userSpaceOnUse"><path d="M40 0H0V40" fill="none" stroke="#fff" stroke-opacity=".04"/></pattern>
</defs>
<rect width="${w}" height="${h}" fill="url(#${id}-bg)"/>
<rect width="${w}" height="${h}" fill="url(#${id}-grid)"/>
<circle cx="${w * 0.82}" cy="${h * 0.12}" r="220" fill="${p.a}" opacity=".35" filter="url(#${id}-blur)"/>
<circle cx="${w * 0.1}" cy="${h * 0.95}" r="240" fill="${p.b}" opacity=".28" filter="url(#${id}-blur)"/>
${inner}
</svg>`;
}

function browser(p, content, { x = 90, y = 70, w = 1020, h = 660 } = {}) {
  return `<g filter="url(#${p.id}-shadow)">
  ${rect(x, y, w, h, p.surface, 18)}
  ${rect(x, y, w, 44, p.chrome, 18)}${rect(x, y + 26, w, 18, p.chrome, 0)}
  <circle cx="${x + 24}" cy="${y + 22}" r="6" fill="#FF5F57"/><circle cx="${x + 44}" cy="${y + 22}" r="6" fill="#FEBC2E"/><circle cx="${x + 64}" cy="${y + 22}" r="6" fill="#28C840"/>
  ${rect(x + w / 2 - 150, y + 11, 300, 22, '#ffffff', 11, 'fill-opacity=".07"')}
  ${text(x + w / 2, y + 27, p.domain, 12, '#fff', 500, 'text-anchor="middle" fill-opacity=".55"')}
  </g>
  <g clip-path="url(#${p.id}-clip)">${content}</g>
  <clipPath id="${p.id}-clip">${rect(x, y + 44, w, h - 44, '#000', 0)}</clipPath>`;
}

function nav(p, x, y, w, dark = true) {
  const c = dark ? '#fff' : p.ink;
  return `${text(x, y, p.name, 19, c, 800)}
  ${[0, 1, 2, 3].map((i) => rect(x + w - 420 + i * 72, y - 10, 52, 8, c, 4, 'fill-opacity=".35"')).join('')}
  ${rect(x + w - 120, y - 20, 120, 32, `url(#${p.id}-a)`, 16)}${text(x + w - 60, y + 1, 'Get started', 12, p.onA, 700, 'text-anchor="middle"')}`;
}

/* ---------------- Layouts ---------------- */

function saas(p) {
  const r = rng(p.id);
  const lp = linePath(r, 700, 300, 340, 150, 10, 0.65);
  const c = `
  ${nav(p, 140, 160, 920)}
  ${text(140, 262, p.headline[0], 50, '#fff', 800, 'letter-spacing="-1.5"')}
  ${text(140, 320, p.headline[1], 50, p.a, 800, 'letter-spacing="-1.5"')}
  ${rect(140, 352, 380, 10, '#fff', 5, 'fill-opacity=".28"')}${rect(140, 374, 300, 10, '#fff', 5, 'fill-opacity=".18"')}
  ${rect(140, 412, 160, 46, `url(#${p.id}-a)`, 23)}${text(220, 441, 'Start free', 15, p.onA, 700, 'text-anchor="middle"')}
  ${rect(314, 412, 150, 46, 'none', 23, `stroke="#fff" stroke-opacity=".3"`)}${text(389, 441, 'Book a demo', 15, '#fff', 600, 'text-anchor="middle"')}
  <g>
    ${rect(670, 200, 400, 300, '#ffffff', 16, 'fill-opacity=".05" stroke="#fff" stroke-opacity=".1"')}
    ${text(700, 240, p.kpiLabel, 13, '#fff', 500, 'fill-opacity=".6"')}
    ${text(700, 280, p.kpi, 34, '#fff', 800)}
    ${rect(880, 256, 74, 26, p.a, 13, 'fill-opacity=".2"')}${text(917, 274, p.kpiDelta, 13, p.a, 700, 'text-anchor="middle"')}
    <path d="${lp.area}" fill="url(#${p.id}-fade)"/><path d="${lp.d}" fill="none" stroke="${p.a}" stroke-width="3.5" stroke-linecap="round"/>
    <circle cx="${lp.last[0]}" cy="${lp.last[1]}" r="7" fill="${p.a}"/><circle cx="${lp.last[0]}" cy="${lp.last[1]}" r="16" fill="${p.a}" opacity=".25"/>
  </g>
  <g transform="translate(250 -10)">${rect(600, 470, 210, 100, p.surface2, 14, `stroke="#fff" stroke-opacity=".12"`)}
  <circle cx="650" cy="520" r="26" fill="none" stroke="#fff" stroke-opacity=".12" stroke-width="8"/>
  <circle cx="650" cy="520" r="26" fill="none" stroke="${p.b}" stroke-width="8" stroke-dasharray="${2 * Math.PI * 26 * 0.72} 999" transform="rotate(-90 650 520)" stroke-linecap="round"/>
  ${text(692, 514, '72%', 22, '#fff', 800)}${rect(692, 528, 90, 8, '#fff', 4, 'fill-opacity=".25"')}</g>
  ${[0, 1, 2, 3, 4].map((i) => rect(140 + i * 96, 540, 76, 22, '#fff', 6, 'fill-opacity=".1"')).join('')}
  ${text(140, 520, 'TRUSTED BY TEAMS AT', 11, '#fff', 600, 'fill-opacity=".4" letter-spacing="2"')}
  ${[0, 1, 2].map((i) => `${rect(140 + i * 310, 610, 290, 110, '#ffffff', 14, 'fill-opacity=".04" stroke="#fff" stroke-opacity=".08"')}<circle cx="${172 + i * 310}" cy="644" r="14" fill="${i % 2 ? p.b : p.a}" opacity=".85"/>${rect(196 + i * 310, 638, 130, 10, '#fff', 5, 'fill-opacity=".5"')}${rect(160 + i * 310, 676, 230, 8, '#fff', 4, 'fill-opacity=".18"')}${rect(160 + i * 310, 694, 180, 8, '#fff', 4, 'fill-opacity=".12"')}`).join('')}`;
  return frame(p, browser(p, c));
}

function dash(p) {
  const r = rng(p.id);
  const lp = linePath(r, 330, 330, 500, 170, 14, 0.55);
  const lp2 = linePath(rng(p.id + 'x'), 330, 330, 500, 170, 14, 0.3);
  const bars = Array.from({ length: 9 }, () => 0.25 + r() * 0.7);
  const c = `
  ${rect(90, 114, 200, 616, p.surface2, 0)}
  ${text(118, 160, p.name, 18, '#fff', 800)}
  ${['Overview', 'Payments', 'Customers', 'Invoices', 'Reports', 'Settings'].map((m, i) => `${i === 0 ? rect(106, 190 + i * 46, 168, 36, `url(#${p.id}-a)`, 10, 'fill-opacity=".9"') : ''}<circle cx="128" cy="${208 + i * 46}" r="6" fill="${i === 0 ? p.onA : '#fff'}" fill-opacity="${i === 0 ? 1 : 0.35}"/>${text(146, 213 + i * 46, m, 14, i === 0 ? p.onA : '#fff', 600, i === 0 ? '' : 'fill-opacity=".6"')}`).join('')}
  ${rect(106, 640, 168, 70, '#fff', 12, 'fill-opacity=".05"')}<circle cx="134" cy="675" r="16" fill="${p.b}"/>${rect(158, 664, 90, 9, '#fff', 4, 'fill-opacity=".5"')}${rect(158, 680, 60, 8, '#fff', 4, 'fill-opacity=".25"')}
  ${text(320, 166, 'Good morning, Alex', 24, '#fff', 800)}${rect(320, 180, 220, 8, '#fff', 4, 'fill-opacity=".25"')}
  ${rect(810, 140, 270, 40, '#fff', 20, 'fill-opacity=".06"')}${rect(836, 157, 120, 7, '#fff', 3, 'fill-opacity=".3"')}
  ${[
    ['Revenue', p.kpi, p.kpiDelta],
    ['Active users', '48.2k', '+18%'],
    ['Conversion', '6.4%', '+2.1%'],
  ]
    .map(
      ([l, v, d], i) => `${rect(320 + i * 256, 210, 236, 96, '#fff', 14, 'fill-opacity=".05" stroke="#fff" stroke-opacity=".08"')}
    ${text(342 + i * 256, 240, l, 13, '#fff', 500, 'fill-opacity=".55"')}${text(342 + i * 256, 282, v, 28, '#fff', 800)}
    ${rect(476 + i * 256, 262, 62, 24, i === 1 ? p.b : p.a, 12, 'fill-opacity=".18"')}${text(507 + i * 256, 279, d, 12, i === 1 ? p.b : p.a, 700, 'text-anchor="middle"')}`
    )
    .join('')}
  ${rect(320, 322, 530, 230, '#fff', 14, 'fill-opacity=".04" stroke="#fff" stroke-opacity=".08"')}
  ${[0, 1, 2, 3].map((i) => `<line x1="340" x2="830" y1="${360 + i * 45}" y2="${360 + i * 45}" stroke="#fff" stroke-opacity=".06"/>`).join('')}
  <path d="${lp.area}" fill="url(#${p.id}-fade)" transform="translate(0,30)"/><path d="${lp.d}" fill="none" stroke="${p.a}" stroke-width="3.5" transform="translate(0,30)"/>
  <path d="${lp2.d}" fill="none" stroke="${p.b}" stroke-width="2.5" stroke-dasharray="6 7" transform="translate(0,30)"/>
  ${rect(866, 322, 214, 230, '#fff', 14, 'fill-opacity=".04" stroke="#fff" stroke-opacity=".08"')}
  ${text(886, 352, 'Weekly volume', 13, '#fff', 600, 'fill-opacity=".6"')}
  ${bars.map((b, i) => rect(888 + i * 20, 530 - b * 150, 12, b * 150, i === bars.length - 1 ? p.a : '#fff', 4, i === bars.length - 1 ? '' : 'fill-opacity=".18"')).join('')}
  ${rect(320, 568, 760, 162, '#fff', 14, 'fill-opacity=".04" stroke="#fff" stroke-opacity=".08"')}
  ${[0, 1, 2].map((i) => `<circle cx="352" cy="${612 + i * 40}" r="13" fill="${[p.a, p.b, '#fff'][i]}" fill-opacity="${i === 2 ? 0.3 : 0.9}"/>${rect(378, 604 + i * 40, 160, 9, '#fff', 4, 'fill-opacity=".5"')}${rect(620, 604 + i * 40, 90, 9, '#fff', 4, 'fill-opacity=".25"')}${rect(980, 600 + i * 40, 70, 20, i === 1 ? p.b : p.a, 10, 'fill-opacity=".2"')}`).join('')}`;
  return frame(p, browser(p, c));
}

function product(p, x, y, s, kind, color, r) {
  // Abstract hand-drawn product silhouettes
  switch (kind) {
    case 'bag':
      return `<path d="M${x - s * 0.5},${y - s * 0.25} h${s} l${s * 0.08},${s * 0.8} h${-s * 1.16} z" fill="${color}"/><path d="M${x - s * 0.22},${y - s * 0.25} q0,${-s * 0.4} ${s * 0.22},${-s * 0.4} q${s * 0.22},0 ${s * 0.22},${s * 0.4}" fill="none" stroke="${color}" stroke-width="${s * 0.06}"/>`;
    case 'bottle':
      return `${rect(x - s * 0.12, y - s * 0.6, s * 0.24, s * 0.18, color, s * 0.04)}${rect(x - s * 0.3, y - s * 0.44, s * 0.6, s * 1, color, s * 0.16)}${rect(x - s * 0.22, y - s * 0.1, s * 0.44, s * 0.3, '#fff', s * 0.04, 'fill-opacity=".35"')}`;
    case 'fruit':
      return `<circle cx="${x}" cy="${y + s * 0.05}" r="${s * 0.42}" fill="${color}"/><path d="M${x},${y - s * 0.36} q${s * 0.2},${-s * 0.3} ${s * 0.4},${-s * 0.2} q${-s * 0.2},${s * 0.25} ${-s * 0.4},${s * 0.2}" fill="#7BE36B"/><circle cx="${x - s * 0.15}" cy="${y - s * 0.08}" r="${s * 0.1}" fill="#fff" fill-opacity=".35"/>`;
    case 'dress':
      return `<path d="M${x - s * 0.14},${y - s * 0.55} h${s * 0.28} l${s * 0.08},${s * 0.35} l${s * 0.3},${s * 0.8} h${-s * 1.04} l${s * 0.3},${-s * 0.8} z" fill="${color}"/><rect x="${x - s * 0.22}" y="${y - s * 0.22}" width="${s * 0.44}" height="${s * 0.05}" fill="#fff" fill-opacity=".4"/>`;
    case 'mountain':
      return `<path d="M${x - s * 0.6},${y + s * 0.45} L${x - s * 0.1},${y - s * 0.4} L${x + s * 0.15},${y} L${x + s * 0.3},${y - s * 0.2} L${x + s * 0.6},${y + s * 0.45} z" fill="${color}"/><circle cx="${x + s * 0.32}" cy="${y - s * 0.45}" r="${s * 0.13}" fill="#FFC857"/>`;
    default:
      return `<circle cx="${x}" cy="${y}" r="${s * 0.4}" fill="${color}"/>`;
  }
}

function shop(p) {
  const r = rng(p.id);
  const kinds = p.products;
  const c = `
  ${nav(p, 140, 160, 920)}
  ${rect(140, 196, 920, 290, p.surface2, 20)}
  <circle cx="880" cy="341" r="122" fill="url(#${p.id}-a)" opacity=".9"/>
  <circle cx="880" cy="341" r="84" fill="${p.surface2}" opacity=".22"/>
  <circle cx="990" cy="236" r="10" fill="${p.b}"/><circle cx="752" cy="440" r="6" fill="${p.a}"/>
  ${product(p, 880, 352, 170, kinds[0], p.hero, r)}
  ${text(180, 258, p.eyebrow, 13, p.a, 700, 'letter-spacing="3"')}
  ${text(180, 316, p.headline[0], 48, '#fff', 800, 'letter-spacing="-1.5"')}
  ${text(180, 370, p.headline[1], 48, '#fff', 800, 'letter-spacing="-1.5" fill-opacity=".55"')}
  ${rect(180, 404, 170, 48, `url(#${p.id}-a)`, 24)}${text(265, 434, 'Shop now', 15, p.onA, 700, 'text-anchor="middle"')}
  ${text(370, 434, 'Free shipping over $80', 13, '#fff', 500, 'fill-opacity=".55"')}
  ${[0, 1, 2, 3]
    .map((i) => {
      const x = 140 + i * 236;
      const tint = [p.a, p.b, p.c || p.a, p.b][i];
      return `${rect(x, 506, 216, 230, '#fff', 16, 'fill-opacity=".05" stroke="#fff" stroke-opacity=".08"')}
      ${rect(x + 12, 518, 192, 150, tint, 12, 'fill-opacity=".14"')}
      ${product(p, x + 108, 600, 110, kinds[(i + 1) % kinds.length], tint, r)}
      ${rect(x + 14, 686, 110, 9, '#fff', 4, 'fill-opacity=".55"')}${text(x + 14, 720, '$' + (39 + Math.floor(r() * 160)), 16, '#fff', 800)}
      <circle cx="${x + 186}" cy="712" r="14" fill="${tint}" fill-opacity=".9"/>${text(x + 186, 717, '+', 16, p.onA, 800, 'text-anchor="middle"')}`;
    })
    .join('')}`;
  return frame(p, browser(p, c));
}

function editorial(p) {
  const c = `
  ${rect(90, 114, 1020, 616, p.paper, 0)}
  ${text(140, 160, p.name.toUpperCase(), 16, p.ink, 800, 'letter-spacing="4"')}
  ${(p.menu || ['About', 'Menu', 'Journal', 'Visit']).map((m, i) => text(700 + i * 90, 160, m, 14, p.ink, 500, 'fill-opacity=".65"')).join('')}
  <line x1="140" x2="1060" y1="184" y2="184" stroke="${p.ink}" stroke-opacity=".15"/>
  <text x="140" y="290" font-size="92" font-weight="400" fill="${p.ink}" ${SERIF} letter-spacing="-3">${xml(p.headline[0])}</text>
  <text x="140" y="378" font-size="92" font-style="italic" font-weight="400" fill="${p.a}" ${SERIF} letter-spacing="-3">${xml(p.headline[1])}</text>
  ${rect(140, 410, 330, 9, p.ink, 4, 'fill-opacity=".25"')}${rect(140, 430, 280, 9, p.ink, 4, 'fill-opacity=".18"')}${rect(140, 450, 300, 9, p.ink, 4, 'fill-opacity=".18"')}
  ${rect(140, 488, 180, 50, p.ink, 25)}${text(230, 519, p.cta, 14, p.paper, 700, 'text-anchor="middle"')}
  <g>
    ${rect(640, 214, 420, 330, p.b, 18)}
    <circle cx="930" cy="300" r="56" fill="${p.a}"/>
    <path d="M640,470 Q760,360 860,430 T1060,400 V544 H640 Z" fill="${p.ink}" fill-opacity=".85"/>
    <path d="M640,500 Q780,430 900,480 T1060,470 V544 H640 Z" fill="${p.a}" fill-opacity=".55"/>
    ${p.plate ? `<ellipse cx="760" cy="330" rx="80" ry="80" fill="${p.paper}"/><ellipse cx="760" cy="330" rx="56" ry="56" fill="${p.a}" fill-opacity=".8"/><circle cx="742" cy="318" r="14" fill="${p.paper}" fill-opacity=".7"/><circle cx="782" cy="342" r="10" fill="${p.ink}" fill-opacity=".5"/>` : ''}
  </g>
  <line x1="140" x2="1060" y1="580" y2="580" stroke="${p.ink}" stroke-opacity=".15"/>
  ${[0, 1, 2].map((i) => `${text(140 + i * 310, 620, '0' + (i + 1), 13, p.a, 800)}${text(176 + i * 310, 620, p.points[i], 16, p.ink, 700)}${rect(140 + i * 310, 640, 260, 8, p.ink, 4, 'fill-opacity=".18"')}${rect(140 + i * 310, 658, 220, 8, p.ink, 4, 'fill-opacity=".14"')}`).join('')}`;
  return frame(p, browser(p, c));
}

function phone(p, x, y, rot, screen, scale = 1) {
  const w = 250 * scale, h = 510 * scale;
  return `<g transform="translate(${x} ${y}) rotate(${rot})" filter="url(#${p.id}-shadow)">
    ${rect(-w / 2, -h / 2, w, h, '#0b0b10', 40 * scale, 'stroke="#2a2a35" stroke-width="3"')}
    ${rect(-w / 2 + 10 * scale, -h / 2 + 10 * scale, w - 20 * scale, h - 20 * scale, p.surface, 32 * scale)}
    ${rect(-36 * scale, -h / 2 + 18 * scale, 72 * scale, 20 * scale, '#000', 10 * scale)}
    <g transform="translate(${-w / 2 + 10 * scale} ${-h / 2 + 10 * scale}) scale(${scale})">${screen}</g>
  </g>`;
}

function mobile(p) {
  const ring = (cx, cy, rr, pct, col, sw = 14) =>
    `<circle cx="${cx}" cy="${cy}" r="${rr}" fill="none" stroke="#fff" stroke-opacity=".08" stroke-width="${sw}"/><circle cx="${cx}" cy="${cy}" r="${rr}" fill="none" stroke="${col}" stroke-width="${sw}" stroke-linecap="round" stroke-dasharray="${2 * Math.PI * rr * pct} 999" transform="rotate(-90 ${cx} ${cy})"/>`;
  const s1 = `${text(22, 70, 'Today', 26, '#fff', 800)}${text(22, 94, 'Thursday · Leg day', 12, '#fff', 500, 'fill-opacity=".5"')}
    ${ring(115, 200, 74, 0.78, p.a)}${ring(115, 200, 54, 0.6, p.b)}${ring(115, 200, 34, 0.42, '#fff')}
    ${text(115, 207, '78%', 22, '#fff', 800, 'text-anchor="middle"')}
    ${[0, 1, 2].map((i) => `${rect(18, 300 + i * 56, 194, 46, '#fff', 12, 'fill-opacity=".06"')}<circle cx="42" cy="${323 + i * 56}" r="11" fill="${[p.a, p.b, '#fff'][i]}"/>${rect(62, 316 + i * 56, 90, 7, '#fff', 3, 'fill-opacity=".6"')}${rect(62, 330 + i * 56, 60, 6, '#fff', 3, 'fill-opacity=".25"')}`).join('')}`;
  const s2 = `${text(22, 70, 'Progress', 24, '#fff', 800)}
    ${Array.from({ length: 7 }, (_, i) => { const hh = [70, 110, 90, 150, 120, 180, 140][i]; return rect(24 + i * 27, 280 - hh, 16, hh, i === 5 ? `url(#${p.id}-a)` : '#fff', 6, i === 5 ? '' : 'fill-opacity=".15"'); }).join('')}
    ${['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => text(32 + i * 27, 302, d, 10, '#fff', 600, 'text-anchor="middle" fill-opacity=".4"')).join('')}
    ${rect(18, 330, 194, 120, `url(#${p.id}-a)`, 18)}${text(36, 370, '12,480', 28, p.onA, 800)}${text(36, 394, 'kcal this month', 12, p.onA, 600, 'fill-opacity=".7"')}${rect(36, 416, 120, 10, p.onA, 5, 'fill-opacity=".25"')}`;
  const s3 = `${text(22, 70, 'Coaches', 24, '#fff', 800)}
    ${[0, 1, 2, 3].map((i) => `${rect(18, 100 + i * 90, 194, 76, '#fff', 14, 'fill-opacity=".06"')}<circle cx="52" cy="${138 + i * 90}" r="22" fill="${i % 2 ? p.b : p.a}" fill-opacity=".85"/>${rect(84, 126 + i * 90, 90, 8, '#fff', 4, 'fill-opacity=".6"')}${rect(84, 142 + i * 90, 64, 7, '#fff', 3, 'fill-opacity=".25"')}`).join('')}`;
  const inner = `
  ${text(90, 140, p.name, 22, '#fff', 800)}
  ${text(90, 200, p.headline[0], 54, '#fff', 800, 'letter-spacing="-1.5"')}
  ${text(90, 262, p.headline[1], 54, p.a, 800, 'letter-spacing="-1.5"')}
  ${rect(90, 296, 170, 50, `url(#${p.id}-a)`, 25)}${text(175, 327, 'Download', 15, p.onA, 800, 'text-anchor="middle"')}
  ${phone(p, 470, 520, -8, s3, 0.86)}
  ${phone(p, 1010, 470, 8, s2, 0.9)}
  ${phone(p, 740, 450, 0, s1, 1)}`;
  return frame(p, inner);
}

/* ---------------- Charts for case studies ---------------- */
function resultChart(p) {
  const { before, after, labels } = p.chart;
  const max = Math.max(...before, ...after) * 1.15;
  const W = 1200, H = 660, x0 = 120, y0 = 120, w = 980, h = 430;
  const bw = w / labels.length;
  const bars = labels
    .map((l, i) => {
      const bx = x0 + i * bw + bw * 0.2;
      const hb = (before[i] / max) * h;
      const ha = (after[i] / max) * h;
      return `${rect(bx, y0 + h - hb, bw * 0.26, hb, '#fff', 8, 'fill-opacity=".18"')}
      ${rect(bx + bw * 0.32, y0 + h - ha, bw * 0.26, ha, `url(#${p.id}c-a)`, 8)}
      ${text(bx + bw * 0.45, y0 + h - ha - 14, '+' + Math.round(((after[i] - before[i]) / before[i]) * 100) + '%', 18, p.a, 800, 'text-anchor="middle"')}
      ${text(bx + bw * 0.29, y0 + h + 40, l, 18, '#fff', 600, 'text-anchor="middle" fill-opacity=".7"')}`;
    })
    .join('');
  return frame(
    { ...p, id: p.id + 'c' },
    `${[0, 1, 2, 3, 4].map((i) => `<line x1="${x0}" x2="${x0 + w}" y1="${y0 + (h / 4) * i}" y2="${y0 + (h / 4) * i}" stroke="#fff" stroke-opacity=".07"/>`).join('')}
    ${text(x0, 70, p.chart.title, 26, '#fff', 800)}
    ${rect(x0 + w - 330, 50, 16, 16, '#fff', 4, 'fill-opacity=".25"')}${text(x0 + w - 306, 64, 'Before', 16, '#fff', 500, 'fill-opacity=".7"')}
    ${rect(x0 + w - 200, 50, 16, 16, p.a, 4)}${text(x0 + w - 176, 64, 'After launch', 16, '#fff', 500, 'fill-opacity=".7"')}
    ${bars}`,
    { w: W, h: H }
  );
}

/* ---------------- Brand assets ---------------- */
const LOGO = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#D4FF3A"/><stop offset="1" stop-color="#7CFFB2"/></linearGradient></defs><rect width="48" height="48" rx="13" fill="#111217"/><path d="M17 12h20l-3 6H23.6l-1.7 4H31l-3 6h-8.6L15 38h-6.5z" fill="url(#g)"/><path d="M5 20h8M3 26h7M6 32h5" stroke="#D4FF3A" stroke-width="2.4" stroke-linecap="round" opacity=".55"/></svg>`;

export function renderArt() {
  const out = { 'logo.svg': LOGO };
  const map = { saas, dash, shop, editorial, mobile };
  for (const p of PROJECTS) {
    out[`work/${p.id}.svg`] = map[p.layout](p);
    if (p.chart) out[`work/${p.id}-results.svg`] = resultChart(p);
  }
  return out;
}
