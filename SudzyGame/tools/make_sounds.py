#!/usr/bin/env python3
"""
Synthesises every sound in Sudzy! from scratch (no samples, no licences needed).

    python3 tools/make_sounds.py

Writes 22.05 kHz mono 16-bit WAVs into app/src/main/assets/{sfx,music}.
Requires numpy.
"""
import os
import wave

import numpy as np

SR = 22050
ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
rng = np.random.default_rng(7)


def t_axis(sec):
    return np.arange(int(SR * sec)) / SR


def save(path, x, peak=0.9):
    x = np.asarray(x, dtype=np.float64)
    m = np.max(np.abs(x)) or 1.0
    x = x / m * peak
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())


def env(n, a=0.005, d=0.1, s=0.0, r=0.05, sustain_len=0.0):
    """Simple ADSR envelope of n samples."""
    out = np.zeros(n)
    ai, di, si, ri = int(a * SR), int(d * SR), int(sustain_len * SR), int(r * SR)
    idx = 0
    for seg, (start, end, ln) in enumerate([(0, 1, ai), (1, s, di), (s, s, si), (s, 0, ri)]):
        ln = max(ln, 1)
        k = min(ln, n - idx)
        if k <= 0:
            break
        out[idx:idx + k] = np.linspace(start, end, ln)[:k]
        idx += k
    return out


def exp_decay(n, tau):
    return np.exp(-np.arange(n) / (SR * tau))


def sweep(f0, f1, sec, curve=1.0):
    t = t_axis(sec)
    k = (t / sec) ** curve
    f = f0 + (f1 - f0) * k
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def noise(sec):
    return rng.uniform(-1, 1, int(SR * sec))


def bell(freq, sec, tau=0.15):
    t = t_axis(sec)
    x = np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-t / (tau * 0.4))
    return x * np.exp(-t / tau) * np.minimum(1, t * 400)


def note_hz(n):
    """MIDI note number to Hz."""
    return 440.0 * 2 ** ((n - 69) / 12)


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def place(buf, x, at):
    i = max(0, int(at * SR))
    end = min(len(buf), i + len(x))
    if end > i:
        buf[i:end] += x[:end - i]


def make_loop(x, fade=0.08):
    """Crossfade the tail into the head so the sound loops without clicks."""
    f = int(fade * SR)
    head, tail = x[:f].copy(), x[-f:].copy()
    ramp = np.linspace(0, 1, f)
    x = x[:-f].copy()
    x[:f] = head * ramp + tail * (1 - ramp)
    return x


