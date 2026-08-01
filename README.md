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

### Trip tracking (works with sideloaded APK)

1. **Open Trippin once** after installing and grant **Location** and **Notifications** when prompted
2. Connect Android Auto to your car
3. Either:
   - Open Trippin on your phone (it auto-starts recording when AA is connected), or
   - Tap the **"Android Auto connected — Tap to start trip recording"** notification, or
   - On the Home screen, tap **Start trip now** when it shows Android Auto connected

You should see a persistent **"Recording trip…"** notification while driving. Trips appear in the **Trips** tab after you disconnect.

**Why open the app?** Android blocks background apps from starting location tracking when AA connects. Trippin works around this by prompting you to open the app (or doing it automatically if already open).

### Car screen UI (requires Play Store install)

Trippin uses Google's **Android for Cars App Library**. The Android Auto **Unknown sources** setting does **not** apply — sideloaded APKs will not appear in **Customize launcher**. Use Google Play **Internal testing** to get the car-screen UI.

## Build from source

```bash
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

Open in Android Studio Hedgehog or newer.

## Permissions

Location (foreground) and notifications for trip tracking while Android Auto is connected.
