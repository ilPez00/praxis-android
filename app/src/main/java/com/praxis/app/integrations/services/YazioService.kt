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
 * Yazio nutrition integration service
 * 
 * Fetches meal and nutrition data from Yazio API
 * and logs to Praxis meal tracker.
 */
class YazioService(
    private val context: Context
) : IntegrationService {
    
    override val integrationType = IntegrationType.YAZIO
    
    private val prefs = context.getSharedPreferences("yazio_integration", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "YazioService"
        const val CLIENT_ID = "YOUR_YAZIO_CLIENT_ID"
        const val CLIENT_SECRET = "YOUR_YAZIO_CLIENT_SECRET"
        const val REDIRECT_URI = "praxis://yazio/callback"
        
        private const val PREF_ACCESS_TOKEN = "access_token"
        private const val PREF_REFRESH_TOKEN = "refresh_token"
        private const val PREF_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val PREF_USER_ID = "user_id"
        private const val PREF_LAST_SYNC = "last_sync"
    }

    override suspend fun isAvailable(): Boolean {
        return getAccessToken() != null
    }

    override suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            val token = getAccessToken()
            if (token == null) {
                Log.w(TAG, "No Yazio token - need to run OAuth flow")
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
                        error = "No Yazio access token"
                    )
                }

                val userId = prefs.getString(PREF_USER_ID, null) ?: run {
                    return@withContext SyncResult(
                        success = false,
                        error = "No Yazio user ID"
                    )
                }

                // Get today's nutrition data
                val nutritionData = fetchTodayNutrition(token, userId)
                
                if (nutritionData != null) {
                    // Log meal tracker entry with nutrition summary
                    IntegrationManager.logTrackerEntry(
                        TrackerEntry(
                            type = "meal",
                            data = mapOf(
                                "calories" to (nutritionData["calories"] ?: 0),
                                "protein_g" to (nutritionData["protein"] ?: 0),
                                "carbs_g" to (nutritionData["carbs"] ?: 0),
                                "fat_g" to (nutritionData["fat"] ?: 0),
                                "meals_count" to (nutritionData["meals_count"] ?: 0),
                                "source" to "Yazio"
                            ),
                            source = "auto_yazio"
                        )
                    )

                    // Update last sync time
                    prefs.edit().putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply()

                    Log.d(TAG, "Yazio sync complete: nutrition data logged")
                    SyncResult(
                        success = true,
                        entriesLogged = 1
                    )
                } else {
                    SyncResult(
                        success = false,
                        error = "No nutrition data available"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Yazio sync failed", e)
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
                    .url("https://api.yazio.com/oauth/token")
                    .post(okhttp3.FormBody.Builder()
                        .add("client_id", CLIENT_ID)
                        .add("client_secret", CLIENT_SECRET)
                        .add("code", code)
                        .add("grant_type", "authorization_code")
                        .add("redirect_uri", REDIRECT_URI)
                        .build())
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    val accessToken = json["access_token"] as String
                    val refreshToken = json["refresh_token"] as String
                    val expiresIn = json["expires_in"] as Int
                    val userId = json["user_id"]?.toString()
                    
                    saveTokens(accessToken, refreshToken, System.currentTimeMillis() + (expiresIn * 1000))
                    userId?.let { prefs.edit().putString(PREF_USER_ID, it).apply() }
                    
                    Log.d(TAG, "Yazio token obtained")
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
                    .url("https://api.yazio.com/oauth/token")
                    .post(okhttp3.FormBody.Builder()
                        .add("client_id", CLIENT_ID)
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
                    val expiresIn = json["expires_in"] as Int
                    
                    saveTokens(accessToken, newRefreshToken, System.currentTimeMillis() + (expiresIn * 1000))
                    Log.d(TAG, "Yazio token refreshed")
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
     * Fetch today's nutrition data from Yazio API
     */
    private suspend fun fetchTodayNutrition(token: String, userId: String): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                // Note: Yazio API endpoint is hypothetical - adjust based on actual API docs
                val request = Request.Builder()
                    .url("https://api.yazio.com/v1/users/$userId/diary?date=today")
                    .header("Authorization", "Bearer $token")
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val json = gson.fromJson(body, Map::class.java)
                    
                    // Extract nutrition summary
                    val summary = json["summary"] as? Map<String, Any> ?: return@withContext null
                    
                    mapOf(
                        "calories" to (summary["calories"] as? Number ?: 0).toInt(),
                        "protein" to (summary["protein_g"] as? Number ?: 0).toInt(),
                        "carbs" to (summary["carbohydrates_g"] as? Number ?: 0).toInt(),
                        "fat" to (summary["fat_g"] as? Number ?: 0).toInt(),
                        "meals_count" to ((json["meals"] as? List<*>)?.size ?: 0)
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch nutrition data", e)
                null
            }
        }
    }
}
