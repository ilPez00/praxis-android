package com.praxis.app.integrations.api

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.praxis.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import java.util.concurrent.TimeUnit

/**
 * Data classes for API requests/responses
 */
data class TrackerLogRequest(
    val type: String,
    val data: Map<String, Any>,
    val source: String = "manual"
)

data class TrackerLogResponse(
    val success: Boolean,
    val entryId: String?,
    val pointsAwarded: Int
)

data class UserIntegrationsResponse(
    val integrations: List<IntegrationStatus>
)

data class IntegrationStatus(
    val type: String,
    val enabled: Boolean,
    val lastSync: String?,
    val connected: Boolean
)

/**
 * Retrofit API interface for Praxis backend
 */
interface PraxisApiService {
    @POST("trackers/log")
    suspend fun logTrackerEntry(
        @Header("Authorization") token: String,
        @Body request: TrackerLogRequest
    ): TrackerLogResponse

    @GET("trackers/my")
    suspend fun getUserTrackers(
        @Header("Authorization") token: String,
        @retrofit2.http.Query("days") days: Int = 14
    ): List<TrackerData>

    @GET("integrations")
    suspend fun getUserIntegrations(
        @Header("Authorization") token: String
    ): UserIntegrationsResponse

    @POST("integrations/{type}/sync")
    suspend fun triggerIntegrationSync(
        @Header("Authorization") token: String,
        @retrofit2.http.Path("type") type: String
    ): TrackerLogResponse

    @GET("trackers/summary/today")
    suspend fun getTodayTrackerSummary(
        @Header("Authorization") token: String
    ): TodaySummary

    @GET("dashboard")
    suspend fun getDashboardData(
        @Header("Authorization") token: String
    ): DashboardData
}

data class TrackerData(
    val id: String,
    val type: String,
    val entries: List<TrackerEntryData>
)

data class TrackerEntryData(
    val id: String,
    val data: Map<String, Any>,
    val loggedAt: String
)

data class TodaySummary(
    val count: Int,
    val trackers: List<String>
)

data class DashboardData(
    val streak: Int,
    val praxisPoints: Int,
    val trackerCount: Int
)

/**
 * API client singleton
 */
object ApiClient {
    private const val TAG = "ApiClient"

    private val api: PraxisApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        retrofit.create(PraxisApiService::class.java)
    }

    fun getInstance(): PraxisApiService = api

    /**
     * Helper to create authenticated requests
     */
    fun createAuthenticatedApi(token: String): PraxisApiService {
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(PraxisApiService::class.java)
    }
}
