# Trippin

Trippin' is an Android Auto app that automatically tracks car trips and lets you tag and manage them on your phone.

## Download

Grab the latest APK from [GitHub Releases](https://github.com/udayabharathi-t/trippin/releases).

## Features

- **Automatic trip tracking** when Android Auto connects — odometer, fuel level, speed, and location samples
- **Multi-car support** — each car is identified on connect; name it and set fuel tank capacity
- **Refuel events** — auto-detected when fuel jumps, or entered manually with cost and price per litre (₹)
- **Trip fuel cost estimate** — computed from fuel used and your last refill price (shown as approximate)
- **Trip tagging** — name, start/end locations (auto-captured coords + manual labels)
- **Local-only storage** — all data stays on your phone; no Google account or API keys required

## Build from source

```bash
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

Open in Android Studio Hedgehog or newer.

## Permissions

Location (foreground) and notifications for trip tracking while Android Auto is connected.
