// Renders scene.html frame by frame with headless Chromium and pipes JPEG frames into ffmpeg.
//   node render.js preview 1.5 10 31 ...     -> build/preview/t_<sec>.png (960x540)
//   node render.js video [--scale 2] [--workers 3] [--fps 30] [--from 0] [--to 270]
// Video mode writes 10-second segments to build/segments/ (already-finished segments are skipped,
// so an interrupted render can be resumed) and then build/video_only.mp4.
const { chromium } = require('playwright');
const { spawn, execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const HERE = __dirname;
const BUILD = path.join(HERE, 'build');
const args = process.argv.slice(2);
const opt = (name, def) => { const i = args.indexOf('--' + name); return i >= 0 ? Number(args[i + 1]) : def; };

async function openPage(browser, scale) {
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, deviceScaleFactor: scale });
  page.on('pageerror', e => { console.error('PAGE ERROR', e.message); process.exitCode = 1; });
  page.on('console', m => { if (m.type() === 'error') console.error('console:', m.text()); });
  await page.goto('file://' + path.join(HERE, 'scene.html') + '?scale=' + scale);
  await page.evaluate(() => window.ready);
  return page;
}

async function preview(times) {
  const browser = await chromium.launch();
  const page = await openPage(browser, 0.5);
  const dir = path.join(BUILD, 'preview');
  fs.mkdirSync(dir, { recursive: true });
  for (const t of times) {
    await page.evaluate(t => window.renderFrame(t), t);
    await page.screenshot({ path: path.join(dir, `t_${String(t).padStart(6, '0')}.png`) });
  }
  await browser.close();
}

async function renderSegment(page, seg, fps, segLen, scale, out) {
  const tmp = out + '.part.mp4';
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(fps), '-c:v', 'mjpeg', '-i', '-',
    '-c:v', 'libx264', '-preset', 'medium', '-crf', '17', '-pix_fmt', 'yuv420p', '-threads', '2',
    '-g', String(fps * 2), '-r', String(fps), '-movflags', '+faststart', tmp], { stdio: ['pipe', 'inherit', 'inherit'] });
  const done = new Promise((res, rej) => ff.on('close', c => c === 0 ? res() : rej(new Error('ffmpeg exit ' + c))));
  const frames = Math.round(segLen * fps);
  for (let f = 0; f < frames; f++) {
    const t = seg * segLen + f / fps;
    await page.evaluate(t => window.renderFrame(t), t);
    const buf = await page.screenshot({ type: 'jpeg', quality: 94 });
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
  }
  ff.stdin.end();
  await done;
  fs.renameSync(tmp, out);
}

async function video() {
  const scale = opt('scale', 2), workers = opt('workers', 3), fps = opt('fps', 30);
  const from = opt('from', 0), to = opt('to', 270), segLen = 10;
  const segDir = path.join(BUILD, 'segments');
  fs.mkdirSync(segDir, { recursive: true });
  const todo = [];
  for (let s = Math.floor(from / segLen); s < Math.ceil(to / segLen); s++) {
    const out = path.join(segDir, `seg_${String(s).padStart(3, '0')}.mp4`);
    if (!fs.existsSync(out)) todo.push([s, out]);
  }
  console.log(`${todo.length} segments to render at ${1920 * scale}x${1080 * scale}, ${workers} workers`);
  const browser = await chromium.launch();
  const t0 = Date.now();
  let finished = 0;
  await Promise.all(Array.from({ length: workers }, async () => {
    const page = await openPage(browser, scale);
    while (todo.length) {
      const [s, out] = todo.shift();
      await renderSegment(page, s, fps, segLen, scale, out);
      finished++;
      const el = (Date.now() - t0) / 1000;
      console.log(`segment ${s} done  (${finished} this run, ${el.toFixed(0)}s elapsed, ~${(el / finished * todo.length / 60).toFixed(1)} min left)`);
    }
    await page.close();
  }));
  await browser.close();
  const list = fs.readdirSync(segDir).filter(f => /^seg_\d+\.mp4$/.test(f)).sort();
  fs.writeFileSync(path.join(segDir, 'list.txt'), list.map(f => `file '${f}'`).join('\n') + '\n');
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', path.join(segDir, 'list.txt'), '-c', 'copy', path.join(BUILD, 'video_only.mp4')], { stdio: 'inherit' });
  console.log('wrote build/video_only.mp4');
}

(async () => {
  if (args[0] === 'preview') await preview(args.slice(1).map(Number));
  else if (args[0] === 'video') await video();
  else { console.error('usage: node render.js preview <t...> | video [--scale 2] [--workers 3]'); process.exit(2); }
})().catch(e => { console.error(e); process.exit(1); });
