# Praxis Android - External App Integrations

## Overview

Praxis Android now features **automatic logging** from external apps and services directly into Praxis trackers. Connect your favorite health, fitness, productivity, and lifestyle apps to automatically track your activities and earn Praxis Points.

## Features

### 🔄 Supported Integrations

#### 1. **Health Connect** (Google) - ⭐ HIGHEST PRIORITY
- **What it tracks:** Steps, workouts, sleep, heart rate, weight, nutrition
- **Setup:** No OAuth required - just grant Android permissions
- **Tracker Mapping:**
  - Steps → `steps` tracker
  - Workouts → `cardio` tracker
  - Sleep → `sleep` tracker
  - Heart Rate → `meditation` tracker
- **Permissions Required:**
  - `android.permission.health.READ_STEPS`
  - `android.permission.health.READ_WORKOUTS`
  - `android.permission.health.READ_SLEEP`
  - `android.permission.health.READ_HEART_RATE`
  - `android.permission.health.READ_WEIGHT`

#### 2. **Strava**
- **What it tracks:** Running, cycling, hiking, swimming activities
- **Setup:** OAuth 2.0 authentication
- **Tracker Mapping:**
  - Run/Ride/Hike/Swim → `cardio` tracker
  - Yoga → `meditation` tracker
- **Data Logged:**
  - Activity type
  - Duration (minutes)
  - Distance (km)
  - Elevation gain (m)
- **OAuth Setup:**
  1. Create app at https://developers.strava.com/
  2. Get Client ID and Secret
  3. Update `StravaService.kt` with your credentials
  4. Redirect URI: `praxis://strava/callback`

#### 3. **Google Calendar**
- **What it tracks:** Events, meetings, appointments
- **Setup:** Google Account picker (OAuth handled by Google Play Services)
- **Tracker Mapping:**
  - All events → `journal` tracker
- **Data Logged:**
  - Event title
  - Start time
  - Duration
  - Location
  - Description
- **Permissions:** `CALENDAR_READONLY` scope

#### 4. **Yazio**
- **What it tracks:** Nutrition, calories, macronutrients
- **Setup:** OAuth 2.0 authentication
- **Tracker Mapping:**
  - Daily nutrition summary → `meal` tracker
- **Data Logged:**
  - Total calories
  - Protein (g)
  - Carbohydrates (g)
  - Fat (g)
  - Number of meals
- **OAuth Setup:**
  1. Contact Yazio for API access
  2. Update `YazioService.kt` with credentials
  3. Redirect URI: `praxis://yazio/callback`

#### 5. **Fitbit**
- **What it tracks:** Activity, sleep, heart rate (alternative to Health Connect)
- **Setup:** OAuth 2.0 authentication
- **Tracker Mapping:**
  - Daily activity → `cardio` tracker
  - Sleep → `sleep` tracker
  - Heart rate → `meditation` tracker
- **Data Logged:**
  - Steps, distance, calories, active minutes
  - Sleep duration and efficiency
  - Heart rate (avg, max, min)
- **OAuth Setup:**
  1. Create app at https://dev.fitbit.com/
  2. Update `FitbitService.kt` with credentials
  3. Redirect URI: `praxis://fitbit/callback`

---

## 🏠 Home Screen Widgets

### Available Widgets

Add Praxis widgets to your Android home screen:

1. **Long-press** on your home screen
2. Tap **Widgets**
3. Find **Praxis**
4. Drag your desired widget to the home screen

### Widget Types

#### 1. **Praxis Trackers Widget**
- Shows today's tracker entry count
- Displays which integrations are connected and auto-syncing
- Tap to open the app
- **Live Updates:** Automatically refreshes when integrations sync

#### 2. **Goal Progress Widget**
- Visual progress bar for your active goals
- Quick +5% button to update progress
- Tap to open goal details

#### 3. **Praxis Stats Widget**
- Current streak count
- Praxis Points earned
- Tap to view full analytics

#### 4. **Daily Axiom Quote Widget**
- Daily motivational guidance from Axiom AI
- Tap to start a coaching session

#### 5. **Axiom Chat Widget**
- Last message from your coaching session
- Tap to continue the conversation

---

## 📱 Quick Settings Tiles

Add Quick Settings tiles to your notification panel for instant access:

### How to Add Tiles

1. Swipe down twice to open Quick Settings
2. Tap the **pencil icon** (Edit)
3. Scroll to find **Praxis Sync** and **Quick Log**
4. Drag them to your active tiles

### Tile Functions

#### **Praxis Sync Tile**
- **Tap to:** Trigger immediate sync of all connected integrations
- **Shows:** Active when syncing, inactive when idle
- **Opens:** App after sync completes

#### **Quick Log Tile**
- **Tap to:** Open the app with Quick Log dialog pre-opened
- **Use for:** Manually logging activities not covered by integrations

---

## ⚙️ Setup Instructions

