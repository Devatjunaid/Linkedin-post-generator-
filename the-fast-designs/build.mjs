// The Fast Designs — zero-dependency static site builder.
// Usage: node build.mjs  →  outputs the deployable site into ./dist
import { promises as fs } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { SITE, NAV } from './site.config.mjs';
import { renderArt } from './src/art.mjs';

const ROOT = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.join(ROOT, 'src');
const PUBLIC = path.join(ROOT, 'public');
const DIST = path.join(ROOT, 'dist');

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');

async function copyDir(from, to) {
  await fs.mkdir(to, { recursive: true });
  for (const entry of await fs.readdir(from, { withFileTypes: true })) {
    const a = path.join(from, entry.name);
    const b = path.join(to, entry.name);
    if (entry.isDirectory()) await copyDir(a, b);
    else await fs.copyFile(a, b);
  }
}

// Each page starts with <!--meta { ...json... } -->
function parsePage(raw, file) {
  const m = raw.match(/^<!--meta\s*([\s\S]*?)-->\s*/);
  if (!m) throw new Error(`Missing meta block in ${file}`);
  return { meta: JSON.parse(m[1]), body: raw.slice(m[0].length) };
}

const logoMark = `<svg class="logo__mark" viewBox="0 0 48 48" aria-hidden="true"><defs><linearGradient id="lg-${'$'}{id}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#D4FF3A"/><stop offset="1" stop-color="#7CFFB2"/></linearGradient></defs><rect width="48" height="48" rx="13" fill="#111217"/><path d="M17 12h20l-3 6H23.6l-1.7 4H31l-3 6h-8.6L15 38h-6.5z" fill="url(#lg-${'$'}{id})"/><path d="M5 20h8M3 26h7M6 32h5" stroke="#D4FF3A" stroke-width="2.4" stroke-linecap="round" opacity=".55"/></svg>`;
const logo = (id) => logoMark.replaceAll('${id}', id);

function header(current) {
  const links = NAV.map(
    (n) =>
      `<li><a href="${n.href}" class="nav__link${n.href === current ? ' is-active' : ''}"${n.href === current ? ' aria-current="page"' : ''} data-scramble>${n.label}</a></li>`
  ).join('');
  return `<a class="skip" href="#main">Skip to content</a>
<div class="progress" aria-hidden="true"><span></span></div>
<header class="header" data-header>
  <div class="container header__inner">
    <a href="/" class="logo" aria-label="${SITE.name} — home">${logo('h')}<span class="logo__text">The Fast<b>Designs</b></span></a>
    <nav class="nav" aria-label="Primary">
      <ul class="nav__list">${links}</ul>
    </nav>
    <a href="/get-a-quote" class="btn btn--primary btn--sm header__cta" data-magnetic><span>Get a Quote</span><i class="btn__arrow" aria-hidden="true">→</i></a>
    <button class="burger" aria-label="Open menu" aria-expanded="false" aria-controls="mobile-menu" data-burger><span></span><span></span></button>
  </div>
</header>
<div class="mmenu" id="mobile-menu" data-mmenu hidden>
  <nav aria-label="Mobile">
    <ul>${NAV.map((n, i) => `<li style="--i:${i}"><a href="${n.href}">${n.label}</a></li>`).join('')}
      <li style="--i:${NAV.length}"><a href="/get-a-quote" class="mmenu__cta">Get a Quote →</a></li>
    </ul>
  </nav>
  <div class="mmenu__foot"><a href="mailto:${SITE.email}">${SITE.email}</a><a href="tel:${SITE.phoneHref}">${SITE.phone}</a></div>
</div>`;
}

