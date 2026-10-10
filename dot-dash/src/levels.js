// Builds the 60 levels: 20 per world, easiest outline first.
// Later levels add extra dots along the longest edges so the count keeps climbing.
import { WORLDS } from './pictures.js';

export const LEVELS_PER_WORLD = 20;
const MIN_EDGE = 11; // never split an edge shorter than this — keeps dots tappable

function dist(a, b) {
  return Math.hypot(a[0] - b[0], a[1] - b[1]);
}

function addDots(pts, target) {
  const out = pts.map((p) => [...p]);
  while (out.length < target) {
    let best = -1;
    let bestLen = 0;
    for (let i = 0; i < out.length; i++) {
      const len = dist(out[i], out[(i + 1) % out.length]);
      if (len > bestLen) { bestLen = len; best = i; }
    }
    if (bestLen < MIN_EDGE * 2) break;
    const a = out[best];
    const b = out[(best + 1) % out.length];
    out.splice(best + 1, 0, [Math.round((a[0] + b[0]) * 5) / 10, Math.round((a[1] + b[1]) * 5) / 10]);
  }
  return out;
}

export const LEVELS = [];

WORLDS.forEach((world, w) => {
  const ordered = [...world.pictures].sort((a, b) => a.pts.length - b.pts.length);
  ordered.forEach((pic, i) => {
    const index = w * LEVELS_PER_WORLD + i;
    const target = Math.max(pic.pts.length, Math.min(40, 6 + Math.round(index * 0.58)));
    LEVELS.push({
      index,
      world: w,
      number: i + 1, // number inside its world
      pic,
      dots: addDots(pic.pts, target),
      egg: (i + 1) % 5 === 0, // every 5th level hatches a Mystery Egg
    });
  });
});
