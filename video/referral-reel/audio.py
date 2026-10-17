"""Procedural phonk beat + SFX + ducked VO mix for the 28 s reel.

Reads build/vo.wav and build/words.json, writes build/mix.wav (stereo 48 kHz).
The beat is exactly 16 bars long (137.14 BPM) and wraps around, so the reel loops seamlessly.
"""
import json, wave
from pathlib import Path
import numpy as np

HERE = Path(__file__).parent
BUILD = HERE / "build"
SR = 48000
DUR = 28.0
N = int(SR * DUR)
BARS = 16
BAR = DUR / BARS
BEAT = BAR / 4
S16 = BEAT / 4
rng = np.random.default_rng(3)


def db(x):
    return 10 ** (x / 20)


def spectral(x, fn):
    f = np.fft.rfftfreq(len(x), 1 / SR)
    return np.fft.irfft(np.fft.rfft(x) * fn(f), len(x)).astype(np.float32)


def lowpass(x, fc, order=4):
    return spectral(x, lambda f: 1 / np.sqrt(1 + (f / fc) ** (2 * order)))


def highpass(x, fc, order=4):
    return spectral(x, lambda f: 1 / np.sqrt(1 + (fc / np.maximum(f, 1e-3)) ** (2 * order)))


def bandpass(x, lo, hi):
    return highpass(lowpass(x, hi), lo)


def place(buf, clip, t, gain=1.0, wrap=False):
    """Add clip at time t; with wrap=True anything past the end continues from the start (loop)."""
    s = int(round(t * SR)) % len(buf) if wrap else int(round(t * SR))
    if s >= len(buf):
        return
    e = min(len(buf), s + len(clip))
    buf[s:e] += clip[: e - s] * gain
    if wrap and s + len(clip) > len(buf):
        rest = clip[e - s:]
        buf[: len(rest)] += rest * gain


def note(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def env_exp(n, tau):
    return np.exp(-np.arange(n) / (tau * SR)).astype(np.float32)


# ---------------------------------------------------------------- instruments

def kick():
    n = int(0.35 * SR); t = np.arange(n) / SR
    f = 48 + 140 * np.exp(-t * 35)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9)
    return np.tanh(x * 2.2).astype(np.float32) * 0.9


def clap():
    n = int(0.25 * SR)
    x = np.zeros(n, np.float32)
    for k, d in enumerate((0, 0.011, 0.022)):
        b = bandpass(rng.standard_normal(n).astype(np.float32), 900, 5000) * env_exp(n, 0.012 if k < 2 else 0.07)
        x[int(d * SR):] += b[: n - int(d * SR)]
    return x / np.abs(x).max()


def hat(open_=False):
    n = int((0.18 if open_ else 0.045) * SR)
    return highpass(rng.standard_normal(n).astype(np.float32), 8000) * env_exp(n, 0.05 if open_ else 0.01)


def cowbell(m, length=0.22):
    """The phonk signature: two detuned square-ish partials through a band-pass, fast decay."""
    n = int(length * SR); t = np.arange(n) / SR
    f = note(m)
    x = np.sign(np.sin(2 * np.pi * f * t)) * 0.6 + np.sign(np.sin(2 * np.pi * f * 1.48 * t)) * 0.4
    x = bandpass(x.astype(np.float32), 400, 4000) * env_exp(n, 0.07)
    return x.astype(np.float32)


def bass808(m, length, glide_from=None):
    n = int(length * SR); t = np.arange(n) / SR
    f = np.full(n, note(m))
    if glide_from is not None:
        f = note(m) + (note(glide_from) - note(m)) * np.exp(-t * 18)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR)
    x = np.tanh(x * 3.0) * np.minimum(1, t / 0.004) * np.exp(-t * 1.2)
    x[-int(0.02 * SR):] *= np.linspace(1, 0, int(0.02 * SR))
    return x.astype(np.float32)


