# Praxis Android - External App Integration Plan

**Created:** March 15, 2026  
**Based on:** praxis_webapp tracker architecture analysis

---

## 📊 Current System Analysis (praxis_webapp)

### How Trackers Work

#### 1. **Tracker Types Definition** (`client/src/features/trackers/trackerTypes.ts`)

```typescript
interface TrackerType {
  id: string;           // e.g., 'lift', 'cardio', 'meal'
  label: string;        // Display name
  icon: string;         // Emoji
  description: string;
  color: string;        // UI color
  bg: string;           // Background color
  border: string;       // Border color
  fields: TrackerField[];  // Input fields
  entryLabel: (data) => string;  // Display format
}

interface TrackerField {
  key: string;          // e.g., 'exercise', 'duration'
  label: string;
  type: 'text' | 'number' | 'select' | 'date';
  placeholder?: string;
  options?: string[];   // For select fields
  optional?: boolean;
}
```

**Current Trackers (18 types):**
- Fitness: `lift`, `cardio`, `steps`, `meal`
- Social: `hangout`
- Learning: `study`, `books`
- Health: `sleep`, `meditation`
- Finance: `budget`, `expenses`, `investments`
- Life: `adventure`, `journal`, `project`, `music`, `job-apps`, `progress`

#### 2. **Database Schema**

```sql
-- Trackers table (user's active trackers)
CREATE TABLE trackers (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  type TEXT NOT NULL,  -- e.g., 'lift', 'cardio'
  goal JSONB,          -- User-defined objectives
  created_at TIMESTAMP
);

-- Tracker entries (individual logs)
CREATE TABLE tracker_entries (
  id UUID PRIMARY KEY,
  tracker_id UUID REFERENCES trackers(id),
  user_id UUID REFERENCES users(id),
  data JSONB NOT NULL,  -- Flexible field data
  logged_at TIMESTAMP
);
```

#### 3. **Backend API** (`src/controllers/trackerController.ts`)

```typescript
// Log a tracker entry
POST /api/trackers/log
Body: { type: 'cardio', data: { activity: 'Running', duration: 30 } }

// Get user's trackers with entries
GET /api/trackers/my?days=14

// Get calendar data (combined activity)
GET /api/trackers/calendar?days=112
Response: {
  calendar: [
    {
      date: '2026-03-15',
      count: 5,
      trackers: ['lift', 'meal'],
      notes: 2,
      goalUpdates: 1,
      activities: [...]
    }
  ]
}
```

#### 4. **Auto-Logging Service** (`src/services/ConnectionTrackerService.ts`)

```typescript
// Example: Auto-log messaging connections
cron.schedule('30 0 * * *', async () => {
  await syncActiveConnections();
});

async function syncActiveConnections() {
  // Find bidirectional message exchanges in last 7 days
  // Log 'connection' tracker entry for each
  await supabase.from('tracker_entries').insert({
    tracker_id: tracker.id,
    user_id: userId,
    data: { target_id: targetId, type: 'active_chat', window: '7d' }
  });
}
```

### How Notes/Diary Work

#### 1. **Diary Feed** (`client/src/features/notes/DiaryFeed.tsx`)

- Aggregates multiple activity types into unified feed
- Shows: trackers, journals, check-ins, bets, achievements, posts, goals
- Grouped by date (Today, Yesterday, This Week, etc.)
- Infinite scroll with pagination

```typescript
interface FeedItem {
  id: string;
  type: 'tracker' | 'journal' | 'checkin' | 'bet' | 'achievement';
  timestamp: string;
  title: string;
  detail?: string;
  icon: string;
  color: string;
  badge: string;
}
```

#### 2. **Journal Entries** (`src/lib/db.ts` - IndexedDB for offline)

```typescript
interface LocalJournalEntry {
  id?: string;
  node_id: string;      // Goal node
  note: string;
  mood: string | null;
  logged_at: string;
  sync_status: 'synced' | 'pending' | 'failed';
}
```

### How Widgets Work

#### 1. **Mobile Widget** (`client/src/features/widgets/MobileWidget.tsx`)

- Fetches user stats (streak, points, tracker count)
- Syncs to native via JavaScript bridge
- PWA-compatible for home screen installation

