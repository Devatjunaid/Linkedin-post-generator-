"""Synthesize the voiceover sentence by sentence with Piper and lay it on the timeline.

Writes build/vo.wav (mono, 48 kHz, full 270 s), build/vo_timing.json and build/captions.srt.
"""
import json, subprocess, sys, wave
from pathlib import Path
import numpy as np

HERE = Path(__file__).parent
BUILD = HERE / "build"
SR_OUT = 48000
LENGTH_SCALE = 0.95   # ~1.05x speaking rate
GAP = 0.22            # pause between sentences


def synth(model, text, path):
    subprocess.run([sys.executable, "-m", "piper", "-m", str(model), "-f", str(path),
                    "--length-scale", str(LENGTH_SCALE), "--noise-scale", "0.6", "--noise-w-scale", "0.7"],
                   input=text.encode(), check=True, capture_output=True)
    with wave.open(str(path)) as w:
        sr = w.getframerate()
        x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32) / 32768
    # trim leading/trailing silence
    idx = np.where(np.abs(x) > 0.01)[0]
    x = x[max(idx[0] - int(0.02 * sr), 0): idx[-1] + int(0.06 * sr)]
    # resample to 48 kHz (linear is fine for speech at this ratio)
    n = int(len(x) * SR_OUT / sr)
    return np.interp(np.linspace(0, len(x) - 1, n), np.arange(len(x)), x).astype(np.float32)


# Spoken spellings that should read naturally on screen
CAPTION_FIXES = {"n eight n": "n8n", "A B tests": "A/B tests", "twenty-four seven": "24/7",
                 "four-agent": "4-agent", "ten hours": "10 hours", "ten-person": "10-person"}


def caption(text):
    for spoken, shown in CAPTION_FIXES.items():
        text = text.replace(spoken, shown)
    return text


def srt_time(t):
    ms = int(round(t * 1000))
    return f"{ms // 3600000:02}:{ms // 60000 % 60:02}:{ms // 1000 % 60:02},{ms % 1000:03}"


def main(model):
    BUILD.mkdir(exist_ok=True)
    spec = json.loads((HERE / "script.json").read_text())
    total = np.zeros(int(spec["duration"] * SR_OUT), np.float32)
    timing, srt = [], []
    for scene in spec["scenes"]:
        t = scene["start"] + scene["vo_offset"]
        anchors = scene.get("at", [None] * len(scene["vo"]))
        for sentence, anchor in zip(scene["vo"], anchors):
            if anchor is not None:
                t = max(t, anchor)  # never overlap the previous line
            audio = synth(model, sentence, BUILD / f"_s.wav")
            dur = len(audio) / SR_OUT
            s = int(t * SR_OUT)
            total[s:s + len(audio)] += audio
            timing.append({"scene": scene["id"], "start": round(t, 3), "end": round(t + dur, 3), "text": caption(sentence)})
            t += dur + GAP
        slack = scene["end"] - t
        print(f"{scene['id']:10s} vo ends {t:6.2f}s  (scene ends {scene['end']}s, slack {slack:5.2f}s)")
        if slack < 0.5:
            raise SystemExit(f"voiceover overruns scene {scene['id']}")
    (BUILD / "_s.wav").unlink()
    total *= 0.89 / np.abs(total).max()  # peak at about -1 dBFS: the "0 dB" lead
    with wave.open(str(BUILD / "vo.wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR_OUT)
        w.writeframes((total * 32767).astype(np.int16).tobytes())
    (BUILD / "vo_timing.json").write_text(json.dumps(timing, indent=1))
    for i, c in enumerate(timing, 1):
        srt.append(f"{i}\n{srt_time(c['start'])} --> {srt_time(c['end'])}\n{c['text']}\n")
    (BUILD / "captions.srt").write_text("\n".join(srt))


if __name__ == "__main__":
    main(Path(sys.argv[1]))
