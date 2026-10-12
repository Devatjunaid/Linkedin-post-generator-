// Local preview server with clean URLs (mirrors Cloudflare behaviour). Usage: node serve.mjs [port]
import http from 'node:http';
import { promises as fs } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const DIST = path.join(path.dirname(fileURLToPath(import.meta.url)), 'dist');
const TYPES = { '.html': 'text/html; charset=utf-8', '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml', '.png': 'image/png', '.xml': 'application/xml', '.txt': 'text/plain', '.ico': 'image/x-icon', '.webmanifest': 'application/manifest+json' };
const port = +process.argv[2] || 8080;
http.createServer(async (req, res) => {
  let p = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  const tries = p.endsWith('/') ? [p + 'index.html'] : [p, p + '.html', p + '/index.html'];
  for (const t of tries) {
    const f = path.join(DIST, path.normalize(t));
    if (!f.startsWith(DIST)) break;
    try {
      const st = await fs.stat(f);
      if (!st.isFile()) continue;
      res.writeHead(200, { 'Content-Type': TYPES[path.extname(f)] || 'application/octet-stream' });
      return res.end(await fs.readFile(f));
    } catch {}
  }
  res.writeHead(404, { 'Content-Type': TYPES['.html'] });
  res.end(await fs.readFile(path.join(DIST, '404.html')).catch(() => 'Not found'));
}).listen(port, () => console.log(`Preview → http://localhost:${port}`));
