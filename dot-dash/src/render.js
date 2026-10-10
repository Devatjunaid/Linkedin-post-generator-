// Turns picture data into SVG markup.
import { INK } from './pictures.js';

const fmt = (n) => Math.round(n * 100) / 100;

function starPoints(cx, cy, rad) {
  const pts = [];
  for (let i = 0; i < 10; i++) {
    const a = (Math.PI * i) / 5 - Math.PI / 2;
    const rr = i % 2 === 0 ? rad : rad * 0.45;
    pts.push(`${fmt(cx + rr * Math.cos(a))},${fmt(cy + rr * Math.sin(a))}`);
  }
  return pts.join(' ');
}

export function artSVG(art) {
  return art.map((item) => {
    const [kind] = item;
    switch (kind) {
      case 'c': return `<circle cx="${item[1]}" cy="${item[2]}" r="${item[3]}" fill="${item[4]}"/>`;
      case 'e': return `<ellipse cx="${item[1]}" cy="${item[2]}" rx="${item[3]}" ry="${item[4]}" fill="${item[5]}"/>`;
      case 'p': return `<path d="${item[1]}" fill="${item[2]}"/>`;
      case 'l': return `<path d="${item[1]}" fill="none" stroke="${item[2]}" stroke-width="${item[3]}" stroke-linecap="round" stroke-linejoin="round"/>`;
      case 'r': return `<rect x="${item[1]}" y="${item[2]}" width="${item[3]}" height="${item[4]}" rx="${item[5]}" fill="${item[6]}"/>`;
      case 't': return `<text x="${item[1]}" y="${item[2]}" text-anchor="middle" font-family="Fredoka" font-weight="700" font-size="${item[5]}" fill="${item[4]}">${item[3]}</text>`;
      case 'star': return `<polygon points="${starPoints(item[1], item[2], item[3])}" fill="${item[4]}" stroke="${item[4]}" stroke-width="0.8" stroke-linejoin="round"/>`;
      case 'eye': {
        const [, x, y, s] = item;
        return `<circle cx="${x}" cy="${y}" r="${fmt(3.3 * s)}" fill="#FFFFFF"/>` +
          `<circle cx="${fmt(x + 0.5 * s)}" cy="${fmt(y + 0.4 * s)}" r="${fmt(1.9 * s)}" fill="${INK}"/>` +
          `<circle cx="${fmt(x + 1.1 * s)}" cy="${fmt(y - 0.4 * s)}" r="${fmt(0.7 * s)}" fill="#FFFFFF"/>`;
      }
      default: return '';
    }
  }).join('');
}

export const polyPoints = (pts) => pts.map(([x, y]) => `${x},${y}`).join(' ');

// The finished, coloured picture (used by the reveal, stickers and colouring).
export function pictureSVG(pic, { fill, outline = 1.6, cls = '' } = {}) {
  return `<g class="pic ${cls}">` +
    `<polygon class="pic-body" points="${polyPoints(pic.pts)}" fill="${fill || pic.fill}" stroke="${INK}" stroke-width="${outline}" stroke-linejoin="round"/>` +
    `<g class="pic-art">${artSVG(pic.art)}</g></g>`;
}

export function pictureIcon(pic, { fill, size = 64, extra = '' } = {}) {
  return `<svg class="pic-icon ${extra}" viewBox="-6 -6 112 112" width="${size}" height="${size}" aria-hidden="true">${pictureSVG(pic, { fill, outline: 2.2 })}</svg>`;
}

export function silhouetteIcon(pic, size = 64) {
  return `<svg class="pic-icon" viewBox="-6 -6 112 112" width="${size}" height="${size}" aria-hidden="true"><polygon points="${polyPoints(pic.pts)}" fill="currentColor" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>`;
}
