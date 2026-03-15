# Praxis Android

Praxis Android app with native home screen widgets for tracking goals, displaying Axiom messages, and monitoring progress.

## Features

### 🏠 Home Screen Widgets

Add Praxis widgets directly to your Android home screen:

- **📅 Daily Axiom Quote** - Daily motivational guidance
- **💬 Axiom Chat** - Last message from your coaching session  
- **📊 Tracker Summary** - Today's tracker entry count
- **🎯 Goal Progress** - Visual progress bar with quick +5% button
- **🔥 Praxis Stats** - Streak counter and Praxis points

### 📱 Hybrid Architecture

- **Native Widgets** - Kotlin-based Android home screen widgets
- **WebView App** - Full-featured React/TypeScript webapp
- **Seamless Sync** - Real-time data sync between webapp and widgets

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

## Documentation

- [Build Instructions](BUILD_ANDROID.md) - Detailed build steps
- [Widget Architecture](BUILD_ANDROID.md#how-widget-sync-works) - How sync works

## Project Structure

```
praxis_android/
├── app/src/main/java/com/praxis/app/
│   ├── widget/           # Home screen widget providers
│   ├── WebAppActivity.kt # WebView wrapper for webapp
│   └── MainActivity.kt   # Native UI (optional)
├── app/src/main/assets/webapp/  # Built React app
└── build_praxis_android.sh      # Build script
```

## Requirements

- Android 8.0+ (API 26)
- Node.js 20+ (for webapp build)
- Android Studio / Gradle 8+

## Tech Stack

- **Android**: Kotlin, Jetpack Compose, AppWidgets
- **Web**: React, TypeScript, Material UI, Vite
- **Backend**: Supabase (auth + database), Railway (API)

## License

Proprietary - Praxis Project
