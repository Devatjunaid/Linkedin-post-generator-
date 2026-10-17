"""Procedural soundtrack: lo-fi synthwave bed, SFX cues, VO ducking and final mix.

Reads build/vo.wav + build/vo_timing.json, writes build/mix.wav (stereo, 48 kHz).
Everything is synthesized with numpy so the soundtrack is royalty-free by construction.
"""
import json, wave
from pathlib import Path
import numpy as np

HERE = Path(__file__).parent
BUILD = HERE / "build"
SR = 48000
DUR = 270.0
N = int(SR * DUR)
BPM = 92
BEAT = 60 / BPM
BAR = 4 * BEAT
rng = np.random.default_rng(7)

# ---------------------------------------------------------------- helpers

def db(x):
    return 10 ** (x / 20)


def spectral(x, fn):
    """Filter a signal by multiplying its spectrum by fn(freqs)."""
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    return np.fft.irfft(X * fn(f), len(x)).astype(np.float32)


def lowpass(x, fc, order=4):
    return spectral(x, lambda f: 1 / np.sqrt(1 + (f / fc) ** (2 * order)))


def highpass(x, fc, order=4):
    return spectral(x, lambda f: 1 / np.sqrt(1 + (fc / np.maximum(f, 1e-3)) ** (2 * order)))


def bandpass(x, lo, hi):
    return highpass(lowpass(x, hi), lo)


def place(buf, clip, t, gain=1.0):
    s = int(t * SR)
    if s >= len(buf):
        return
    e = min(len(buf), s + len(clip))
    buf[s:e] += clip[: e - s] * gain


def saw(freq, n, phase=0.0):
    ph = (phase + np.cumsum(np.full(n, freq / SR) if np.isscalar(freq) else freq / SR)) % 1.0
    return (2 * ph - 1).astype(np.float32)


def note(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def adsr(n, a, d, s, r):
    a, d, r = int(a * SR), int(d * SR), int(r * SR)
    sustain = max(n - a - d - r, 0)
    env = np.concatenate([np.linspace(0, 1, a, endpoint=False), np.linspace(1, s, d, endpoint=False),
                          np.full(sustain, s), np.linspace(s, 0, r)])
    return env[:n].astype(np.float32) if len(env) >= n else np.pad(env, (0, n - len(env))).astype(np.float32)


def reverb(x, seconds=2.4, mix=0.25):
    n = int(seconds * SR)
    ir = rng.standard_normal(n).astype(np.float32) * np.exp(-np.linspace(0, 7, n)).astype(np.float32)
    ir = lowpass(ir, 5000)
    size = 1 << int(np.ceil(np.log2(len(x) + n)))
    wet = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[: len(x)].astype(np.float32)
    wet *= np.sqrt(np.mean(x ** 2) / (np.mean(wet ** 2) + 1e-12))
    return x * (1 - mix) + wet * mix


def smooth_gate(intervals, ramp):
    g = np.zeros(N, np.float32)
    for a, b in intervals:
        g[max(int(a * SR), 0): int(b * SR)] = 1
    k = int(ramp * SR)
    kernel = np.hanning(2 * k + 1).astype(np.float32)
    kernel /= kernel.sum()
    return np.convolve(g, kernel, mode="same")


def section_gain(points):
    """Piecewise-linear automation from [(time, gain)]."""
    t = np.arange(N, dtype=np.float32) / SR
    ts, gs = zip(*points)
    return np.interp(t, ts, gs).astype(np.float32)


# ---------------------------------------------------------------- music

# A minor synthwave loop: Am - F - C - G, with a darker Am - Dm - F - E turn every 4th phrase
PROG_A = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]
PROG_B = [[57, 60, 64], [50, 53, 57], [53, 57, 60], [52, 56, 59]]


def chord_at(bar):
    phrase = bar // 4
    return (PROG_B if phrase % 4 == 3 else PROG_A)[bar % 4]


