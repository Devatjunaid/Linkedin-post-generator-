// Renders every mockup to PNG with the pre-installed Chromium.  Usage: node render.js [screenName...]
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const { css } = require('./lib');
const screens = require('./screens');
const extras = fs.existsSync(path.join(__dirname, 'extras.js')) ? require('./extras') : {};

const OUT = path.join(__dirname, '..');
const BUILD = path.join(__dirname, 'build');
fs.mkdirSync(BUILD, { recursive: true });

const jobs = [
  ['01-home', screens.home],
  ['02-adventure-map', screens.mapScreen],
  ['03-robot-workshop', screens.workshop],
  ['04-reward-stars', screens.reward],
  ['05-robot-garage', screens.garage],
  ['06-settings', screens.settings],
  ...(extras.jobs || [])
];

(async () => {
  const only = process.argv.slice(2);
  const browser = await chromium.launch();
  for (const [name, fn, size = [540, 960], scale = 2] of jobs) {
    if (only.length && !only.some(o => name.includes(o))) continue;
    const html = `<!doctype html><html><head><meta charset="utf-8"><style>${css('file://' + __dirname)}
      body{width:${size[0]}px;height:${size[1]}px}</style></head><body>${fn()}</body></html>`;
    const file = path.join(BUILD, name + '.html');
    fs.writeFileSync(file, html);
    const page = await browser.newPage({ viewport: { width: size[0], height: size[1] }, deviceScaleFactor: scale });
    await page.goto('file://' + file);
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(150);
    await page.screenshot({ path: path.join(OUT, name + '.png') });
    await page.close();
    console.log('rendered', name);
  }
  await browser.close();
})();