function footer() {
  const year = new Date().getFullYear();
  return `<footer class="footer">
  <div class="container">
    <div class="footer__cta" data-reveal>
      <p class="eyebrow">Have a project in mind?</p>
      <a href="/get-a-quote" class="footer__big" ><span>Let’s make it</span> <em>fast.</em><span class="footer__big-arrow" aria-hidden="true">↗</span></a>
    </div>
    <div class="footer__grid">
      <div class="footer__brand">
        <a href="/" class="logo" aria-label="${SITE.name} — home">${logo('f')}<span class="logo__text">The Fast<b>Designs</b></span></a>
        <p>${SITE.name} is a web design and development agency building high‑performance websites, e‑commerce stores and web apps for ambitious brands worldwide.</p>
      </div>
      <div>
        <h2 class="footer__h">Explore</h2>
        <ul>${NAV.map((n) => `<li><a href="${n.href}">${n.label}</a></li>`).join('')}<li><a href="/get-a-quote">Get a Quote</a></li></ul>
      </div>
      <div>
        <h2 class="footer__h">Services</h2>
        <ul>
          <li><a href="/services#web-design">Website Design</a></li>
          <li><a href="/services#web-development">Web Development</a></li>
          <li><a href="/services#ecommerce">E‑commerce</a></li>
          <li><a href="/services#ui-ux">UI/UX Design</a></li>
          <li><a href="/services#seo">SEO &amp; Performance</a></li>
          <li><a href="/services#care">Care &amp; Growth</a></li>
        </ul>
      </div>
      <div>
        <h2 class="footer__h">Contact</h2>
        <ul>
          <li><a href="mailto:${SITE.email}">${SITE.email}</a></li>
          <li><a href="tel:${SITE.phoneHref}">${SITE.phone}</a></li>
          <li><a href="${SITE.whatsapp}" target="_blank" rel="noopener">WhatsApp us</a></li>
          <li>${SITE.location}</li>
        </ul>
      </div>
    </div>
    <div class="footer__bottom">
      <p>© ${year} ${SITE.name}. All rights reserved.</p>
      <ul class="social">
        ${SITE.social.map((s) => `<li><a href="${s.href}" target="_blank" rel="noopener" aria-label="${s.label}">${s.short}</a></li>`).join('')}
      </ul>
      <button class="totop" data-totop aria-label="Back to top">↑ Top</button>
    </div>
  </div>
  <div class="footer__word" aria-hidden="true">FAST</div>
</footer>`;
}

function layout({ meta, body, route }) {
  const url = SITE.url + (route === '/' ? '/' : route);
  const title = meta.title;
  const og = SITE.url + '/assets/img/og.png';
  const breadcrumbs =
    route === '/'
      ? null
      : {
          '@context': 'https://schema.org',
          '@type': 'BreadcrumbList',
          itemListElement: [
            { '@type': 'ListItem', position: 1, name: 'Home', item: SITE.url + '/' },
            { '@type': 'ListItem', position: 2, name: meta.crumb || title, item: url },
          ],
        };
  const org = {
    '@context': 'https://schema.org',
    '@type': 'ProfessionalService',
    name: SITE.name,
    url: SITE.url + '/',
    logo: SITE.url + '/assets/img/logo.svg',
    image: og,
    email: SITE.email,
    telephone: SITE.phone,
    description: SITE.description,
    areaServed: 'Worldwide',
    sameAs: SITE.social.map((s) => s.href),
    knowsAbout: ['Web Design', 'Web Development', 'E-commerce Development', 'UI/UX Design', 'Search Engine Optimization', 'Core Web Vitals'],
  };
  const schemas = [org, breadcrumbs, ...(meta.schema || [])].filter(Boolean);

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)}</title>
<meta name="description" content="${esc(meta.description)}">
<link rel="canonical" href="${url}">
<meta name="robots" content="${meta.noindex ? 'noindex, follow' : 'index, follow, max-image-preview:large'}">
<meta name="theme-color" content="#07070A">
<meta property="og:type" content="website">
<meta property="og:site_name" content="${SITE.name}">
<meta property="og:title" content="${esc(title)}">
<meta property="og:description" content="${esc(meta.description)}">
<meta property="og:url" content="${url}">
<meta property="og:image" content="${og}">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="${esc(title)}">
<meta name="twitter:description" content="${esc(meta.description)}">
<meta name="twitter:image" content="${og}">
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/assets/img/apple-touch-icon.png">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Syne:wght@500;600;700;800&display=swap" rel="stylesheet">
<link rel="stylesheet" href="/assets/css/main.css?v=${BUILD}">
${schemas.map((s) => `<script type="application/ld+json">${JSON.stringify(s)}</script>`).join('\n')}
</head>
<body class="page-${meta.slug}" data-page="${meta.slug}">
<div class="loader" data-loader aria-hidden="true">
  <div class="loader__inner">
    <div class="loader__lines"><i></i><i></i><i></i><i></i><i></i></div>
    <div class="loader__word">The Fast Designs</div>
    <div class="loader__count" data-loader-count>00</div>
  </div>
