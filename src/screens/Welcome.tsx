/* ---------- welcome / onboarding screen ---------- */
import React from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  BarChart3,
  LucideIcon,
  PiggyBank,
  TrendingUp,
} from 'lucide-react-native';
import { C } from '../theme';
import { Logo } from '../components/ui';

export function Welcome({ start }: { start: () => void }) {
  const props: [LucideIcon, string, string][] = [
    [
      TrendingUp,
      'See your future balance',
      'Project your 401(k) to any retirement age in seconds.',
    ],
    [
      PiggyBank,
      "Don't leave match on the table",
      'Find out exactly what your employer match adds up to.',
    ],
    [
      BarChart3,
      'Compare scenarios',
      'Save plans and see how small changes compound over time.',
    ],
  ];

  return (
    <View style={styles.wrap}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <Logo size={52} />
        <Text style={styles.h1}>Know what your{'\n'}retirement looks like</Text>
        <Text style={styles.lead}>Free, private, and runs the math for you.</Text>

        <View style={styles.list}>
          {props.map(([Icon, t, d]) => (
            <View key={t} style={styles.row}>
              <View style={styles.iconBox}>
                <Icon size={19} color={C.gold} />
              </View>
              <View style={styles.flex}>
                <Text style={styles.rowTitle}>{t}</Text>
                <Text style={styles.rowDesc}>{d}</Text>
              </View>
            </View>
          ))}
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <Pressable style={styles.cta} onPress={start}>
          <Text style={styles.ctaText}>Get started</Text>
        </Pressable>
        <Text style={styles.fine}>No account needed · Estimates only</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: C.bg },
  scroll: { paddingHorizontal: 24, paddingTop: 24, paddingBottom: 10 },
  h1: {
    fontSize: 27,
    fontWeight: '800',
    color: C.text,
    marginTop: 18,
    lineHeight: 32,
  },
  lead: { fontSize: 14, color: C.muted, marginTop: 8 },
  list: { marginTop: 26, gap: 16 },
  row: { flexDirection: 'row', gap: 13, alignItems: 'flex-start' },
  iconBox: {
    width: 38,
    height: 38,
    borderRadius: 11,
    backgroundColor: 'rgba(232,176,75,0.12)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  flex: { flex: 1 },
  rowTitle: { fontSize: 14.5, fontWeight: '700', color: C.text },
  rowDesc: { fontSize: 12.5, color: C.muted, marginTop: 2, lineHeight: 17 },
  footer: { paddingHorizontal: 20, paddingTop: 10, paddingBottom: 22 },
  cta: {
    paddingVertical: 15,
    borderRadius: 14,
    backgroundColor: C.gold,
    alignItems: 'center',
  },
  ctaText: { fontSize: 16, fontWeight: '800', color: '#171002' },
  fine: { textAlign: 'center', fontSize: 11, color: C.muted, marginTop: 12 },
});
