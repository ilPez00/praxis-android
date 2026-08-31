> # ⚠️ Superseded — this repository is archived
>
> **The Praxis Android app is not developed here any more.** It lives in
> [`praxis_webapp`](https://github.com/ilPez00/praxis_webapp) under
> `client/android` — a Capacitor shell with a native Kotlin/Compose layer
> (widgets, Health Connect, UsageStats screen-time, biometric). Everything that
> was in this repository was absorbed there: all 105 Kotlin files, and that copy
> is a strict superset.
>
> The **iOS** client lives in [`praxis-ios`](https://github.com/ilPez00/praxis-ios).
>
> ### Why this repo looks strange in the history
>
> Between 2026-08-29 and 2026-08-31 the `main` branch of this repository held the
> **iOS** project, after an iOS port was force-pushed over the Android history.
> That was a mistake. On 2026-08-31 `main` was restored to the Android history it
> should always have carried, and the iOS tree was moved to its own repository.
>
> This repository is kept read-only rather than deleted: GitHub only redirects
> *renamed* repositories, so a deleted name can be claimed by anyone, and older
> commits and documents still reference this one.

# Praxis Android

Praxis Android app with **external app integrations**, native home screen widgets, and Quick Settings tiles for seamless goal tracking and accountability.

## 🎉 New Features (April 2026)

### 🔄 External App Integrations

Connect your favorite apps to automatically log activities and earn Praxis Points:

- **🏃 Health Connect (Google)** - Steps, workouts, sleep, heart rate
- **🏃‍♂️ Strava** - Running, cycling, hiking activities
- **📅 Google Calendar** - Events and appointments
- **🥗 Yazio** - Nutrition and meal tracking
- **⌚ Fitbit** - Activity, sleep, heart rate (alternative to Health Connect)

### 🏠 Home Screen Widgets

Add Praxis widgets directly to your Android home screen:

- **📊 Tracker Summary** - Today's entry count + connected integrations status
- **🎯 Goal Progress** - Visual progress bar with quick +5% button
- **🔥 Praxis Stats** - Streak counter and Praxis points
- **📅 Daily Axiom Quote** - Daily motivational guidance
- **💬 Axiom Chat** - Last message from your coaching session

### 📱 Quick Settings Tiles

Instant access from your notification panel:

- **🔄 Praxis Sync** - Trigger immediate sync of all integrations
- **➕ Quick Log** - Fast manual entry dialog

### 📱 Hybrid Architecture

- **Native Widgets** - Kotlin-based Android home screen widgets
- **WebView App** - Full-featured React/TypeScript webapp
- **Seamless Sync** - Real-time data sync between webapp and widgets
- **Background Sync** - WorkManager runs automatic sync every 6 hours

## Quick Start

### Build and Install

```bash
# Build everything
./build_praxis_android.sh

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Add Widgets

1. Long-press home screen
2. Tap **Widgets**
3. Find **Praxis**
4. Drag desired widget to home screen

### Add Quick Settings Tiles

1. Swipe down twice to open Quick Settings
2. Tap **Edit** (pencil icon)
3. Find **Praxis Sync** and **Quick Log**
4. Drag to active tiles

### Connect External Apps

1. Open app → Profile → Settings → Integrations
2. Tap **Connect** on your apps (Health Connect, Strava, etc.)
3. Complete authorization
4. Data will auto-sync every 6 hours

## Documentation

- **[Quick Start Guide](QUICK_START.md)** - Get started in 5 minutes
- **[Integrations Guide](INTEGRATIONS_GUIDE.md)** - Complete setup and usage guide
- **[Implementation Summary](IMPLEMENTATION_SUMMARY.md)** - Technical implementation details
- **[Build Instructions](BUILD_ANDROID.md)** - Detailed build steps
- **[Widget Architecture](BUILD_ANDROID.md#how-widget-sync-works)** - How sync works
- **[Roadmap](ROADMAP.md)** - Feature roadmap and future plans

## Project Structure

```
praxis_android/
├── app/src/main/java/com/praxis/app/
│   ├── integrations/              # External app integration framework
│   │   ├── IntegrationType.kt    # Supported integrations enum
│   │   ├── IntegrationManager.kt # Central sync coordinator
│   │   ├── models/               # Data classes
│   │   ├── api/                  # Backend API client
│   │   ├── services/             # Integration implementations
│   │   │   ├── HealthConnectService.kt
│   │   │   ├── StravaService.kt
│   │   │   ├── GoogleCalendarService.kt
│   │   │   ├── YazioService.kt
│   │   │   └── FitbitService.kt
│   │   └── worker/               # Background sync
│   │       └── IntegrationSyncWorker.kt
│   ├── tiles/                    # Quick Settings tiles
│   │   └── QuickSettingsTiles.kt
│   ├── ui/screens/               # Jetpack Compose screens
│   │   └── IntegrationsSettingsScreen.kt
│   ├── widget/                   # Home screen widgets
│   │   ├── TrackerWidgetProvider.kt
│   │   ├── ProgressWidgetProvider.kt
│   │   ├── StatsWidgetProvider.kt
│   │   ├── AxiomWidgetProvider.kt
│   │   └── DailyMessageWidgetProvider.kt
│   ├── WebAppActivity.kt         # WebView wrapper for webapp
│   └── MainActivity.kt           # Native UI entry point
├── app/src/main/assets/webapp/   # Built React app
├── QUICK_START.md                # 5-minute setup guide
├── INTEGRATIONS_GUIDE.md         # Complete integration documentation
├── IMPLEMENTATION_SUMMARY.md     # Technical implementation details
└── build_praxis_android.sh       # Build script
```

## Requirements

- Android 8.0+ (API 26)
- Node.js 20+ (for webapp build)
- Android Studio / Gradle 8+
- Health Connect app (optional, for fitness data)

## Tech Stack

- **Android**: Kotlin, Jetpack Compose, AppWidgets, Quick Settings Tiles
- **Integrations**: Health Connect, OAuth 2.0, REST APIs
- **Background**: WorkManager for periodic sync
- **Networking**: Retrofit, OkHttp, Gson
- **Web**: React, TypeScript, Material UI, Vite
- **Backend**: Supabase (auth + database), Railway (API)

## 🚀 What's New

See [INTEGRATIONS_GUIDE.md](INTEGRATIONS_GUIDE.md) for complete details on:
- Connecting external apps
- Widget configuration
- Quick Settings tiles
- Managing auto-sync

## License

Proprietary - Praxis Project
