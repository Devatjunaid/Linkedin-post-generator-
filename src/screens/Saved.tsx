/* ---------- saved plans screen (persistent) ---------- */
import React, { useState } from 'react';
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Bookmark, ChevronRight, Plus, Trash2 } from 'lucide-react-native';
import { C, moneyShort } from '../theme';
import { Inputs, project } from '../calc';
import { SavedPlan } from '../storage';
import { BannerAdSlot } from '../components/ui';

export function SavedScreen({
  plans,
  currentInputs,
  onOpen,
  onSaveNew,
  onDelete,
}: {
  plans: SavedPlan[];
  currentInputs: Inputs;
  onOpen: (inputs: Inputs) => void;
  onSaveNew: (name: string) => void;
  onDelete: (id: string) => void;
}) {
  const [naming, setNaming] = useState(false);
  const [name, setName] = useState('');

  const openNamer = () => {
    setName(`Plan ${plans.length + 1}`);
    setNaming(true);
  };
  const confirm = () => {
    onSaveNew(name);
    setNaming(false);
  };

  return (
    <View style={styles.wrap}>
      <View style={styles.header}>
        <Text style={styles.title}>Saved plans</Text>
        <Pressable style={styles.newBtn} onPress={openNamer}>
          <Plus size={15} color={C.gold} />
          <Text style={styles.newText}>New</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.scroll}>
        {plans.length === 0 ? (
          <View style={styles.empty}>
            <Bookmark size={34} color={C.muted} />
            <Text style={styles.emptyTitle}>No saved plans yet</Text>
            <Text style={styles.emptyDesc}>
              Tap “New” to save your current calculator inputs as a plan you can
              revisit and compare.
            </Text>
          </View>
        ) : (
          plans.map(p => {
            const r = project(p.inputs);
            return (
              <Pressable
                key={p.id}
                style={styles.cardRow}
                onPress={() => onOpen(p.inputs)}>
                <View style={styles.flex}>
                  <View style={styles.nameRow}>
                    <Text style={styles.planName}>{p.name}</Text>
                    {p.tag ? (
                      <Text style={styles.tag}>{p.tag}</Text>
                    ) : null}
                  </View>
                  <Text style={styles.meta}>
                    At {p.inputs.contribPct}% · retire {p.inputs.retireAge}
                  </Text>
                  <Text style={styles.amount}>{moneyShort(r.final)}</Text>
                </View>
                <Pressable
                  hitSlop={10}
                  onPress={() => onDelete(p.id)}
                  style={styles.trash}>
                  <Trash2 size={18} color={C.muted} />
                </Pressable>
                <ChevronRight size={20} color={C.muted} />
              </Pressable>
            );
          })
        )}
      </ScrollView>

      <BannerAdSlot />

      <Modal transparent visible={naming} animationType="fade" onRequestClose={() => setNaming(false)}>
        <Pressable style={styles.backdrop} onPress={() => setNaming(false)}>
          <Pressable style={styles.sheet} onPress={() => {}}>
            <Text style={styles.sheetTitle}>Save plan</Text>
            <Text style={styles.sheetDesc}>
              Saving your current inputs: {currentInputs.contribPct}% contribution,
              retire at {currentInputs.retireAge}.
            </Text>
            <TextInput
              value={name}
              onChangeText={setName}
              placeholder="Plan name"
              placeholderTextColor={C.muted}
              style={styles.input}
              autoFocus
            />
            <View style={styles.sheetBtns}>
              <Pressable
                style={[styles.sheetBtn, styles.cancelBtn]}
                onPress={() => setNaming(false)}>
                <Text style={styles.cancelText}>Cancel</Text>
              </Pressable>
              <Pressable
                style={[styles.sheetBtn, styles.saveBtn]}
                onPress={confirm}>
                <Text style={styles.saveText}>Save</Text>
              </Pressable>
            </View>
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: C.bg },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 6,
    paddingBottom: 12,
  },
  title: { fontSize: 22, fontWeight: '800', color: C.text },
  newBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: 'rgba(232,176,75,0.12)',
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  newText: { fontSize: 12.5, fontWeight: '700', color: C.gold },
  scroll: { paddingHorizontal: 16, paddingBottom: 12, gap: 10 },
  cardRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: C.surface,
    borderWidth: 1,
    borderColor: C.line,
    borderRadius: 16,
    paddingHorizontal: 15,
    paddingVertical: 14,
  },
  flex: { flex: 1 },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  planName: { fontSize: 15, fontWeight: '700', color: C.text },
  tag: {
    fontSize: 9.5,
    fontWeight: '700',
    color: C.gold,
    backgroundColor: 'rgba(232,176,75,0.12)',
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 20,
    overflow: 'hidden',
  },
  meta: { fontSize: 12, color: C.muted, marginTop: 4 },
  amount: {
    fontSize: 19,
    fontWeight: '800',
    color: C.gold,
    marginTop: 6,
  },
  trash: { padding: 4 },
  empty: { alignItems: 'center', paddingTop: 60, paddingHorizontal: 24, gap: 10 },
  emptyTitle: { fontSize: 16, fontWeight: '700', color: C.text },
  emptyDesc: {
    fontSize: 13,
    color: C.muted,
    textAlign: 'center',
    lineHeight: 19,
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
  input: {
    backgroundColor: C.surface2,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: C.line,
    color: C.text,
    fontSize: 15,
    fontWeight: '600',
    paddingHorizontal: 14,
    paddingVertical: 11,
    marginTop: 14,
  },
  sheetBtns: { flexDirection: 'row', gap: 10, marginTop: 16 },
  sheetBtn: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  cancelBtn: { backgroundColor: C.surface2 },
  cancelText: { color: C.text, fontWeight: '700', fontSize: 14 },
  saveBtn: { backgroundColor: C.gold },
  saveText: { color: '#171002', fontWeight: '800', fontSize: 14 },
});
