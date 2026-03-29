# MirrorAir

MirrorAir is an Android receiver project intended to let a Mac discover an Android tablet or phone as an AirPlay-style mirroring target.

## What exists now

- Minimal Android UI with a fullscreen render surface, centered receiver name, connection status, and `Start Receiver` / `Stop Receiver`.
- Foreground `ReceiverService` with multicast lock and DNS-SD advertisement for `_airplay._tcp` and `_raop._tcp`.
- Config-driven receiver name from `app/src/main/assets/receiver.properties`.
- Vendored upstream `RPiPlay` code in `vendor/RPiPlay`.
- Static download website in `docs/` for GitHub Pages or any static host.

## What is still missing

Mac mirroring is **not fully working yet**.

The hard part is the native Android port:

- `RPiPlay` currently targets Raspberry Pi and desktop Linux.
- Its renderers rely on Linux and Raspberry Pi stacks, not Android `MediaCodec`, `AudioTrack`, or `Surface`.
- The JNI bridge in `app/src/main/cpp/openairplay_bridge.cpp` is still a placeholder shell around the Android surface lifecycle.

So the repo is now structured for the real port, but it still needs native protocol, decode, and render adaptation before a Mac can actually mirror its screen onto Android.

## Website

The static website lives in `docs/`.

- `docs/index.html`
- `docs/styles.css`
- `docs/app.js`

For Vercel, the repo now includes `vercel.json` so the site is served from `docs/` at the domain root.

This can be published with GitHub Pages and used as the download page for the APK once you build one and place it in `docs/downloads/`.

## Download assets

Use `scripts/prepare-downloads.sh` after generating an APK in Android Studio. It copies the APK into `docs/downloads/` so the website can serve it directly.
