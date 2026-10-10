// Copies the two game fonts into www/fonts so the app works fully offline.
import { copyFileSync, mkdirSync } from 'node:fs';

const files = [
  ['@fontsource/fredoka/files/fredoka-latin-500-normal.woff2', 'fredoka-500.woff2'],
  ['@fontsource/fredoka/files/fredoka-latin-600-normal.woff2', 'fredoka-600.woff2'],
  ['@fontsource/fredoka/files/fredoka-latin-700-normal.woff2', 'fredoka-700.woff2'],
  ['@fontsource/nunito/files/nunito-latin-700-normal.woff2', 'nunito-700.woff2'],
  ['@fontsource/nunito/files/nunito-latin-800-normal.woff2', 'nunito-800.woff2'],
  ['@fontsource/nunito/files/nunito-latin-900-normal.woff2', 'nunito-900.woff2'],
];

mkdirSync('www/fonts', { recursive: true });
for (const [from, to] of files) copyFileSync(`node_modules/${from}`, `www/fonts/${to}`);
console.log(`Copied ${files.length} fonts to www/fonts`);
