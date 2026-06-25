/* ---------- palette + formatting helpers ---------- */

export const C = {
  bg: '#0E1318',
  surface: '#181F27',
  surface2: '#202934',
  line: 'rgba(255,255,255,0.07)',
  gold: '#E8B04B',
  goldDeep: '#C8923A',
  text: '#F3F5F8',
  muted: '#8A94A3',
  blue: '#7BA9D6',
  green: '#4ADE80',
} as const;

export const money = (n: number) =>
  '$' + Math.round(n).toLocaleString('en-US');

export const moneyShort = (n: number) =>
  n >= 1e6
    ? '$' + (n / 1e6).toFixed(n >= 1e7 ? 0 : 1) + 'M'
    : '$' + Math.round(n / 1e3) + 'k';
