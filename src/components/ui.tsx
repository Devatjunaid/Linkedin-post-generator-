/* ---------- shared UI building blocks ---------- */
import React, { useEffect, useRef, useState } from 'react';
import {
  PanResponder,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import Svg, { Circle, Path, Rect } from 'react-native-svg';
import { BannerAd, BannerAdSize } from 'react-native-google-mobile-ads';
import {
  Bookmark,
  Calculator as CalcIcon,
  Settings as Cog,
  LucideIcon,
} from 'lucide-react-native';
import { C } from '../theme';
import { AD_UNITS } from '../ads';

/* ---------- logo ---------- */
export function Logo({ size = 64 }: { size?: number }) {
  return (
    <Svg width={size} height={size} viewBox="0 0 64 64" fill="none">
      <Rect
        x="2"
        y="2"
        width="60"
        height="60"
        rx="17"
        fill="#13191F"
        stroke={C.gold}
        strokeWidth="1.5"
      />
      <Path
        d="M16 40 L27 30 L35 36 L48 22"
        stroke={C.gold}
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <Circle cx="48" cy="22" r="4" fill={C.gold} />
      <Path
        d="M42 22 H48 V28"
        stroke={C.gold}
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

/* ---------- AdMob banner ---------- */
export function BannerAdSlot() {
  return (
    <View style={styles.bannerWrap}>
      <BannerAd
        unitId={AD_UNITS.banner}
        size={BannerAdSize.ANCHORED_ADAPTIVE_BANNER}
        requestOptions={{ requestNonPersonalizedAdsOnly: true }}
      />
    </View>
  );
}

/* ---------- bottom navigation ---------- */
export type Tab = 'calc' | 'saved' | 'settings';

export function BottomNav({
  tab,
  go,
}: {
  tab: Tab;
  go: (t: Tab) => void;
}) {
  const items: [Tab, string, LucideIcon][] = [
    ['calc', 'Calculate', CalcIcon],
    ['saved', 'Saved', Bookmark],
    ['settings', 'Settings', Cog],
  ];
  return (
    <View style={styles.navBar}>
      {items.map(([k, label, Icon]) => {
        const on = tab === k;
        return (
          <Pressable key={k} onPress={() => go(k)} style={styles.navItem}>
            <Icon
              size={21}
              strokeWidth={on ? 2.4 : 1.9}
              color={on ? C.gold : C.muted}
            />
            <Text style={[styles.navLabel, on ? styles.navLabelOn : styles.navLabelOff]}>
              {label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

/* ---------- numeric field ---------- */
export function Field({
  label,
  prefix,
  value,
  onChange,
}: {
  label: string;
  prefix?: string;
  value: number;
  onChange: (n: number) => void;
}) {
  const [text, setText] = useState(String(value));

  // keep local text in sync when the value changes from outside (e.g. loading a plan)
  useEffect(() => {
    if (Number(text) !== value) {
      setText(String(value));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <View style={styles.fieldRow}>
        {prefix ? <Text style={styles.fieldPrefix}>{prefix}</Text> : null}
        <TextInput
          value={text}
          keyboardType="numeric"
          placeholderTextColor={C.muted}
          onChangeText={t => {
            setText(t);
            const n = Number(t);
            if (t !== '' && !Number.isNaN(n)) {
              onChange(n);
            }
          }}
          onBlur={() => {
            const n = Number(text);
            if (text === '' || Number.isNaN(n)) {
              setText(String(value));
            }
          }}
          style={styles.fieldInput}
        />
      </View>
    </View>
  );
}

/* ---------- slider ---------- */
export function Slider({
  label,
  value,
  onChange,
  min,
  max,
  step,
}: {
  label: string;
  value: number;
  onChange: (n: number) => void;
  min: number;
  max: number;
  step: number;
}) {
  const [w, setW] = useState(0);
  const widthRef = useRef(0);
  const pct = max > min ? (value - min) / (max - min) : 0;

  // fresh handler each render captures current props
  const handleRef = useRef((_x: number) => {});
  handleRef.current = (x: number) => {
    const wv = widthRef.current;
    if (wv <= 0) {
      return;
    }
    const p = Math.max(0, Math.min(1, x / wv));
    const raw = min + p * (max - min);
    let stepped = Math.round(raw / step) * step;
    stepped = Math.max(min, Math.min(max, Number(stepped.toFixed(2))));
    onChange(stepped);
  };

  const pan = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: e => handleRef.current(e.nativeEvent.locationX),
      onPanResponderMove: e => handleRef.current(e.nativeEvent.locationX),
    }),
  ).current;

  const thumb = 18;

  return (
    <View style={styles.field}>
      <View style={styles.sliderHead}>
        <Text style={styles.fieldLabel}>{label}</Text>
        <Text style={styles.sliderValue}>{value}%</Text>
      </View>
      <View
        style={styles.sliderTouch}
        onLayout={e => {
          const ww = e.nativeEvent.layout.width;
          widthRef.current = ww;
          setW(ww);
        }}
        {...pan.panHandlers}>
        <View style={styles.sliderTrack}>
          <View style={[styles.sliderFill, { width: `${pct * 100}%` }]} />
        </View>
        <View
          style={[
            styles.sliderThumb,
            { left: Math.max(0, pct * w - thumb / 2) },
          ]}
        />
      </View>
    </View>
  );
}

/* ---------- count-up animation hook ---------- */
export function useCountUp(target: number, on: boolean) {
  const [v, setV] = useState(0);
  const raf = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (!on) {
      return;
    }
    const start = Date.now();
    const tick = () => {
      const p = Math.min(1, (Date.now() - start) / 900);
      setV(target * (1 - Math.pow(1 - p, 3)));
      if (p < 1) {
        raf.current = requestAnimationFrame(tick);
      }
    };
    raf.current = requestAnimationFrame(tick);
    return () => {
      if (raf.current !== undefined) {
        cancelAnimationFrame(raf.current);
      }
    };
  }, [target, on]);

  return v;
}

const styles = StyleSheet.create({
  bannerWrap: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 4,
  },
  navBar: {
    flexDirection: 'row',
    borderTopWidth: 1,
    borderTopColor: C.line,
    backgroundColor: '#11171D',
    paddingBottom: 6,
  },
  navItem: {
    flex: 1,
    paddingTop: 10,
    paddingBottom: 6,
    alignItems: 'center',
    gap: 3,
  },
  navLabel: { fontSize: 10.5 },
  navLabelOn: { color: C.gold, fontWeight: '700' },
  navLabelOff: { color: C.muted, fontWeight: '500' },
  field: {
    backgroundColor: C.surface2,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 11,
    borderWidth: 1,
    borderColor: C.line,
  },
  fieldLabel: { fontSize: 11, color: C.muted, fontWeight: '600', marginBottom: 3 },
  fieldRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  fieldPrefix: { color: C.gold, fontWeight: '700', fontSize: 17 },
  fieldInput: {
    flex: 1,
    color: C.text,
    fontSize: 17,
    fontWeight: '700',
    padding: 0,
  },
  sliderHead: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  sliderValue: { fontSize: 13, color: C.gold, fontWeight: '800' },
  sliderTouch: { height: 24, justifyContent: 'center' },
  sliderTrack: {
    height: 5,
    borderRadius: 5,
    backgroundColor: '#2C3743',
    overflow: 'hidden',
  },
  sliderFill: { height: 5, borderRadius: 5, backgroundColor: C.gold },
  sliderThumb: {
    position: 'absolute',
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: C.gold,
    borderWidth: 3,
    borderColor: '#11171D',
  },
});
