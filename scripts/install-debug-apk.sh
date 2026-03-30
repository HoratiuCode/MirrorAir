#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"

if ! command -v adb >/dev/null 2>&1; then
  printf 'adb is not installed.\n' >&2
  printf 'Recommended: brew install android-platform-tools\n' >&2
  exit 1
fi

cd "${ROOT_DIR}"

printf 'Building debug APK...\n'
./gradlew assembleDebug

if [ ! -f "${APK_PATH}" ]; then
  printf 'Debug APK not found at %s\n' "${APK_PATH}" >&2
  exit 1
fi

adb start-server >/dev/null

if ! adb devices | sed '1d' | grep -q 'device$'; then
  printf 'No Android device detected.\n' >&2
  printf 'Connect a device with USB debugging enabled and approve this Mac, then run again.\n' >&2
  exit 1
fi

printf 'Installing %s\n' "${APK_PATH}"
adb install -r "${APK_PATH}"

printf 'Install complete.\n'