def build_music():
    bars = int(np.ceil(DUR / BAR)) + 1
    pad = np.zeros(N, np.float32)
    bass = np.zeros(N, np.float32)
    arp = np.zeros(N, np.float32)
    kick = np.zeros(N, np.float32)
    snare = np.zeros(N, np.float32)
    hats = np.zeros(N, np.float32)

    bar_n = int(BAR * SR)
    for b in range(bars):
        t0 = b * BAR
        ch = chord_at(b)
        # pad: 3 detuned saws per chord tone, slow swell
        n = int(BAR * SR * 1.15)
        env = adsr(n, 0.6, 0.4, 0.8, 0.9)
        tone = np.zeros(n, np.float32)
        for m in ch:
            for det in (-0.12, 0.0, 0.11):
                tone += saw(note(m) * 2 ** (det / 12), n, rng.random())
        place(pad, tone * env / 9, t0)
        # bass: root, eighth-note pulse
        root = note(ch[0] - 24)
        for k in range(8):
            ln = int(BEAT / 2 * SR * 0.9)
            w = saw(root, ln) * 0.6 + np.sign(np.sin(2 * np.pi * root * np.arange(ln) / SR)) * 0.4
            place(bass, w * adsr(ln, 0.005, 0.12, 0.55, 0.05), t0 + k * BEAT / 2)
        # arp: chord tones up an octave, 16ths
        pattern = [0, 1, 2, 1, 0, 2, 1, 2]
        for k in range(16):
            m = ch[pattern[k % 8]] + 12 + (12 if k % 8 == 6 else 0)
            ln = int(0.22 * SR)
            ph = 2 * np.pi * note(m) * np.arange(ln) / SR
            w = (np.sin(ph) + 0.3 * np.sin(2 * ph)) * np.exp(-np.arange(ln) / (0.06 * SR))
            place(arp, w.astype(np.float32), t0 + k * BEAT / 4)
        # drums
        for k in range(4):
            place(kick, KICK, t0 + k * BEAT)
            if k in (1, 3):
                place(snare, SNARE, t0 + k * BEAT)
            for h in range(2):
                place(hats, HAT, t0 + k * BEAT + h * BEAT / 2, 1.0 if h else 0.55)

    pad = lowpass(pad, 1400)
    bass = lowpass(bass, 380)
    arp = lowpass(arp, 4200)
    # tape-style ping-pong delay on the arp (dotted eighth)
    d = int(BEAT * 0.75 * SR)
    arp_l, arp_r = arp.copy(), arp.copy()
    arp_l[d:] += 0.45 * arp[:-d]
    arp_r[2 * d:] += 0.35 * arp[:-2 * d]

    # arrangement automation (scene-aware)
    drums_g = section_gain([(0, 0), (7.6, 0), (8.2, 0.55), (19.8, 0.55), (20, 1), (29.5, 1), (30, 0.8),
                            (60, 0.8), (60.2, 1), (74, 1), (75, 0.8), (165, 0.8), (195, 1), (225, 1),
                            (226, 0.0), (232, 0.0), (236, 0.8), (262, 1), (270, 1)])
    arp_g = section_gain([(0, 0), (8, 0), (12, 0.8), (20, 1), (226, 1), (230, 0.4), (240, 1), (270, 1)])
    bass_g = section_gain([(0, 0.25), (7.8, 0.25), (8.2, 0.8), (19.9, 0.8), (20, 1), (270, 1)])

    drums = lowpass(kick * 1.0 + snare * 0.55 + hats * 0.22, 9000) * drums_g
    crackle = highpass((rng.random(N) > 0.9994).astype(np.float32) * rng.standard_normal(N).astype(np.float32), 1500) * 0.15
    # gentle sidechain pump from the kick
    pump = 1 - 0.35 * np.clip(np.convolve(np.abs(kick), np.ones(int(0.12 * SR)) / (0.12 * SR), "same") * 6, 0, 1)

    mid = reverb(pad * 0.55 * pump, 3.0, 0.35) + bass * 0.5 * bass_g * pump + drums + crackle
    left = mid + reverb(arp_l, 2.0, 0.3) * 0.22 * arp_g
    right = mid + reverb(arp_r, 2.0, 0.3) * 0.22 * arp_g
    return np.stack([left, right])


def make_kick():
    n = int(0.45 * SR)
    t = np.arange(n) / SR
    f = 45 + 85 * np.exp(-t * 28)
    return (np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7)).astype(np.float32)


def make_snare():
    n = int(0.3 * SR)
    t = np.arange(n) / SR
    noise = bandpass(rng.standard_normal(n).astype(np.float32), 900, 7000) * np.exp(-t * 16)
    body = np.sin(2 * np.pi * 185 * t) * np.exp(-t * 30)
    return (noise * 0.8 + body * 0.5).astype(np.float32)


