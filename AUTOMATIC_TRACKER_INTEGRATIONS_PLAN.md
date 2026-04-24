# Praxis Android - Automatic Tracker Integration Plan

## Overview

This document outlines the plan to implement **automatic logging** for external apps and services into Praxis trackers. The system will automatically log:
- Goal completions
- Active user connections (matched users talked to >1 week)
- QR codes scanned at events
- Health/Fitness data (Health Connect, Strava, Fitbit, etc.)
- Financial data (Mint, YNAB, Coinbase, etc.)
- Learning progress (Duolingo, Anki, Coursera)
- Mental health data (Headspace, Sleep Cycle, Oura)
- Social activity (Telegram, Discord, LinkedIn)
- And more...

---

## Current System Analysis

### How Notes Work (praxis_webapp)

**Location:** `client/src/features/notes/`

**Architecture:**
1. **NotesPage.tsx** - Main notes interface
   - Fetches goal tree from Supabase (`goal_trees` table)
   - Displays notes per goal node
   - Actions: Journal, Bet, Verify, Share
   - Uses `NotesCardTree` for hierarchical display

2. **Data Model:**
   ```typescript
   GoalNode {
     id: string
     domain: Domain
     name: string
     weight: number
     progress: number
     parentId?: string
     customDetails?: string
   }
   ```

3. **Storage:** 
   - Notes stored in `goal_trees.nodes` JSONB array
   - Each node can have journal entries

### How Trackers Work

**Location:** 
- Frontend: `client/src/features/trackers/`
- Backend: `src/controllers/trackerController.ts`, `src/services/`

**Architecture:**

1. **Tracker Types** (`trackerTypes.ts`):
   - 18 predefined tracker types (lift, meal, cardio, study, books, sleep, meditation, budget, expenses, investments, adventure, journal, project, music, job-apps, progress, etc.)
   - Each has fields, colors, icons, entry labels
   - `DOMAIN_TRACKER_MAP` links goal domains to auto-activated trackers

2. **Database Schema:**
   ```sql
   trackers {
     id: uuid
     user_id: uuid
     type: text (e.g., 'lift', 'meal', 'connection')
     goal: jsonb (user-defined objectives)
     created_at: timestamp
   }
   
   tracker_entries {
     id: uuid
     tracker_id: uuid (FK)
     user_id: uuid
     data: jsonb (flexible field data)
     logged_at: timestamp
   }
   ```

3. **Backend Services:**
   - `ConnectionTrackerService.ts` - Auto-logs messaging connections
   - `EngagementMetricService.ts` - Reads trackers for engagement scoring
   - `AxiomScanService.ts` - Reads trackers for AI analysis

4. **API Endpoints:**
   ```
   GET  /api/trackers/my?days=14     - Get user's trackers with entries
   POST /api/trackers/log            - Log new entry
   PATCH /api/trackers/:type/objective - Set tracker goals
   GET  /api/trackers/summary/today  - Today's entry count
   ```

5. **Automatic Logging Example** (ConnectionTrackerService):
   ```typescript
   // Runs daily at 00:30
   cron.schedule('30 0 * * *', async () => {
     await syncActiveConnections();
   });
   
   async function syncActiveConnections() {
     // Find bidirectional message exchanges in last 7 days
     // Log 'connection' tracker entry for each mutual connection
   }
   ```

### How Widgets Work

**Location:** `client/src/features/widgets/MobileWidget.tsx`

**Architecture:**
1. **MobileWidget** - PWA widget page
   - Fetches user data, streak, praxis points
   - Fetches today's tracker count
   - Fetches last Axiom message
   - Syncs to native via `AndroidBridge.syncWidgetData()`

