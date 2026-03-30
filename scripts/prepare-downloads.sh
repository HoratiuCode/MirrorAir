#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOWNLOAD_DIR="${ROOT_DIR}/docs/downloads"
ROOT_DOWNLOAD_DIR="${ROOT_DIR}/downloads"

mkdir -p "${DOWNLOAD_DIR}"
mkdir -p "${ROOT_DOWNLOAD_DIR}"

find "${ROOT_DIR}/app/build/outputs/apk" -type f \( -name "*.apk" -o -name "*.aab" \) -print0 | while IFS= read -r -d '' file; do
  cp "${file}" "${DOWNLOAD_DIR}/"
  cp "${file}" "${ROOT_DOWNLOAD_DIR}/"
  if [[ "${file}" == *.apk ]]; then
    cp "${file}" "${DOWNLOAD_DIR}/MirrorAir-v2.apk"
    cp "${file}" "${ROOT_DOWNLOAD_DIR}/MirrorAir-v2.apk"
  fi
done

printf 'Copied Android build outputs into %s and %s\n' "${DOWNLOAD_DIR}" "${ROOT_DOWNLOAD_DIR}"
