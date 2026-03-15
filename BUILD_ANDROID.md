# Praxis Android - Build Instructions

## Overview

Praxis Android is a hybrid app that combines:
1. **Native Kotlin widgets** for Android home screen
2. **React/TypeScript webapp** loaded via WebView

## Available Widgets

When you add Praxis widgets to your Android home screen, you can see:

| Widget | Description |
|--------|-------------|
| **Daily Axiom Quote** | Shows daily motivational quote from Axiom |
| **Axiom Chat** | Last message from your Axiom coaching session |
| **Tracker Summary** | Number of tracker entries today |
| **Goal Progress** | Current progress on your top goal (+5% button) |
| **Praxis Stats** | Your streak and Praxis points |

## Building the App

### Option 1: Automated Script (Recommended)

```bash
cd /home/gio/Praxis/praxis_android
./build_praxis_android.sh
```

This script:
1. Builds the webapp
2. Copies it to Android assets
3. Builds the APK

### Option 2: Manual Steps

#### 1. Build the webapp
```bash
cd /home/gio/Praxis/praxis_webapp/client
npm run build
```

#### 2. Copy to Android assets
```bash
rm -rf ../praxis_android/app/src/main/assets/webapp
cp -r dist ../praxis_android/app/src/main/assets/webapp
```

#### 3. Build Android APK
```bash
cd /home/gio/Praxis/praxis_android
./gradlew assembleDebug
```

### Output

APK location: `app/build/outputs/apk/debug/app-debug.apk`

## Installing on Device

### Via USB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via Wireless Debugging
```bash
adb connect <device-ip>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Adding Widgets to Home Screen

1. Long-press on Android home screen
2. Tap **Widgets**
3. Find **Praxis** in the list
4. Drag desired widget to home screen

## How Widget Sync Works

```
┌─────────────────┐
│  Praxis Webapp  │
│  (React/TS)     │
│                 │
│  Dashboard      │
│  Component      │
└────────┬────────┘
         │
         │ JavaScript Interface
         │ AndroidWidget.syncWidgetData()
         │
         ▼
┌─────────────────┐
│ WebAppActivity  │
│ (Kotlin)        │
│                 │
│ Parses JSON     │
│ Updates Store   │
└────────┬────────┘
         │
         │ WidgetDataStore.save()
         │ (SharedPreferences)
         │
         ▼
┌─────────────────┐
│ WidgetDataStore │
│                 │
│ - streak        │
│ - points        │
│ - goal info     │
│ - axiom quote   │
│ - tracker count │
└────────┬────────┘
         │
         │ Broadcasts update
         │
         ▼
┌─────────────────────────────────────────┐
│  Home Screen Widgets                    │
│  - DailyMessageWidgetProvider           │
│  - AxiomWidgetProvider                  │
│  - TrackerWidgetProvider                │
│  - ProgressWidgetProvider               │
│  - StatsWidgetProvider                  │
└─────────────────────────────────────────┘
```

## Architecture

### Webapp → Native Communication

The webapp calls the native Android interface whenever widget data changes:

```typescript
// In DesktopWidget.tsx
if ((window as any).AndroidWidget?.syncWidgetData) {
  (window as any).AndroidWidget.syncWidgetData(JSON.stringify({
    streak: 5,
    praxisPoints: 100,
    quote: "Progress is real.",
    trackers: 3,
    lastAxiom: "Great work today!"
  }));
}
```

### Native → Widget Updates

The native side caches data in SharedPreferences and broadcasts updates:

```kotlin
// In WidgetDataStore.kt
fun save(context: Context, user: User, quote: String?, trackerCount: Int, lastAxiom: String?) {
    prefs(context).edit().apply {
        putInt(KEY_STREAK, user.currentStreak)
        putInt(KEY_POINTS, user.praxisPoints)
        putString(KEY_AXIOM_QUOTE, quote)
        putInt(KEY_TRACKER_COUNT, trackerCount)
        apply()
    }
    refreshAllWidgets(context)  // Triggers widget redraw
}
```

## Project Structure

```
praxis_android/
├── app/
│   ├── src/main/
│   │   ├── java/com/praxis/app/
│   │   │   ├── widget/
│   │   │   │   ├── AxiomWidgetProvider.kt
│   │   │   │   ├── DailyMessageWidgetProvider.kt
│   │   │   │   ├── ProgressWidgetProvider.kt
│   │   │   │   ├── StatsWidgetProvider.kt
│   │   │   │   ├── TrackerWidgetProvider.kt
│   │   │   │   └── WidgetDataStore.kt
│   │   │   ├── WebAppActivity.kt      # WebView wrapper
│   │   │   ├── MainActivity.kt         # Native UI (optional)
│   │   │   └── data/                   # Models & API
│   │   ├── assets/
│   │   │   └── webapp/                 # Built React app
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── widget_axiom.xml
│   │   │   │   ├── widget_daily_message.xml
│   │   │   │   ├── widget_tracker.xml
│   │   │   │   ├── widget_progress.xml
│   │   │   │   └── widget_stats.xml
│   │   │   └── xml/
│   │   │       └── *_widget_info.xml   # Widget metadata
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── build_praxis_android.sh
```

## Troubleshooting

### Widgets show "No data"
- Open the Praxis app first to load data
- The widgets cache data from the last app session

### Webapp doesn't load
- Check that `app/src/main/assets/webapp/` contains the built files
- Rebuild the webapp: `npm run build`

### Build fails with Kotlin errors
- Run `./gradlew clean` and rebuild
- Ensure all dependencies are installed: `./gradlew build --refresh-dependencies`

### Widget doesn't update
- Force refresh by re-adding the widget
- Check logcat for errors: `adb logcat | grep Praxis`

## Release Build

For a signed release APK:

```bash
./gradlew assembleRelease
```

The keystore is configured in `app/build.gradle.kts`:
- Store: `app/praxis-release.keystore`
- Password: `praxis123`

Release APK location: `app/build/outputs/apk/release/app-release.apk`

## Next Steps

1. **Add widget refresh on app open** - Update widgets when user opens the app
2. **Add widget configuration** - Let users choose which goal/tracker to display
3. **Add dark mode support** - Match system theme in widgets
4. **Add interactive widgets** - Quick log tracker entry from widget
5. **Migrate to Material 3 widgets** - Use new Material You design
