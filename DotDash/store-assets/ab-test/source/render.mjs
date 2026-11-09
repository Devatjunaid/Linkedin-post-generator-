// Renders the store art in art.html to PNGs. Usage: node render.mjs <outDir> [ids...]
import { chromium } from '/opt/node22/lib/node_modules/playwright/index.mjs';
import fs from 'node:fs';
import path from 'node:path';

const here = path.dirname(new URL(import.meta.url).pathname);
const game = path.resolve(here, '../../../../dot-dash');
const out = process.argv[2];
const only = process.argv.slice(3);
fs.mkdirSync(out, { recursive: true });

const types = { '.js': 'text/javascript', '.html': 'text/html', '.woff2': 'font/woff2' };
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1200, height: 2000 } });
page.on('console', (m) => console.log('page:', m.text()));
page.on('pageerror', (e) => console.log('ERR:', e.message));
await page.route('http://art.local/**', (route) => {
  const u = new URL(route.request().url()).pathname;
  let f;
  if (u.startsWith('/src/')) f = path.join(game, u);
  else if (u.startsWith('/fonts/')) f = path.join(game, 'www', u);
  else f = path.join(here, u);
  if (!fs.existsSync(f)) return route.fulfill({ status: 404, body: 'nf' });
  route.fulfill({ body: fs.readFileSync(f), contentType: types[path.extname(f)] || 'application/octet-stream' });
});
await page.goto('http://art.local/art.html');
await page.waitForFunction(() => window.ready === true);
await page.evaluate(() => document.fonts.ready);
await page.waitForTimeout(300);
const ids = await page.$$eval('.asset', (els) => els.map((e) => e.id));
for (const id of ids) {
  if (only.length && !only.includes(id)) continue;
  const el = await page.$('#' + id);
  await el.screenshot({ path: path.join(out, id + '.png'), omitBackground: false });
  console.log('rendered', id);
}
await browser.close();
