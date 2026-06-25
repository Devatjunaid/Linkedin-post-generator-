/* ---------- calculator (input) screen ---------- */
import React from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { C } from '../theme';
import { Inputs } from '../calc';
import { BannerAdSlot, Field, Slider } from '../components/ui';

export function CalcScreen({
  inp,
  set,
  onCalc,
}: {
  inp: Inputs;
  set: <K extends keyof Inputs>(k: K, v: Inputs[K]) => void;
  onCalc: () => void;
}) {
  return (
    <View style={styles.wrap}>
      <View style={styles.header}>
        <Text style={styles.kicker}>401(K) PLANNER</Text>
        <Text style={styles.title}>Plan your golden years</Text>
      </View>

      <ScrollView
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled">
        <View style={styles.grid}>
          <View style={styles.flex}>
            <Field
              label="Current age"
              value={inp.currentAge}
              onChange={v => set('currentAge', v)}
            />
          </View>
          <View style={styles.flex}>
            <Field
              label="Retire at"
              value={inp.retireAge}
              onChange={v => set('retireAge', v)}
            />
          </View>
        </View>

        <Field
          label="Annual salary"
          prefix="$"
          value={inp.salary}
          onChange={v => set('salary', v)}
        />
        <Field
          label="Current 401(k) balance"
          prefix="$"
          value={inp.balance}
          onChange={v => set('balance', v)}
        />

        <Slider
          label="Your contribution"
          value={inp.contribPct}
          min={0}
          max={25}
          step={0.5}
          onChange={v => set('contribPct', v)}
        />
        <Slider
          label="Employer match"
          value={inp.matchPct}
          min={0}
          max={10}
          step={0.5}
          onChange={v => set('matchPct', v)}
        />
        <Slider
          label="Expected annual return"
          value={inp.returnPct}
          min={1}
          max={12}
          step={0.5}
          onChange={v => set('returnPct', v)}
        />
      </ScrollView>

      <View style={styles.ctaWrap}>
        <Pressable style={styles.cta} onPress={onCalc}>
          <Text style={styles.ctaText}>Calculate my future →</Text>
        </Pressable>
      </View>

      <BannerAdSlot />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: C.bg },
  header: { paddingHorizontal: 20, paddingTop: 6, paddingBottom: 12 },
  kicker: { fontSize: 12, color: C.gold, fontWeight: '700', letterSpacing: 1 },
  title: { fontSize: 22, fontWeight: '800', color: C.text, marginTop: 2 },
  scroll: { paddingHorizontal: 16, paddingBottom: 12, gap: 10 },
  grid: { flexDirection: 'row', gap: 10 },
  flex: { flex: 1 },
  ctaWrap: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 12 },
  cta: {
    paddingVertical: 15,
    borderRadius: 14,
    backgroundColor: C.gold,
    alignItems: 'center',
  },
  ctaText: { fontSize: 16, fontWeight: '800', color: '#171002' },
});