def make_hat():
    n = int(0.06 * SR)
    return (highpass(rng.standard_normal(n).astype(np.float32), 7000) * np.exp(-np.arange(n) / (0.012 * SR))).astype(np.float32)


KICK, SNARE, HAT = make_kick(), make_snare(), make_hat()

# ---------------------------------------------------------------- sfx


def sweep_noise(length, f0, f1, q=0.6, curve=2.0):
    """Band-limited noise whose centre frequency glides from f0 to f1 (a whoosh)."""
    n = int(length * SR)
    noise = rng.standard_normal(n).astype(np.float32)
    out = np.zeros(n, np.float32)
    hop, win = 512, 2048
    w = np.hanning(win).astype(np.float32)
    f = np.fft.rfftfreq(win, 1 / SR)
    for s in range(0, n - win, hop):
        p = (s / n) ** (1 / curve)
        fc = f0 * (f1 / f0) ** p
        band = np.exp(-0.5 * (np.log(np.maximum(f, 1) / fc) / q) ** 2)
        out[s:s + win] += np.fft.irfft(np.fft.rfft(noise[s:s + win] * w) * band, win).astype(np.float32) * w
    env = np.sin(np.linspace(0, np.pi, n)) ** 1.5
    return out * env / (np.abs(out).max() + 1e-9)


def whoosh():
    return sweep_noise(1.1, 300, 6000, 0.7, 1.4)


def impact():
    """Low bass drop: pitched sub sweep + noise transient."""
    n = int(3.2 * SR)
    t = np.arange(n) / SR
    f = 30 + 90 * np.exp(-t * 3.5)
    sub = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 1.1)
    hit = lowpass(rng.standard_normal(n).astype(np.float32), 900) * np.exp(-t * 14) * 0.7
    x = np.tanh((sub + hit) * 1.6)
    return (x / np.abs(x).max()).astype(np.float32)


def typing(length):
    n = int(length * SR)
    out = np.zeros(n, np.float32)
    t = 0.0
    while t < length - 0.03:
        ln = int(0.012 * SR)
        click = bandpass(rng.standard_normal(ln * 4).astype(np.float32), 1800, 9000)[:ln]
        click *= np.exp(-np.arange(ln) / (0.0025 * SR)) * rng.uniform(0.5, 1.0)
        place_local(out, click, t)
        t += rng.uniform(0.055, 0.13)
    return out / (np.abs(out).max() + 1e-9)


def place_local(buf, clip, t):
    s = int(t * SR)
    e = min(len(buf), s + len(clip))
    buf[s:e] += clip[: e - s]


def blip(freq=1320, length=0.12):
    n = int(length * SR)
    t = np.arange(n) / SR
    return (np.sin(2 * np.pi * freq * t) * np.exp(-t * 35)).astype(np.float32)


def glitch(length=0.5):
    n = int(length * SR)
    x = np.zeros(n, np.float32)
    s = 0
    while s < n:
        ln = int(rng.uniform(0.015, 0.06) * SR)
        kind = rng.integers(3)
        t = np.arange(ln) / SR
        if kind == 0:
            seg = np.sign(np.sin(2 * np.pi * rng.uniform(80, 900) * t))
        elif kind == 1:
            seg = np.round(rng.standard_normal(ln) * 2) / 2
        else:
            seg = np.zeros(ln)
        x[s:s + ln] = seg[: max(0, min(ln, n - s))] * rng.uniform(0.3, 1)
        s += ln
    return lowpass(x, 6000) * 0.8


def hum(length):
    n = int(length * SR)
    t = np.arange(n) / SR
    x = sum(np.sin(2 * np.pi * 55 * k * t + k) / k for k in (1, 2, 3, 5))
    x *= 0.6 + 0.4 * np.sin(2 * np.pi * 0.5 * t)
    env = np.minimum(1, t / 2) * np.minimum(1, (length - t) / 0.4)
    return (x * env / 2.2).astype(np.float32)


def riser(length):
    return sweep_noise(length, 200, 9000, 0.5, 0.7) * np.linspace(0.2, 1, int(length * SR)).astype(np.float32)