2. **Data Sync Bridge:**
   ```typescript
   const syncPayload = {
     streak: widgetData.streak,
     pp: widgetData.praxisPoints,
     quote: getAxiomQuote(widgetData.streak),
     trackers: widgetData.trackerCount,
     lastAxiom: widgetData.lastAxiomMessage
   };
   
   if ((window as any).AndroidBridge?.syncWidgetData) {
     (window as any).AndroidBridge.syncWidgetData(JSON.stringify(syncPayload));
   }
   ```

3. **Android Native Side** (praxis_android):
   - `WebAppActivity.kt` - WebView with JavaScript interface
   - `WidgetDataStore.kt` - SharedPreferences cache
   - Conky widgets read from `~/.config/praxis/widget-data.json`

---

## Implementation Plan for praxis_android

### Phase 1: Foundation (Week 1-2)

#### 1.1 Add Integration Framework

**File:** `app/src/main/java/com/praxis/app/integrations/`

```
integrations/
├── IntegrationManager.kt      # Central manager for all integrations
├── IntegrationType.kt          # Enum of supported integrations
├── models/
│   ├── TrackerEntry.kt        # Data class for entries
│   └── IntegrationConfig.kt   # User config for each integration
└── services/
    ├── HealthConnectService.kt
    ├── StravaService.kt
    ├── FitbitService.kt
    └── ...
```

**IntegrationManager.kt:**
```kotlin
object IntegrationManager {
    private val integrations = mutableMapOf<IntegrationType, IntegrationService>()
    
    fun initialize(context: Context) {
        // Register all available integrations
        register(IntegrationType.HEALTH_CONNECT, HealthConnectService())
        register(IntegrationType.STRAVA, StravaService())
        // ...
    }
    
    suspend fun syncAll() {
        integrations.values.forEach { it.sync() }
    }
    
    suspend fun logEntry(type: TrackerType, data: Map<String, Any>) {
        // Call backend API to log tracker entry
        val response = api.logTrackerEntry(
            type = type.name,
            data = data,
            userId = currentUser.id
        )
        // Update local widget cache
        WidgetDataStore.incrementTrackerCount(context)
    }
}
```

#### 1.2 Add Backend API Support

**praxis_webapp changes needed:**

**New endpoint:** `POST /api/integrations/sync`
```typescript
// src/routes/integrationRoutes.ts
router.post('/sync', authenticateToken, async (req, res) => {
  const { type, data } = req.body;
  const userId = req.user.id;
  
  // Get or create tracker
  const tracker = await getOrCreateTracker(userId, type);
  
  // Log entry
  await supabase
    .from('tracker_entries')
    .insert({
      tracker_id: tracker.id,
      user_id: userId,
      data: data,
      logged_at: new Date().toISOString(),
      source: 'auto_integration' // Mark as auto-logged
    });
  
  // Award Praxis Points
  await awardPoints(userId, PP_PER_AUTO_LOG);
  
  res.json({ ok: true });
});
```

#### 1.3 Add Automatic Goal Completion Detection

**File:** `app/src/main/java/com/praxis/app/goals/GoalCompletionTracker.kt`

```kotlin
class GoalCompletionTracker {
    // Monitor goal tree changes
    fun onGoalProgressUpdated(goalId: String, newProgress: Int) {
        if (newProgress >= 100) {
            // Goal completed!
            logGoalCompletion(goalId)
        }
    }
    
    private suspend fun logGoalCompletion(goalId: String) {
        IntegrationManager.logEntry(
            type = TrackerType.GOAL_COMPLETION,
            data = mapOf(
                "goal_id" to goalId,
                "completion_date" to System.currentTimeMillis()
            )
        )
    }
}
```

---

### Phase 2: Health & Fitness Integrations (Week 3-4)

#### 2.1 Health Connect (Google) - **HIGHEST PRIORITY**

**Why:** Central hub for steps, workouts, HR, calories, sleep, weight

**File:** `app/src/main/java/com/praxis/app/integrations/services/HealthConnectService.kt`

