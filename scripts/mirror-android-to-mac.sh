#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="${ROOT_DIR}/mirror-to-mac.json"

if ! command -v scrcpy >/dev/null 2>&1; then
  printf 'scrcpy is not installed.\n' >&2
  printf 'Install it on macOS, then run this script again.\n' >&2
  printf 'Recommended: brew install scrcpy android-platform-tools\n' >&2
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  printf 'adb is not installed.\n' >&2
  printf 'Recommended: brew install android-platform-tools\n' >&2
  exit 1
fi

if [ ! -f "${CONFIG_FILE}" ]; then
  printf 'Missing config file: %s\n' "${CONFIG_FILE}" >&2
  exit 1
fi

MAX_SIZE="$(sed -n 's/.*"max_size":[[:space:]]*\([0-9][0-9]*\).*/\1/p' "${CONFIG_FILE}" | head -n 1)"
BIT_RATE="$(sed -n 's/.*"bit_rate":[[:space:]]*"\([^"]*\)".*/\1/p' "${CONFIG_FILE}" | head -n 1)"
MAX_FPS="$(sed -n 's/.*"max_fps":[[:space:]]*\([0-9][0-9]*\).*/\1/p' "${CONFIG_FILE}" | head -n 1)"
WINDOW_TITLE="$(sed -n 's/.*"window_title":[[:space:]]*"\([^"]*\)".*/\1/p' "${CONFIG_FILE}" | head -n 1)"
STAY_AWAKE="$(sed -n 's/.*"stay_awake":[[:space:]]*\(true\|false\).*/\1/p' "${CONFIG_FILE}" | head -n 1)"
TURN_SCREEN_OFF="$(sed -n 's/.*"turn_screen_off":[[:space:]]*\(true\|false\).*/\1/p' "${CONFIG_FILE}" | head -n 1)"
ENABLE_AUDIO="$(sed -n 's/.*"audio":[[:space:]]*\(true\|false\).*/\1/p' "${CONFIG_FILE}" | head -n 1)"

adb start-server >/dev/null

if ! adb devices | sed '1d' | grep -q 'device$'; then
  printf 'No Android device detected.\n' >&2
  printf 'Connect your phone/tablet with USB, enable Developer Options and USB debugging, then approve the Mac on the device.\n' >&2
  exit 1
fi

SCRCPY_ARGS=(
  --window-title "${WINDOW_TITLE:-MirrorAir Android Mirror}"
  --max-size "${MAX_SIZE:-1600}"
  --video-bit-rate "${BIT_RATE:-8M}"
  --max-fps "${MAX_FPS:-60}"
)

if [ "${STAY_AWAKE:-true}" = "true" ]; then
  SCRCPY_ARGS+=(--stay-awake)
fi

if [ "${TURN_SCREEN_OFF:-false}" = "true" ]; then
  SCRCPY_ARGS+=(--turn-screen-off)
fi

if [ "${ENABLE_AUDIO:-false}" = "false" ]; then
  SCRCPY_ARGS+=(--no-audio)
fi

exec scrcpy "${SCRCPY_ARGS[@]}"
