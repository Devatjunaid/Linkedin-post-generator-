// The connect-the-dots board: tap (or drag through) the dots in order, then the
// picture is revealed, comes alive and the stars are awarded.
import { register, go, setMusic } from '../app.js';
import { zipSVG, starSVG, icons } from '../art.js';
import { skyHTML, $, $$, onTap, wait, confetti, cheer, article } from '../ui.js';
import { store } from '../store.js';
import { WORLDS } from '../pictures.js';
import { LEVELS, LEVELS_PER_WORLD } from '../levels.js';
import { pictureSVG, pictureIcon } from '../render.js';
import { sfx } from '../audio.js';
import { say } from '../speech.js';
import { levelOpenAd } from '../ads.js';
import { colorOverlay } from './overlays.js';
import { GOLDEN_ZIP_STARS, EGG_STARS } from '../config.js';

const NS = 'http://www.w3.org/2000/svg';
// Dots are as big as the picture allows without overlapping.
function dotRadius(dots) {
  let min = Infinity;
  for (let i = 0; i < dots.length; i++) {
    for (let j = i + 1; j < dots.length; j++) {
      min = Math.min(min, Math.hypot(dots[i][0] - dots[j][0], dots[i][1] - dots[j][1]));
    }
  }
  return Math.max(3, Math.min(5.2, min * 0.48));
}

function svgEl(tag, attrs = {}, cls = '') {
  const n = document.createElementNS(NS, tag);
  for (const [k, v] of Object.entries(attrs)) n.setAttribute(k, v);
  if (cls) n.setAttribute('class', cls);
  return n;
}

// Zoom the board to the picture so dots are as big as possible.
function boardViewBox(dots, r) {
  const xs = dots.map((d) => d[0]);
  const ys = dots.map((d) => d[1]);
  const pad = r + 7;
  let x0 = Math.min(...xs) - pad;
  let y0 = Math.min(...ys) - pad;
  let w = Math.max(...xs) + pad - x0;
  let h = Math.max(...ys) + pad - y0;
  // Keep very thin pictures from zooming in too far.
  if (w < 70) { x0 -= (70 - w) / 2; w = 70; }
  if (h < 70) { y0 -= (70 - h) / 2; h = 70; }
  return `${x0.toFixed(1)} ${y0.toFixed(1)} ${w.toFixed(1)} ${h.toFixed(1)}`;
}

const starsFor = (wrong, hint) => Math.max(1, 3 - (wrong > 2 ? 1 : 0) - (hint ? 1 : 0));