```kotlin
class HealthConnectService : IntegrationService {
    private lateinit var healthConnectClient: HealthConnectClient
    
    override suspend fun sync() {
        // Request permissions if needed
        if (!hasPermissions()) requestPermissions()
        
        // Read today's data
        val steps = readStepsToday()
        val workouts = readWorkoutsToday()
        val sleep = readSleepLastNight()
        val weight = readWeightToday()
        
        // Log to trackers
        if (steps > 0) {
            IntegrationManager.logEntry(
                type = TrackerType.STEPS,
                data = mapOf("steps" to steps, "source" to "Health Connect")
            )
        }
        
        workouts.forEach { workout ->
            IntegrationManager.logEntry(
                type = TrackerType.CARDIO,
                data = mapOf(
                    "activity" to workout.activityType,
                    "duration" to workout.durationMinutes,
                    "distance" to workout.distanceKm,
                    "source" to "Health Connect"
                )
            )
        }
    }
    
    private suspend fun readStepsToday(): Int {
        val response = healthConnectClient.readSteps(
            timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
        )
        return response.total
    }
}
```

**Permissions:** `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_WORKOUTS" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />
<uses-permission android:name="androidx.health.permission.STEPS" />
```

#### 2.2 Strava Integration

**File:** `app/src/main/java/com/praxis/app/integrations/services/StravaService.kt`

```kotlin
class StravaService : IntegrationService {
    private val api = StravaApi()
    
    override suspend fun sync() {
        val activities = api.getRecentActivities(hours = 24)
        
        activities.forEach { activity ->
            val trackerType = when(activity.type) {
                "Run" -> TrackerType.CARDIO
                "Ride" -> TrackerType.CARDIO
                "Hike" -> TrackerType.ADVENTURE
                else -> return@forEach
            }
            
            IntegrationManager.logEntry(
                type = trackerType,
                data = mapOf(
                    "activity" to activity.type,
                    "duration" to activity.elapsedTime / 60, // minutes
                    "distance" to activity.distance / 1000.0, // km
                    "elevation" to activity.elevationGain,
                    "source" to "Strava"
                )
            )
        }
    }
}
```

**OAuth Flow:**
1. User clicks "Connect Strava" in settings
2. Opens Strava OAuth URL
3. Receives auth code
4. Exchanges for access token
5. Stores token encrypted in SharedPreferences

#### 2.3 Fitbit / Garmin / Whoop / Oura

Similar pattern - each service has:
- OAuth authentication
- REST API for fetching data
- Data mapping to Praxis tracker types

---

### Phase 3: Financial Integrations (Week 5-6)

#### 3.1 Google Sheets / Excel API

**File:** `app/src/main/java/com/praxis/app/integrations/services/GoogleSheetsService.kt`

```kotlin
class GoogleSheetsService : IntegrationService {
    // User configures spreadsheet ID and range
    // App reads investment/portfolio data automatically
    
    override suspend fun sync() {
        val spreadsheetId = config.getString("spreadsheet_id")
        val range = config.getString("range", "Portfolio!A2:E100")
        
        val sheets = Sheets.Builder(httpTransport, jsonFactory, credentials)
            .build()
        
        val response = sheets.spreadsheets().values()
            .get(spreadsheetId, range)
            .execute()
        
        response.values?.forEach { row ->
            IntegrationManager.logEntry(
                type = TrackerType.INVESTMENTS,
                data = mapOf(
                    "action" to row[0], // Buy/Sell
                    "asset" to row[1],  // AAPL
                    "quantity" to row[2].toDouble(),
                    "price" to row[3].toDouble(),
                    "source" to "Google Sheets"
                )
            )
        }
    }
}
```

#### 3.2 Coinbase / Binance (Crypto)

```kotlin
class CoinbaseService : IntegrationService {
    override suspend fun sync() {
        val portfolio = api.getPortfolio()
        
        // Log significant changes (>5% value change)
        if (portfolio.changePercent24h > 5) {
            IntegrationManager.logEntry(
                type = TrackerType.INVESTMENTS,
                data = mapOf(
                    "action" to "Portfolio Update",
                    "asset" to "Crypto Portfolio",
                    "value_eur" to portfolio.valueEur,
                    "change_24h" to portfolio.changePercent24h,
                    "source" to "Coinbase"
                )
            )
        }
    }
}
```

