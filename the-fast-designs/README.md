# The Fast Designs — Agency Website

A fast, dark, animation-rich website for **The Fast Designs**, a web design and development agency.
It is plain HTML, CSS and JavaScript with **no frameworks and no dependencies**, built for Cloudflare.

**Pages:** Home · Services · Portfolio · Case Studies · Testimonials · Contact · Get a Quote · 404

## Features

- **Motion:** preloader, page-to-page panel transitions, a hero "warp-speed" canvas that reacts to the mouse and to scroll speed, a rotating headline, split-text reveals, scroll-scrubbed statement text, a horizontal-scroll process section, parallax, magnetic buttons, a custom cursor, 3D tilt cards, spotlight hover, text scramble, animated counters, Lighthouse-style gauges and marquees.
- **Original graphics:** every portfolio mockup, case-study chart and illustration is generated as SVG in code (`src/art.mjs`). There are no stock images.
- **SEO:** a unique title and meta description per page, canonical URLs, Open Graph and Twitter cards, JSON-LD schema (ProfessionalService, BreadcrumbList, Service list, FAQPage), an auto-generated `sitemap.xml` and `robots.txt`, semantic HTML and descriptive alt text.
- **Accessibility:** keyboard-friendly, skip link, focus styles, ARIA labels. Respects `prefers-reduced-motion`, which turns animations off.
- **Forms:** a contact form and a 4-step quote form, both with validation. They send email through Web3Forms (free).
- **Performance:** no frameworks, cache headers, lazy-loaded images, and graphics drawn in code.

## Project structure

```
site.config.mjs     ← EDIT: domain, email, phone, WhatsApp, socials, form key
build.mjs           ← builds everything into /dist
serve.mjs           ← local preview server
src/pages/*.html    ← page content (one file per page, SEO meta at the top)
src/projects.mjs    ← portfolio projects (drives the generated mockups)
src/art.mjs         ← SVG artwork generator
public/             ← CSS, JS, favicon, social image (copied as-is)
wrangler.jsonc      ← Cloudflare config
```

## Run locally

```bash
npm run dev        # builds and serves at http://localhost:8080
```

You need Node 18 or newer. There is nothing to install.

## Before going live: checklist

1. **`site.config.mjs`**: set your real `url` (your domain), `email`, `phone`, `whatsapp`, `location` and social links.
2. **Forms**: get a free access key at <https://web3forms.com> (enter the email that should receive leads) and paste it into `formAccessKey`. Until you do, the forms open the visitor's email app instead.
3. **Sample content**: the portfolio projects, case studies, testimonials, client names and statistics (150+ projects, 98%, etc.) are **placeholders written to show the layout**. Replace them with your real work, real client quotes and real numbers before you launch. Publishing made-up reviews or results can mislead customers and breach advertising rules.
4. Rebuild with `npm run build`.

## Deploy to Cloudflare

**Option A: Git integration (recommended, auto-deploys on every push)**

1. Push this folder to its own GitHub repository.
2. In the Cloudflare dashboard go to **Workers & Pages → Create → Import a repository** and pick the repo.
3. Set **Build command** to `npm run build` and **Deploy command** to `npx wrangler deploy`. Leave root directory as `/`.
4. Deploy. You'll get a `*.workers.dev` preview URL.
5. In the project open **Settings → Domains & Routes → Add → Custom domain**, enter `yourdomain.com`, then repeat for `www.yourdomain.com`. Cloudflare creates the DNS records and SSL certificate for you, as long as the domain is already active on Cloudflare.

**Option B: From your computer**

```bash
npm run build
npx wrangler login
npx wrangler deploy
```

## Editing content

- **Text:** edit the matching file in `src/pages/`. The JSON block at the top of each file controls its SEO title and description.
- **Portfolio graphics:** add or edit entries in `src/projects.mjs` (colours, headline, layout `saas | dash | shop | editorial | mobile`), then rebuild.
- **Colours and fonts:** edit the CSS variables at the top of `public/assets/css/main.css`.