# C# minor / phrygian riff (16ths over one bar), -1 = rest
RIFF_A = [61, -1, 61, 64, -1, 61, 66, -1, 64, -1, 61, -1, 62, -1, 61, 59]
RIFF_B = [61, -1, 61, 64, -1, 68, 66, -1, 64, -1, 66, 64, 62, -1, 61, -1]
BASS_ROOTS = [37, 37, 34, 36]  # C#1 - A#0 - C1 under the riff, one per bar
HATS = "x.x.x.x.x.x.xxxx"


def build_beat():
    k, c, h, cb, b = (np.zeros(N, np.float32) for _ in range(5))
    K, CL, HC, HO = kick(), clap(), hat(), hat(True)
    for bar in range(BARS):
        t0 = bar * BAR
        for s in (0, 6, 10) if bar % 2 else (0, 7, 10, 14):
            place(k, K, t0 + s * S16, wrap=True)
        place(c, CL, t0 + 2 * BEAT, wrap=True)
        if bar % 4 == 3:
            place(c, CL, t0 + 3.75 * BEAT, 0.6, wrap=True)
        for s, ch in enumerate(HATS if bar % 2 else HATS[:12] + "x.x."):
            if ch == "x":
                place(h, HC, t0 + s * S16, 0.9 if s % 4 == 0 else 0.55, wrap=True)
        place(h, HO, t0 + 3.5 * BEAT, 0.5, wrap=True)
        riff = RIFF_B if bar % 4 == 3 else RIFF_A
        for s, m in enumerate(riff):
            if m > 0:
                place(cb, cowbell(m), t0 + s * S16, wrap=True)
        root = BASS_ROOTS[bar % 4]
        place(b, bass808(root, BAR * 0.6, glide_from=root + 12 if bar % 2 else None), t0, wrap=True)
        place(b, bass808(root + 3, BAR * 0.35, glide_from=root), t0 + 2.5 * BEAT, 0.8, wrap=True)
    cb_l, cb_r = cb.copy(), cb.copy()
    d = int(S16 * 3 * SR)
    cb_l += 0.35 * np.roll(cb, d); cb_r += 0.3 * np.roll(cb, 2 * d)  # roll == wrap-around delay
    bass = lowpass(b, 900)
    pump = 1 - 0.45 * np.clip(np.convolve(np.abs(k), np.ones(int(0.1 * SR)) / (0.1 * SR), "same") * 6, 0, 1)
    drums = k * 1.0 + c * 0.45 + h * 0.18
    mid = drums + bass * 0.55 * pump
    return np.stack([mid + cb_l * 0.3 * pump, mid + cb_r * 0.3 * pump])


# ---------------------------------------------------------------- sfx

def sweep_noise(length, f0, f1, q=0.6, curve=1.4):
    n = int(length * SR)
    noise = rng.standard_normal(n).astype(np.float32)
    out = np.zeros(n, np.float32)
    hop, win = 512, 2048
    w = np.hanning(win).astype(np.float32)
    f = np.fft.rfftfreq(win, 1 / SR)
    for s in range(0, n - win, hop):
        fc = f0 * (f1 / f0) ** ((s / n) ** (1 / curve))
        band = np.exp(-0.5 * (np.log(np.maximum(f, 1) / fc) / q) ** 2)
        out[s:s + win] += np.fft.irfft(np.fft.rfft(noise[s:s + win] * w) * band, win).astype(np.float32) * w
    return out * (np.sin(np.linspace(0, np.pi, n)) ** 1.5) / (np.abs(out).max() + 1e-9)


def click():
    n = int(0.05 * SR); t = np.arange(n) / SR
    x = np.sin(2 * np.pi * 2200 * t) * np.exp(-t * 170) + bandpass(rng.standard_normal(n).astype(np.float32), 2000, 9000) * np.exp(-t * 260) * 0.7
    return x.astype(np.float32)


def cash_register():
    """'Cha-ching': drawer rattle then two bright bell strikes."""
    n = int(1.4 * SR); t = np.arange(n) / SR
    x = np.zeros(n, np.float32)
    rattle = bandpass(rng.standard_normal(int(0.12 * SR)).astype(np.float32), 2500, 9000) * env_exp(int(0.12 * SR), 0.03)
    x[: len(rattle)] += rattle * 0.6
    for t0, g in ((0.09, 0.8), (0.2, 1.0)):
        s = int(t0 * SR); tt = t[: n - s]
        bell = sum(a * np.sin(2 * np.pi * f * tt) * np.exp(-tt * d) for f, a, d in
                   ((2093, 1, 4), (4186, 0.5, 6), (2794, 0.4, 5), (6272, 0.2, 9)))
        x[s:] += bell.astype(np.float32) * g
    return x / np.abs(x).max()


