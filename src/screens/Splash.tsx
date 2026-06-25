/* ---------- splash screen ---------- */
import React, { useEffect, useRef } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';
import { C } from '../theme';
import { Logo } from '../components/ui';

export function Splash() {
  const pop = useRef(new Animated.Value(0)).current;
  const dotsRef = useRef([
    new Animated.Value(0.3),
    new Animated.Value(0.3),
    new Animated.Value(0.3),
  ]);
  const dots = dotsRef.current;

  useEffect(() => {
    Animated.spring(pop, {
      toValue: 1,
      friction: 6,
      tension: 60,
      useNativeDriver: true,
    }).start();

    const loops = dots.map((d, i) =>
      Animated.loop(
        Animated.sequence([
          Animated.delay(i * 160),
          Animated.timing(d, {
            toValue: 1,
            duration: 350,
            useNativeDriver: true,
          }),
          Animated.timing(d, {
            toValue: 0.3,
            duration: 350,
            useNativeDriver: true,
          }),
        ]),
      ),
    );
    loops.forEach(l => l.start());
    return () => loops.forEach(l => l.stop());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <View style={styles.wrap}>
      <Animated.View
        style={[
          styles.center,
          {
            opacity: pop,
            transform: [
              {
                translateY: pop.interpolate({
                  inputRange: [0, 1],
                  outputRange: [10, 0],
                }),
              },
            ],
          },
        ]}>
        <Logo size={84} />
        <Text style={styles.title}>Nestworth</Text>
        <Text style={styles.sub}>401(k) Retirement Planner</Text>
      </Animated.View>

      <View style={styles.dots}>
        {dots.map((d, i) => (
          <Animated.View key={i} style={[styles.dot, { opacity: d }]} />
        ))}
      </View>
      <Text style={styles.version}>v1.0.0</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    flex: 1,
    backgroundColor: C.bg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  center: { alignItems: 'center' },
  title: {
    fontSize: 27,
    fontWeight: '800',
    color: C.text,
    marginTop: 18,
    letterSpacing: -0.5,
  },
  sub: { fontSize: 13, color: C.muted, marginTop: 4, fontWeight: '500' },
  dots: {
    position: 'absolute',
    bottom: 64,
    flexDirection: 'row',
    gap: 7,
  },
  dot: { width: 7, height: 7, borderRadius: 9, backgroundColor: C.gold },
  version: { position: 'absolute', bottom: 26, fontSize: 11, color: C.muted },
});
