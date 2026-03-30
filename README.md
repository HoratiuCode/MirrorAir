# MirrorAir

![MirrorAir Apple](./red-apple_1f34e.png)

MirrorAir is an experimental Android receiver project intended to let a Mac discover an Android tablet or phone as an AirPlay-style mirroring target.

## Stack

This repository is **not** a React app.

- Android receiver app: Kotlin + Gradle + CMake/NDK in `app/`
- Download website: plain `index.html`, `styles.css`, and `app.js`
- Bundled native dependencies: `third_party/rpiplay` and `third_party/libplist`

There is no `package.json`, no `node_modules`, and no npm/yarn/pnpm install step in the current project.

## What exists now

- Minimal Android UI with a fullscreen render surface, receiver name, code, status text, and `Start Receiver` / `Stop Receiver`.
- Foreground `ReceiverService` with multicast lock and DNS-SD advertisement for `_airplay._tcp` and `_raop._tcp`.
- Bundled `RPiPlay`, `libplist`, Android OpenSSL, and a first-pass Android `MediaCodec` video path.
- Config-driven receiver name from `app/src/main/assets/receiver.properties`.
- Static download website at the repo root for Vercel, plus `docs/` for GitHub Pages.

## Why the app may stop or fail to start

If the APK opens and immediately closes, the most likely causes are:

- the native receiver library fails to load on the device ABI
- Android `MediaCodec` rejects the incoming H.264 setup for that device
- the foreground service is restricted or denied on that Android version
- Wi-Fi or notification permissions were not granted
- the AirPlay session starts but the Android renderer path still has a runtime bug

The app now guards native loading more carefully, but real device validation is still required.

## Current technical status

Mac mirroring is still **experimental**.

What is integrated:

- AirPlay-style service discovery
- bundled `RPiPlay` receiver core
- bundled `libplist`
- Android OpenSSL through Gradle prefab
- Android `SurfaceView` + `MediaCodec` receiver path

What is still not proven:

- reliable end-to-end Mac to Android mirroring on real hardware
- device-independent H.264 compatibility across Android vendors
- stable rendering on every Android version and chipset

## Website

The static website is available in two places:

- `index.html`
- `styles.css`
- `app.js`
- `downloads/`
- `docs/index.html`
- `docs/styles.css`
- `docs/app.js`

Vercel should use the repo root directly. GitHub Pages can still use `docs/`.

## Build And Install

Build the Android debug APK:

```bash
./gradlew assembleDebug
```

Install it directly over USB with `adb`:

```bash
scripts/install-debug-apk.sh
```

This direct install path is useful if the Android Package Installer UI keeps stopping on the device.

This can be published with GitHub Pages and used as the download page for the APK once you place `MirrorAir-v2.apk` in `downloads/` or `docs/downloads/`.

## Download assets

Use `scripts/prepare-downloads.sh` after generating an APK. It copies the APK into both `downloads/` and `docs/downloads/`.

## Android To Mac

If you want the reverse direction right now, use:

- `scripts/mirror-android-to-mac.sh`
- `mirror-to-mac.json`

This launches `scrcpy` on macOS to mirror an Android device to your Mac. It requires:

- `scrcpy`
- `adb`
- USB debugging enabled on the Android device

## Source Layout

The project no longer needs the external folder in `Downloads` for the AirPlay engine source snapshot.

- active bundled copy: `third_party/rpiplay`
- active bundled plist dependency: `third_party/libplist`
- older cloned vendor copy: `vendor/RPiPlay`

Use `third_party/rpiplay` as the self-contained project source base going forward.