---

### Phase 4: Learning & Mental Health (Week 7-8)

#### 4.1 Duolingo

```kotlin
class DuolingoService : IntegrationService {
    override suspend fun sync() {
        val streak = api.getUserStreak(username)
        val xpToday = api.getXPToday(username)
        
        if (xpToday > 0) {
            IntegrationManager.logEntry(
                type = TrackerType.STUDY,
                data = mapOf(
                    "subject" to "Language Learning",
                    "duration" to xpToday / 10, // approx minutes
                    "topic" to "Duolingo XP: $xpToday",
                    "streak" to streak,
                    "source" to "Duolingo"
                )
            )
        }
    }
}
```

#### 4.2 Headspace / Calm

```kotlin
class HeadspaceService : IntegrationService {
    override suspend fun sync() {
        val sessions = api.getRecentSessions(days = 1)
        
        sessions.forEach { session ->
            IntegrationManager.logEntry(
                type = TrackerType.MEDITATION,
                data = mapOf(
                    "duration" to session.durationMinutes,
                    "type" to session.category, // "Guided", "Sleep", etc.
                    "feeling" to "Calm", // Default for auto-log
                    "source" to "Headspace"
                )
            )
        }
    }
}
```

---

### Phase 5: Social & Career (Week 9-10)

#### 5.1 Telegram Bot API

**File:** `app/src/main/java/com/praxis/app/integrations/services/TelegramService.kt`

```kotlin
class TelegramService : IntegrationService {
    // Uses Telegram Bot API (not user API)
    // User adds Praxis bot to groups/chats
    
    override suspend fun sync() {
        val updates = botApi.getUpdates()
        
        // Count messages sent by user in last 24h
        val messageCount = updates.count { it.from.id == userId }
        
        if (messageCount > 10) { // Only log if significant activity
            IntegrationManager.logEntry(
                type = TrackerType.SOCIAL,
                data = mapOf(
                    "platform" to "Telegram",
                    "message_count" to messageCount,
                    "groups_active" to uniqueGroupCount,
                    "source" to "Telegram Bot"
                )
            )
        }
    }
}
```

#### 5.2 LinkedIn

```kotlin
class LinkedInService : IntegrationService {
    override suspend fun sync() {
        val profileUpdates = api.getProfileUpdates(days = 7)
        val applications = api.getJobApplications(days = 7)
        
        applications.forEach { app ->
            IntegrationManager.logEntry(
                type = TrackerType.JOB_APPS,
                data = mapOf(
                    "role" to app.jobTitle,
                    "company" to app.companyName,
                    "status" to app.status, // "Applied", "Interview", etc.
                    "source" to "LinkedIn"
                )
            )
        }
    }
}
```

---

### Phase 6: QR Code & Event Logging (Week 11)

#### 6.1 QR Code Scanner Integration

**File:** `app/src/main/java/com/praxis/app/ui/QRScannerActivity.kt`

```kotlin
class QRScannerActivity : AppCompatActivity() {
    private lateinit var scanner: BarcodeScanner
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        scanner = BarcodeScanner(this, onScanResult = { result ->
            handleQRCode(result)
        })
    }
    
    private suspend fun handleQRCode(qrData: String) {
        val eventData = parseEventData(qrData)
        
        IntegrationManager.logEntry(
            type = TrackerType.EVENT,
            data = mapOf(
                "event_name" to eventData.name,
                "event_id" to eventData.id,
                "scan_time" to System.currentTimeMillis(),
                "location" to eventData.location,
                "source" to "QR Scan"
            )
        )
        
        toast("Event logged! +10 PP")
    }
}
```

