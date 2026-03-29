Android integration notes for the bundled `RPiPlay` source.

This folder contains Android-only adaptation layers used while porting the desktop/Linux receiver core into the app.

Current scope:
- `dnssd_android_stub.c` replaces the desktop Bonjour dependency used by `lib/dnssd.c`
- service advertisement still belongs to the Kotlin `NsdManager` layer in the app
- this is only one seam in the port; video/audio decode and rendering are still pending

The current app is not yet a full AirPlay receiver. This folder exists so the native receiver can be integrated incrementally without depending on platform APIs that do not exist on Android.
