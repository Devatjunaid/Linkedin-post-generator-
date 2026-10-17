// Renders reel.html frame by frame with headless Chromium and pipes JPEG frames into ffmpeg.
//   node render.js preview 1.2 3 9 ...   -> build/preview/t_<sec>.png (540x960)
//   node render.js video [--workers 3]   -> build/video_only.mp4 (1080x1920, 30 fps)
const { chromium } = require('playwright');
const { spawn, execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const HERE = __dirname, BUILD = path.join(HERE, 'build');
const spec = JSON.parse(fs.readFileSync(path.join(HERE, 'script.json'), 'utf8'));
const WORDS = JSON.parse(fs.readFileSync(path.join(BUILD, 'words.json'), 'utf8'));
const args = process.argv.slice(2);
const opt = (name, def) => { const i = args.indexOf('--' + name); return i >= 0 ? Number(args[i + 1]) : def; };

async function openPage(browser, scale) {
  const page = await browser.newPage({ viewport: { width: spec.width, height: spec.height }, deviceScaleFactor: scale });
  page.on('pageerror', e => { console.error('PAGE ERROR', e.message); process.exitCode = 1; });
  await page.addInitScript(w => { window.WORDS = w; }, WORDS);
  await page.goto('file://' + path.join(HERE, 'reel.html') + '?scale=' + scale);
  await page.evaluate(() => window.ready);
  return page;
}

async function preview(times) {
  const browser = await chromium.launch();
  const page = await openPage(browser, 0.5);
  fs.mkdirSync(path.join(BUILD, 'preview'), { recursive: true });
  for (const t of times) {
    await page.evaluate(t => window.renderFrame(t), t);
    await page.screenshot({ path: path.join(BUILD, 'preview', `t_${t.toFixed(2).padStart(5, '0')}.png`) });
  }
  await browser.close();
}

async function renderSegment(page, start, frames, fps, out) {
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(fps), '-c:v', 'mjpeg', '-i', '-',
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '16', '-pix_fmt', 'yuv420p', '-threads', '2', '-g', String(fps), '-r', String(fps), out],
    { stdio: ['pipe', 'inherit', 'inherit'] });
  const done = new Promise((res, rej) => ff.on('close', c => c === 0 ? res() : rej(new Error('ffmpeg exit ' + c))));
  for (let f = 0; f < frames; f++) {
    await page.evaluate(t => window.renderFrame(t), (start + f) / fps);
    const buf = await page.screenshot({ type: 'jpeg', quality: 95 });
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
  }
  ff.stdin.end();
  await done;
}

async function video() {
  const fps = spec.fps, total = Math.round(spec.duration * fps), workers = opt('workers', 3);
  const segFrames = Math.ceil(total / workers);
  const dir = path.join(BUILD, 'segments');
  fs.rmSync(dir, { recursive: true, force: true }); fs.mkdirSync(dir, { recursive: true });
  const browser = await chromium.launch();
  const segs = [];
  await Promise.all(Array.from({ length: workers }, async (_, i) => {
    const start = i * segFrames, frames = Math.min(segFrames, total - start);
    const out = path.join(dir, `seg_${i}.mp4`); segs[i] = out;
    const page = await openPage(browser, 1);
    await renderSegment(page, start, frames, fps, out);
    console.log(`segment ${i} done (${frames} frames)`);
  }));
  await browser.close();
  fs.writeFileSync(path.join(dir, 'list.txt'), segs.map(s => `file '${s}'`).join('\n') + '\n');
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', path.join(dir, 'list.txt'), '-c', 'copy', path.join(BUILD, 'video_only.mp4')], { stdio: 'inherit' });
  console.log('wrote build/video_only.mp4');
}

(async () => {
  if (args[0] === 'preview') await preview(args.slice(1).map(Number));
  else if (args[0] === 'video') await video();
  else { console.error('usage: node render.js preview <t...> | video [--workers 3]'); process.exit(2); }
})().catch(e => { console.error(e); process.exit(1); });
