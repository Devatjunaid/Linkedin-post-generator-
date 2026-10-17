#!/usr/bin/env bash
# Full pipeline: voiceover -> phonk mix -> frames -> 1080x1920 MP4.
# Usage: ./build.sh /path/to/piper-voice.onnx
set -euo pipefail
cd "$(dirname "$0")"
VOICE=${1:?pass a Piper .onnx voice, e.g. en-us-ryan-high.onnx}
python3 tts.py "$VOICE"
python3 audio.py
node render.js video --workers 3
ffmpeg -y -loglevel error -i build/video_only.mp4 -i build/mix.wav \
  -af "loudnorm=I=-14:TP=-1.0:LRA=9" -c:v copy -c:a aac -b:a 256k -ar 48000 -movflags +faststart -shortest \
  build/referral-reel-9x16.mp4
echo "done: build/referral-reel-9x16.mp4 (+ build/captions.srt)"
