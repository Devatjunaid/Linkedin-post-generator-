#!/usr/bin/env bash
# Full pipeline: voiceover -> soundtrack -> 4K frames -> final MP4.
# Usage: ./build.sh /path/to/piper-voice.onnx
set -euo pipefail
cd "$(dirname "$0")"
VOICE=${1:?pass a Piper .onnx voice, e.g. en-us-ryan-high.onnx}
python3 tts.py "$VOICE"
python3 audio.py
node render.js video --scale 2 --workers 3
ffmpeg -y -loglevel error -i build/video_only.mp4 -i build/mix.wav \
  -af "loudnorm=I=-14:TP=-1.0:LRA=11" -c:v copy -c:a aac -b:a 320k -ar 48000 \
  -metadata title="4 AI Agents That Automate 99% of Your Work" -movflags +faststart -shortest \
  build/4-ai-agents-4k.mp4
echo "done: build/4-ai-agents-4k.mp4 (+ build/captions.srt)"
