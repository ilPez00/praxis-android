package com.praxis.app.integrations.services

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.Calendar as GoogleCalendar
import com.praxis.app.integrations.IntegrationType
import com.praxis.app.integrations.models.SyncResult
import com.praxis.app.integrations.models.TrackerEntry
import com.praxis.app.integrations.IntegrationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

/**
 * Google Calendar integration service
 * 
 * Fetches events from Google Calendar and logs to Praxis trackers.
 * Helps track appointments, meetings, and scheduled activities.
 */
class GoogleCalendarService(
    private val context: Context
) : IntegrationService {
    
    override val integrationType = IntegrationType.GOOGLE_CALENDAR
    
    private val prefs = context.getSharedPreferences("google_calendar_integration", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "GoogleCalendarService"
        private const val PREF_ACCOUNT_NAME = "account_name"
        private const val PREF_LAST_SYNC = "last_sync"
        private const val APPLICATION_NAME = "Praxis"
    }

    override suspend fun isAvailable(): Boolean {
        val accountName = prefs.getString(PREF_ACCOUNT_NAME, null)
        return accountName != null
    }

    override suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val accountName = prefs.getString(PREF_ACCOUNT_NAME, null)
                if (accountName == null) {
                    Log.w(TAG, "No Google account selected")
                    return@withContext false
                }

                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(CalendarScopes.CALENDAR_READONLY)
                ).apply {
                    selectedAccountName = accountName
                }

                // Test if we can access the calendar
                val service = GoogleCalendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName(APPLICATION_NAME)
                    .build()

                // Try to fetch calendar list to verify access
                service.calendarList().list().setMaxResults(1).execute()
                Log.d(TAG, "Google Calendar initialized successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Google Calendar", e)
                false
            }
        }
    }

    override suspend fun sync(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val accountName = prefs.getString(PREF_ACCOUNT_NAME, null) ?: run {
                    return@withContext SyncResult(
                        success = false,
                        error = "No Google account configured"
                    )
                }

                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(CalendarScopes.CALENDAR_READONLY)
                ).apply {
                    selectedAccountName = accountName
                }

                val service = GoogleCalendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName(APPLICATION_NAME)
                    .build()

                // Get events from next 24 hours
                val now = com.google.api.client.util.DateTime(System.currentTimeMillis())
                val oneDayLater = com.google.api.client.util.DateTime(System.currentTimeMillis() + 86400000)

                val events = service.events().list("primary")
                    .setMaxResults(50)
                    .setTimeMin(now)
                    .setTimeMax(oneDayLater)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                val items = events.items
                
                var entriesLogged = 0
                items.forEach { event ->
                    val trackerData = mapOf(
                        "event_title" to (event.summary ?: "Untitled Event"),
                        "start_time" to (event.start?.dateTime?.value ?: event.start?.date?.value ?: 0L),
                        "duration_minutes" to calculateEventDuration(event),
                        "location" to (event.location ?: ""),
                        "description" to (event.description ?: ""),
                        "source" to "Google Calendar"
                    )

                    IntegrationManager.logTrackerEntry(
                        TrackerEntry(
                            type = "journal", // Best fit for general event tracking
                            data = trackerData,
                            source = "auto_google_calendar"
                        )
                    )
                    entriesLogged++
                }

                // Update last sync time
                prefs.edit().putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply()

                Log.d(TAG, "Google Calendar sync complete: $entriesLogged events logged")
                SyncResult(
                    success = true,
                    entriesLogged = entriesLogged
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google Calendar sync failed", e)
                SyncResult(
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    override suspend fun getLastSyncTime(): Long? {
        val lastSync = prefs.getLong(PREF_LAST_SYNC, -1)
        return if (lastSync > 0) lastSync else null
    }

    /**
     * Set the Google account to use
     */
    fun setAccount(accountName: String) {
        prefs.edit().putString(PREF_ACCOUNT_NAME, accountName).apply()
    }

    /**
     * Get configured account name
     */
    fun getAccount(): String? = prefs.getString(PREF_ACCOUNT_NAME, null)

    /**
     * Calculate event duration in minutes
     */
    private fun calculateEventDuration(event: Event): Long {
        return try {
            when {
                event.start?.dateTime != null && event.end?.dateTime != null -> {
                    val start = event.start.dateTime.value
                    val end = event.end.dateTime.value
                    (end - start) / 60000 // Convert ms to minutes
                }
                event.start?.date != null && event.end?.date != null -> {
                    // All-day event
                    24 * 60 // Assume full day
                }
                else -> 60L // Default 1 hour
            }
        } catch (e: Exception) {
            60L
        }
    }

    /**
     * Get upcoming events for display in UI
     */
    suspend fun getUpcomingEvents(hours: Int = 24): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                val accountName = prefs.getString(PREF_ACCOUNT_NAME, null) ?: return@withContext emptyList()

                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(CalendarScopes.CALENDAR_READONLY)
                ).apply {
                    selectedAccountName = accountName
                }

                val service = GoogleCalendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName(APPLICATION_NAME)
                    .build()

                val now = com.google.api.client.util.DateTime(System.currentTimeMillis())
                val later = com.google.api.client.util.DateTime(System.currentTimeMillis() + (hours * 3600000L))

                val events = service.events().list("primary")
                    .setMaxResults(20)
                    .setTimeMin(now)
                    .setTimeMax(later)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                events.items.map { event ->
                    mapOf(
                        "title" to (event.summary ?: "Untitled"),
                        "start" to (event.start?.dateTime?.value ?: event.start?.date?.value ?: 0L),
                        "location" to (event.location ?: ""),
                        "description" to (event.description ?: "")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get upcoming events", e)
                emptyList()
            }
        }
    }
}