# ---------------------------------------------------------------- effects
def sfx():
    out = {}
    out["click"] = sweep(1500, 900, 0.035) * exp_decay(int(0.035 * SR), 0.008)
    out["tick"] = sweep(2400, 1800, 0.018) * exp_decay(int(0.018 * SR), 0.004)
    out["pop"] = mix(sweep(950, 260, 0.09, 0.6) * exp_decay(int(0.09 * SR), 0.03), noise(0.01) * 0.3)
    t = t_axis(0.38)
    f = 170 + 230 * (t / 0.38) + 40 * np.sin(2 * np.pi * 14 * t) * np.exp(-t / 0.12)
    out["boing"] = np.sin(2 * np.pi * np.cumsum(f) / SR) * exp_decay(len(t), 0.13)

    def honk(sec):
        tt = t_axis(sec)
        ff = 1 - 0.04 * tt / sec
        s = np.sign(np.sin(2 * np.pi * 330 * ff * tt)) + 0.8 * np.sign(np.sin(2 * np.pi * 415 * ff * tt))
        return lowpass(s, 1800) * env(len(tt), 0.01, 0.02, 0.8, 0.04, sec - 0.07)
    h = np.zeros(int(0.45 * SR))
    place(h, honk(0.15), 0.0)
    place(h, honk(0.2), 0.2)
    out["honk"] = h

    t = t_axis(0.2)
    f = 1700 + 900 * (t / 0.2) + 120 * np.sin(2 * np.pi * 32 * t)
    out["squeak"] = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * t / 0.2) ** 2

    out["drip"] = sweep(520, 1500, 0.09, 0.4) * exp_decay(int(0.09 * SR), 0.035)
    out["coin"] = mix(bell(988, 0.12, 0.05) * 0.8, np.concatenate([np.zeros(int(0.07 * SR)), bell(1319, 0.35, 0.12)]))
    star = np.zeros(int(0.6 * SR))
    for i, n in enumerate([84, 88, 91, 96]):
        place(star, bell(note_hz(n), 0.4, 0.14) * 0.7, i * 0.07)
    out["star"] = star
    combo = np.zeros(int(0.45 * SR))
    for i, n in enumerate([79, 83, 86, 91, 95]):
        place(combo, bell(note_hz(n), 0.25, 0.08) * 0.6, i * 0.045)
    out["combo"] = combo
    tada = np.zeros(int(1.6 * SR))
    for i, n in enumerate([72, 76, 79, 84]):
        place(tada, bell(note_hz(n), 0.5, 0.18) * 0.6, i * 0.09)
    tt = t_axis(1.1)
    vib = 1 + 0.006 * np.sin(2 * np.pi * 6 * tt)
    chord = sum(np.sin(2 * np.pi * note_hz(n) * vib * tt) + 0.3 * np.sin(4 * np.pi * note_hz(n) * vib * tt) for n in [72, 76, 79, 84])
    place(tada, chord * env(len(tt), 0.02, 0.2, 0.6, 0.5, 0.4) * 0.35, 0.4)
    out["tada"] = tada
    n = noise(0.5)
    k = np.sin(np.pi * t_axis(0.5) / 0.5) ** 2
    out["whoosh"] = lowpass(highpass(n, 300), 2500) * k
    st = np.zeros(int(0.35 * SR))
    place(st, out["pop"], 0)
    for i, f in enumerate([2093, 2637, 3136]):
        place(st, bell(f, 0.2, 0.05) * 0.4, 0.05 + i * 0.04)
    out["sticker"] = st
    sp = np.zeros(int(0.5 * SR))
    for i in range(7):
        place(sp, bell(rng.uniform(2000, 4200), 0.2, 0.05) * 0.4, rng.uniform(0, 0.3))
    out["sparkle"] = sp

    # looping tool sounds (1.2 s each)
    L = 1.3
    n = noise(L)
    out["spray"] = make_loop(lowpass(highpass(n, 900), 6000) * (0.85 + 0.15 * np.sin(2 * np.pi * 7 * t_axis(L))))
    foam = lowpass(highpass(noise(L), 400), 3000) * 0.5
    for _ in range(26):
        place(foam, sweep(rng.uniform(500, 1200), rng.uniform(1200, 2500), 0.04) * exp_decay(int(0.04 * SR), 0.012) * 0.8, rng.uniform(0, L - 0.05))
    out["foam"] = make_loop(foam)
    sc = lowpass(highpass(noise(L), 1200), 4000) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 5 * t_axis(L))))
    out["scrub"] = make_loop(sc)
    dr = lowpass(noise(L), 700) * 1.5 + 0.25 * np.sin(2 * np.pi * 118 * t_axis(L)) + 0.12 * np.sin(2 * np.pi * 236 * t_axis(L))
    out["dryer"] = make_loop(dr)
    po = lowpass(highpass(noise(L), 2000), 7000) * (0.4 + 0.6 * np.sin(np.pi * 2.3 * t_axis(L)) ** 2)
    for _ in range(5):
        place(po, bell(rng.uniform(2500, 4000), 0.15, 0.04) * 0.3, rng.uniform(0, L - 0.2))
    out["polish"] = make_loop(po)
    return out


# ---------------------------------------------------------------- music
def pluck(freq, sec, bright=0.5):
    """Karplus-Strong string — a cheerful ukulele-ish pluck."""
    n = int(SR * sec)
    period = max(2, int(SR / freq))
    buf = rng.uniform(-1, 1, period)
    out = np.zeros(n)
    decay = 0.996 - (1 - bright) * 0.004
    for i in range(n):
        j = i % period
        nxt = buf[(j + 1) % period]
        out[i] = buf[j]
        buf[j] = decay * 0.5 * (buf[j] + nxt)
    return out * np.minimum(1, np.arange(n) / 30)


def marimba(freq, sec):
    t = t_axis(sec)
    return (np.sin(2 * np.pi * freq * t) + 0.25 * np.sin(2 * np.pi * freq * 4 * t) * np.exp(-t / 0.03)) * np.exp(-t / 0.22) * np.minimum(1, t * 600)


def bass(freq, sec):
    t = t_axis(sec)
    x = np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(4 * np.pi * freq * t) + 0.12 * np.sin(6 * np.pi * freq * t)
    return x * np.exp(-t / 0.25) * np.minimum(1, t * 300)


def kick():
    return sweep(140, 45, 0.18, 0.5) * exp_decay(int(0.18 * SR), 0.07)


def clap():
    n = highpass(noise(0.16), 900)
    e = np.zeros(len(n))
    for k in (0, 0.012, 0.024):
        i = int(k * SR)
        e[i:] += np.exp(-np.arange(len(n) - i) / (SR * (0.008 if k < 0.02 else 0.05)))
    return lowpass(n * e, 5000) * 0.6