**QR Code Format for Events:**
```json
{
  "type": "praxis_event",
  "event_id": "evt_12345",
  "name": "Praxis Meetup Milano",
  "location": "Milano, IT",
  "date": "2026-03-15"
}
```

---

### Phase 7: Goal Completion Auto-Log (Week 12)

#### 7.1 Goal Tree Monitor

**File:** `app/src/main/java/com/praxis/app/goals/GoalTreeMonitor.kt`

```kotlin
class GoalTreeMonitor {
    private var lastKnownProgress = mutableMapOf<String, Int>()
    
    fun startMonitoring() {
        // Poll goal tree every 5 minutes
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val currentTree = api.getGoalTree()
                
                currentTree.nodes.forEach { node ->
                    val oldProgress = lastKnownProgress[node.id] ?: 0
                    val newProgress = node.progress
                    
                    // Detect completion (progress reached 100%)
                    if (oldProgress < 100 && newProgress >= 100) {
                        logGoalCompletion(node)
                    }
                    
                    // Detect significant progress jumps (>20%)
                    if (newProgress - oldProgress >= 20) {
                        logProgressMilestone(node, newProgress)
                    }
                    
                    lastKnownProgress[node.id] = newProgress
                }
                
                delay(5 * 60 * 1000) // 5 minutes
            }
        }
    }
    
    private suspend fun logGoalCompletion(node: GoalNode) {
        IntegrationManager.logEntry(
            type = TrackerType.GOAL_COMPLETION,
            data = mapOf(
                "goal_name" to node.name,
                "goal_id" to node.id,
                "domain" to node.domain,
                "completion_date" to System.currentTimeMillis(),
                "source" to "Auto-Detection"
            )
        )
        
        // Show celebration notification
        showCompletionNotification(node)
    }
}
```

---

## User Configuration UI

### Settings Screen

**File:** `app/src/main/java/com/praxis/app/ui/settings/IntegrationsSettingsFragment.kt`

```kotlin
class IntegrationsSettingsFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // List of available integrations
        val integrations = listOf(
            IntegrationItem(
                type = IntegrationType.HEALTH_CONNECT,
                name = "Health Connect",
                description = "Steps, workouts, sleep, weight",
                icon = R.drawable.ic_health,
                connected = false
            ),
            IntegrationItem(
                type = IntegrationType.STRAVA,
                name = "Strava",
                description = "Running, cycling, hiking",
                icon = R.drawable.ic_strava,
                connected = false
            ),
            // ...
        )
        
        recyclerView.adapter = IntegrationsAdapter(
            integrations = integrations,
            onConnectClick = { integration ->
                when (integration.type) {
                    IntegrationType.HEALTH_CONNECT -> requestHealthConnectPermissions()
                    IntegrationType.STRAVA -> launchStravaOAuth()
                    // ...
                }
            }
        )
    }
}
```

### Configuration Options per Integration

Each integration has:
- **Enable/Disable toggle**
- **Sync frequency** (Real-time, Hourly, Daily)
- **Data filters** (e.g., only log workouts >30 min)
- **Tracker type mapping** (e.g., Strava Run → Cardio Tracker)

---

## Backend Changes Required (praxis_webapp)

### 1. New Database Tables

```sql
-- Integration configurations
CREATE TABLE user_integrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    integration_type TEXT NOT NULL,
    access_token_encrypted TEXT,
    refresh_token_encrypted TEXT,
    config JSONB DEFAULT '{}',
    enabled BOOLEAN DEFAULT true,
    last_sync TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, integration_type)
);

-- Auto-log audit trail
CREATE TABLE auto_logged_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracker_entry_id UUID REFERENCES tracker_entries(id),
    integration_type TEXT NOT NULL,
    original_data JSONB,
    synced_at TIMESTAMP DEFAULT NOW()
);
```

### 2. New API Endpoints