register('play', {
  render({ index }) {
    const lv = LEVELS[index];
    const world = WORLDS[lv.world];
    const r = dotRadius(lv.dots);
    const dots = lv.dots.map(([x, y], i) => {
      const fs = (i + 1 >= 10 ? 0.92 : 1.08) * r;
      return `<g class="dot" data-i="${i}" transform="translate(${x} ${y})">
        <circle class="dot-ring" r="${r}"/><circle class="dot-bg" r="${r}"/>
        <text font-size="${fs.toFixed(2)}" y="${(fs * 0.36).toFixed(2)}">${i + 1}</text></g>`;
    }).join('');
    return `<div class="play" style="--wc:${world.color}">
      ${skyHTML(18)}
      <header class="topbar">
        <button class="icon-btn" data-back aria-label="Back to map">${icons.map()}</button>
        <div class="play-title"><b>Level ${lv.number}</b><small>${world.name}</small></div>
        <div class="pill play-stars" aria-label="Stars you can still win">${starSVG(22)}${starSVG(22)}${starSVG(22)}</div>
      </header>
      <div class="progress"><div class="progress-bar"><i></i></div><span class="progress-txt">0/${lv.dots.length}</span></div>
      <div class="board-wrap">
        <svg class="board" viewBox="${boardViewBox(lv.dots, r)}" role="img" aria-label="Connect the dots puzzle">
          <g class="pic-layer"></g>
          <g class="lines"></g>
          <g class="dots">${dots}</g>
          <g class="fx"></g>
          <g class="board-zip" style="opacity:0"><g transform="translate(-7 -15)">${zipSVG({ size: 14, gold: store.goldenZip() })}</g></g>
        </svg>
        <div class="streak" hidden></div>
        <div class="cheer-big" hidden></div>
      </div>
      <div class="play-bottom">
        <div class="zip-say">${zipSVG({ size: 58, gold: store.goldenZip(), cls: 'bob' })}<div class="bubble bubble-left say-text">Tap number <b>1</b> to start!</div></div>
        <div class="play-actions">
          <button class="btn btn-mint" data-hint>${icons.hint()}Hint</button>
          <button class="btn btn-sky" data-say>${icons.speak()}Say it</button>
        </div>
      </div>
      <div class="result" hidden></div>
    </div>`;
  },

  mount(root, { index }) {
    const lv = LEVELS[index];
    setMusic(lv.world);
    const svg = $('.board', root);
    const linesG = $('.lines', root);
    const fxG = $('.fx', root);
    const zipG = $('.board-zip', root);
    const dotEls = $$('.dot', root);
    const sayText = $('.say-text', root);
    const streakEl = $('.streak', root);
    const dots = lv.dots;
    const rainbow = store.goldenZip();
    const st = { next: 0, wrong: 0, hint: false, streak: 0, done: false, last: Date.now(), dragging: false };
    const timers = [];
    const later = (fn, ms) => timers.push(setTimeout(fn, ms));

    const setSay = (html) => { sayText.innerHTML = html; };
    const markNext = () => {
      dotEls.forEach((d) => d.classList.remove('next', 'hinted'));
      if (st.next < dots.length) dotEls[st.next].classList.add('next');
    };
    const updateStars = () => {
      const n = starsFor(st.wrong, st.hint);
      $('.play-stars', root).innerHTML = [0, 1, 2].map((k) => starSVG(22, k < n)).join('');
    };
    const updateProgress = () => {
      $('.progress-bar i', root).style.width = `${(st.next / dots.length) * 100}%`;
      $('.progress-txt', root).textContent = `${st.next}/${dots.length}`;
    };
    markNext();

    // Screen → board coordinates.
    const toBoard = (ev) => {
      const m = svg.getScreenCTM();
      if (!m) return { x: -99, y: -99, scale: 1 };
      const p = new DOMPoint(ev.clientX, ev.clientY).matrixTransform(m.inverse());
      return { x: p.x, y: p.y, scale: m.a };
    };

    const trailColor = (i) => (rainbow || st.streak >= 10 ? `hsl(${(i * 37) % 360} 95% 65%)` : '#FFE58A');

    const drawSeg = (a, b, i) => {
      const glow = svgEl('line', { x1: a[0], y1: a[1], x2: b[0], y2: b[1], pathLength: 1, stroke: trailColor(i) }, 'seg seg-glow');
      const core = svgEl('line', { x1: a[0], y1: a[1], x2: b[0], y2: b[1], pathLength: 1 }, 'seg seg-core');
      linesG.append(glow, core);
    };

    const sparkle = (p) => {
      for (let k = 0; k < 6; k++) {
        const ang = (Math.PI * 2 * k) / 6 + Math.random() * 0.5;
        const s = svgEl('circle', { cx: p[0], cy: p[1], r: 0.9, style: `--dx:${Math.cos(ang) * 8}px;--dy:${Math.sin(ang) * 8}px` }, 'spark');
        s.setAttribute('fill', ['#FFE58A', '#FFFFFF', '#3EE0B0'][k % 3]);
        fxG.appendChild(s);
        later(() => s.remove(), 700);
      }
    };

    const moveZip = (p) => {
      zipG.style.opacity = '1';
      zipG.style.transform = `translate(${p[0]}px, ${p[1]}px)`;
    };

    const connect = () => {
      const i = st.next;
      const d = dotEls[i];
      d.classList.remove('next', 'hinted', 'pop');
      void d.getBBox();
      d.classList.add('on', 'pop');
      if (i > 0) drawSeg(dots[i - 1], dots[i], i);
      sfx.dot(i);
      sparkle(dots[i]);
      moveZip(dots[i]);
      st.streak++;
      st.next++;
      st.last = Date.now();
      updateProgress();
      if (st.streak >= 5) {
        streakEl.hidden = false;
        streakEl.textContent = `Streak ×${st.streak}`;
        streakEl.classList.remove('bump');
        void streakEl.offsetWidth;
        streakEl.classList.add('bump');
      }
      if (st.next === dots.length) {
        finish();
        return;
      }
      markNext();
      setSay(i === 0 ? `Now find <b>${st.next + 1}</b>!` : `Find number <b>${st.next + 1}</b>!`);
    };

    const wrong = (i) => {
      const d = dotEls[i];
      d.classList.remove('shake');
      void d.getBBox();
      d.classList.add('shake');
      sfx.wrong();
      st.wrong++;
      st.streak = 0;
      streakEl.hidden = true;
      st.last = Date.now();
      updateStars();
      setSay(`Oops! That's <b>${i + 1}</b>. Find <b>${st.next + 1}</b>!`);
    };

    const handle = (ev, isTap) => {
      if (st.done) return;
      const p = toBoard(ev);
      const hitR = Math.max(6, 22 / p.scale);
      const target = dots[st.next];
      if (Math.hypot(p.x - target[0], p.y - target[1]) <= hitR) {
        connect();
        return;
      }
      if (!isTap) return;
      let nearest = -1;
      let nd = Infinity;
      dots.forEach((dt, i) => {
        const dd = Math.hypot(p.x - dt[0], p.y - dt[1]);
        if (dd < nd) { nd = dd; nearest = i; }
      });
      // Tapping a dot that's already joined is fine; only a wrong new dot counts.
      if (nd <= hitR * 0.85 && nearest > st.next) wrong(nearest);
    };

    svg.addEventListener('pointerdown', (ev) => {
      st.dragging = true;
      try { svg.setPointerCapture(ev.pointerId); } catch (_) { /* old webviews */ }
      handle(ev, true);
    });
    svg.addEventListener('pointermove', (ev) => { if (st.dragging) handle(ev, false); });
    const stop = () => { st.dragging = false; };
    svg.addEventListener('pointerup', stop);
    svg.addEventListener('pointercancel', stop);

    // Gentle nudge if the child gets stuck (not counted as a hint).
    const idle = setInterval(() => {
      if (st.done || Date.now() - st.last < 9000) return;
      st.last = Date.now();
      const d = dotEls[st.next];
      d.classList.remove('hinted');
      void d.getBBox();
      d.classList.add('hinted');
      setSay(`Psst… number <b>${st.next + 1}</b> is glowing!`);
      say(`${st.next + 1}`);
    }, 1000);

    onTap($('[data-hint]', root), () => {
      if (st.done) return;
      if (!st.hint) {
        st.hint = true;
        updateStars();
      }
      const d = dotEls[st.next];
      d.classList.remove('hinted');
      void d.getBBox();
      d.classList.add('hinted');
      sfx.sparkle();
      setSay(`It's right here: <b>${st.next + 1}</b>!`);
      say(`${st.next + 1}`);
      st.last = Date.now();
    });
    onTap($('[data-say]', root), () => {
      if (st.done) return;
      say(`${st.next + 1}`);
      sfx.tap();
    });
    onTap($('[data-back]', root), () => { sfx.tap(); go('map', { world: lv.world }); });

    // ---- reveal + reward ---------------------------------------------------
    const finish = async () => {
      st.done = true;
      streakEl.hidden = true;
      dotEls.forEach((d) => d.classList.remove('next'));
      const starsBefore = store.totalStars();
      const worldsBefore = WORLDS.map((_, w) => store.worldUnlocked(w));
      const stars = starsFor(st.wrong, st.hint);
      await wait(220);
      drawSeg(dots[dots.length - 1], dots[0], dots.length);
      moveZip(dots[0]);
      sfx.whoosh();
      await wait(550);

      $('.play', root).classList.add('revealed');
      $('.pic-layer', root).innerHTML = pictureSVG(lv.pic, { fill: store.data.colors[lv.pic.id], cls: 'reveal-in' });
      sfx.sparkle();
      confetti(root);
      await wait(650);
      $('.pic', root).classList.add('alive');
      sfx.voice(lv.pic.voice);
      const big = $('.cheer-big', root);
      big.textContent = cheer();
      big.hidden = false;
      await wait(900);
      say(`It's ${article(lv.pic.name)} ${lv.pic.name}!`);

      const res = store.finishLevel(lv.index, stars);
      if (res.egg) store.addBonus(EGG_STARS);
      const goldNow = starsBefore < GOLDEN_ZIP_STARS && store.totalStars() >= GOLDEN_ZIP_STARS;
      const newWorld = WORLDS.findIndex((_, w) => !worldsBefore[w] && store.worldUnlocked(w));
      showResult(stars, res, goldNow, newWorld);
    };

    const showResult = (stars, res, goldNow, newWorld) => {
      $('.play-bottom', root).hidden = true;
      const box = $('.result', root);
      const extras = [];
      if (res.firstTime) extras.push(`<div class="result-row">${pictureIcon(lv.pic, { size: 44, fill: store.data.colors[lv.pic.id] })}<span>New sticker for your book!</span></div>`);
      if (res.egg) extras.push(`<div class="result-row egg-row"><svg viewBox="0 0 100 120" width="40" height="44" aria-hidden="true"><ellipse cx="50" cy="66" rx="40" ry="50" fill="#FFC93C" stroke="#241C5C" stroke-width="6"/><path d="M14 64 L26 56 L36 68 L48 56 L60 68 L72 56 L86 64" fill="none" stroke="#241C5C" stroke-width="5"/></svg><span>Mystery Egg hatched! +${EGG_STARS} ${starSVG(16)}</span></div>`);
      if (newWorld > 0) extras.push(`<div class="result-row">${icons.next({ size: 26, stroke: '#3EE0B0' })}<span>New world open: <b>${WORLDS[newWorld].name}</b>!</span></div>`);
      if (goldNow) extras.push(`<div class="result-row">${zipSVG({ size: 44, gold: true })}<span>Zip turned <b>GOLDEN</b>!</span></div>`);
      box.innerHTML = `<div class="result-stars">${[0, 1, 2].map((k) => `<span class="rs ${k < stars ? 'won' : ''}" style="--k:${k}">${starSVG(k === 1 ? 64 : 50, k < stars)}</span>`).join('')}</div>
        <div class="result-name">It's ${article(lv.pic.name)} <b>${lv.pic.name}</b>!</div>
        ${extras.join('')}
        <div class="result-actions">
          <button class="round-btn" data-replay aria-label="Play again">${icons.replay()}</button>
          <button class="round-btn coral" data-paint aria-label="Color it in">${icons.paint()}</button>
          <button class="btn btn-mint btn-next" data-next>${lv.index === LEVELS.length - 1 ? 'DONE' : 'NEXT'}${icons.next()}</button>
        </div>`;
      box.hidden = false;
      requestAnimationFrame(() => box.classList.add('show'));
      for (let k = 0; k < stars; k++) later(() => sfx.star(k), 250 + k * 280);
      if (res.egg || newWorld > 0 || goldNow) later(() => sfx.unlock(), 1200);

      onTap($('[data-replay]', box), () => { sfx.pop(); levelOpenAd(() => go('play', { index: lv.index })); });
      onTap($('[data-paint]', box), () => {
        sfx.pop();
        colorOverlay(lv.pic, (color) => {
          const body = $('.pic-body', root);
          if (body) body.setAttribute('fill', color);
        });
      });
      onTap($('[data-next]', box), () => {
        sfx.pop();
        const nextIndex = lv.index + 1;
        if (nextIndex >= LEVELS.length) { go('home'); return; }
        if (!store.levelUnlocked(nextIndex)) { go('map', { world: lv.world }); return; }
        if (LEVELS[nextIndex].world !== lv.world && (lv.index + 1) % LEVELS_PER_WORLD === 0) {
          go('map', { world: LEVELS[nextIndex].world });
          return;
        }
        levelOpenAd(() => go('play', { index: nextIndex }));
      });
    };

    return () => {
      clearInterval(idle);
      timers.forEach(clearTimeout);
    };
  },

  back({ index }) {
    go('map', { world: LEVELS[index].world });
  },
});