def hat():
    return highpass(noise(0.05), 6000) * exp_decay(int(0.05 * SR), 0.012) * 0.35


def song(bpm, bars, chords, melody, seed_shift=0, swing=0.0):
    beat = 60 / bpm
    length = bars * 4 * beat
    tail = 1.5
    buf = np.zeros(int((length + tail) * SR))
    K, C, Hh = kick(), clap(), hat()
    for bar in range(bars):
        root, tones = chords[bar % len(chords)]
        t0 = bar * 4 * beat
        for b in range(4):
            if b % 2 == 0:
                place(buf, K * 0.9, t0 + b * beat)
            elif b == 3 and bar % 2 == 1:
                place(buf, K * 0.6, t0 + (b + 0.5) * beat)
            if b % 2 == 1:
                place(buf, C, t0 + b * beat)
            for e in range(2):
                place(buf, Hh * (1.0 if e == 1 else 0.6), t0 + (b + e * (0.5 + swing)) * beat)
            # bass: root on beats, octave hop on the "and"
            place(buf, bass(note_hz(root - 24), beat * 0.9) * 0.55, t0 + b * beat)
            if b in (1, 3):
                place(buf, bass(note_hz(root - 12), beat * 0.4) * 0.35, t0 + (b + 0.5) * beat)
            # ukulele strum on the off-beats
            for k, n in enumerate(tones):
                place(buf, pluck(note_hz(n), 0.6) * 0.16, t0 + (b + 0.5) * beat + k * 0.012)
        for step, n in melody[bar % len(melody)]:
            if n:
                place(buf, marimba(note_hz(n), 0.5) * 0.42, t0 + step * 0.5 * beat)
    # wrap the tail around for a seamless loop
    L = int(length * SR)
    out = buf[:L].copy()
    out[:len(buf) - L] += buf[L:]
    return out


def music():
    C, Am, F, G = (60, [60, 64, 67, 72]), (57, [57, 60, 64, 69]), (53, [53, 57, 60, 65]), (55, [55, 59, 62, 67])
    m1 = [
        [(0, 76), (1, 79), (2, 84), (4, 81), (5, 79), (6, 76)],
        [(0, 76), (2, 74), (3, 72), (4, 74), (6, 76)],
        [(0, 77), (1, 76), (2, 72), (4, 72), (5, 74), (6, 77)],
        [(0, 79), (2, 77), (3, 76), (4, 74), (6, 71)],
        [(0, 76), (1, 79), (2, 84), (4, 86), (5, 84), (6, 81)],
        [(0, 79), (2, 76), (4, 72), (6, 76)],
        [(0, 77), (1, 81), (2, 84), (4, 81), (6, 77)],
        [(0, 79), (1, 83), (2, 86), (4, 84), (6, None)],
    ]
    menu = song(118, 8, [C, Am, F, G], m1)
    Fm, Cm, Dm, Bb = (53, [53, 57, 60, 65]), (60, [60, 64, 67, 72]), (50, [50, 53, 57, 62]), (58, [58, 62, 65, 70])
    m2 = [
        [(0, 81), (1, 84), (2, 81), (3, 79), (4, 77), (6, 79)],
        [(0, 79), (1, 76), (2, 72), (4, 76), (5, 79), (6, 84)],
        [(0, 81), (2, 77), (3, 74), (4, 77), (6, 81)],
        [(0, 82), (1, 81), (2, 77), (4, 74), (5, 77), (6, 82)],
        [(0, 84), (1, 81), (2, 84), (3, 86), (4, 89), (6, 84)],
        [(0, 84), (2, 79), (4, 76), (5, 79), (6, 84)],
        [(0, 86), (1, 84), (2, 81), (4, 77), (6, 74)],
        [(0, 77), (1, 79), (2, 81), (3, 82), (4, 84), (6, None)],
    ]
    wash = song(128, 8, [Fm, Cm, Dm, Bb], m2, swing=0.04)
    return {"menu": menu, "wash": wash}


if __name__ == "__main__":
    loops = {"spray", "foam", "scrub", "dryer", "polish"}
    for name, x in sfx().items():
        if name not in loops:  # tiny fade so one-shots never end in a click
            f = min(len(x) // 3, int(0.006 * SR))
            x = x.copy(); x[-f:] *= np.linspace(1, 0, f)
        save(os.path.join(ROOT, "sfx", name + ".wav"), x)
    for name, x in music().items():
        save(os.path.join(ROOT, "music", name + ".wav"), x, peak=0.8)
    print("sounds written to", os.path.abspath(ROOT))
