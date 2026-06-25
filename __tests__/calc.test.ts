/**
 * @format
 */
import { BASE, project } from '../src/calc';

test('no growth when return is 0% — balance is just contributions + match', () => {
  const r = project({ ...BASE, returnPct: 0 });
  expect(r.years).toBe(BASE.retireAge - BASE.currentAge);
  expect(Math.round(r.growth)).toBe(0);
  expect(Math.round(r.final)).toBe(Math.round(r.youTotal + r.matchTotal));
});

test('positive return produces growth and a higher balance', () => {
  const r = project(BASE);
  expect(r.final).toBeGreaterThan(r.youTotal + r.matchTotal);
  expect(r.growth).toBeGreaterThan(0);
  expect(r.monthly).toBeCloseTo((r.final * 0.04) / 12, 5);
});

test('series spans current age through retirement age', () => {
  const r = project(BASE);
  expect(r.series[0].age).toBe(BASE.currentAge);
  expect(r.series[r.series.length - 1].age).toBe(BASE.retireAge);
});

test('retiring at the current age yields the starting balance', () => {
  const r = project({ ...BASE, retireAge: BASE.currentAge });
  expect(r.years).toBe(0);
  expect(r.final).toBe(BASE.balance);
});