def bell():
    n = int(1.6 * SR); t = np.arange(n) / SR
    x = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t * d) for f, a, d in ((1568, 1, 2.6), (3136, 0.4, 4), (2350, 0.3, 3.4)))
    return (x / 2).astype(np.float32)


def typing(length):
    n = int(length * SR); out = np.zeros(n, np.float32); t = 0.0
    while t < length - 0.03:
        ln = int(0.012 * SR)
        c = bandpass(rng.standard_normal(ln * 4).astype(np.float32), 1800, 9000)[:ln] * env_exp(ln, 0.0025) * rng.uniform(0.5, 1)
        s = int(t * SR); out[s:s + ln] += c[: n - s]
        t += rng.uniform(0.045, 0.09)
    return out / (np.abs(out).max() + 1e-9)


def blip(freq, length=0.1):
    n = int(length * SR); t = np.arange(n) / SR
    return (np.sin(2 * np.pi * freq * t) * np.exp(-t * 40)).astype(np.float32)


def sub_drop():
    n = int(1.5 * SR); t = np.arange(n) / SR
    f = 32 + 80 * np.exp(-t * 4)
    return np.tanh(np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 1.6) * 1.8).astype(np.float32)


def build_sfx():
    sfx = np.zeros(N, np.float32)
    swoosh = sweep_noise(0.55, 400, 9000, 0.7)
    for t in (1.85, 4.75, 13.75, 17.75, 20.75, 24.75):   # zoom + scene cuts
        place(sfx, swoosh, t, 0.55)
    for t, g in ((0.35, 0.6), (8.0, 1.0), (22.2, 0.7), (24.4, 0.7), (26.4, 0.8)):
        place(sfx, click(), t, g)
    place(sfx, typing(0.7), 0.8, 0.3)
    place(sfx, typing(0.6), 23.2, 0.25)
    for k in range(12):  # terminal lines landing
        place(sfx, blip(1046 * 2 ** ((k % 4) / 12 * 3), 0.08), 8.55 + k * 0.42, 0.22)
    place(sfx, cash_register(), 16.0, 0.6)
    place(sfx, bell(), 26.6, 0.35)
    place(sfx, bell(), 26.95, 0.3)
    place(sfx, sub_drop(), 21.0, 0.6)  # the "confident drop" into the CTA
    place(sfx, sub_drop(), 18.0, 0.4)
    return sfx


def main():
    with wave.open(str(BUILD / "vo.wav")) as w:
        vo = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32) / 32768
    vo = np.pad(vo, (0, max(0, N - len(vo))))[:N]
    words = json.loads((BUILD / "words.json").read_text())

    gate = np.zeros(N, np.float32)
    for wd in words:
        gate[int((wd["start"] - 0.12) * SR): int((wd["end"] + 0.2) * SR)] = 1
    k = int(0.08 * SR); kern = np.hanning(2 * k + 1).astype(np.float32); kern /= kern.sum()
    speech = np.convolve(gate, kern, "same")
    mask = np.zeros(N, bool)
    for wd in words:
        mask[int(wd["start"] * SR): int(wd["end"] * SR)] = True
    vo_rms = np.sqrt(np.mean(vo[mask] ** 2))

    beat = build_beat()
    beat *= vo_rms / np.sqrt(np.mean(beat ** 2))
    beat *= db(-18) * speech + db(-6) * (1 - speech)   # -18 dB under the voice, up between lines

    sfx = build_sfx() * (vo_rms / 0.12)
    mix = beat + (vo + sfx)[None, :]
    mix *= 0.93 / np.abs(mix).max()
    with wave.open(str(BUILD / "mix.wav"), "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((mix.T * 32767).astype(np.int16).tobytes())
    print("mix written")


if __name__ == "__main__":
    main()
