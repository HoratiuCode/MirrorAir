# MirrorNode

MirrorNode is a minimal Android Studio project scaffold for a black-screen AirPlay-like receiver UI backed by a foreground service, multicast support, and DNS-SD advertisement for `_airplay._tcp` and `_raop._tcp`.

## What is implemented

- `MainActivity` with a black background, centered receiver name, connection state text, and `Start Receiver` / `Stop Receiver` buttons.
- Automatic receiver startup when the app launches.
- `ReceiverService` foreground service with notification channel, multicast lock, and sticky restart behavior.
- `receiver.properties` config copied to the app files directory on first launch and used to customize the receiver name.
- Bonjour-style local network advertisement through `NsdManager`.
- Native bridge wiring through CMake and JNI so an open-source AirPlay-compatible engine can be hosted in-process.

## Critical limitation

This scaffold does **not** yet contain a real AirPlay mirroring engine. The bundled native layer is a placeholder loop so the Android project structure is buildable and the service lifecycle works.

To make Mac screen mirroring actually work, you need to port and link a real receiver core such as:

- `RPiPlay` for AirPlay mirroring support
- `openairplay` if you want to build against older AirPlay receiver code
- another Android-portable AirPlay-compatible implementation with mirroring support

That porting step is non-trivial because those projects are typically Linux or Raspberry Pi oriented and depend on platform-specific audio, video, crypto, and event-loop components that need Android NDK replacements.

## Integration point

Replace the placeholder implementation in:

- `app/src/main/cpp/openairplay_bridge.cpp`

Then extend:

- `app/src/main/cpp/CMakeLists.txt`

to compile the chosen receiver engine and its dependencies.

## Config path on device

On first launch, the asset file is copied to:

- `Context.filesDir/receiver.properties`

Keys:

- `receiver.name`
- `airplay.port`
- `raop.port`
