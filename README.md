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

Trip recording starts automatically when Android Auto connects — you do **not** need Trippin to appear in the car launcher. After connecting to your car, you should see a persistent **"Recording trip…"** notification on your phone. When you disconnect, check the **Trips** tab in the Trippin app on your phone.

### Car screen UI (requires Play Store install)

Trippin uses Google's **Android for Cars App Library**. Google explicitly blocks Car App Library apps from appearing on real car head units unless they are installed from a **trusted source** (Google Play).

The Android Auto **Unknown sources** developer setting only applies to media and messaging apps — **it does not apply to Trippin**. Enabling it, reinstalling, or force-stopping Android Auto will not make Trippin appear in **Customize launcher** when installed from a GitHub APK.

To get Trippin on your car screen:

1. Publish the app to Google Play **Internal testing** (free, no public review required)
2. Add yourself as a tester and install Trippin from the Play Store testing link
3. Trippin should then appear in **Customize launcher**

For development without Play Store, use Google's [Desktop Head Unit (DHU)](https://developer.android.com/training/cars/testing/dhu).

## Build from source

```bash
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

Open in Android Studio Hedgehog or newer.

## Permissions

Location (foreground) and notifications for trip tracking while Android Auto is connected.
