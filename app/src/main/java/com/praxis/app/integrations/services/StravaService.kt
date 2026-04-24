package com.praxis.app.integrations.services

import android.content.Context
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.praxis.app.integrations.IntegrationType
import com.praxis.app.integrations.models.SyncResult
import com.praxis.app.integrations.models.TrackerEntry
import com.praxis.app.integrations.IntegrationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Strava integration service
 * 
 * Fetches activities from Strava API and logs to Praxis trackers.
 * Supports running, cycling, hiking, and other cardio activities.
 */
class StravaService(
    private val context: Context
) : IntegrationService {
    
    override val integrationType = IntegrationType.STRAVA
    
    private val prefs = context.getSharedPreferences("strava_integration", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "StravaService"
        const val CLIENT_ID = "YOUR_STRAVA_CLIENT_ID" // Replace with actual client ID
        const val CLIENT_SECRET = "YOUR_STRAVA_CLIENT_SECRET"
        const val REDIRECT_URI = "praxis://strava/callback"
        
        private const val PREF_ACCESS_TOKEN = "access_token"
        private const val PREF_REFRESH_TOKEN = "refresh_token"
        private const val PREF_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val PREF_LAST_SYNC = "last_sync"
    }

    override suspend fun isAvailable(): Boolean {
        return getAccessToken() != null
    }

    override suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            val token = getAccessToken()
            if (token == null) {
                Log.w(TAG, "No Strava token - need to run OAuth flow")
                return@withContext false
            }
            
            // Check if token is expired
            val expiresAt = prefs.getLong(PREF_TOKEN_EXPIRES_AT, 0)
            if (System.currentTimeMillis() >= expiresAt) {
                Log.d(TAG, "Token expired, refreshing...")
                return@withContext refreshAccessToken()
            }
            
            true
        }
    }

    override suspend fun sync(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken() ?: run {
                    return@withContext SyncResult(
                        success = false,
                        error = "No Strava access token"
                    )
                }

                // Get activities from last 24 hours
                val activities = fetchRecentActivities(token, hours = 24)
                
                var entriesLogged = 0
                activities.forEach { activity ->
                    val trackerType = mapActivityToTrackerType(activity["type"] as? String ?: "Unknown")
                    if (trackerType != null) {
                        IntegrationManager.logTrackerEntry(
                            TrackerEntry(
                                type = trackerType,
                                data = mapOf<String, Any>(
                                    "activity" to (activity["type"] as? String ?: "Unknown"),
                                    "duration" to (activity["duration"] as? Double ?: 0.0),
                                    "distance" to (activity["distance"] as? Double ?: 0.0),
                                    "elevation" to (activity["elevation"] as? Double ?: 0.0),
                                    "source" to "Strava"
                                ),
                                source = "auto_strava"
                            )
                        )
                        entriesLogged++
                    }
                }

                // Update last sync time
                prefs.edit().putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply()

                Log.d(TAG, "Strava sync complete: $entriesLogged entries logged")
                SyncResult(
                    success = true,
                    entriesLogged = entriesLogged
                )
            } catch (e: Exception) {
                Log.e(TAG, "Strava sync failed", e)
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
     * Launch Strava OAuth authorization
     */
    fun launchOAuth(context: Context) {
        val authUrl = buildAuthorizationUrl()
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(context, android.net.Uri.parse(authUrl))
    }

    /**
     * Handle OAuth callback with authorization code
     */
    suspend fun handleOAuthCallback(code: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                exchangeCodeForToken(code)
                true
            } catch (e: Exception) {
                Log.e(TAG, "OAuth callback failed", e)
                false
            }
        }
    }

    /**
     * Build Strava authorization URL
     */
    private fun buildAuthorizationUrl(): String {
        val redirectUriEncoded = URLEncoder.encode(REDIRECT_URI, "UTF-8")
        return "https://www.strava.com/oauth/authorize?" +
                "client_id=$CLIENT_ID&" +
                "redirect_uri=$redirectUriEncoded&" +
                "response_type=code&" +
                "approval_prompt=force&" +
                "scope=read,activity:read_all"
    }

    /**
     * Exchange authorization code for access token
     */
    private suspend fun exchangeCodeForToken(code: String) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.strava.com/oauth/token")
                    .post(okhttp3.FormBody.Builder()
                        .add("client_id", CLIENT_ID.toString())
                        .add("client_secret", CLIENT_SECRET)
                        .add("code", code)
                        .add("grant_type", "authorization_code")
                        .build())
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    val accessToken = json["access_token"] as String
                    val refreshToken = json["refresh_token"] as String
                    val expiresAt = json["expires_at"] as Double
                    
                    saveTokens(accessToken, refreshToken, expiresAt.toLong())
                    Log.d(TAG, "Strava token obtained")
                } else {
                    throw Exception("Failed to get token: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token exchange failed", e)
                throw e
            }
        }
    }

    /**
     * Refresh access token
     */
    private suspend fun refreshAccessToken(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = prefs.getString(PREF_REFRESH_TOKEN, null) ?: return@withContext false
                
                val request = Request.Builder()
                    .url("https://www.strava.com/oauth/token")
                    .post(okhttp3.FormBody.Builder()
                        .add("client_id", CLIENT_ID.toString())
                        .add("client_secret", CLIENT_SECRET)
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .build())
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    val accessToken = json["access_token"] as String
                    val newRefreshToken = json["refresh_token"] as String
                    val expiresAt = json["expires_at"] as Double
                    
                    saveTokens(accessToken, newRefreshToken, expiresAt.toLong())
                    Log.d(TAG, "Strava token refreshed")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh failed", e)
                false
            }
        }
    }

    /**
     * Save tokens to SharedPreferences
     */
    private fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs.edit()
            .putString(PREF_ACCESS_TOKEN, accessToken)
            .putString(PREF_REFRESH_TOKEN, refreshToken)
            .putLong(PREF_TOKEN_EXPIRES_AT, expiresAt)
            .apply()
    }

    /**
     * Get access token from SharedPreferences
     */
    private fun getAccessToken(): String? {
        return prefs.getString(PREF_ACCESS_TOKEN, null)
    }

    /**
     * Fetch recent activities from Strava API
     */
    private suspend fun fetchRecentActivities(token: String, hours: Int): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.strava.com/api/v3/athlete/activities?per_page=10")
                    .header("Authorization", "Bearer $token")
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val activities = gson.fromJson(body, List::class.java) as List<Map<String, Any>>
                    
                    // Filter by time and map to our format
                    activities.mapNotNull { activity ->
                        val elapsed_time = activity["elapsed_time"] as? Int ?: 0
                        val distance = (activity["distance"] as? Double ?: 0.0) / 1000.0 // Convert to km
                        val elevation = (activity["total_elevation_gain"] as? Double ?: 0.0)
                        
                        mapOf(
                            "type" to (activity["type"] as? String ?: "Unknown"),
                            "duration" to (elapsed_time / 60.0), // Convert to minutes
                            "distance" to distance,
                            "elevation" to elevation
                        )
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch activities", e)
                emptyList()
            }
        }
    }

    /**
     * Map Strava activity type to Praxis tracker type
     */
    private fun mapActivityToTrackerType(activityType: String): String? {
        return when (activityType.lowercase()) {
            "run", "trail_run" -> "cardio"
            "ride", "gravel_ride", "mountain_bike_ride", "ebike_ride" -> "cardio"
            "hike", "walk" -> "cardio"
            "swim" -> "cardio"
            "yoga" -> "meditation"
            else -> null
        }
    }
}
