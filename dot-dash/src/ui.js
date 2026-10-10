// Small DOM helpers shared by every screen.
export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

export function el(html) {
  const t = document.createElement('template');
  t.innerHTML = html.trim();
  return t.content.firstElementChild;
}

export const wait = (ms) => new Promise((res) => setTimeout(res, ms));

export function onTap(node, fn) {
  if (node) node.addEventListener('click', fn);
}

const CONFETTI = ['#FFC93C', '#FF5D73', '#3EE0B0', '#5CC8FF', '#C3A6FF', '#FFFFFF'];

export function confetti(root = document.body, count = 70) {
  const box = document.createElement('div');
  box.className = 'confetti';
  for (let i = 0; i < count; i++) {
    const p = document.createElement('i');
    p.style.left = `${Math.random() * 100}%`;
    p.style.background = CONFETTI[i % CONFETTI.length];
    p.style.animationDelay = `${Math.random() * 0.6}s`;
    p.style.animationDuration = `${1.8 + Math.random() * 1.4}s`;
    p.style.setProperty('--spin', `${Math.random() * 720 - 360}deg`);
    p.style.setProperty('--drift', `${Math.random() * 120 - 60}px`);
    if (i % 3 === 0) p.style.borderRadius = '50%';
    box.appendChild(p);
  }
  root.appendChild(box);
  setTimeout(() => box.remove(), 3600);
}

// Twinkling night sky behind every screen.
export function skyHTML(count = 26) {
  let s = '';
  for (let i = 0; i < count; i++) {
    const size = 1.5 + Math.random() * 2.5;
    s += `<i style="left:${Math.random() * 100}%;top:${Math.random() * 100}%;width:${size}px;height:${size}px;animation-delay:${(Math.random() * 3).toFixed(2)}s"></i>`;
  }
  return `<div class="sky" aria-hidden="true">${s}</div>`;
}

const CHEERS = ['AMAZING!', 'SUPER!', 'WOW!', 'GREAT JOB!', 'AWESOME!', 'YOU DID IT!', 'BRILLIANT!'];
export const cheer = () => CHEERS[Math.floor(Math.random() * CHEERS.length)];

const NUMBER_WORDS = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve', 'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen', 'twenty'];
const TENS = ['', '', 'twenty', 'thirty', 'forty', 'fifty'];
export function numberWord(n) {
  if (n <= 20) return NUMBER_WORDS[n];
  const t = Math.floor(n / 10);
  const o = n % 10;
  return o ? `${TENS[t]} ${NUMBER_WORDS[o]}` : TENS[t];
}

export const article = (name) => (/^[AEIOU]/i.test(name) ? 'an' : 'a');
