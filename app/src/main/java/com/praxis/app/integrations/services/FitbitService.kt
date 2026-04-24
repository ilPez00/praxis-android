package com.praxis.app.integrations.services

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.praxis.app.integrations.IntegrationType
import com.praxis.app.integrations.models.SyncResult
import com.praxis.app.integrations.models.TrackerEntry
import com.praxis.app.integrations.IntegrationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fitbit integration service
 * 
 * Fetches activity, sleep, and heart rate data from Fitbit API
 * and logs to Praxis trackers.
 */
class FitbitService(
    private val context: Context
) : IntegrationService {
    
    override val integrationType = IntegrationType.FITBIT
    
    private val prefs = context.getSharedPreferences("fitbit_integration", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "FitbitService"
        const val CLIENT_ID = "YOUR_FITBIT_CLIENT_ID"
        const val CLIENT_SECRET = "YOUR_FITBIT_CLIENT_SECRET"
        const val REDIRECT_URI = "praxis://fitbit/callback"
        
        private const val PREF_ACCESS_TOKEN = "access_token"
        private const val PREF_REFRESH_TOKEN = "refresh_token"
        private const val PREF_USER_ID = "user_id"
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
                Log.w(TAG, "No Fitbit token - need to run OAuth flow")
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
                        error = "No Fitbit access token"
                    )
                }

                var entriesLogged = 0

                // Fetch and log activities
                val activities = fetchActivitiesToday(token)
                if (activities != null) {
                    IntegrationManager.logTrackerEntry(
                        TrackerEntry(
                            type = "cardio",
                            data = mapOf(
                                "steps" to (activities["steps"] ?: 0),
                                "distance_km" to (activities["distance"] ?: 0.0),
                                "calories" to (activities["calories"] ?: 0),
                                "active_minutes" to (activities["active_minutes"] ?: 0),
                                "source" to "Fitbit"
                            ),
                            source = "auto_fitbit"
                        )
                    )
                    entriesLogged++
                }

                // Fetch and log sleep
                val sleep = fetchSleepToday(token)
                if (sleep != null) {
                    IntegrationManager.logTrackerEntry(
                        TrackerEntry(
                            type = "sleep",
                            data = mapOf(
                                "duration_hours" to (sleep["duration_hours"] ?: 0.0),
                                "efficiency" to (sleep["efficiency"] ?: 0),
                                "awake_count" to (sleep["awake_count"] ?: 0),
                                "source" to "Fitbit"
                            ),
                            source = "auto_fitbit"
                        )
                    )
                    entriesLogged++
                }

                // Fetch and log heart rate
                val heartRate = fetchHeartRateToday(token)
                if (heartRate != null) {
                    IntegrationManager.logTrackerEntry(
                        TrackerEntry(
                            type = "meditation",
                            data = mapOf(
                                "heart_rate_avg" to (heartRate["avg"] ?: 0),
                                "heart_rate_max" to (heartRate["max"] ?: 0),
                                "heart_rate_min" to (heartRate["min"] ?: 0),
                                "source" to "Fitbit"
                            ),
                            source = "auto_fitbit"
                        )
                    )
                    entriesLogged++
                }

                // Update last sync time
                prefs.edit().putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply()

                Log.d(TAG, "Fitbit sync complete: $entriesLogged entries logged")
                SyncResult(
                    success = true,
                    entriesLogged = entriesLogged
                )
            } catch (e: Exception) {
                Log.e(TAG, "Fitbit sync failed", e)
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
     * Exchange authorization code for access token
     */
    private suspend fun exchangeCodeForToken(code: String) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.fitbit.com/oauth2/token")
                    .post(okhttp3.FormBody.Builder()
                        .add("client_id", CLIENT_ID)
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("redirect_uri", REDIRECT_URI)
                        .build())
                    .header("Authorization", "Basic " + android.util.Base64.encodeToString(
                        "$CLIENT_ID:$CLIENT_SECRET".toByteArray(),
                        android.util.Base64.NO_WRAP
                    ))
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    val accessToken = json["access_token"] as String
                    val refreshToken = json["refresh_token"] as String
                    val expiresIn = json["expires_in"] as Int
                    val userId = json["user_id"]?.toString()
                    
                    saveTokens(accessToken, refreshToken, userId, System.currentTimeMillis() + (expiresIn * 1000))
                    Log.d(TAG, "Fitbit token obtained")
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
                    .url("https://api.fitbit.com/oauth2/token")
                    .post(okhttp3.FormBody.Builder()
                        .add("client_id", CLIENT_ID)
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .build())
                    .header("Authorization", "Basic " + android.util.Base64.encodeToString(
                        "$CLIENT_ID:$CLIENT_SECRET".toByteArray(),
                        android.util.Base64.NO_WRAP
                    ))
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    val accessToken = json["access_token"] as String
                    val newRefreshToken = json["refresh_token"] as String
                    val expiresIn = json["expires_in"] as Int
                    
                    saveTokens(
                        accessToken,
                        newRefreshToken,
                        prefs.getString(PREF_USER_ID, null),
                        System.currentTimeMillis() + (expiresIn * 1000)
                    )
                    Log.d(TAG, "Fitbit token refreshed")
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
    private fun saveTokens(accessToken: String, refreshToken: String, userId: String?, expiresAt: Long) {
        prefs.edit()
            .putString(PREF_ACCESS_TOKEN, accessToken)
            .putString(PREF_REFRESH_TOKEN, refreshToken)
            .putString(PREF_USER_ID, userId)
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
     * Fetch today's activity summary from Fitbit API
     */
    private suspend fun fetchActivitiesToday(token: String): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                val userId = prefs.getString(PREF_USER_ID, "-") ?: "-"
                val request = Request.Builder()
                    .url("https://api.fitbit.com/1/user/$userId/activities/date/today.json")
                    .header("Authorization", "Bearer $token")
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    val summary = json["summary"] as? Map<String, Any> ?: return@withContext null
                    
                    mapOf(
                        "steps" to (summary["steps"] as? Number ?: 0).toInt(),
                        "distance" to ((summary["distances"] as? List<*>)?.firstOrNull()?.let { 
                            (it as Map<String, Any>)["distance"] as? Double ?: 0.0 
                        } ?: 0.0),
                        "calories" to (summary["caloriesOut"] as? Number ?: 0).toInt(),
                        "active_minutes" to (summary["veryActiveMinutes"] as? Number ?: 0).toInt()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch activities", e)
                null
            }
        }
    }

    /**
     * Fetch last night's sleep data from Fitbit API
     */
    private suspend fun fetchSleepToday(token: String): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                val userId = prefs.getString(PREF_USER_ID, "-") ?: "-"
                val request = Request.Builder()
                    .url("https://api.fitbit.com/1.2/user/$userId/sleep/date/today.json")
                    .header("Authorization", "Bearer $token")
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    val sleeps = json["sleep"] as? List<*> ?: return@withContext null
                    
                    if (sleeps.isNotEmpty()) {
                        val sleep = sleeps.first() as Map<String, Any>
                        val durationMs = sleep["duration"] as? Number ?: 0
                        val durationHours = durationMs.toDouble() / 3600000.0
                        
                        mapOf(
                            "duration_hours" to durationHours,
                            "efficiency" to (sleep["efficiency"] as? Number ?: 0).toInt(),
                            "awake_count" to (sleep["awakeCount"] as? Number ?: 0).toInt()
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch sleep", e)
                null
            }
        }
    }

    /**
     * Fetch today's heart rate from Fitbit API
     */
    private suspend fun fetchHeartRateToday(token: String): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                val userId = prefs.getString(PREF_USER_ID, "-") ?: "-"
                val request = Request.Builder()
                    .url("https://api.fitbit.com/1/user/$userId/activities/heart/date/today/1d.json")
                    .header("Authorization", "Bearer $token")
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    val activitiesHeart = json["activities-heart"] as? List<*> ?: return@withContext null
                    
                    if (activitiesHeart.isNotEmpty()) {
                        val heartData = activitiesHeart.first() as Map<String, Any>
                        val zones = heartData["value"] as? Map<String, Any> ?: return@withContext null
                        
                        mapOf(
                            "avg" to (zones["restingHeartRate"] as? Number ?: 72).toInt(),
                            "max" to 160, // Would need to parse from heart rate zones
                            "min" to (zones["restingHeartRate"] as? Number ?: 60).toInt()
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch heart rate", e)
                null
            }
        }
    }
}
