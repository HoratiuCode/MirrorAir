#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOWNLOAD_DIR="${ROOT_DIR}/docs/downloads"

mkdir -p "${DOWNLOAD_DIR}"

find "${ROOT_DIR}/app/build/outputs/apk" -type f \( -name "*.apk" -o -name "*.aab" \) -print0 | while IFS= read -r -d '' file; do
  cp "${file}" "${DOWNLOAD_DIR}/"
done

printf 'Copied Android build outputs into %s\n' "${DOWNLOAD_DIR}"
