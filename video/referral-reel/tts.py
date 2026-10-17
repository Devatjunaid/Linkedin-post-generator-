"""Voiceover for the reel: Piper TTS, sped up (uniformly) just enough to fit the reel.

Writes build/vo.wav (mono 48 kHz, full length), build/words.json (word timings for the
kinetic captions) and build/captions.srt.
"""
import json, re, subprocess, sys, wave
from pathlib import Path
import numpy as np

HERE = Path(__file__).parent
BUILD = HERE / "build"
SR_OUT = 48000
BASE_SCALE = 1 / 1.1      # 1.1x conversational speed
MIN_SCALE = 0.70          # never faster than ~1.4x
MAX_DRIFT = 0.8           # a scene's narration may start at most this late vs. its visuals
GAP = 0.10                # pause between sentences
DASH_PAUSE = 0.16         # "--" becomes a short breath pause

SPOKEN = {"2026": "twenty twenty-six", "1-click": "one-click", "40": "forty"}


def spoken(text):
    """Text as Piper should say it: no caps (they get spelled out), numbers in words, dashes as commas."""
    for k, v in SPOKEN.items():
        text = text.replace(k, v)
    text = text.replace("--", ",")
    return re.sub(r"\b[A-Z]{2,}\b", lambda m: m.group(0).lower(), text)


def synth(model, text, scale):
    tmp = BUILD / "_s.wav"
    subprocess.run([sys.executable, "-m", "piper", "-m", str(model), "-f", str(tmp),
                    "--length-scale", f"{scale:.3f}", "--noise-scale", "0.667", "--noise-w-scale", "0.8"],
                   input=spoken(text).encode(), check=True, capture_output=True)
    with wave.open(str(tmp)) as w:
        sr = w.getframerate()
        x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32) / 32768
    tmp.unlink()
    idx = np.where(np.abs(x) > 0.01)[0]
    x = x[max(idx[0] - int(0.015 * sr), 0): idx[-1] + int(0.05 * sr)]
    n = int(len(x) * SR_OUT / sr)
    x = np.interp(np.linspace(0, len(x) - 1, n), np.arange(len(x)), x).astype(np.float32)
    if text.rstrip().endswith("--"):
        x = np.concatenate([x, np.zeros(int(DASH_PAUSE * SR_OUT), np.float32)])
    return x


def word_times(sentence, start, dur):
    """Spread a sentence's duration over its words, weighted by length (punctuation adds a little)."""
    words = sentence.replace("--", "").split()
    weights = [len(re.sub(r"\W", "", w)) + 2 + (2 if re.search(r"[,.!?]$", w) else 0) for w in words]
    if sentence.rstrip().endswith("--"):
        dur -= DASH_PAUSE
    total, t, out = sum(weights), start, []
    for w, k in zip(words, weights):
        d = dur * k / total
        out.append({"w": w, "start": round(t, 3), "end": round(t + d, 3),
                    "emph": bool(re.fullmatch(r"[A-Z0-9$]{2,}[,.!?]?|\d+\S*", w))})
        t += d
    return out


def srt_time(t):
    ms = int(round(t * 1000))
    return f"00:00:{ms // 1000:02},{ms % 1000:03}"


def main(model):
    BUILD.mkdir(exist_ok=True)
    spec = json.loads((HERE / "script.json").read_text())
    total = np.zeros(int(spec["duration"] * SR_OUT), np.float32)
    words, srt = [], []
    sentences = [(sc, s) for sc in spec["scenes"] for s in sc["vo"]]
    limit = spec["duration"] - 0.35  # leave a breath before the loop point
    scale = BASE_SCALE
    while True:
        clips = [synth(model, s, scale) for _, s in sentences]
        # each scene starts at its cue, or right after the previous line if that ran long
        t, plan = 0.0, []
        for (sc, s), clip in zip(sentences, clips):
            if s == sc["vo"][0]:
                t = max(t, sc["start"] + sc["vo_offset"])
            plan.append((sc, s, clip, t))
            t += len(clip) / SR_OUT + GAP
        end_t = t - GAP
        drift = max(t0 - (sc["start"] + sc["vo_offset"]) for sc, s, _, t0 in plan if s == sc["vo"][0])
        if (end_t <= limit and drift <= MAX_DRIFT) or scale <= MIN_SCALE:
            break
        scale = max(MIN_SCALE, scale * 0.96)
    print(f"speed {1 / scale:.2f}x, narration ends {end_t:.2f}s")
    if end_t > limit:
        raise SystemExit("narration does not fit the reel")
    for sc, s, clip, t in plan:
        if s == sc["vo"][0]:
            print(f"  {sc['id']:9s} starts {t:5.2f}s (cue {sc['start'] + sc['vo_offset']:.2f}s)")
        a = int(t * SR_OUT)
        total[a:a + len(clip)] += clip
        dur = len(clip) / SR_OUT
        words += word_times(s, t, dur)
        srt.append((t, t + dur, s.replace("--", " —").replace("  ", " ")))
    total *= 0.89 / np.abs(total).max()
    with wave.open(str(BUILD / "vo.wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR_OUT)
        w.writeframes((total * 32767).astype(np.int16).tobytes())
    (BUILD / "words.json").write_text(json.dumps(words, indent=0))
    (BUILD / "captions.srt").write_text("\n".join(f"{i}\n{srt_time(a)} --> {srt_time(b)}\n{s}\n" for i, (a, b, s) in enumerate(srt, 1)))


if __name__ == "__main__":
    main(Path(sys.argv[1]))
