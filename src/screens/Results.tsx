/* ---------- results / projection screen ---------- */
import React from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { C, money } from '../theme';
import { Projection } from '../calc';
import { BannerAdSlot, useCountUp } from '../components/ui';
import { AreaChart } from '../components/AreaChart';

export function ResultsScreen({
  res,
  onBack,
}: {
  res: Projection;
  onBack: () => void;
}) {
  const shown = useCountUp(res.final, true);
  const endAge = res.series[res.series.length - 1].age;

  const parts = [
    { label: 'Your contributions', val: res.youTotal, color: C.gold },
    { label: 'Employer match', val: res.matchTotal, color: C.blue },
    { label: 'Investment growth', val: res.growth, color: C.green },
  ];
  const total = parts.reduce((s, p) => s + p.val, 0) || 1;

  return (
    <View style={styles.wrap}>
      <View style={styles.header}>
        <Pressable onPress={onBack} hitSlop={10}>
          <Text style={styles.back}>‹</Text>
        </Pressable>
        <Text style={styles.headerTitle}>Your projection</Text>
      </View>

      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.hero}>
          <Text style={styles.heroLabel}>BALANCE AT AGE {endAge}</Text>
          <Text style={styles.heroValue}>{money(shown)}</Text>
          <Text style={styles.heroSub}>
            ≈ <Text style={styles.heroGreen}>{money(res.monthly)}</Text>
            <Text style={styles.heroMuted}> /mo retirement income</Text>
          </Text>
        </View>

        <View style={styles.card}>
          <AreaChart data={res.series} height={160} />
        </View>

        <View style={[styles.card, styles.breakdown]}>
          <Text style={styles.breakdownTitle}>WHERE IT COMES FROM</Text>
          <View style={styles.bar}>
            {parts.map(p => (
              <View
                key={p.label}
                style={{ flex: p.val / total, backgroundColor: p.color }}
              />
            ))}
          </View>
          {parts.map(p => (
            <View key={p.label} style={styles.legendRow}>
              <View style={styles.legendLeft}>
                <View style={[styles.swatch, { backgroundColor: p.color }]} />
                <Text style={styles.legendLabel}>{p.label}</Text>
              </View>
              <Text style={styles.legendVal}>{money(p.val)}</Text>
            </View>
          ))}
        </View>
      </ScrollView>

      <BannerAdSlot />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: C.bg },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 18,
    paddingTop: 4,
    paddingBottom: 6,
  },
  back: { color: C.gold, fontSize: 30, lineHeight: 32 },
  headerTitle: { fontSize: 15, fontWeight: '700', color: C.text },
  scroll: { paddingHorizontal: 16, paddingBottom: 12 },
  hero: {
    backgroundColor: C.surface,
    borderWidth: 1,
    borderColor: C.line,
    borderRadius: 18,
    paddingVertical: 18,
    paddingHorizontal: 16,
    alignItems: 'center',
  },
  heroLabel: { fontSize: 11, color: C.muted, fontWeight: '600', letterSpacing: 0.5 },
  heroValue: {
    fontSize: 37,
    fontWeight: '800',
    color: C.gold,
    marginVertical: 3,
  },
  heroSub: { fontSize: 12, color: C.text },
  heroGreen: { color: C.green, fontWeight: '700' },
  heroMuted: { color: C.muted },
  card: {
    backgroundColor: C.surface,
    borderWidth: 1,
    borderColor: C.line,
    borderRadius: 18,
    padding: 12,
    marginTop: 12,
  },
  breakdown: { paddingHorizontal: 16, paddingVertical: 14 },
  breakdownTitle: {
    fontSize: 12,
    color: C.muted,
    fontWeight: '700',
    marginBottom: 10,
  },
  bar: {
    flexDirection: 'row',
    height: 8,
    borderRadius: 5,
    overflow: 'hidden',
    marginBottom: 12,
  },
  legendRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 5,
  },
  legendLeft: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  swatch: { width: 9, height: 9, borderRadius: 3 },
  legendLabel: { fontSize: 13, color: C.text },
  legendVal: { fontSize: 13, fontWeight: '700', color: C.text },
});
