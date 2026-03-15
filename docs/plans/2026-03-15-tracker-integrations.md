# Tracker Integrations Plan - Praxis Android

## Overview
This plan outlines the implementation of automated trackers in the Praxis Android app, allowing users to sync data from various health, productivity, and social platforms.

## Core Infrastructure
### 1. Data Models
- **Tracker**: `id, type, goal, created_at`
- **TrackerEntry**: `tracker_id, data, logged_at`
- **SyncStatus**: `tracker_type, last_synced_at, connection_status`

### 2. API Repository (`ApiRepository.kt`)
- `suspend fun getMyTrackers(): Result<List<Tracker>>`
- `suspend fun logTracker(type: String, data: Map<String, Any>): Result<Unit>`
- `suspend fun updateTrackerGoal(type: String, goal: Map<String, Any>): Result<Unit>`

### 3. Sync Engine (`WorkManager`)
- `TrackerSyncWorker`: A periodic worker (every 1-4 hours) that iterates through connected services and triggers their respective sync logic.

---

## Service Integrations (Categorized)

### 1. Fitness & Health (High Priority)
- **Health Connect (Google)**:
  - Integration: `androidx.health.connect`
  - Metrics: Steps, Heart Rate, Calories, Sleep, Workouts.
  - *Status: Primary hub for Android health data.*
- **Strava / Fitbit / Garmin**:
  - Integration: OAuth2 + REST API.
  - Metrics: Running/Cycling distance, specific workout stats.

### 2. Career & Productivity
- **LinkedIn**:
  - Integration: LinkedIn API (OAuth).
  - Metrics: Profile updates, networking activity.
- **RescueTime**:
  - Integration: API Key.
  - Metrics: Focus time, productivity score.
- **Notion**:
  - Integration: Notion Public API (Internal Integration Token).
  - Metrics: Task completion in specific databases.

### 3. Academics / Learning
- **Duolingo**: Web scraping or unofficial API (limited).
- **Anki**: Local file intent or specialized sync.

### 4. Mental Health & Reflection
- **Sleep Cycle / Headspace**: Via Health Connect or direct OAuth where available.
- **Toggl Track**: API Key. Metrics: Time spent on "Reflective" tags.

### 5. Culture & Hobbies
- **Spotify**: OAuth + Web API. Metrics: Listening time, top genres.
- **Goodreads**: RSS feed or Web API. Metrics: Books read progress.

### 6. Social Engagement
- **Telegram / Discord**: Bot API or specialized hooks to track message counts in specific self-monitored channels.

---

## UI/UX Implementation

### 1. Tracker Settings Screen (`TrackerSettingsScreen.kt`)
- Grouped by the 9 Praxis domains.
- "Connect" buttons triggering OAuth flows or permission requests.
- "Configure" dialogs for mapping specific app metrics to Praxis goals.

### 2. Dashboard Integration
- "Active Trackers" horizontal scroll or list section.
- Visual progress bars for tracker-linked goals.

### 3. Widget Updates
- `TrackerWidgetProvider` enhancement to show real-time metrics (e.g., "Current Focus Time: 2h 15m").

## Security & Privacy
- Use `EncryptedSharedPreferences` for storing OAuth tokens and API keys.
- Clear disclosure to the user about what data is being synced and why.
- Option to "Wipe Local Sync Data" and "Disconnect All".
