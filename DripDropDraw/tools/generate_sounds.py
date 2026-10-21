"""
Generates every sound and the background music for Drip Drop Draw from code.

    python3 tools/generate_sounds.py

Writes 16-bit mono WAV files into app/src/main/res/raw. Needs numpy.
"""
import os
import wave

import numpy as np

RATE = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
rng = np.random.default_rng(7)


def t_axis(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def env(n, attack=0.005, release=None, curve=6.0):
    """Quick attack then exponential decay."""
    t = np.arange(n) / RATE
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    length = n / RATE
    d = np.exp(-curve * t / length)
    out = a * d
    if release:
        r = int(release * RATE)
        out[-r:] *= np.linspace(1, 0, r)
    return out


def sweep(f0, f1, seconds, shape="exp"):
    """Sine whose pitch glides from f0 to f1."""
    n = int(seconds * RATE)
    if shape == "exp":
        f = f0 * (f1 / f0) ** (np.arange(n) / n)
    else:
        f = np.linspace(f0, f1, n)
    phase = 2 * np.pi * np.cumsum(f) / RATE
    return np.sin(phase)


def tone(freq, seconds, harmonics=(1.0,), decay=5.0):
    t = t_axis(seconds)
    s = sum(a * np.sin(2 * np.pi * freq * (i + 1) * t) for i, a in enumerate(harmonics))
    return s * env(len(t), curve=decay)


def lowpass(x, alpha):
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y


def bandpass(x, freq, q=4.0):
    """Simple two-pole resonator (RBJ band-pass)."""
    w0 = 2 * np.pi * freq / RATE
    alpha = np.sin(w0) / (2 * q)
    b0, b1, b2 = alpha, 0.0, -alpha
    a0, a1, a2 = 1 + alpha, -2 * np.cos(w0), 1 - alpha
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i, v in enumerate(x):
        out = (b0 * v + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1 = x1, v
        y2, y1 = y1, out
        y[i] = out
    return y


def save(name, data, gain=0.8):
    data = np.asarray(data, dtype=np.float64)
    peak = np.max(np.abs(data)) or 1.0
    data = data / peak * gain
    # Tiny fades so nothing clicks.
    fade = min(64, len(data) // 4)
    data[:fade] *= np.linspace(0, 1, fade)
    data[-fade:] *= np.linspace(1, 0, fade)
    pcm = (data * 32767).astype("<i2")
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())


def concat(*parts, gap=0.0):
    g = np.zeros(int(gap * RATE))
    out = []
    for p in parts:
        out.append(p)
        out.append(g)
    return np.concatenate(out)


def mix_at(base, part, start):
    i = int(start * RATE)
    end = min(len(base), i + len(part))
    base[i:end] += part[: end - i]


# ---- user interface -----------------------------------------------------------------------

def make_tap():
    s = sweep(900, 600, 0.07) * env(int(0.07 * RATE), curve=7)
    save("sfx_tap", s, 0.6)


def make_pop():
    n = int(0.09 * RATE)
    s = sweep(350, 1300, 0.09) * env(n, curve=5)
    save("sfx_pop", s, 0.7)


def make_coin():
    a = tone(988, 0.07, (1, 0.3, 0.15), 3)
    b = tone(1319, 0.25, (1, 0.3, 0.15), 6)
    save("sfx_coin", concat(a, b), 0.6)


# ---- game ---------------------------------------------------------------------------------

def make_drop():
    """The 'bloop' of a water drop landing in the cup."""
    n = int(0.13 * RATE)
    s = sweep(260, 1100, 0.13) * env(n, attack=0.002, curve=7)
    s += 0.05 * rng.standard_normal(n) * env(n, curve=25)
    save("sfx_drop", s, 0.55)


def make_draw():
    """Soft pencil scratch."""
    n = int(0.16 * RATE)
    noise = rng.standard_normal(n)
    s = bandpass(noise, 3200, 1.2) * np.sin(np.linspace(0, np.pi, n)) ** 2
    s *= 1 + 0.4 * np.sin(2 * np.pi * 38 * t_axis(0.16))
    save("sfx_draw", s, 0.35)


def make_sizzle():
    n = int(0.7 * RATE)
    noise = rng.standard_normal(n)
    hp = noise - lowpass(noise, 0.25)
    crackle = (rng.random(n) > 0.995) * rng.standard_normal(n) * 4
    s = (hp + crackle) * env(n, attack=0.01, curve=3.5)
    save("sfx_sizzle", s, 0.45)


def make_bonk():
    n = int(0.18 * RATE)
    s = sweep(220, 110, 0.18) * env(n, attack=0.001, curve=9)
    s += 0.3 * rng.standard_normal(n) * env(n, curve=60)
    save("sfx_bonk", s, 0.6)


def make_win():
    notes = [523.25, 659.25, 783.99, 1046.5]
    parts = [tone(f, 0.12, (1, 0.5, 0.2), 3) for f in notes]
    chord = sum(tone(f, 0.7, (1, 0.4, 0.15), 4) for f in (523.25, 659.25, 783.99, 1046.5))
    s = concat(*parts, chord)
    sparkle = np.zeros_like(s)
    for k, f in enumerate((2093, 2637, 3136)):
        mix_at(sparkle, tone(f, 0.25, (1,), 8) * 0.25, 0.5 + 0.07 * k)
    save("sfx_win", s + sparkle, 0.75)


def make_star():
    a = tone(1568, 0.08, (1, 0.2), 5)
    b = tone(2093, 0.3, (1, 0.2), 6)
    save("sfx_star", concat(a, b), 0.55)


def make_fail():
    """Cartoon 'wah wah'."""
    def wah(f0, f1, secs):
        n = int(secs * RATE)
        t = np.arange(n) / RATE
        f = np.linspace(f0, f1, n) * (1 + 0.02 * np.sin(2 * np.pi * 6 * t))
        ph = 2 * np.pi * np.cumsum(f) / RATE
        tri = 2 / np.pi * np.arcsin(np.sin(ph))
        open_close = 0.5 - 0.5 * np.cos(2 * np.pi * t / secs)
        return bandpass(tri, 700 + 900 * open_close.mean(), 1.5) * np.sin(np.pi * t / secs) ** 0.6
    s = concat(wah(392, 370, 0.28), wah(349, 330, 0.28), wah(311, 262, 0.6), gap=0.03)
    save("sfx_fail", s, 0.6)


def make_giggle():
    """A squeaky little 'hee hee hee' from a happy cup."""
    parts = []
    for k, f in enumerate((820, 760, 860, 720, 780)):
        secs = 0.11
        n = int(secs * RATE)
        t = np.arange(n) / RATE
        freq = f * (1 + 0.12 * np.sin(np.pi * t / secs))
        ph = 2 * np.pi * np.cumsum(freq) / RATE
        saw = 2 * ((ph / (2 * np.pi)) % 1) - 1
        voice = bandpass(saw, 2400, 3) + 0.6 * bandpass(saw, 3400, 4) + 0.3 * saw
        breath = 0.15 * rng.standard_normal(n)
        parts.append((voice + breath) * np.sin(np.pi * t / secs) ** 1.5)
    save("sfx_giggle", concat(*parts, gap=0.045), 0.55)


def make_whoosh():
    n = int(0.6 * RATE)
    noise = rng.standard_normal(n)
    out = np.zeros(n)
    chunk = 512
    for i in range(0, n, chunk):
        f = 400 + 2600 * (i / n)
        seg = noise[i:i + chunk]
        out[i:i + chunk] = bandpass(seg, f, 1.0)
    out *= np.sin(np.linspace(0, np.pi, n)) ** 2
    save("sfx_whoosh", out, 0.5)


# ---- music --------------------------------------------------------------------------------

def pluck(freq, secs, bright=0.5):
    """Karplus-Strong plucked string (ukulele-ish)."""
    n = int(secs * RATE)
    period = int(RATE / freq)
    buf = rng.uniform(-1, 1, period)
    out = np.zeros(n)
    for i in range(n):
        out[i] = buf[i % period]
        nxt = buf[(i + 1) % period]
        buf[i % period] = 0.996 * (bright * buf[i % period] + (1 - bright) * nxt)
    return out


def marimba(freq, secs):
    t = t_axis(secs)
    s = np.sin(2 * np.pi * freq * t) + 0.25 * np.sin(2 * np.pi * freq * 4 * t) * np.exp(-30 * t)
    return s * np.exp(-6 * t)


def make_music():
    bpm = 108
    beat = 60 / bpm
    bars = 8
    total = bars * 4 * beat
    n = int(total * RATE)
    track = np.zeros(n + RATE * 2)

    def note(name_freq, start, secs, inst, vol):
        mix_at(track, inst(name_freq, secs) * vol, start)

    # C  G  Am  F  C  G  F  G
    chords = [
        (261.63, 329.63, 392.00), (196.00, 246.94, 293.66),
        (220.00, 261.63, 329.63), (174.61, 220.00, 261.63),
        (261.63, 329.63, 392.00), (196.00, 246.94, 293.66),
        (174.61, 220.00, 261.63), (196.00, 246.94, 293.66),
    ]
    bass = [130.81, 98.00, 110.00, 87.31, 130.81, 98.00, 87.31, 98.00]
    # Strum pattern: down on 1, 2, 2+, 3+, 4
    strum = [0, 1, 1.5, 2.5, 3]
    for b, chord in enumerate(chords):
        bar_start = b * 4 * beat
        for k, s in enumerate(strum):
            for j, f in enumerate(chord):
                note(f, bar_start + s * beat + j * 0.012, beat * 1.4, pluck, 0.16 if k == 0 else 0.11)
        note(bass[b], bar_start, beat * 1.8, lambda f, d: pluck(f, d, 0.7), 0.3)
        note(bass[b] * 1.5, bar_start + 2 * beat, beat * 1.5, lambda f, d: pluck(f, d, 0.7), 0.22)

    # A bouncy little tune on marimba (MIDI-ish note numbers relative to C5).
    C5 = 523.25

    def st(semi):
        return C5 * 2 ** (semi / 12)

    melody = [
        # bar, beat, semitone, length
        (0, 0, 0, 0.5), (0, 0.5, 4, 0.5), (0, 1, 7, 1), (0, 2.5, 4, 0.5), (0, 3, 7, 1),
        (1, 0, 2, 0.5), (1, 0.5, 7, 0.5), (1, 1, 11, 1), (1, 2.5, 7, 0.5), (1, 3, 5, 1),
        (2, 0, 4, 0.5), (2, 0.5, 9, 0.5), (2, 1, 12, 1), (2, 2.5, 9, 0.5), (2, 3, 7, 1),
        (3, 0, 5, 0.5), (3, 0.5, 9, 0.5), (3, 1, 12, 0.5), (3, 1.5, 9, 0.5), (3, 2, 7, 2),
        (4, 0, 12, 0.5), (4, 0.5, 11, 0.5), (4, 1, 9, 0.5), (4, 1.5, 7, 0.5), (4, 2, 4, 1), (4, 3, 7, 1),
        (5, 0, 7, 0.5), (5, 0.5, 5, 0.5), (5, 1, 2, 1), (5, 2.5, 7, 0.5), (5, 3, 11, 1),
        (6, 0, 9, 0.5), (6, 0.5, 5, 0.5), (6, 1, 0, 1), (6, 2, 5, 0.5), (6, 2.5, 9, 0.5), (6, 3, 12, 1),
        (7, 0, 11, 0.5), (7, 0.5, 7, 0.5), (7, 1, 2, 1), (7, 2, 7, 1), (7, 3, -1, 1),
    ]
    for bar, bt, semi, length in melody:
        note(st(semi), (bar * 4 + bt) * beat, max(0.35, length * beat * 1.6), marimba, 0.32)

    # Light shaker on off-beats.
    for i in range(bars * 8):
        start = i * beat / 2
        m = int(0.05 * RATE)
        shaker = rng.standard_normal(m) * env(m, curve=12)
        shaker = shaker - lowpass(shaker, 0.3)
        mix_at(track, shaker * (0.05 if i % 2 else 0.025), start)

    # Wrap the tail back to the start so the loop is seamless.
    loop = track[:n].copy()
    tail = track[n:]
    loop[: len(tail)] += tail
    save("music_loop", loop, 0.7)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    make_tap()
    make_pop()
    make_coin()
    make_drop()
    make_draw()
    make_sizzle()
    make_bonk()
    make_win()
    make_star()
    make_fail()
    make_giggle()
    make_whoosh()
    make_music()
    for f in sorted(os.listdir(OUT)):
        print(f, os.path.getsize(os.path.join(OUT, f)))
