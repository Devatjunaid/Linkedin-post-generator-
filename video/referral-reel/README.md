# Referral reel — 28 s vertical short

A 1080×1920, 30 fps, 28-second reel built entirely from code. It shows a mock AI-agent tool and ends on a link-in-bio call to action.

**Before posting, set the real tool:** edit `TOOL` at the top of `reel.html` (name, URL, your handle), then re-run `node render.js video` and the ffmpeg mux in `build.sh`. The render takes under a minute.

| Step | File | What it does |
|---|---|---|
| Script | `script.json` | Narration for the four scenes and when each scene starts |
| Voice | `tts.py` | Piper TTS (`en-us-ryan-high`), sped up evenly until the narration fits 28 s, with each scene's lines starting at most 0.8 s after its visuals (about 1.4×). Turns `--` into breath pauses and writes estimated word timings for the captions |
| Audio | `audio.py` | Phonk beat (cowbell riff, distorted 808, trap hats, 137 BPM, so 28 s is exactly 16 bars and loops cleanly) ducked to −18 dB under the voice. Sound effects: tap clicks, the "Run Agent" click at 0:08, a cash-register ding at 0:16, swooshes on cuts, a sub drop into the CTA, and a bell |
| Visuals | `reel.html` | Mock phone screen recording with a tap cursor, sticker headlines and word-by-word kinetic captions. The last 0.3 s dissolves back to the first frame so the reel loops |
| Render | `render.js` | Headless Chromium → x264 |

```sh
./build.sh path/to/en-us-ryan-high.onnx
node render.js preview 3 9 16    # 540×960 stills in build/preview/
```

The dashboard and logs are a demo UI and are labelled "Demo UI · results vary". The CTA scene carries a referral disclosure, which FTC rules and the platforms require for affiliate links.
