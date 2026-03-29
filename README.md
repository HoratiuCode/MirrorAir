# MirrorAir

MirrorAir is an Android receiver project intended to let a Mac discover an Android tablet or phone as an AirPlay-style mirroring target.

## What exists now

- Minimal Android UI with a fullscreen render surface, centered receiver name, connection status, and `Start Receiver` / `Stop Receiver`.
- Foreground `ReceiverService` with multicast lock and DNS-SD advertisement for `_airplay._tcp` and `_raop._tcp`.
- Config-driven receiver name from `app/src/main/assets/receiver.properties`.
- Vendored upstream `RPiPlay` code in `vendor/RPiPlay`.
- Bundled self-contained `RPiPlay` snapshot in `third_party/rpiplay`.
- Static download website at the repo root for Vercel, plus `docs/` for GitHub Pages.

## What is still missing

Mac mirroring is **not fully working yet**.

The hard part is the native Android port:

- `RPiPlay` currently targets Raspberry Pi and desktop Linux.
- Its renderers rely on Linux and Raspberry Pi stacks, not Android `MediaCodec`, `AudioTrack`, or `Surface`.
- The JNI bridge in `app/src/main/cpp/openairplay_bridge.cpp` is still a placeholder shell around the Android surface lifecycle.

So the repo is now structured for the real port, but it still needs native protocol, decode, and render adaptation before a Mac can actually mirror its screen onto Android.

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

This can be published with GitHub Pages and used as the download page for the APK once you build one and place it in `docs/downloads/`.

## Download assets

Use `scripts/prepare-downloads.sh` after generating an APK in Android Studio. It copies the APK into both `downloads/` and `docs/downloads/`.

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
- older cloned vendor copy: `vendor/RPiPlay`

Use `third_party/rpiplay` as the self-contained project source base going forward.