```typescript
const syncPayload = {
  streak: widgetData.streak,
  pp: widgetData.praxisPoints,
  quote: getAxiomQuote(widgetData.streak),
  trackers: widgetData.trackerCount,
  lastAxiom: widgetData.lastAxiomMessage
};

// Android bridge
if ((window as any).AndroidBridge?.syncWidgetData) {
  (window as any).AndroidBridge.syncWidgetData(JSON.stringify(syncPayload));
}
```

#### 2. **Habit Calendar** (`client/src/features/analytics/AnalyticsPage.tsx`)

- 16-week GitHub-style contribution graph
- Color intensity = activity count
- Shows trackers + notes + goal updates
- Filter toggles (Trackers/Notes/Goals)

---

## 🎯 Integration Architecture for Android

### Core Design Principles

1. **Unified Tracker Interface** - All integrations log to same `tracker_entries` table
2. **OAuth Token Management** - Secure storage in Android Keystore
3. **Background Sync** - WorkManager for periodic data fetching
4. **Offline-First** - Cache data locally, sync when online
5. **User Control** - Enable/disable integrations, configure sync frequency

### Database Schema Extensions

```sql
-- Integration configurations
CREATE TABLE user_integrations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  integration_type TEXT NOT NULL,  -- e.g., 'health_connect', 'strava'
  access_token_encrypted TEXT,
  refresh_token_encrypted TEXT,
  token_expires_at TIMESTAMP,
  config JSONB DEFAULT '{}',       -- User preferences
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

-- Add source field to tracker_entries
ALTER TABLE tracker_entries 
ADD COLUMN source TEXT DEFAULT 'manual';  -- 'manual', 'auto_health_connect', etc.
```

---

## 📱 Implementation Plan by Category

### Phase 1: Fitness & Health (Weeks 1-4)

#### 1.1 Health Connect (Google) - **HIGHEST PRIORITY**

**Why First:**
- One integration covers: steps, workouts, HR, calories, sleep, weight
- Official Google API, well-documented
- Highest user value

**Implementation:**

```kotlin
// app/src/main/java/com/praxis/app/integrations/healthconnect/HealthConnectService.kt

class HealthConnectService @Inject constructor(
    private val context: Context,
    private val trackerRepository: TrackerRepository
) : IntegrationService {
    
    private lateinit var healthConnectClient: HealthConnectClient
    
    override suspend fun sync(): Result<Unit> {
        // Request permissions if needed
        if (!hasPermissions()) {
            requestPermissions()
            return Result.failure(SecurityException("Permissions not granted"))
        }
        
        // Read today's data
        val steps = readStepsToday()
        val workouts = readWorkoutsToday()
        val sleep = readSleepLastNight()
        val weight = readWeightToday()
        val heartRate = readHeartRateToday()
        
        // Log to Praxis trackers
        if (steps > 0) {
            trackerRepository.logEntry(
                type = "steps",
                data = mapOf(
                    "steps" to steps,
                    "goal" to 10000,
                    "source" to "Health Connect"
                ),
                source = "auto_health_connect"
            )
        }
        
        workouts.forEach { workout ->
            trackerRepository.logEntry(
                type = "cardio",
                data = mapOf(
                    "activity" to workout.activityType,
                    "duration" to workout.durationMinutes,
                    "distance" to workout.distanceKm,
                    "calories" to workout.calories,
                    "source" to "Health Connect"
                ),
                source = "auto_health_connect"
            )
        }
        
        // Sleep tracking
        if (sleep != null) {
            trackerRepository.logEntry(
                type = "sleep",
                data = mapOf(
                    "duration" to sleep.durationHours,
                    "quality" to sleep.quality,
                    "source" to "Health Connect"
                ),
                source = "auto_health_connect"
            )
        }
        
        return Result.success(Unit)
    }
    
    private suspend fun readStepsToday(): Int {
        val response = healthConnectClient.readSteps(
            timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
        )
        return response.total
    }
    
    // ... other readers
}
```

**AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_WORKOUTS" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_WEIGHT" />
<uses-permission android:name="androidx.health.permission.STEPS" />
```

**UI - Settings Screen:**
```kotlin
// app/src/main/java/com/praxis/app/ui/settings/IntegrationsSettingsFragment.kt

class IntegrationsSettingsFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val integrations = listOf(
            IntegrationItem(
                type = IntegrationType.HEALTH_CONNECT,
                name = "Health Connect",
                description = "Steps, workouts, sleep, weight, heart rate",
                icon = R.drawable.ic_health_connect,
                connected = false,
                lastSync = null
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

---

#### 1.2 Strava Integration

**Implementation:**

```kotlin
// app/src/main/java/com/praxis/app/integrations/strava/StravaService.kt

class StravaService @Inject constructor(
    private val api: StravaApi,
    private val tokenStorage: SecureTokenStorage,
    private val trackerRepository: TrackerRepository
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        val activities = api.getRecentActivities(hours = 24)
        
        activities.forEach { activity ->
            val trackerType = when(activity.type.lowercase()) {
                "run" -> "cardio"
                "ride" -> "cardio"
                "hike" -> "adventure"
                "walk" -> "cardio"
                else -> return@forEach
            }
            
            trackerRepository.logEntry(
                type = trackerType,
                data = mapOf(
                    "activity" to activity.type,
                    "duration" to activity.elapsedTime / 60, // minutes
                    "distance" to activity.distance / 1000.0, // km
                    "elevation" to activity.elevationGain,
                    "pace" to activity.averageSpeed,
                    "source" to "Strava"
                ),
                source = "auto_strava"
            )
        }
        
        return Result.success(Unit)
    }
}

// OAuth Flow
class StravaOAuthActivity : AppCompatActivity() {
    
    private val stravaAuth = StravaAuth(
        clientId = BUILD_CONFIG.STRAVA_CLIENT_ID,
        clientSecret = BUILD_CONFIG.STRAVA_CLIENT_SECRET,
        redirectUri = "praxis://strava/callback"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Launch OAuth
        val authUrl = stravaAuth.getAuthorizationUrl(
            scopes = listOf("read", "activity:read_all")
        )
        customTabs.launchUrl(authUrl)
    }
    
    // Handle callback
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val code = intent.data?.getQueryParameter("code")
        if (code != null) {
            lifecycleScope.launch {
                val tokens = stravaAuth.exchangeCodeForToken(code)
                tokenStorage.saveTokens("strava", tokens)
                // Start initial sync
                stravaService.sync()
            }
        }
    }
}
```

---

#### 1.3 Fitbit Integration

Similar pattern to Strava, but with Fitbit-specific API:

```kotlin
// Fitbit API endpoints
class FitbitApi {
    // https://dev.fitbit.com/build/reference/web-api/
    
    suspend fun getActivitiesToday(): FitbitActivities {
        return retrofit.get(
            url = "/1/user/-/activities/date/today.json",
            headers = authHeaders
        )
    }
    
    suspend fun getSleepToday(): FitbitSleep {
        return retrofit.get(
            url = "/1.2/user/-/sleep/date/today.json",
            headers = authHeaders
        )
    }
    
    suspend fun getHeartRateToday(): FitbitHeartRate {
        return retrofit.get(
            url = "/1/user/-/activities/heart/date/today/1d.json",
            headers = authHeaders
        )
    }
}
```

---

### Phase 2: Financial & Career (Weeks 5-8)

#### 2.1 Google Sheets / Excel API

**Use Case:** Manual or automated portfolio tracking

```kotlin
// app/src/main/java/com/praxis/app/integrations/sheets/GoogleSheetsService.kt

class GoogleSheetsService @Inject constructor(
    private val sheetsApi: SheetsApi,
    private val config: IntegrationConfig
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        val spreadsheetId = config.getString("spreadsheet_id")
        val range = config.getString("range", "Portfolio!A2:E100")
        
        val response = sheetsApi.values().get(spreadsheetId, range).execute()
        
        response.values?.forEach { row ->
            // Row format: [Action, Asset, Quantity, Price, Date]
            trackerRepository.logEntry(
                type = "investments",
                data = mapOf(
                    "action" to row[0], // Buy/Sell
                    "asset" to row[1],  // AAPL
                    "quantity" to row[2].toDouble(),
                    "price" to row[3].toDouble(),
                    "source" to "Google Sheets"
                ),
                source = "auto_sheets"
            )
        }
        
        return Result.success(Unit)
    }
}
```

**User Configuration UI:**
```kotlin
// Settings dialog for Google Sheets integration
AlertDialog.Builder(context)
    .setTitle("Configure Google Sheets")
    .setView(R.layout.dialog_sheets_config)
    .setPositiveButton("Connect") { _, _ ->
        val spreadsheetId = editText.text.toString()
        val range = editText2.text.toString()
        config.save("spreadsheet_id", spreadsheetId)
        config.save("range", range)
        // Launch OAuth
        googleOAuth.launch()
    }
    .show()
```

---

#### 2.2 LinkedIn Integration

**Use Case:** Job applications, networking activity

```kotlin
// app/src/main/java/com/praxis/app/integrations/linkedin/LinkedInService.kt

class LinkedInService @Inject constructor(
    private val linkedInApi: LinkedInApi,
    private val tokenStorage: SecureTokenStorage
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        // Get recent job applications
        val applications = linkedInApi.getJobApplications(days = 7)
        
        applications.forEach { app ->
            trackerRepository.logEntry(
                type = "job-apps",
                data = mapOf(
                    "role" to app.jobTitle,
                    "company" to app.companyName,
                    "status" to app.status, // "Applied", "Interview", "Offer"
                    "location" to app.location,
                    "source" to "LinkedIn"
                ),
                source = "auto_linkedin"
            )
        }
        
        // Get profile updates
        val updates = linkedInApi.getProfileUpdates(days = 7)
        updates.forEach { update ->
            trackerRepository.logEntry(
                type = "career",
                data = mapOf(
                    "update_type" to update.type,
                    "content" to update.content,
                    "engagement" to update.engagement,
                    "source" to "LinkedIn"
                ),
                source = "auto_linkedin"
            )
        }
        
        return Result.success(Unit)
    }
}
```

**LinkedIn API Setup:**
1. Create app at https://www.linkedin.com/developers/apps
2. Request scopes: `r_liteprofile`, `w_member_social`, `r_fullprofile`
3. OAuth 2.0 authorization code flow

---

### Phase 3: Learning & Productivity (Weeks 9-12)

#### 3.1 Duolingo Integration

```kotlin
// app/src/main/java/com/praxis/app/integrations/duolingo/DuolingoService.kt

class DuolingoService @Inject constructor(
    private val duolingoApi: DuolingoApi,
    private val config: IntegrationConfig
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        val username = config.getString("duolingo_username")
        
        val streak = duolingoApi.getUserStreak(username)
        val xpToday = duolingoApi.getXPToday(username)
        val skills = duolingoApi.getSkills(username)
        
        if (xpToday > 0) {
            trackerRepository.logEntry(
                type = "study",
                data = mapOf(
                    "subject" to "Language Learning",
                    "duration" to xpToday / 10, // approx minutes
                    "topic" to "Duolingo XP: $xpToday",
                    "streak" to streak,
                    "source" to "Duolingo"
                ),
                source = "auto_duolingo"
            )
        }
        
        return Result.success(Unit)
    }
}

// Duolingo API (unofficial)
class DuolingoApi {
    // https://www.duolingo.com/2017-06-30/users?username={username}
    suspend fun getUserStreak(username: String): Int {
        val response = httpClient.get("https://www.duolingo.com/2017-06-30/users?username=$username")
        val json = Json.parseToJsonElement(response.body).jsonObject
        val users = json["users"]!!.jsonArray
        return users[0].jsonObject["streak"]!!.int
    }
}
```

---

#### 3.2 RescueTime Integration

**Use Case:** Automatic productivity tracking across all apps/websites

```kotlin
// app/src/main/java/com/praxis/app/integrations/rescuetime/RescueTimeService.kt

class RescueTimeService @Inject constructor(
    private val rescueTimeApi: RescueTimeApi,
    private val config: IntegrationConfig
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        val apiKey = config.getString("rescuetime_api_key")
        
        // Get today's activity
        val report = rescueTimeApi.getReport(
            apiKey = apiKey,
            perspective = "interval",
            restrict_kind = "activity",
            interval = "day"
        )
        
        // Categorize activities
        val productiveTime = report.rows
            .filter { it.efficiency > 0 }
            .sumOf { it.duration }
        
        val distractingTime = report.rows
            .filter { it.efficiency < 0 }
            .sumOf { it.duration }
        
        // Log to tracker
        trackerRepository.logEntry(
            type = "productivity",
            data = mapOf(
                "productive_minutes" to productiveTime / 60,
                "distracting_minutes" to distractingTime / 60,
                "efficiency_score" to report.efficiencyScore,
                "top_activity" to report.topActivity,
                "source" to "RescueTime"
            ),
            source = "auto_rescuetime"
        )
        
        return Result.success(Unit)
    }
}
```

---

### Phase 4: Social & Communication (Weeks 13-16)

#### 4.1 Telegram Bot API

```kotlin
// app/src/main/java/com/praxis/app/integrations/telegram/TelegramService.kt

class TelegramService @Inject constructor(
    private val telegramBot: TelegramBot,
    private val config: IntegrationConfig
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        // Get updates from bot
        val updates = telegramBot.getUpdates()
        
        // Count messages sent by user in last 24h
        val messageCount = updates.count { 
            it.from.id == userId && 
            it.date > System.currentTimeMillis() / 1000 - 86400 
        }
        
        // Count unique groups
        val uniqueGroups = updates
            .mapNotNull { it.chat }
            .filter { it.type == "group" || it.type == "supergroup" }
            .distinctBy { it.id }
            .size
        
        if (messageCount > 10) { // Only log if significant activity
            trackerRepository.logEntry(
                type = "social",
                data = mapOf(
                    "platform" to "Telegram",
                    "message_count" to messageCount,
                    "groups_active" to uniqueGroups,
                    "source" to "Telegram Bot"
                ),
                source = "auto_telegram"
            )
        }
        
        return Result.success(Unit)
    }
}

// User adds Praxis bot to groups
// Bot token stored securely
class TelegramBot {
    private val botToken = BUILD_CONFIG.TELEGRAM_BOT_TOKEN
    
    suspend fun getUpdates(): List<TelegramUpdate> {
        return httpClient.get(
            "https://api.telegram.org/bot$botToken/getUpdates"
        ).body()
    }
}
```

---

#### 4.2 Discord Integration

```kotlin
// app/src/main/java/com/praxis/app/integrations/discord/DiscordService.kt

class DiscordService @Inject constructor(
    private val discordApi: DiscordApi,
    private val tokenStorage: SecureTokenStorage
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        val userId = tokenStorage.getUserId("discord")
        
        // Get user's recent messages
        val messages = discordApi.getUserMessages(
            userId = userId,
            limit = 100,
            hours = 24
        )
        
        // Get server activity
        val servers = discordApi.getUserServers(userId)
        
        if (messages.size > 20) {
            trackerRepository.logEntry(
                type = "social",
                data = mapOf(
                    "platform" to "Discord",
                    "message_count" to messages.size,
                    "servers_active" to servers.size,
                    "source" to "Discord API"
                ),
                source = "auto_discord"
            )
        }
        
        return Result.success(Unit)
    }
}
```

---

### Phase 5: Culture & Hobbies (Weeks 17-20)

#### 5.1 Spotify Integration

```kotlin
// app/src/main/java/com/praxis/app/integrations/spotify/SpotifyService.kt

class SpotifyService @Inject constructor(
    private val spotifyApi: SpotifyApi,
    private val tokenStorage: SecureTokenStorage
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        // Get recently played tracks
        val recentlyPlayed = spotifyApi.getRecentlyPlayed(limit = 50)
        
        // Get top artists
        val topArtists = spotifyApi.getTopArtists(timeRange = "short_term")
        
        // Calculate listening time
        val totalListeningMinutes = recentlyPlayed.items
            .groupBy { it.playedAt.toLocalDate() }
            .mapValues { it.value.sumOf { track -> track.track.durationMs } }
            .mapValues { it.value / 60000 }
        
        val todayMinutes = totalListeningMinutes[LocalDate.now()] ?: 0
        
        if (todayMinutes > 30) {
            trackerRepository.logEntry(
                type = "music",
                data = mapOf(
                    "listening_minutes" to todayMinutes,
                    "top_artist" to topArtists.items.firstOrNull()?.name,
                    "tracks_played" to recentlyPlayed.items.size,
                    "source" to "Spotify"
                ),
                source = "auto_spotify"
            )
        }
        
        return Result.success(Unit)
    }
}
```

---

#### 5.2 Goodreads Integration

```kotlin
// app/src/main/java/com/praxis/app/integrations/goodreads/GoodreadsService.kt

class GoodreadsService @Inject constructor(
    private val goodreadsApi: GoodreadsApi,
    private val config: IntegrationConfig
) : IntegrationService {
    
    override suspend fun sync(): Result<Unit> {
        val userId = config.getString("goodreads_user_id")
        
        // Get currently reading
        val currentlyReading = goodreadsApi.getCurrentUserReading(userId)
        
        // Get recent reviews
        val recentReviews = goodreadsApi.getUserReviews(userId, limit = 10)
        
        // Log reading progress
        currentlyReading.forEach { book ->
            trackerRepository.logEntry(
                type = "books",
                data = mapOf(
                    "title" to book.title,
                    "author" to book.author,
                    "progress_percent" to book.progress,
                    "pages_read" to book.pagesRead,
                    "source" to "Goodreads"
                ),
                source = "auto_goodreads"
            )
        }
        
        return Result.success(Unit)
    }
}
```

---

## 🔧 Common Infrastructure

### 1. Token Storage (Secure)

```kotlin
// app/src/main/java/com/praxis/app/integrations/auth/SecureTokenStorage.kt

class SecureTokenStorage @Inject constructor(
    private val context: Context
) {
    private val keystore = AndroidKeystore(context)
    
    fun saveTokens(integration: String, tokens: OAuthTokens) {
        val encrypted = encrypt(tokens.toJson())
        preferences.edit()
            .putString("${integration}_access_token", encrypted)
            .putLong("${integration}_expires_at", tokens.expiresAt)
            .apply()
    }
    
    fun getTokens(integration: String): OAuthTokens? {
        val encrypted = preferences.getString("${integration}_access_token", null)
        ?: return null
        val decrypted = decrypt(encrypted)
        return OAuthTokens.fromJson(decrypted)
    }
    
    private fun encrypt(data: String): String {
        // Use Android Keystore for encryption
        return keystore.encrypt(data)
    }
    
    private fun decrypt(data: String): String {
        return keystore.decrypt(data)
    }
}
```

### 2. Background Sync (WorkManager)

```kotlin
// app/src/main/java/com/praxis/app/integrations/sync/IntegrationSyncWorker.kt

class IntegrationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    
    override suspend fun doWork(): Result {
        val integrationType = inputData.getString("integration_type")
        ?: return Result.failure()
        
        val service = when (integrationType) {
            "health_connect" -> healthConnectService
            "strava" -> stravaService
            "linkedin" -> linkedInService
            // ...
            else -> return Result.failure()
        }
        
        return try {
            service.sync()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

// Schedule sync
fun scheduleSync(integrationType: String, frequency: SyncFrequency) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
    
    val workRequest = when (frequency) {
        SyncFrequency.REAL_TIME -> PeriodicWorkRequestBuilder<IntegrationSyncWorker>(15, TimeUnit.MINUTES)
        SyncFrequency.HOURLY -> PeriodicWorkRequestBuilder<IntegrationSyncWorker>(1, TimeUnit.HOURS)
        SyncFrequency.DAILY -> PeriodicWorkRequestBuilder<IntegrationSyncWorker>(1, TimeUnit.DAYS)
    }
    .setInputData(workDataOf("integration_type" to integrationType))
    .setConstraints(constraints)
    .build()
    
    WorkManager.getInstance(context).enqueue(workRequest)
}
```

### 3. Integration Manager

```kotlin
// app/src/main/java/com/praxis/app/integrations/IntegrationManager.kt

@Singleton
class IntegrationManager @Inject constructor(
    private val context: Context,
    private val trackerRepository: TrackerRepository,
    private val integrationRepository: IntegrationRepository
) {
    private val services = mutableMapOf<IntegrationType, IntegrationService>()
    
    fun initialize() {
        // Register all available integrations
        register(IntegrationType.HEALTH_CONNECT, HealthConnectService())
        register(IntegrationType.STRAVA, StravaService())
        register(IntegrationType.LINKEDIN, LinkedInService())
        register(IntegrationType.DUOLINGO, DuolingoService())
        register(IntegrationType.RESCUETIME, RescueTimeService())
        register(IntegrationType.TELEGRAM, TelegramService())
        register(IntegrationType.DISCORD, DiscordService())
        register(IntegrationType.SPOTIFY, SpotifyService())
        register(IntegrationType.GOODREADS, GoodreadsService())
        // ...
    }
    
    suspend fun syncAll() {
        val enabledIntegrations = integrationRepository.getEnabledIntegrations()
        
        enabledIntegrations.forEach { integration ->
            val service = services[integration.type]
            if (service != null && shouldSync(integration)) {
                try {
                    service.sync()
                    integrationRepository.updateLastSync(integration.id, Instant.now())
                } catch (e: Exception) {
                    // Log error, notify user if critical
                    Timber.e(e, "Sync failed for ${integration.type}")
                }
            }
        }
    }
    
    private fun shouldSync(integration: UserIntegration): Boolean {
        val lastSync = integration.lastSync ?: return true
        val frequency = integration.syncFrequency
        
        return when (frequency) {
            SyncFrequency.REAL_TIME -> Duration.between(lastSync, Instant.now()).toMinutes() > 15
            SyncFrequency.HOURLY -> Duration.between(lastSync, Instant.now()).toHours() > 1
            SyncFrequency.DAILY -> Duration.between(lastSync, Instant.now()).toDays() > 0
        }
    }
}
```

---

## 📊 Recommended Implementation Order

### Tier 1 (Weeks 1-4) - **Highest Value**
1. **Health Connect** - Covers fitness, sleep, weight, nutrition in one integration
2. **LinkedIn** - Career & job applications
3. **Strava** - Running/cycling (if not using Health Connect)

### Tier 2 (Weeks 5-8) - **High Value**
4. **Google Sheets** - Manual portfolio tracking
5. **Duolingo** - Language learning
6. **RescueTime** - Productivity across all apps

### Tier 3 (Weeks 9-12) - **Medium Value**
7. **Telegram** - Social engagement
8. **Discord** - Community activity
9. **Spotify** - Music listening

### Tier 4 (Weeks 13-16) - **Nice to Have**
10. **Goodreads** - Books
11. **Fitbit** - Alternative to Health Connect
12. **Notion** - Task completion

---

## 🎯 Success Metrics

Track these KPIs post-launch:

1. **Integration Adoption Rate**
   - % of users connecting ≥1 integration
   - Most popular integrations

2. **Auto-Logged Entries**
   - Number of tracker entries from integrations vs manual
   - Target: 50% auto-logged within 3 months

3. **User Retention**
   - Do users with integrations stay longer?
   - Target: 20% higher D30 retention

4. **Praxis Points Earned**
   - PP from auto-logged vs manual entries
   - Target: 40% from auto-logged

---

## 🔐 Security Considerations

1. **Token Storage**
   - Always use Android Keystore
   - Never store raw tokens in SharedPreferences
   - Encrypt before storing

2. **OAuth Flows**
   - Use PKCE for public clients
   - Store client secrets in BuildConfig (not in code)
   - Refresh tokens before expiry

3. **Permissions**
   - Request minimum required scopes
   - Explain why each permission is needed
   - Allow users to revoke access

4. **Rate Limiting**
   - Respect API rate limits
   - Implement exponential backoff
   - Cache responses when possible

---

## 📝 Next Steps

1. **Create GitHub issues** for each integration
2. **Set up developer accounts** for each service:
   - Google Cloud Console (Health Connect)
   - Strava Developers
   - LinkedIn Developers
   - etc.
3. **Implement Health Connect first** (proof of concept)
4. **Build settings UI** for integration management
5. **Test with beta users** and iterate

---

**This plan provides a complete roadmap for integrating 40+ external apps into Praxis trackers.**

Start with Health Connect for maximum impact, then expand based on user feedback and demand.