```typescript
// src/routes/integrationRoutes.ts

// Connect integration (OAuth callback)
GET  /api/integrations/:type/connect
POST /api/integrations/:type/callback

// Disconnect integration
DELETE /api/integrations/:type

// Manual sync trigger
POST /api/integrations/:type/sync

// List connected integrations
GET  /api/integrations

// Get integration status
GET  /api/integrations/:type/status
```

### 3. Backend Services

```typescript
// src/services/IntegrationSyncService.ts

export class IntegrationSyncService {
    // Runs every hour for all users
    public static start() {
        cron.schedule('0 * * * *', async () => {
            await this.syncAllUsers();
        });
    }
    
    private static async syncAllUsers() {
        const users = await getAllUsersWithIntegrations();
        
        for (const user of users) {
            const integrations = await getUserIntegrations(user.id);
            
            for (const integration of integrations) {
                if (integration.enabled && shouldSync(integration)) {
                    await syncIntegration(user.id, integration);
                }
            }
        }
    }
}
```

---

## Recommended Implementation Order

### Tier 1 (Highest Value, Easiest)
1. ✅ **Health Connect** - One integration covers fitness, sleep, weight, nutrition
2. ✅ **QR Code Scanner** - Event logging
3. ✅ **Goal Completion Auto-Detection** - From goal tree monitoring

### Tier 2 (High Value)
4. **LinkedIn** - Career & networking
5. **Strava + Fitbit** - Fitness (if not using Health Connect)
6. **Google Sheets** - Manual portfolio tracking

### Tier 3 (Medium Value)
7. **RescueTime** - Productivity across all apps
8. **Telegram / Discord** - Social engagement
9. **Spotify / Goodreads** - Culture & hobbies

### Tier 4 (Lower Priority)
10. **Duolingo** - Language learning
11. **Headspace / Calm** - Meditation
12. **Coinbase / Binance** - Crypto (niche)

---

## Technical Considerations

### Battery Optimization
- Use WorkManager for background sync
- Batch API calls (log multiple entries in one request)
- Respect Android Doze mode

### Data Privacy
- Encrypt all OAuth tokens (Android Keystore)
- Never store raw credentials
- User can revoke access anytime

### Rate Limiting
- Respect API rate limits (Strava: 100 requests/day, LinkedIn: 500/day)
- Implement exponential backoff
- Cache responses when possible

### Error Handling
- Retry failed syncs with exponential backoff
- Notify user if integration disconnects
- Log errors for debugging

---

## Success Metrics

Track these KPIs:
- **Integration Adoption Rate** - % of users connecting ≥1 integration
- **Auto-Logged Entries** - Number of tracker entries from integrations vs manual
- **User Retention** - Do users with integrations stay longer?
- **Praxis Points Earned** - PP from auto-logged vs manual entries

---

## Next Steps

1. **Create GitHub issues** for each integration
2. **Set up backend database migrations** for `user_integrations` table
3. **Implement Health Connect** as proof of concept
4. **Build settings UI** for integration management
5. **Test with beta users** and iterate

---

## Appendix: Integration API Reference

### Health Connect (Google)
- **Docs:** https://developer.android.com/health-and-fitness/guides/health-connect
- **Permissions:** Runtime permissions for each data type
- **Rate Limits:** None (local API)

### Strava
- **API:** https://developers.strava.com/docs/reference/
- **OAuth:** https://www.strava.com/oauth/token
- **Rate Limits:** 100 requests/day, 1000 requests/hour

### Fitbit
- **API:** https://dev.fitbit.com/build/reference/web-api/
- **OAuth 2.0:** PKCE flow
- **Rate Limits:** 150 requests/hour

### LinkedIn
- **API:** https://learn.microsoft.com/en-us/linkedin/
- **OAuth 2.0:** Authorization code flow
- **Rate Limits:** 500 requests/day

### Telegram Bot
- **API:** https://core.telegram.org/bots/api
- **No OAuth:** Bot token only
- **Rate Limits:** ~30 messages/second
