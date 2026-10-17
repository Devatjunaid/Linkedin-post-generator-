# 4 AI Agents That Automate 99% of Your Work — video build

A YouTube explainer built entirely from code: 4:30 long, 3840×2160, 30 fps, 16:9.

| Step | File | What it does |
|---|---|---|
| Script | `script.json` | Narration for the six scenes, with the time each line starts so it lands on its visual beat |
| Voice | `tts.py` | Generates the narration with the [Piper](https://github.com/rhasspy/piper) neural TTS (`en-us-ryan-high`) at 1.05× speed, places each line on the timeline, and writes `captions.srt` |
| Audio | `audio.py` | Generates a lo-fi synthwave music bed (A minor, 92 BPM) plus whooshes, bass drops, typing, glitch, UI blips, gear ticks, a click and a bell. Ducks the music to −18 dB under the narration and lifts it to −7 dB between lines, then swells and fades it out at the end |
| Visuals | `scene.html` | Canvas motion graphics; `renderFrame(t)` draws any moment of the video and gives the same frame every time |
| Render | `render.js` | Headless Chromium → JPEG frames → x264, in 10 s segments that can resume after an interruption |
| Mux | `build.sh` | Joins picture and sound, normalises loudness to −14 LUFS for YouTube, and writes `build/4-ai-agents-4k.mp4` |

```sh
pip install piper-tts numpy
# voice: https://github.com/rhasspy/piper/releases/download/v0.0.2/voice-en-us-ryan-high.tar.gz
./build.sh path/to/en-us-ryan-high.onnx
node render.js preview 12 50 104      # quick 960×540 stills in build/preview/
```

Requires Node with `playwright` (Chromium), Python 3 with numpy and piper-tts, and ffmpeg.

## Scene map

| Time | Scene | Visuals |
|---|---|---|
| 0:00–0:30 | Hook | Over-the-shoulder shot of someone at a laptop at 3:47 AM with the glitch title "USING AI WRONG?" → camera swoops into a glassmorphic Agent OS dashboard with glowing node links → terminal, code and revenue montage on the bass drop → "THE 4-AGENT ECOSYSTEM" |
| 0:30–1:15 | Agent 01, Market Intelligence | Rotating dot-matrix globe sending data packets into the Research core → database filling with green rows, the viral-score gauge reaching 98% and the rows re-sorting → "10 HOURS ➔ 3 SECONDS" |
| 1:15–2:00 | Agent 02, Creative | Split screen: scrambled text decoding into a script / a 3D torus rendering → self-correction loop where red fluff lines become green, then the "0% FLUFF / 100% RETENTION" pop-up |
| 2:00–2:45 | Agent 03, Execution | One click on RUN WORKFLOW lights 10 connected apps in sequence → interlocking gears of light |
| 2:45–3:45 | Agent 04, Optimizer | Neon revenue dashboard with a thumbnail A/B swap from 3.2% to 9.6% CTR → "80 HRS/WK vs 2 HRS/WK" week grids |
| 3:45–4:30 | Outro | Zoom out from a solo founder with 4 orbiting agent avatars → Subscribe button click, bell ring, end-screen cards, fade out |

App icons are simple generic glyphs with text labels, not brand logos.
