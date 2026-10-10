// Animated splash: Zip flies in drawing a glowing line through five dots,
// then the logo bounces in letter by letter.
import { register, go } from '../app.js';
import { logoHTML, zipSVG } from '../art.js';
import { skyHTML, $ } from '../ui.js';
import { sfx } from '../audio.js';

const TRAIL = 'M24 150 C 70 196, 104 86, 160 112 S 246 166, 296 70';
const DOT_AT = [0, 0.24, 0.5, 0.74, 1];

register('splash', {
  render() {
    return `<div class="splash">
      ${skyHTML(40)}
      <svg class="splash-trail" viewBox="0 0 320 210" aria-hidden="true">
        <path class="splash-glow" d="${TRAIL}"/>
        <path class="splash-line" d="${TRAIL}"/>
        <g class="splash-dots"></g>
        <g class="splash-zip">${zipSVG({ size: 46 }).replace('<svg ', '<svg x="-23" y="-34" ')}</g>
      </svg>
      ${logoHTML('logo-intro')}
      <p class="splash-tag">Connect the dots. Light up the picture!</p>
      <div class="splash-load" aria-hidden="true"><i></i><i></i><i></i></div>
    </div>`;
  },
  mount(root) {
    const line = $('.splash-line', root);
    const glow = $('.splash-glow', root);
    const zip = $('.splash-zip', root);
    const dotsG = $('.splash-dots', root);
    const len = line.getTotalLength();
    [line, glow].forEach((p) => { p.style.strokeDasharray = `${len}`; p.style.strokeDashoffset = `${len}`; });

    const dots = DOT_AT.map((f, i) => {
      const pt = line.getPointAtLength(f * len);
      const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
      g.setAttribute('transform', `translate(${pt.x} ${pt.y})`);
      g.innerHTML = `<g class="sdot" style="animation-delay:${0.15 + i * 0.1}s"><circle r="12"/><text y="5">${i + 1}</text></g>`;
      dotsG.appendChild(g);
      return g.firstChild;
    });

    let raf = 0;
    let lit = 0;
    const start = performance.now() + 750;
    const dur = 1300;
    const frame = (now) => {
      const t = Math.min(1, Math.max(0, (now - start) / dur));
      const eased = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
      const off = len * (1 - eased);
      line.style.strokeDashoffset = `${off}`;
      glow.style.strokeDashoffset = `${off}`;
      const pt = line.getPointAtLength(eased * len);
      zip.setAttribute('transform', `translate(${pt.x} ${pt.y})`);
      zip.style.opacity = now >= start ? '1' : '0';
      while (lit < DOT_AT.length && eased >= DOT_AT[lit] - 0.001) {
        dots[lit].classList.add('on');
        sfx.dot(lit * 2);
        lit++;
      }
      if (t < 1) raf = requestAnimationFrame(frame);
    };
    raf = requestAnimationFrame(frame);

    let left = false;
    const leave = () => {
      if (left) return;
      left = true;
      go('home', { fromSplash: true });
    };
    const timer = setTimeout(leave, 3900);
    const skipReady = setTimeout(() => root.addEventListener('pointerup', leave), 900);

    return () => {
      cancelAnimationFrame(raf);
      clearTimeout(timer);
      clearTimeout(skipReady);
      root.removeEventListener('pointerup', leave);
    };
  },
});