### Step 1: Build and Install

```bash
cd /home/gio/Praxis/praxis_android
./build_praxis_android.sh

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Connect Integrations

1. Open Praxis app
2. Navigate to **Settings** → **Integrations**
3. Tap **Connect** on the integration you want to enable
4. Follow the OAuth/permission flow
5. Integration will start syncing automatically

### Step 3: Configure Auto-Sync

- Integrations sync automatically every **6 hours** via WorkManager
- To trigger manual sync:
  - Use the **Sync All** button in Integrations settings
  - Tap the **Praxis Sync** Quick Settings tile
  - Pull down on the widget (if supported)

### Step 4: Add Widgets (Optional)

1. Long-press home screen
2. Tap **Widgets**
3. Find **Praxis**
4. Drag widget to home screen

---

## 🔧 Architecture

### File Structure

```
praxis_android/app/src/main/java/com/praxis/app/
├── integrations/
│   ├── IntegrationType.kt              # Enum of supported integrations
│   ├── IntegrationManager.kt           # Central manager for all integrations
│   ├── models/
│   │   └── TrackerModels.kt            # Data classes (TrackerEntry, IntegrationConfig, etc.)
│   ├── api/
│   │   └── PraxisApi.kt                # Retrofit API client and interfaces
│   ├── services/
│   │   ├── IntegrationService.kt       # Base interface for all services
│   │   ├── HealthConnectService.kt     # Health Connect implementation
│   │   ├── StravaService.kt            # Strava implementation
│   │   ├── GoogleCalendarService.kt    # Google Calendar implementation
│   │   ├── YazioService.kt             # Yazio implementation
│   │   └── FitbitService.kt            # Fitbit implementation
│   └── worker/
│       └── IntegrationSyncWorker.kt    # WorkManager background sync worker
├── tiles/
│   └── QuickSettingsTiles.kt           # Quick Settings tiles for sync and quick log
├── ui/screens/
│   └── IntegrationsSettingsScreen.kt   # Settings UI for managing integrations
└── widget/
    └── TrackerWidgetProvider.kt        # Enhanced widget with integration status
```

### Data Flow

```
External App (Strava, Health Connect, etc.)
    ↓
IntegrationService.sync()
    ↓
IntegrationManager.logTrackerEntry()
    ↓
Praxis Backend API (/api/trackers/log)
    ↓
Praxis Points awarded
    ↓
Widget UI updates
```

### Background Sync

- **WorkManager** schedules periodic sync every 6 hours
- Constraints: Requires network connection
- Retry logic: Failed syncs are retried automatically
- Battery optimized: Respects Android Doze mode

---

## 🛠️ Adding New Integrations

### Step 1: Define Integration Type

Add to `IntegrationType.kt`:

```kotlin
enum class IntegrationType {
    // ...existing types...
    
    NEW_INTEGRATION(
        id = "new_integration",
        displayName = "New Integration",
        description = "What it tracks",
        iconResId = android.R.drawable.ic_menu_add,
        requiresOAuth = true
    )
}
```

### Step 2: Create Service

Create `NewIntegrationService.kt` implementing `IntegrationService`:

```kotlin
class NewIntegrationService(
    private val context: Context
) : IntegrationService {
    
    override val integrationType = IntegrationType.NEW_INTEGRATION
    
    override suspend fun isAvailable(): Boolean {
        // Check if integration is connected
    }
    
    override suspend fun initialize(): Boolean {
        // Setup OAuth, permissions, etc.
    }
    
    override suspend fun sync(): SyncResult {
        // Fetch data from external API
        // Log to Praxis trackers
    }
}
```

### Step 3: Register Service

In `MainActivity.kt` or `Application` class:

```kotlin
IntegrationManager.registerIntegration(
    IntegrationType.NEW_INTEGRATION,
    NewIntegrationService(context)
)
```

### Step 4: Update Settings UI

Add to `getIntegrationItems()` in `IntegrationsSettingsScreen.kt`:

```kotlin
IntegrationItem(
    type = IntegrationType.NEW_INTEGRATION,
    name = "New Integration",
    description = "What it tracks",
    icon = android.R.drawable.ic_menu_add,
    isConnected = IntegrationManager.isConnected(IntegrationType.NEW_INTEGRATION),
    lastSync = null
)
```

### Step 5: Update `handleConnect()`

Add OAuth/connection logic in `IntegrationsSettingsScreen.kt`:

```kotlin
IntegrationType.NEW_INTEGRATION -> {
    // Launch OAuth or permission flow
}
```

---

## 🔐 Security & Privacy

### Token Storage

- All OAuth tokens are stored encrypted in **SharedPreferences**
- In production, use **Android Keystore** for encryption
- Never store raw credentials

### Permissions

- Health Connect requires runtime permissions for each data type
- Users can revoke permissions at any time in Android Settings
- Integrations can be disabled in-app

### Data Transmission

- All API calls use HTTPS
- Bearer tokens sent in Authorization header
- Backend validates tokens and awards Praxis Points

---

## 📊 Backend Requirements

### API Endpoints Used

```
POST /api/trackers/log
  Body: { type, data, source }
  Response: { success, entryId, pointsAwarded }