</div>
<div class="transition" data-transition aria-hidden="true"><i></i><i></i><i></i><i></i><i></i></div>
<div class="cursor" aria-hidden="true"><div class="cursor__dot"></div><div class="cursor__ring"><span class="cursor__label"></span></div></div>
<div class="noise" aria-hidden="true"></div>
<svg width="0" height="0" style="position:absolute" aria-hidden="true" focusable="false"><defs><linearGradient id="ig" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#D4FF3A"/><stop offset="1" stop-color="#7CFFB2"/></linearGradient><linearGradient id="gaugeGrad" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#D4FF3A"/><stop offset="1" stop-color="#7CFFB2"/></linearGradient></defs></svg>
${header(route)}
<main id="main">
${body}
</main>
${footer()}
<script src="/assets/js/main.js?v=${BUILD}" defer></script>
</body>
</html>
`;
}

const BUILD = Date.now().toString(36);

async function build() {
  await fs.rm(DIST, { recursive: true, force: true });
  await copyDir(PUBLIC, DIST);

  // Generated artwork (portfolio mockups, charts, OG source)
  const art = renderArt();
  await fs.mkdir(path.join(DIST, 'assets/img/work'), { recursive: true });
  for (const [file, svg] of Object.entries(art)) {
    await fs.mkdir(path.dirname(path.join(DIST, 'assets/img', file)), { recursive: true });
    await fs.writeFile(path.join(DIST, 'assets/img', file), svg);
  }

  const pages = (await fs.readdir(path.join(SRC, 'pages'))).filter((f) => f.endsWith('.html'));
  const routes = [];
  for (const file of pages) {
    const raw = await fs.readFile(path.join(SRC, 'pages', file), 'utf8');
    const { meta, body } = parsePage(raw, file);
    const route = meta.route;
    let html = layout({ meta, body: body.replaceAll('{{EMAIL}}', SITE.email).replaceAll('{{PHONE}}', SITE.phone).replaceAll('{{PHONE_HREF}}', SITE.phoneHref).replaceAll('{{WHATSAPP}}', SITE.whatsapp).replaceAll('{{LOCATION}}', SITE.location), route });
    const out = route === '/' ? 'index.html' : route === '/404' ? '404.html' : route.slice(1) + '.html';
    await fs.writeFile(path.join(DIST, out), html);
    if (route !== '/404') routes.push({ route, priority: meta.priority ?? 0.8 });
  }

  const today = new Date().toISOString().slice(0, 10);
  const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${routes
  .sort((a, b) => b.priority - a.priority)
  .map((r) => `  <url><loc>${SITE.url}${r.route === '/' ? '/' : r.route}</loc><lastmod>${today}</lastmod><priority>${r.priority}</priority></url>`)
  .join('\n')}
</urlset>
`;
  await fs.writeFile(path.join(DIST, 'sitemap.xml'), sitemap);
  await fs.writeFile(path.join(DIST, 'robots.txt'), `User-agent: *\nAllow: /\n\nSitemap: ${SITE.url}/sitemap.xml\n`);

  // Inject runtime config for the forms
  const jsPath = path.join(DIST, 'assets/js/main.js');
  const js = await fs.readFile(jsPath, 'utf8');
  await fs.writeFile(jsPath, js.replace('__FORM_ACCESS_KEY__', SITE.formAccessKey).replaceAll('__SITE_EMAIL__', SITE.email));

  console.log(`✔ Built ${pages.length} pages + ${Object.keys(art).length} graphics → dist/`);
}

build().catch((e) => {
  console.error(e);
  process.exit(1);
});
