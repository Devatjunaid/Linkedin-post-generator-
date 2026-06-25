/**
 * Nestworth — 401(k) Retirement Planner
 * @format
 */
import React, { useEffect, useMemo, useState } from 'react';
import { StatusBar, StyleSheet, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import mobileAds from 'react-native-google-mobile-ads';

import { C } from './src/theme';
import { BASE, Inputs, project } from './src/calc';
import {
  AppSettings,
  SavedPlan,
  deletePlan,
  loadPlans,
  loadSettings,
  savePlan,
  saveSettings,
} from './src/storage';
import { useInterstitial } from './src/ads';
import { BottomNav, Tab } from './src/components/ui';
import { Splash } from './src/screens/Splash';
import { Welcome } from './src/screens/Welcome';
import { CalcScreen } from './src/screens/Calc';
import { ResultsScreen } from './src/screens/Results';
import { SavedScreen } from './src/screens/Saved';
import { SettingsScreen } from './src/screens/Settings';

type Screen = 'splash' | 'welcome' | 'calc' | 'results' | 'saved' | 'settings';
const TAB_SCREENS: Tab[] = ['calc', 'saved', 'settings'];

function App() {
  const [inp, setInp] = useState<Inputs>(BASE);
  const [screen, setScreen] = useState<Screen>('splash');
  const [plans, setPlans] = useState<SavedPlan[]>([]);
  const [settings, setSettings] = useState<AppSettings>({
    defaultReturn: BASE.returnPct,
  });

  const res = useMemo(() => project(inp), [inp]);
  const showInterstitialThen = useInterstitial();

  const set = <K extends keyof Inputs>(k: K, v: Inputs[K]) =>
    setInp(s => ({ ...s, [k]: v }));

  /* init: ads, persisted data, then leave splash */
  useEffect(() => {
    mobileAds()
      .initialize()
      .catch(() => {});

    let active = true;
    const started = Date.now();
    (async () => {
      const [p, s] = await Promise.all([loadPlans(), loadSettings()]);
      if (!active) {
        return;
      }
      setPlans(p);
      setSettings(s);
      setInp(prev => ({ ...prev, returnPct: s.defaultReturn }));
      const wait = Math.max(0, 1600 - (Date.now() - started));
      setTimeout(() => active && setScreen('welcome'), wait);
    })();

    return () => {
      active = false;
    };
  }, []);

  /* calculate -> interstitial -> results */
  const onCalc = () => showInterstitialThen(() => setScreen('results'));

  /* saved-plan handlers */
  const openPlan = (inputs: Inputs) => {
    setInp(inputs);
    setScreen('results');
  };
  const saveNew = async (name: string) => {
    setPlans(await savePlan(name, inp));
  };
  const removePlan = async (id: string) => {
    setPlans(await deletePlan(id));
  };

  /* settings */
  const changeReturn = (n: number) => {
    const next = { ...settings, defaultReturn: n };
    setSettings(next);
    saveSettings(next).catch(() => {});
    set('returnPct', n);
  };

  let content: React.ReactNode;
  if (screen === 'splash') {
    content = <Splash />;
  } else if (screen === 'welcome') {
    content = <Welcome start={() => setScreen('calc')} />;
  } else if (screen === 'calc') {
    content = <CalcScreen inp={inp} set={set} onCalc={onCalc} />;
  } else if (screen === 'results') {
    content = <ResultsScreen res={res} onBack={() => setScreen('calc')} />;
  } else if (screen === 'saved') {
    content = (
      <SavedScreen
        plans={plans}
        currentInputs={inp}
        onOpen={openPlan}
        onSaveNew={saveNew}
        onDelete={removePlan}
      />
    );
  } else {
    content = (
      <SettingsScreen settings={settings} onChangeReturn={changeReturn} />
    );
  }

  const showNav = TAB_SCREENS.includes(screen as Tab);

  return (
    <SafeAreaProvider>
      <StatusBar barStyle="light-content" backgroundColor={C.bg} />
      <SafeAreaView style={styles.root} edges={['top', 'bottom']}>
        <View style={styles.body}>{content}</View>
        {showNav && <BottomNav tab={screen as Tab} go={t => setScreen(t)} />}
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: C.bg },
  body: { flex: 1, minHeight: 0 },
});

export default App;