GET /api/trackers/my?days=14
  Response: List of trackers with entries

GET /api/trackers/summary/today
  Response: { count, trackers }

GET /api/integrations
  Response: List of user's connected integrations

POST /api/integrations/{type}/sync
  Trigger manual sync for specific integration
```

### Database Schema (Already Exists)

```sql
-- tracker_entries table
CREATE TABLE tracker_entries (
  id UUID PRIMARY KEY,
  tracker_id UUID REFERENCES trackers(id),
  user_id UUID REFERENCES users(id),
  data JSONB NOT NULL,
  source TEXT DEFAULT 'manual',
  logged_at TIMESTAMP
);

-- Suggested addition for audit trail
CREATE TABLE auto_logged_entries (
  id UUID PRIMARY KEY,
  tracker_entry_id UUID REFERENCES tracker_entries(id),
  integration_type TEXT NOT NULL,
  original_data JSONB,
  synced_at TIMESTAMP DEFAULT NOW()
);
```

---

## 🧪 Testing

### Manual Testing

1. **Health Connect:**
   - Install Health Connect app from Play Store
   - Add sample data (steps, workouts)
   - Connect integration in Praxis
   - Verify entries are logged

2. **Strava:**
   - Use Strava test account
   - Create test activities
   - Complete OAuth flow
   - Check if activities are logged

3. **Widgets:**
   - Add widgets to home screen
   - Verify they update when data changes
   - Test click actions

4. **Quick Settings Tiles:**
   - Add tiles to Quick Settings
   - Tap to trigger sync
   - Verify sync completes

### Automated Testing (Future)

- Unit tests for each IntegrationService
- Mock API responses
- Test OAuth flows with mock servers
- Widget update tests

---

## 🚀 Next Steps

### Phase 2: Enhanced Features

- [ ] **Smart Notifications:** Notify users when auto-logging occurs
- [ ] **Conflict Resolution:** Handle duplicate entries from multiple integrations
- [ ] **Custom Mapping:** Allow users to choose which tracker type each integration logs to
- [ ] **Sync History:** View past sync operations and entries logged
- [ ] **Batch Sync:** Log multiple entries in single API call for efficiency

### Phase 3: Additional Integrations

- [ ] **Duolingo** - Language learning → `study` tracker
- [ ] **Spotify** - Music listening → `music` tracker
- [ ] **RescueTime** - Productivity → custom tracker
- [ ] **Telegram/Discord** - Social activity → social tracker
- [ ] **Goodreads** - Reading → `books` tracker
- [ ] **Coinbase** - Crypto portfolio → `investments` tracker

### Phase 4: Advanced Features

- [ ] **Goal Completion Detection:** Auto-log when goals reach 100%
- [ ] **QR Code Event Logging:** Scan QR codes at events to log attendance
- [ ] **AI Suggestions:** Use Gemini to suggest optimal tracker objectives based on auto-logged data
- [ ] **Leaderboards:** Compete with friends on auto-logged activities

---

## 🐛 Troubleshooting

### Integration Not Connecting

1. Check internet connection
2. Verify OAuth credentials are correct
3. Check logcat for error messages: `adb logcat | grep Praxis`
4. Try disconnecting and reconnecting

### Widgets Not Updating

1. Ensure app has been opened at least once
2. Check if WorkManager sync is scheduled
3. Try removing and re-adding widget
4. Restart device

### Sync Not Running

1. Check if integration is enabled in Settings
2. Verify network connection
3. Check WorkManager status: `adb shell dumpsys jobscheduler`
4. Trigger manual sync from Settings or Quick Settings tile

### Permission Denied (Health Connect)

1. Go to Android Settings → Apps → Praxis → Permissions
2. Grant all Health Connect permissions
3. Restart app
4. Reconnect integration

---

## 📞 Support

For issues or questions:
- Check logcat: `adb logcat | grep -E "Praxis|Integration"`
- Review integration documentation in `/home/gio/Praxis/praxis_android/`
- Check backend logs on Railway dashboard
- Review API endpoint responses in Network tab

---

## 📝 License

Proprietary - Praxis Project

---

## 🎉 Success!

You now have a fully integrated Android app that automatically logs activities from external apps into Praxis trackers. Users can:

✅ Connect Health Connect, Strava, Google Calendar, Yazio, and Fitbit  
✅ View live tracker data on home screen widgets  
✅ Quick-sync from Quick Settings tiles  
✅ Earn Praxis Points for auto-logged activities  
✅ Manage integrations in Settings screen  

**Build something amazing! 💪**
