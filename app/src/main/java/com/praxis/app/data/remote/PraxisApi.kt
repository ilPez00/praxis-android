package com.praxis.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface PraxisApi {
    @GET("users/{id}")
    suspend fun getProfile(@Path("id") id: String): Response<ApiProfile>

    @PUT("users/{id}")
    suspend fun updateProfile(
        @Path("id") id: String,
        @Body body: UpdateProfileRequest,
    ): Response<ApiProfile>

    @POST("users/complete-onboarding")
    suspend fun completeOnboarding(@Body body: CompleteOnboardingRequest): Response<Map<String, String>>

    @GET("goals/{userId}")
    suspend fun getGoalTree(@Path("userId") userId: String): Response<ApiGoalTree>

    @POST("goals")
    suspend fun createOrUpdateGoalTree(@Body body: GoalTreeRequest): Response<ApiGoalTree>

    @PATCH("goals/{userId}/node/{nodeId}/progress")
    suspend fun updateNodeProgress(
        @Path("userId") userId: String,
        @Path("nodeId") nodeId: String,
        @Body body: UpdateProgressRequest,
    ): Response<ApiGoalNode>

    @GET("matches/{userId}")
    suspend fun getMatches(@Path("userId") userId: String): Response<List<ApiMatch>>

    @GET("achievements")
    suspend fun getAchievements(): Response<List<ApiAchievement>>

    @GET("groups")
    suspend fun getRooms(): Response<List<ApiRoom>>

    @GET("messages/{user1Id}/{user2Id}")
    suspend fun getMessages(
        @Path("user1Id") user1Id: String,
        @Path("user2Id") user2Id: String,
    ): Response<List<ApiMessage>>

    @POST("messages")
    suspend fun sendMessage(@Body body: SendMessageRequest): Response<ApiMessage>

    // Checkins — require Bearer token
    @GET("checkins/today")
    suspend fun getTodayCheckin(): Response<ApiCheckin>

    @POST("checkins")
    suspend fun checkIn(): Response<ApiCheckin>
}
