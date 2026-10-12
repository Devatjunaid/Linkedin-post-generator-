/* =========================================================
   The Fast Designs — interactions & motion
   Vanilla JS, no dependencies.
   ========================================================= */
(() => {
  const doc = document.documentElement;
  doc.classList.add('js');
  const $ = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => [...c.querySelectorAll(s)];
  const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const finePointer = matchMedia('(hover: hover) and (pointer: fine)').matches;
  const lerp = (a, b, t) => a + (b - a) * t;
  const clamp = (v, a, b) => Math.min(b, Math.max(a, v));
  const store = {
    get(k) { try { return sessionStorage.getItem(k); } catch { return null; } },
    set(k, v) { try { sessionStorage.setItem(k, v); } catch {} },
    del(k) { try { sessionStorage.removeItem(k); } catch {} },
  };

  /* ---------- Loader + page-enter transition ---------- */
  const loader = $('[data-loader]');
  const transition = $('[data-transition]');
  const cameFromNav = store.get('tfd-nav') === '1';
  const seenLoader = store.get('tfd-loaded') === '1';
  store.del('tfd-nav');

  function start() {
    document.body.classList.add('is-ready');
    initReveals();
    initHeroRotator();
  }

  if (reduced || seenLoader || cameFromNav) {
    loader?.classList.add('is-skip');
    if (cameFromNav && !reduced) {
      transition.classList.add('is-entering');
      setTimeout(() => transition.classList.remove('is-entering'), 1100);
      setTimeout(start, 250);
    } else start();
  } else {
    store.set('tfd-loaded', '1');
    const count = $('[data-loader-count]');
    const t0 = performance.now();
    const dur = 1500;
    const tick = (now) => {
      const p = clamp((now - t0) / dur, 0, 1);
      const e = 1 - Math.pow(1 - p, 3);
      count.textContent = String(Math.round(e * 100)).padStart(2, '0');
      if (p < 1) requestAnimationFrame(tick);
      else {
        loader.classList.add('is-done');
        setTimeout(start, 350);
        setTimeout(() => loader.classList.add('is-skip'), 1200);
      }
    };
    requestAnimationFrame(tick);
  }

  // Page-leave transition for internal links
  document.addEventListener('click', (e) => {
    const a = e.target.closest('a');
    if (!a || reduced) return;
    if (e.defaultPrevented || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey || e.button !== 0) return;
    if (a.target === '_blank' || a.hasAttribute('download')) return;
    const url = new URL(a.href, location.href);
    if (url.origin !== location.origin) return;
    if (url.pathname === location.pathname && url.hash) return; // in-page anchor
    if (url.href === location.href) { e.preventDefault(); return; }
    e.preventDefault();
    closeMenu();
    store.set('tfd-nav', '1');
    transition.classList.remove('is-entering');
    transition.classList.add('is-leaving');
    setTimeout(() => { location.href = url.href; }, 780);
  });
  window.addEventListener('pageshow', (e) => {
    if (e.persisted) { transition.classList.remove('is-leaving'); store.del('tfd-nav'); }
  });

  /* ---------- Header, progress, mobile menu ---------- */
  const header = $('[data-header]');
  const bar = $('.progress span');
  const burger = $('[data-burger]');
  const menu = $('[data-mmenu]');
  let lastY = scrollY;
  function onScrollUI() {
    const y = scrollY;
    header.classList.toggle('is-scrolled', y > 20);
    const menuOpen = burger.getAttribute('aria-expanded') === 'true';
    header.classList.toggle('is-hidden', !menuOpen && y > 400 && y > lastY + 2);
    if (y < lastY - 2) header.classList.remove('is-hidden');
    lastY = y;
    const max = doc.scrollHeight - innerHeight;
    bar.style.transform = `scaleX(${max > 0 ? y / max : 0})`;
  }
  function closeMenu() {
    if (burger.getAttribute('aria-expanded') !== 'true') return;
    burger.setAttribute('aria-expanded', 'false');
    burger.setAttribute('aria-label', 'Open menu');
    menu.classList.remove('is-open');
    document.body.style.overflow = '';
    setTimeout(() => { if (!menu.classList.contains('is-open')) menu.hidden = true; }, 800);
  }
  burger.addEventListener('click', () => {
    const open = burger.getAttribute('aria-expanded') !== 'true';
    if (!open) return closeMenu();
    menu.hidden = false;
    requestAnimationFrame(() => menu.classList.add('is-open'));
    burger.setAttribute('aria-expanded', 'true');
    burger.setAttribute('aria-label', 'Close menu');
    document.body.style.overflow = 'hidden';
  });
  document.addEventListener('keydown', (e) => e.key === 'Escape' && closeMenu());
  $('[data-totop]')?.addEventListener('click', () => scrollTo({ top: 0, behavior: reduced ? 'auto' : 'smooth' }));

  /* ---------- Split text ---------- */
  function splitWords(el) {
    let i = 0;
    const walk = (node) => {
      [...node.childNodes].forEach((n) => {
        if (n.nodeType === 3) {
          const parts = n.textContent.split(/(\s+)/);
          const frag = document.createDocumentFragment();
          parts.forEach((p) => {
            if (!p) return;
            if (/^\s+$/.test(p)) { frag.appendChild(document.createTextNode(' ')); return; }
            const w = document.createElement('span');
            w.className = 'w';
            const inner = document.createElement('span');
            inner.textContent = p;
            inner.style.setProperty('--wi', i++);
            w.appendChild(inner);
            frag.appendChild(w);
          });
          n.replaceWith(frag);
        } else if (n.nodeType === 1 && !n.hasAttribute('data-nosplit') && n.tagName !== 'BR') {
          walk(n);
        } else if (n.nodeType === 1 && n.hasAttribute('data-nosplit')) {
          // wrap whole element as one "word"
          const w = document.createElement('span');
          w.className = 'w';
          const inner = document.createElement('span');
          inner.style.setProperty('--wi', i++);
          n.replaceWith(w);
          inner.appendChild(n);
          w.appendChild(inner);
        }
      });
    };
    walk(el);
  }
  $$('[data-split]').forEach((el) => {
    if (!el.hasAttribute('aria-label')) el.setAttribute('aria-label', el.textContent.replace(/\s+/g, ' ').trim());
    splitWords(el);
    $$('.w', el).forEach((w) => w.setAttribute('aria-hidden', 'true'));
  });

  /* ---------- Reveal on scroll ---------- */
  function initReveals() {
    const targets = $$('[data-reveal], [data-split], [data-line], .gauge, .icon-draw, [data-count]');
    if (!('IntersectionObserver' in window) || reduced) {
      targets.forEach((t) => { t.classList.add('is-in'); if (t.dataset.count) runCount(t, true); });
      return;
    }
    const io = new IntersectionObserver(
      (entries) => entries.forEach((en) => {
        if (!en.isIntersecting) return;
        en.target.classList.add('is-in');
        if (en.target.dataset.count) runCount(en.target);
        io.unobserve(en.target);
      }),
      { rootMargin: '0px 0px -12% 0px', threshold: 0.08 }
    );
    targets.forEach((t) => io.observe(t));
    // Auto stagger children of [data-stagger]
    $$('[data-stagger]').forEach((g) => {
      const step = parseFloat(g.dataset.stagger) || 0.08;
      [...g.children].forEach((c, i) => {
        if (!c.hasAttribute('data-reveal')) { c.setAttribute('data-reveal', ''); io.observe(c); }
        c.style.setProperty('--d', `${i * step}s`);
      });
    });
  }

  function runCount(el, instant) {
    const end = parseFloat(el.dataset.count);
    const dec = (el.dataset.count.split('.')[1] || '').length;
    const suffix = el.dataset.suffix || '';
    const fmt = (v) => v.toFixed(dec) + (suffix ? `<sup>${suffix}</sup>` : '');
    if (instant) { el.innerHTML = fmt(end); return; }
    const t0 = performance.now();
    const dur = 2000;
    const step = (now) => {
      const p = clamp((now - t0) / dur, 0, 1);
      el.innerHTML = fmt(end * (1 - Math.pow(1 - p, 4)));
      if (p < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }

  /* ---------- Hero word rotator ---------- */
  function initHeroRotator() {
    const r = $('[data-rotator]');
    if (!r) return;
    const items = [...r.children];
    let i = 0;
    items[0].classList.add('is-active');
    if (reduced) return;
    setInterval(() => {
      const cur = items[i];
      i = (i + 1) % items.length;
      const nxt = items[i];
      cur.classList.remove('is-active');
      cur.classList.add('is-out');
      nxt.classList.remove('is-out');
      nxt.classList.add('is-active');
      setTimeout(() => cur.classList.remove('is-out'), 900);
    }, 2600);
  }

  /* ---------- Scroll-driven effects (parallax, scrub text, h-scroll) ---------- */
  const parallax = $$('[data-parallax]');
  const scrubs = $$('[data-scrub]');
  scrubs.forEach((el) => {
    const words = el.innerHTML.split(/(<[^>]+>|\s+)/).map((p) => {
      if (!p || /^\s+$/.test(p) || p.startsWith('<')) return p;
      return `<span class="sw">${p}</span>`;
    });
    el.innerHTML = words.join('');
    el._words = $$('.sw', el);
  });
  const hscrolls = $$('[data-hscroll]').map((sec) => ({ sec, track: $('.hscroll__track', sec), bar: $('.hscroll__bar span', sec) }));
  function sizeHScroll() {
    const desktop = innerWidth > 860 && !reduced;
    hscrolls.forEach((h) => {
      if (!desktop) { h.sec.style.height = ''; h.track.style.transform = ''; return; }
      const dist = h.track.scrollWidth - innerWidth;
      h.dist = Math.max(0, dist);
      h.sec.style.height = `${h.dist + innerHeight}px`;
    });
  }

  function onScrollFX() {
    const vh = innerHeight;
    if (!reduced) {
      parallax.forEach((el) => {
        const r = el.getBoundingClientRect();
        if (r.bottom < -200 || r.top > vh + 200) return;
        const speed = parseFloat(el.dataset.parallax) || 0.1;
        const off = (r.top + r.height / 2 - vh / 2) * speed;
        el.style.transform = `translate3d(0, ${(-off).toFixed(1)}px, 0) scale(${el.dataset.scale || 1.04})`;
      });
    }
    scrubs.forEach((el) => {
      const r = el.getBoundingClientRect();
      const p = clamp((vh * 0.85 - r.top) / (r.height + vh * 0.35), 0, 1);
      const n = reduced ? el._words.length : Math.round(p * el._words.length);
      el._words.forEach((w, i) => w.classList.toggle('on', i < n));
    });
    hscrolls.forEach((h) => {
      if (!h.dist) return;
      const r = h.sec.getBoundingClientRect();
      const p = clamp(-r.top / (h.sec.offsetHeight - vh), 0, 1);
      h.track.style.transform = `translate3d(${(-p * h.dist).toFixed(1)}px,0,0)`;
      if (h.bar) h.bar.style.transform = `scaleX(${p})`;
    });
  }
  let ticking = false;
  const onScroll = () => {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(() => { onScrollUI(); onScrollFX(); ticking = false; });
  };
  addEventListener('scroll', onScroll, { passive: true });
  addEventListener('resize', () => { sizeHScroll(); onScroll(); });
  addEventListener('load', () => { sizeHScroll(); onScroll(); });
  sizeHScroll();
  onScrollUI();
  onScrollFX();

  /* ---------- Hero canvas: warp-speed field ---------- */
  const canvas = $('[data-warp]');
  if (canvas) {
    const ctx = canvas.getContext('2d');
    const colors = ['212,255,58', '124,255,178', '139,108,255', '255,255,255'];
    let W, H, DPR, stars = [], visible = true;
    const mouse = { x: 0.5, y: 0.5, tx: 0.5, ty: 0.5 };
    let speed = 0.6, targetSpeed = 0.6, lastScroll = scrollY;
    const N = innerWidth < 700 ? 260 : 600;
    function resize() {
      DPR = Math.min(devicePixelRatio || 1, 2);
      W = canvas.clientWidth; H = canvas.clientHeight;
      canvas.width = W * DPR; canvas.height = H * DPR;
      ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
    }
    const spawn = (s = {}) => {
      s.x = (Math.random() - 0.5) * 2;
      s.y = (Math.random() - 0.5) * 2;
      s.z = Math.random() * 1 + 0.05;
      s.pz = s.z;
      s.c = colors[Math.random() < 0.55 ? 3 : Math.floor(Math.random() * 3)];
      return s;
    };
    resize();
    for (let i = 0; i < N; i++) stars.push(spawn());
    addEventListener('resize', resize);
    addEventListener('pointermove', (e) => { mouse.tx = e.clientX / innerWidth; mouse.ty = e.clientY / innerHeight; }, { passive: true });
    new IntersectionObserver(([en]) => { visible = en.isIntersecting; if (visible) requestAnimationFrame(frame); }).observe(canvas);

    function frame() {
      if (!visible) return;
      mouse.x = lerp(mouse.x, mouse.tx, 0.04);
      mouse.y = lerp(mouse.y, mouse.ty, 0.04);
      const sv = Math.abs(scrollY - lastScroll);
      lastScroll = scrollY;
      targetSpeed = 0.6 + Math.min(sv * 0.08, 4);
      speed = lerp(speed, targetSpeed, 0.06);
      ctx.fillStyle = 'rgba(7,7,10,0.38)';
      ctx.fillRect(0, 0, W, H);
      const cx = W * (0.35 + mouse.x * 0.3);
      const cy = H * (0.35 + mouse.y * 0.3);
      const scale = Math.max(W, H) * 0.5;
      for (const s of stars) {
        s.pz = s.z;
        s.z -= 0.0045 * speed;
        if (s.z <= 0.02) { spawn(s); s.z = 1; s.pz = 1; continue; }
        const sx = cx + (s.x / s.z) * scale, sy = cy + (s.y / s.z) * scale;
        const px = cx + (s.x / s.pz) * scale, py = cy + (s.y / s.pz) * scale;
        if (sx < -50 || sx > W + 50 || sy < -50 || sy > H + 50) { spawn(s); s.z = 1; s.pz = 1; continue; }
        const a = clamp((1 - s.z) * 1.3, 0, 1);
        ctx.strokeStyle = `rgba(${s.c},${a * (s.c === '255,255,255' ? 0.55 : 0.9)})`;
        ctx.lineWidth = (1 - s.z) * 2.2 + 0.3;
        ctx.beginPath();
        ctx.moveTo(px, py);
        ctx.lineTo(sx, sy);
        ctx.stroke();
      }
      if (!reduced) requestAnimationFrame(frame);
    }
    ctx.fillStyle = '#07070a';
    ctx.fillRect(0, 0, W, H);
    if (reduced) { for (let i = 0; i < 60; i++) frame(); } else requestAnimationFrame(frame);
  }

  /* ---------- Pointer effects (desktop only) ---------- */
  if (finePointer && !reduced) {
    doc.classList.add('has-cursor');
    const cur = $('.cursor');
    const dot = $('.cursor__dot');
    const ring = $('.cursor__ring');
    const label = $('.cursor__label');
    const pos = { x: innerWidth / 2, y: innerHeight / 2 }, rpos = { ...pos };
    addEventListener('pointermove', (e) => { pos.x = e.clientX; pos.y = e.clientY; dot.style.transform = `translate(${pos.x}px,${pos.y}px)`; }, { passive: true });
    (function loop() {
      rpos.x = lerp(rpos.x, pos.x, 0.18);
      rpos.y = lerp(rpos.y, pos.y, 0.18);
      ring.style.transform = `translate(${rpos.x}px,${rpos.y}px)`;
      requestAnimationFrame(loop);
    })();
    document.addEventListener('pointerover', (e) => {
      const v = e.target.closest('[data-cursor]');
      const h = e.target.closest('a, button, label, summary, [data-hover]');
      cur.classList.toggle('is-view', !!v);
      cur.classList.toggle('is-hover', !v && !!h);
      label.textContent = v ? v.dataset.cursor : '';
    });
    document.addEventListener('pointerleave', () => cur.style.opacity = '0');
    document.addEventListener('pointerenter', () => cur.style.opacity = '1');

    // Magnetic buttons
    $$('[data-magnetic]').forEach((el) => {
      el.addEventListener('pointermove', (e) => {
        const r = el.getBoundingClientRect();
        const x = e.clientX - r.left - r.width / 2;
        const y = e.clientY - r.top - r.height / 2;
        el.style.transform = `translate(${x * 0.25}px, ${y * 0.35}px)`;
      });
      el.addEventListener('pointerleave', () => { el.style.transform = ''; });
    });

    // 3D tilt
    $$('[data-tilt]').forEach((el) => {
      el.style.transformStyle = 'preserve-3d';
      el.addEventListener('pointermove', (e) => {
        const r = el.getBoundingClientRect();
        const x = (e.clientX - r.left) / r.width - 0.5;
        const y = (e.clientY - r.top) / r.height - 0.5;
        el.style.transform = `perspective(1000px) rotateY(${x * 8}deg) rotateX(${-y * 8}deg) translateZ(0)`;
      });
      el.addEventListener('pointerleave', () => { el.style.transform = ''; });
    });

    // Spotlight cards
    $$('.svc, .pkg, .ccard').forEach((el) => {
      el.addEventListener('pointermove', (e) => {
        const r = el.getBoundingClientRect();
        el.style.setProperty('--mx', `${e.clientX - r.left}px`);
        el.style.setProperty('--my', `${e.clientY - r.top}px`);
      });
    });

    // Text scramble on nav
    const glyphs = '!<>-_\\/[]{}—=+*^?#ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    $$('[data-scramble]').forEach((el) => {
      const orig = el.textContent;
      let raf;
      el.addEventListener('pointerenter', () => {
        let f = 0;
        cancelAnimationFrame(raf);
        const run = () => {
          el.textContent = orig.split('').map((ch, i) => (ch === ' ' || i < f / 2 ? ch : glyphs[(Math.random() * glyphs.length) | 0])).join('');
          f++;
          if (f / 2 <= orig.length) raf = requestAnimationFrame(run);
          else el.textContent = orig;
        };
        run();
      });
    });
  }

  /* ---------- Portfolio filter ---------- */
  const filters = $$('[data-filter]');
  if (filters.length) {
    const items = $$('[data-cat]');
    filters.forEach((btn) =>
      btn.addEventListener('click', () => {
        filters.forEach((b) => { b.classList.toggle('is-active', b === btn); b.setAttribute('aria-pressed', b === btn); });
        const f = btn.dataset.filter;
        btn.closest('section').querySelector('.work-grid')?.classList.toggle('is-filtered', f !== 'all');
        items.forEach((it, i) => {
          const show = f === 'all' || it.dataset.cat.split(' ').includes(f);
          it.classList.toggle('is-hidden', !show);
          it.classList.remove('is-flip');
          if (show) { void it.offsetWidth; it.style.animationDelay = `${(i % 6) * 0.06}s`; it.classList.add('is-flip'); it.classList.add('is-in'); }
        });
        onScroll();
      })
    );
  }

  /* ---------- Slider ---------- */
  $$('[data-slider]').forEach((sl) => {
    const track = $('.slider__track', sl);
    const slides = [...track.children];
    const cur = $('[data-slider-cur]', sl);
    let i = 0, timer;
    const go = (n) => {
      i = (n + slides.length) % slides.length;
      track.style.transform = `translateX(${-i * 100}%)`;
      slides.forEach((s, k) => s.setAttribute('aria-hidden', k !== i));
      if (cur) cur.textContent = String(i + 1).padStart(2, '0');
    };
    const auto = () => { clearInterval(timer); if (!reduced) timer = setInterval(() => go(i + 1), 7000); };
    $('[data-prev]', sl)?.addEventListener('click', () => { go(i - 1); auto(); });
    $('[data-next]', sl)?.addEventListener('click', () => { go(i + 1); auto(); });
    sl.addEventListener('pointerenter', () => clearInterval(timer));
    sl.addEventListener('pointerleave', auto);
    let sx = null;
    sl.addEventListener('touchstart', (e) => (sx = e.touches[0].clientX), { passive: true });
    sl.addEventListener('touchend', (e) => {
      if (sx === null) return;
      const dx = e.changedTouches[0].clientX - sx;
      if (Math.abs(dx) > 40) { go(i + (dx < 0 ? 1 : -1)); auto(); }
      sx = null;
    });
    go(0);
    auto();
  });

  /* ---------- FAQ smooth open ---------- */
  $$('.faq details').forEach((d) => {
    const s = $('summary', d);
    const a = $('.faq__a', d);
    s.addEventListener('click', (e) => {
      if (reduced || !a.animate) return;
      e.preventDefault();
      if (d.open) {
        const h = a.offsetHeight;
        a.animate([{ height: h + 'px', opacity: 1 }, { height: '0px', opacity: 0 }], { duration: 450, easing: 'cubic-bezier(.16,1,.3,1)' }).onfinish = () => (d.open = false);
      } else {
        d.open = true;
        const h = a.offsetHeight;
        a.animate([{ height: '0px', opacity: 0 }, { height: h + 'px', opacity: 1 }], { duration: 600, easing: 'cubic-bezier(.16,1,.3,1)' });
      }
    });
  });

  /* ---------- Forms ---------- */
  const ACCESS_KEY = '__FORM_ACCESS_KEY__';
  const SITE_EMAIL = '__SITE_EMAIL__';
  const emailOk = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v);

  function validate(scope) {
    let ok = true;
    $$('[required]', scope).forEach((f) => {
      const field = f.closest('.field');
      let valid = f.type === 'email' ? emailOk(f.value.trim()) : f.value.trim().length > 0;
      if (f.type === 'checkbox') valid = f.checked;
      field?.classList.toggle('is-invalid', !valid);
      if (!valid && ok) { f.focus(); ok = false; }
    });
    $$('[data-required-group]', scope).forEach((g) => {
      const valid = !!$('input:checked', g);
      const err = $('.field__err', g.parentElement);
      g.parentElement.classList.toggle('is-invalid', !valid);
      if (err) err.style.display = valid ? '' : 'block';
      if (!valid && ok) { ok = false; g.scrollIntoView({ behavior: reduced ? 'auto' : 'smooth', block: 'center' }); }
    });
    return ok;
  }
  $$('.field input, .field textarea, .field select').forEach((f) =>
    f.addEventListener('input', () => f.closest('.field').classList.remove('is-invalid'))
  );
  $$('[data-required-group] input').forEach((i) =>
    i.addEventListener('change', () => { const p = i.closest('[data-required-group]').parentElement; p.classList.remove('is-invalid'); const e = $('.field__err', p); if (e) e.style.display = ''; })
  );

  function collect(form) {
    const data = {};
    new FormData(form).forEach((v, k) => { data[k] = data[k] ? `${data[k]}, ${v}` : v; });
    return data;
  }

  async function submit(form, onSuccess) {
    const status = $('.form-status', form);
    const btn = $('[type=submit]', form);
    if ($('.hp input', form)?.value) return; // bot
    const data = collect(form);
    delete data.botcheck;
    status.className = 'form-status';
    if (ACCESS_KEY.startsWith('YOUR_') || ACCESS_KEY.startsWith('__')) {
      // Not configured yet: fall back to the visitor's email app
      const body = Object.entries(data).map(([k, v]) => `${k}: ${v}`).join('\n');
      location.href = `mailto:${SITE_EMAIL}?subject=${encodeURIComponent(data.subject || 'New enquiry — The Fast Designs')}&body=${encodeURIComponent(body)}`;
      onSuccess?.();
      return;
    }
    btn.classList.add('is-loading');
    btn.disabled = true;
    try {
      const res = await fetch('https://api.web3forms.com/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ access_key: ACCESS_KEY, from_name: 'The Fast Designs website', ...data }),
      });
      const json = await res.json().catch(() => ({}));
      if (!res.ok || json.success === false) throw new Error(json.message || 'Request failed');
      form.reset();
      if (onSuccess) onSuccess();
      else { status.textContent = 'Thanks! Your message is in. We reply within one business day.'; status.classList.add('is-ok'); }
    } catch (err) {
      status.innerHTML = `Something went wrong sending your message. Please email us directly at <a href="mailto:${SITE_EMAIL}"><u>${SITE_EMAIL}</u></a>.`;
      status.classList.add('is-err');
    } finally {
      btn.classList.remove('is-loading');
      btn.disabled = false;
    }
  }

  const contact = $('[data-form="contact"]');
  contact?.addEventListener('submit', (e) => { e.preventDefault(); if (validate(contact)) submit(contact); });

  // Multi-step quote form
  const quote = $('[data-form="quote"]');
  if (quote) {
    const steps = $$('.qstep', quote);
    const marks = $$('.qsteps li');
    const done = $('.qdone');
    let cur = 0;
    const show = (n, focus = true) => {
      cur = n;
      steps.forEach((s, i) => s.classList.toggle('is-active', i === n));
      marks.forEach((m, i) => { m.classList.toggle('is-current', i === n); m.classList.toggle('is-done', i < n); });
      const top = quote.closest('.panel').getBoundingClientRect().top + scrollY - 110;
      if (focus && scrollY > top) scrollTo({ top, behavior: reduced ? 'auto' : 'smooth' });
      const first = $('input, textarea, select', steps[n]);
      if (focus && first) setTimeout(() => first.focus({ preventScroll: true }), 300);
    };
    $$('[data-next]', quote).forEach((b) => b.addEventListener('click', () => { if (validate(steps[cur])) show(cur + 1); }));
    $$('[data-back]', quote).forEach((b) => b.addEventListener('click', () => show(cur - 1)));
    marks.forEach((m, i) => $('button', m).addEventListener('click', () => { if (i < cur) show(i); }));
    // Pre-select service from ?service=
    const pre = new URLSearchParams(location.search).get('service');
    if (pre) { const box = $(`input[name="services"][value="${CSS.escape(pre)}"]`, quote); if (box) box.checked = true; }
    quote.addEventListener('submit', (e) => {
      e.preventDefault();
      if (!validate(steps[cur])) return;
      submit(quote, () => {
        quote.hidden = true;
        $('.qsteps').hidden = true;
        done.classList.add('is-active');
      });
    });
    show(0, false);
  }
})();
