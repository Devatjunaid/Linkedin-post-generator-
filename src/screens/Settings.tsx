/* ---------- settings screen (functional; no paid upsell) ---------- */
import React, { useState } from 'react';
import {
  Linking,
  Modal,
  Pressable,
  ScrollView,
  Share,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  ChevronRight,
  DollarSign,
  Info,
  LucideIcon,
  Moon,
  Share2,
  Shield,
  Star,
  TrendingUp,
} from 'lucide-react-native';
import { C } from '../theme';
import { AppSettings } from '../storage';
import { BannerAdSlot } from '../components/ui';

const PACKAGE = 'com.nestworth';
const PRIVACY_URL = 'https://nestworth.app/privacy';
const RETURN_OPTIONS = [4, 5, 6, 7, 8, 9, 10, 11, 12];

export function SettingsScreen({
  settings,
  onChangeReturn,
}: {
  settings: AppSettings;
  onChangeReturn: (n: number) => void;
}) {
  const [pickReturn, setPickReturn] = useState(false);
  const [about, setAbout] = useState(false);

  const rateApp = () =>
    Linking.openURL(`market://details?id=${PACKAGE}`).catch(() =>
      Linking.openURL(
        `https://play.google.com/store/apps/details?id=${PACKAGE}`,
      ),
    );

  const shareApp = () =>
    Share.share({
      message:
        'Nestworth — a free 401(k) retirement planner. See what your retirement could look like: ' +
        `https://play.google.com/store/apps/details?id=${PACKAGE}`,
    });

  const openPrivacy = () => Linking.openURL(PRIVACY_URL).catch(() => {});

  const row = (
    Icon: LucideIcon,
    label: string,
    right?: React.ReactNode,
    onPress?: () => void,
    last?: boolean,
  ) => (
    <Pressable
      onPress={onPress}
      style={[styles.row, last && styles.rowLast]}>
      <Icon size={18} color={C.muted} />
      <Text style={styles.rowLabel}>{label}</Text>
      {right ?? <ChevronRight size={17} color={C.muted} />}
    </Pressable>
  );

  return (
    <View style={styles.wrap}>
      <Text style={styles.title}>Settings</Text>

      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.sectionTitle}>PREFERENCES</Text>
        <View style={styles.section}>
          {row(
            DollarSign,
            'Currency',
            <Text style={styles.rowRight}>USD ($)</Text>,
          )}
          {row(
            Moon,
            'Dark theme',
            <View style={styles.toggle}>
              <View style={styles.toggleKnob} />
            </View>,
          )}
          {row(
            TrendingUp,
            'Default return rate',
            <Text style={styles.rowRight}>{settings.defaultReturn}%</Text>,
            () => setPickReturn(true),
            true,
          )}
        </View>

        <Text style={styles.sectionTitle}>SUPPORT</Text>
        <View style={styles.section}>
          {row(Star, 'Rate Nestworth', undefined, rateApp)}
          {row(Share2, 'Share app', undefined, shareApp)}
          {row(Shield, 'Privacy policy', undefined, openPrivacy)}
          {row(Info, 'About & disclaimer', undefined, () => setAbout(true), true)}
        </View>

        <Text style={styles.footerNote}>
          Nestworth v1.0.0 · Estimates are not financial advice
        </Text>
      </ScrollView>

      <BannerAdSlot />

      {/* default return rate picker */}
      <Modal
        transparent
        visible={pickReturn}
        animationType="fade"
        onRequestClose={() => setPickReturn(false)}>
        <Pressable style={styles.backdrop} onPress={() => setPickReturn(false)}>
          <Pressable style={styles.sheet} onPress={() => {}}>
            <Text style={styles.sheetTitle}>Default return rate</Text>
            <Text style={styles.sheetDesc}>
              Used as the starting expected return for new calculations.
            </Text>
            <View style={styles.options}>
              {RETURN_OPTIONS.map(opt => {
                const on = opt === settings.defaultReturn;
                return (
                  <Pressable
                    key={opt}
                    style={[styles.option, on && styles.optionOn]}
                    onPress={() => {
                      onChangeReturn(opt);
                      setPickReturn(false);
                    }}>
                    <Text
                      style={[styles.optionText, on && styles.optionTextOn]}>
                      {opt}%
                    </Text>
                  </Pressable>
                );
              })}
            </View>
          </Pressable>
        </Pressable>
      </Modal>

      {/* about & disclaimer */}
      <Modal
        transparent
        visible={about}
        animationType="fade"
        onRequestClose={() => setAbout(false)}>
        <Pressable style={styles.backdrop} onPress={() => setAbout(false)}>
          <Pressable style={styles.sheet} onPress={() => {}}>
            <Text style={styles.sheetTitle}>About Nestworth</Text>
            <Text style={styles.aboutText}>
              Nestworth is a free 401(k) retirement planner that projects your
              future balance based on your inputs.{'\n\n'}
              All figures are estimates for illustration only and are not
              financial advice. Actual returns vary and are not guaranteed.
              Consult a qualified financial professional before making
              decisions.{'\n\n'}
              Calculations run entirely on your device.
            </Text>
            <Pressable
              style={[styles.option, styles.closeBtn]}
              onPress={() => setAbout(false)}>
              <Text style={styles.optionTextOn}>Close</Text>
            </Pressable>
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: C.bg },
  title: {
    paddingHorizontal: 20,
    paddingTop: 6,
    paddingBottom: 12,
    fontSize: 22,
    fontWeight: '800',
    color: C.text,
  },
  scroll: { paddingHorizontal: 16, paddingBottom: 12 },
  sectionTitle: {
    fontSize: 11,
    fontWeight: '700',
    color: C.muted,
    letterSpacing: 0.6,
    paddingHorizontal: 4,
    paddingBottom: 7,
  },
  section: {
    backgroundColor: C.surface,
    borderWidth: 1,
    borderColor: C.line,
    borderRadius: 16,
    overflow: 'hidden',
    marginBottom: 16,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 15,
    paddingVertical: 13,
    borderBottomWidth: 1,
    borderBottomColor: C.line,
  },
  rowLast: { borderBottomWidth: 0 },
  rowLabel: { flex: 1, fontSize: 14, color: C.text },
  rowRight: { fontSize: 13, color: C.muted },
  toggle: {
    width: 38,
    height: 22,
    borderRadius: 20,
    backgroundColor: C.gold,
    justifyContent: 'center',
  },
  toggleKnob: {
    position: 'absolute',
    right: 3,
    width: 16,
    height: 16,
    borderRadius: 16,
    backgroundColor: '#171002',
  },
  footerNote: {
    textAlign: 'center',
    fontSize: 11,
    color: C.muted,
    paddingVertical: 6,
  },
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.55)',
    justifyContent: 'center',
    paddingHorizontal: 28,
  },
  sheet: {
    backgroundColor: C.surface,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: C.line,
    padding: 18,
  },
  sheetTitle: { fontSize: 17, fontWeight: '800', color: C.text },
  sheetDesc: { fontSize: 12.5, color: C.muted, marginTop: 6, lineHeight: 18 },
  options: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 14,
  },
  option: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 12,
    backgroundColor: C.surface2,
    borderWidth: 1,
    borderColor: C.line,
  },
  optionOn: { backgroundColor: C.gold, borderColor: C.gold },
  optionText: { color: C.text, fontWeight: '700', fontSize: 14 },
  optionTextOn: { color: '#171002', fontWeight: '800', fontSize: 14 },
  closeBtn: {
    marginTop: 16,
    alignItems: 'center',
    backgroundColor: C.gold,
    borderColor: C.gold,
  },
  aboutText: { fontSize: 13, color: C.text, lineHeight: 20, marginTop: 12 },
});
