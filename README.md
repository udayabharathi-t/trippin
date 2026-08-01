# Trippin'

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

## Android Auto setup

Trip recording starts automatically when Android Auto connects — you do **not** need to open Trippin from the car launcher for tracking to work.

To show Trippin in the car app drawer (and in **Customize launcher** on your phone), sideloaded APKs require Android Auto developer mode:

1. Open **Android Auto** on your phone → **Settings** → tap **Version** 10 times to enable developer mode
2. In **Developer settings**, enable **Unknown sources**
3. Uninstall the old Trippin APK, then install the latest release APK
4. Force-stop Android Auto (Settings → Apps → Android Auto → Force stop), then reconnect to your car
5. On your phone: Android Auto → **Customize launcher** — Trippin should now appear in the list

**Note:** Google officially limits Car App Library apps on real head units to Play Store distribution. The steps above work for sideloaded installs in most cases, but some car units may still hide non-Play-Store apps. Trip tracking in the background is unaffected.

## Build from source

```bash
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

Open in Android Studio Hedgehog or newer.

## Permissions

Location (foreground) and notifications for trip tracking while Android Auto is connected.
