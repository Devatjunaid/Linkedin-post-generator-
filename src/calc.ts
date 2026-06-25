/* ---------- 401(k) projection ---------- */

export type Inputs = {
  currentAge: number;
  retireAge: number;
  salary: number;
  balance: number;
  contribPct: number;
  matchPct: number;
  returnPct: number;
};

export type Projection = {
  years: number;
  final: number;
  youTotal: number;
  matchTotal: number;
  growth: number;
  monthly: number;
  series: { age: number; bal: number }[];
};

export const BASE: Inputs = {
  currentAge: 30,
  retireAge: 65,
  salary: 75000,
  balance: 25000,
  contribPct: 6,
  matchPct: 3,
  returnPct: 7,
};

export function project(i: Inputs): Projection {
  const years = Math.max(0, i.retireAge - i.currentAge);
  const annualContrib = i.salary * (i.contribPct / 100);
  const annualMatch = i.salary * (i.matchPct / 100);
  const r = i.returnPct / 100;

  let bal = i.balance;
  let youTotal = i.balance;
  let matchTotal = 0;
  const series = [{ age: i.currentAge, bal }];

  for (let y = 1; y <= years; y++) {
    bal = bal * (1 + r) + annualContrib + annualMatch;
    youTotal += annualContrib;
    matchTotal += annualMatch;
    series.push({ age: i.currentAge + y, bal });
  }

  return {
    years,
    final: bal,
    youTotal,
    matchTotal,
    growth: Math.max(0, bal - youTotal - matchTotal),
    monthly: (bal * 0.04) / 12,
    series,
  };
}
