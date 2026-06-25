/* ---------- on-device persistence (AsyncStorage) ---------- */
import AsyncStorage from '@react-native-async-storage/async-storage';
import { BASE, Inputs } from './calc';

const PLANS_KEY = 'nestworth.savedPlans.v1';
const SETTINGS_KEY = 'nestworth.settings.v1';

export type SavedPlan = {
  id: string;
  name: string;
  tag?: string;
  inputs: Inputs;
  createdAt: number;
};

export type AppSettings = {
  defaultReturn: number;
};

const DEFAULT_SETTINGS: AppSettings = { defaultReturn: BASE.returnPct };

const newId = () =>
  Date.now().toString(36) + Math.random().toString(36).slice(2, 7);

/* ----- plans ----- */
export async function loadPlans(): Promise<SavedPlan[]> {
  try {
    const raw = await AsyncStorage.getItem(PLANS_KEY);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

async function writePlans(plans: SavedPlan[]): Promise<SavedPlan[]> {
  await AsyncStorage.setItem(PLANS_KEY, JSON.stringify(plans));
  return plans;
}

export async function savePlan(
  name: string,
  inputs: Inputs,
  tag?: string,
): Promise<SavedPlan[]> {
  const plans = await loadPlans();
  const plan: SavedPlan = {
    id: newId(),
    name: name.trim() || 'Untitled plan',
    tag,
    inputs,
    createdAt: Date.now(),
  };
  return writePlans([plan, ...plans]);
}

export async function deletePlan(id: string): Promise<SavedPlan[]> {
  const plans = await loadPlans();
  return writePlans(plans.filter(p => p.id !== id));
}

/* ----- settings ----- */
export async function loadSettings(): Promise<AppSettings> {
  try {
    const raw = await AsyncStorage.getItem(SETTINGS_KEY);
    if (!raw) {
      return DEFAULT_SETTINGS;
    }
    return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export async function saveSettings(s: AppSettings): Promise<AppSettings> {
  await AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify(s));
  return s;
}