def click():
    n = int(0.05 * SR)
    t = np.arange(n) / SR
    return (np.sin(2 * np.pi * 2400 * t) * np.exp(-t * 160) + bandpass(rng.standard_normal(n).astype(np.float32), 2000, 8000) * np.exp(-t * 220) * 0.5).astype(np.float32)


def bell():
    n = int(2.2 * SR)
    t = np.arange(n) / SR
    x = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t * d) for f, a, d in
            ((1568, 1, 2.2), (3136, 0.4, 3.5), (4180, 0.25, 5), (2350, 0.3, 3)))
    return (x / 2).astype(np.float32)


def gear_ticks(length):
    out = np.zeros(int(length * SR), np.float32)
    for k in range(int(length / (BEAT / 2))):
        place_local(out, click() * 0.35, k * BEAT / 2)
    return out


def build_sfx():
    sfx = np.zeros(N, np.float32)
    w = whoosh()
    for t in (7.6, 29.55, 44.6, 59.6, 74.55, 89.6, 119.55, 139.6, 164.55, 181.6, 194.6, 224.55, 249.6):
        place(sfx, w, t, 0.55)
    for t in (20.0, 64.6, 102.5, 203.0, 226.0):
        place(sfx, impact(), t, 0.6)
    for t, d in ((1.2, 1.4), (4.2, 2.2), (27.6, 1.0), (30.4, 0.9), (60.3, 1.2), (64.7, 0.8), (75.4, 0.9),
                 (120.4, 0.9), (165.4, 0.9), (195.6, 1.0), (199.6, 1.0)):
        place(sfx, typing(d), t, 0.28)
    for t in (1.45, 3.4, 5.6):
        place(sfx, glitch(0.45 if t == 1.45 else 0.2), t, 0.45)
    place(sfx, hum(9.5), 19.6, 0.35)
    for k in range(10):  # database rows landing
        place(sfx, blip(1046 + 60 * k), 46.0 + k * 0.85, 0.25)
    place(sfx, blip(1760, 0.4), 55.2, 0.35)  # viral meter hits 98
    for k in range(6):  # red -> green fixes
        place(sfx, blip(880 * 2 ** (k / 12 * 2)), 93.5 + k * 1.3, 0.25)
    place(sfx, click(), 124.0, 0.8)
    for k in range(10):  # app chain lighting up
        place(sfx, blip(660 * 2 ** (k / 10), 0.16), 124.6 + k * 0.85, 0.28)
    place(sfx, gear_ticks(24), 140.5, 0.4)
    place(sfx, blip(1568, 0.3), 182.3, 0.35)  # thumbnail swap
    place(sfx, riser(4.5), 244.8, 0.35)
    place(sfx, click(), 253.0, 0.9)
    place(sfx, bell(), 254.0, 0.35)
    place(sfx, bell(), 254.45, 0.3)
    return sfx


# ---------------------------------------------------------------- mix

def main():
    with wave.open(str(BUILD / "vo.wav")) as w:
        vo = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32) / 32768
    vo = np.pad(vo, (0, max(0, N - len(vo))))[:N]
    timing = json.loads((BUILD / "vo_timing.json").read_text())

    speech = smooth_gate([(c["start"] - 0.25, c["end"] + 0.35) for c in timing], 0.25)
    speech_mask = smooth_gate([(c["start"], c["end"]) for c in timing], 0.001) > 0.5
    vo_rms = np.sqrt(np.mean(vo[speech_mask] ** 2))

    music = build_music()
    music_rms = np.sqrt(np.mean(music ** 2))
    music *= vo_rms / music_rms  # music now sits at the voice's level; automation below sets the offset

    # -18 dB under narration, rising to -7 dB in the gaps/transitions; fade in at the top, swell + fade at the end
    duck = db(-18) * speech + db(-7) * (1 - speech)
    ends = section_gain([(0, 0.4), (1.5, 1), (258, 1), (262, 1.6), (265.5, 1.6), (270, 0)])
    music *= duck * ends

    sfx = build_sfx() * (vo_rms / 0.12)
    mix = music + (vo + sfx)[None, :]
    mix *= section_gain([(0, 1), (269.4, 1), (270, 0)])
    peak = np.abs(mix).max()
    mix *= 0.93 / peak
    with wave.open(str(BUILD / "mix.wav"), "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((mix.T * 32767).astype(np.int16).tobytes())
    print(f"mix written, peak normalised from {peak:.2f}")


if __name__ == "__main__":
    main()
