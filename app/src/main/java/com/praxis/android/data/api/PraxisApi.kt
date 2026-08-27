package com.praxis.android.data.api

import com.praxis.android.data.model.AuthRequest
import com.praxis.android.data.model.AuthResponse
import com.praxis.android.data.model.CheckInResponse
import com.praxis.android.data.model.Comment
import com.praxis.android.data.model.CreatePostRequest
import com.praxis.android.data.model.AddCommentRequest
import com.praxis.android.data.model.NotebookEntry
import com.praxis.android.data.model.NotebookEntriesResponse
import com.praxis.android.data.model.CreateEntryRequest
import com.praxis.android.data.model.UpdateEntryRequest
import com.praxis.android.data.model.NotebookStatsResponse
import com.praxis.android.data.model.GoalNode
import com.praxis.android.data.model.CreateGoalRequest
import com.praxis.android.data.model.UpdateGoalRequest
import com.praxis.android.data.model.UpdateProgressRequest
import com.praxis.android.data.model.Message
import com.praxis.android.data.model.SendMessageRequest
import com.praxis.android.data.model.Group
import com.praxis.android.data.model.Friend
import com.praxis.android.data.model.Match
import com.praxis.android.data.model.UserProfile
import com.praxis.android.data.model.ProfileResponse
import com.praxis.android.data.model.UpdateProfileRequest
import com.praxis.android.data.model.DeleteAccountRequest
import com.praxis.android.data.model.LeaderboardEntry
import com.praxis.android.data.model.GamificationProfile
import com.praxis.android.data.model.Achievement
import com.praxis.android.data.model.Challenge
import com.praxis.android.data.model.Event
import com.praxis.android.data.model.Place
import com.praxis.android.data.model.SearchResponse
import com.praxis.android.data.model.Post
import com.praxis.android.data.model.Bet
import com.praxis.android.data.model.CreateBetRequest
import com.praxis.android.data.model.Duel
import com.praxis.android.data.model.Board
import com.praxis.android.data.model.Cohort
import com.praxis.android.data.model.SeasonalEvent
import com.praxis.android.data.model.FriendRequest
import com.praxis.android.data.model.UpgradePlan
import com.praxis.android.data.model.Notification
import com.praxis.android.data.model.Tracker
import com.praxis.android.data.model.CustomSchema
import com.praxis.android.data.model.OracleConnection
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PraxisApi {
    @POST("auth/signup")
    suspend fun signup(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @GET("checkins/today")
    suspend fun getTodayCheckin(@Query("userId") userId: String): Response<CheckInResponse>

    @POST("checkins")
    suspend fun checkIn(@Query("userId") userId: String, @Header("X-Idempotency-Key") idempotencyKey: String): Response<CheckInResponse>

    @GET("checkins")
    suspend fun getCheckins(@Query("userId") userId: String, @Query("days") days: Int = 7): Response<List<CheckInResponse>>

    @GET("posts")
    suspend fun getPosts(@Query("context") context: String = "general", @Query("userId") userId: String? = null, @Query("limit") limit: Int = 20): Response<List<Post>>

    @GET("posts/by-user/")
    suspend fun getUserPosts(@Query("userId") userId: String): Response<List<Post>>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") postId: String): Response<Post>

    @POST("posts")
    suspend fun createPost(@Body request: CreatePostRequest, @Header("X-Idempotency-Key") idempotencyKey: String): Response<Post>

    @DELETE("posts/{id}")
    suspend fun deletePost(@Path("id") postId: String): Response<Unit>

    @POST("posts/{id}/likes")
    suspend fun toggleLike(@Path("id") postId: String): Response<Post>

    @GET("posts/{id}/comments")
    suspend fun getComments(@Path("id") postId: String): Response<List<Comment>>

    @POST("posts/{id}/comments")
    suspend fun addComment(@Path("id") postId: String, @Body request: AddCommentRequest): Response<Comment>

    @DELETE("posts/{postId}/comments/{commentId}")
    suspend fun deleteComment(@Path("postId") postId: String, @Path("commentId") commentId: String): Response<Unit>

    @GET("notebook/entries")
    suspend fun getNotebookEntries(
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 50,
        @Query("entry_type") entryType: String? = null,
        @Query("domain") domain: String? = null,
        @Query("tag") tag: String? = null,
        @Query("search") search: String? = null
    ): Response<NotebookEntriesResponse>

    @POST("notebook/entries")
    suspend fun createNotebookEntry(@Body request: CreateEntryRequest, @Header("X-Idempotency-Key") idempotencyKey: String): Response<NotebookEntry>

    @PATCH("notebook/entries/{id}")
    suspend fun updateNotebookEntry(@Path("id") entryId: String, @Body request: UpdateEntryRequest): Response<NotebookEntry>

    @DELETE("notebook/entries/{id}")
    suspend fun deleteNotebookEntry(@Path("id") entryId: String): Response<Unit>

    @GET("notebook/stats")
    suspend fun getNotebookStats(@Query("userId") userId: String): Response<NotebookStatsResponse>

    @GET("notebook/tags")
    suspend fun getNotebookTags(@Query("userId") userId: String): Response<List<String>>

    @PATCH("notebook/entries/{id}/pin")
    suspend fun pinNotebookEntry(@Path("id") entryId: String): Response<NotebookEntry>

    @GET("notebook/entries/{id}/rhymes")
    suspend fun getEntryRhymes(@Path("id") entryId: String): Response<List<NotebookEntry>>

    @GET("goals/{userId}")
    suspend fun getGoals(@Path("userId") userId: String): Response<List<GoalNode>>

    @POST("goals/{userId}")
    suspend fun createGoal(@Path("userId") userId: String, @Body request: CreateGoalRequest): Response<GoalNode>

    @PATCH("goals/{userId}/node/{nodeId}")
    suspend fun updateGoalNode(@Path("userId") userId: String, @Path("nodeId") nodeId: String, @Body request: UpdateGoalRequest): Response<GoalNode>

    @PATCH("goals/{userId}/node/{nodeId}/progress")
    suspend fun updateGoalProgress(@Path("userId") userId: String, @Path("nodeId") nodeId: String, @Body request: UpdateProgressRequest): Response<GoalNode>

    @DELETE("goals/{userId}/node/{nodeId}")
    suspend fun deleteGoalNode(@Path("userId") userId: String, @Path("nodeId") nodeId: String): Response<Unit>

    @POST("goals/{userId}/node/{nodeId}/tracker-progress")
    suspend fun trackerProgress(@Path("userId") userId: String, @Path("nodeId") nodeId: String, @Body request: Map<String, Any>): Response<GoalNode>

    @GET("messages/{user1Id}/{user2Id}")
    suspend fun getMessages(@Path("user1Id") user1Id: String, @Path("user2Id") user2Id: String): Response<List<Message>>

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest, @Header("X-Idempotency-Key") idempotencyKey: String): Response<Message>

    @GET("groups")
    suspend fun getGroups(@Query("type") type: String? = null): Response<List<Group>>

    @GET("groups/{roomId}/messages")
    suspend fun getGroupMessages(@Path("roomId") roomId: String): Response<List<Message>>

    @POST("groups/{roomId}/messages")
    suspend fun sendGroupMessage(@Path("roomId") roomId: String, @Body request: SendMessageRequest): Response<Message>

    @POST("groups/{roomId}/join")
    suspend fun joinGroup(@Path("roomId") roomId: String): Response<Group>

    @DELETE("groups/{roomId}/leave")
    suspend fun leaveGroup(@Path("roomId") roomId: String): Response<Unit>

    @GET("groups/{roomId}/members")
    suspend fun getGroupMembers(@Path("roomId") roomId: String): Response<List<UserProfile>>

    @GET("friends")
    suspend fun getFriends(): Response<List<Friend>>

    @GET("friends/requests/incoming")
    suspend fun getIncomingFriendRequests(): Response<List<FriendRequest>>

    @POST("friends/request/{targetUserId}")
    suspend fun sendFriendRequest(@Path("targetUserId") targetUserId: String): Response<Unit>

    @POST("friends/accept/{requestId}")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: String): Response<Unit>

    @DELETE("friends/requests/{requestId}")
    suspend fun rejectFriendRequest(@Path("requestId") requestId: String): Response<Unit>

    @DELETE("friends/{friendId}")
    suspend fun unfriend(@Path("friendId") friendId: String): Response<Unit>

    @GET("matches/{userId}")
    suspend fun getMatches(@Path("userId") userId: String): Response<List<Match>>

    @POST("sparring/request")
    suspend fun sendSparringRequest(@Body request: Map<String, String>): Response<Unit>

    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Response<ProfileResponse>

    @GET("users/me")
    suspend fun getMyProfile(): Response<ProfileResponse>

    @PUT("users/{id}")
    suspend fun updateProfile(@Path("id") userId: String, @Body request: UpdateProfileRequest): Response<ProfileResponse>

    @GET("users/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 100): Response<List<LeaderboardEntry>>

    @GET("gamification/profile")
    suspend fun getGamificationProfile(): Response<GamificationProfile>

    @GET("gamification/achievements")
    suspend fun getAchievements(): Response<List<Achievement>>

    @GET("gamification/quests")
    suspend fun getDailyQuests(): Response<List<Map<String, Any>>>

    @GET("challenges")
    suspend fun getChallenges(): Response<List<Challenge>>

    @POST("challenges/{challengeId}/join")
    suspend fun joinChallenge(@Path("challengeId") challengeId: String): Response<Unit>

    @DELETE("challenges/{challengeId}/leave")
    suspend fun leaveChallenge(@Path("challengeId") challengeId: String): Response<Unit>

    @GET("events")
    suspend fun getEvents(): Response<List<Event>>

    @POST("events/{id}/rsvp")
    suspend fun rsvpEvent(@Path("id") eventId: String): Response<Unit>

    @DELETE("events/{id}/rsvp")
    suspend fun removeRsvp(@Path("id") eventId: String): Response<Unit>

    @GET("places")
    suspend fun getPlaces(): Response<List<Place>>

    @GET("search")
    suspend fun search(@Query("q") query: String): Response<SearchResponse>

    @GET("bets")
    suspend fun getBets(): Response<List<Bet>>

    @POST("bets")
    suspend fun createBet(@Body request: CreateBetRequest): Response<Bet>

    @DELETE("bets/{betId}")
    suspend fun cancelBet(@Path("betId") betId: String): Response<Unit>

    @POST("bets/challenge")
    suspend fun createDuel(@Body request: Map<String, String>): Response<Duel>

    @POST("bets/duel/{id}/accept")
    suspend fun acceptDuel(@Path("id") duelId: String): Response<Duel>

    @POST("bets/duel/{id}/decline")
    suspend fun declineDuel(@Path("id") duelId: String): Response<Unit>

    @GET("bets/duel/{id}")
    suspend fun getDuel(@Path("id") duelId: String): Response<Duel>

    @GET("cohorts")
    suspend fun getCohorts(): Response<List<Cohort>>

    @POST("cohorts/{id}/join")
    suspend fun joinCohort(@Path("id") cohortId: String): Response<Unit>

    @GET("seasonal-events/active")
    suspend fun getActiveSeasonalEvents(): Response<List<SeasonalEvent>>

    @GET("notifications")
    suspend fun getNotifications(): Response<List<Notification>>

    @POST("notifications/read-all")
    suspend fun markNotificationsRead(): Response<Unit>

    @GET("upgrade/plans")
    suspend fun getUpgradePlans(): Response<List<UpgradePlan>>

    @GET("fails")
    suspend fun getFails(): Response<List<Map<String, Any>>>

    @GET("fails/stats")
    suspend fun getFailsStats(): Response<Map<String, Any>>

    @POST("fails")
    suspend fun createFail(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("fails/{id}/retry")
    suspend fun retryFail(@Path("id") failId: String): Response<Map<String, Any>>

    @GET("trackers/my")
    suspend fun getMyTrackers(): Response<List<Tracker>>

    @GET("trackers/calendar")
    suspend fun getTrackerCalendar(@Query("days") days: Int = 90): Response<List<Map<String, Any>>>

    @GET("trackers/summary/today")
    suspend fun getTodayTrackerSummary(): Response<List<Map<String, Any>>>

    @GET("oracle/connections")
    suspend fun getOracleConnections(): Response<List<OracleConnection>>

    @POST("oracle/connect")
    suspend fun connectOracle(@Body request: Map<String, String>): Response<OracleConnection>

    @DELETE("oracle/connect/{provider}")
    suspend fun disconnectOracle(@Path("provider") provider: String): Response<Unit>

    @DELETE("auth/account")
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<Unit>

    @GET("users/me/export")
    suspend fun exportData(): Response<Unit>

    @GET("cv/{username}")
    suspend fun getCv(@Path("username") username: String): Response<Map<String, Any>>

    @GET("verify/{credentialId}")
    suspend fun verifyCredential(@Path("credentialId") credentialId: String): Response<Map<String, Any>>

    @POST("users/complete-onboarding")
    suspend fun completeOnboarding(@Body request: Map<String, Any>): Response<Unit>

    @GET("users/stats/public")
    suspend fun getPublicStats(@Query("userId") userId: String): Response<Map<String, Any>>

    @GET("notifications/unread-count")
    suspend fun getUnreadNotificationsCount(): Response<Map<String, Any>>

    @POST("notifications/read")
    suspend fun markNotificationRead(@Path("id") notificationId: String): Response<Unit>

    @GET("physis/today")
    suspend fun getPhysisToday(): Response<Map<String, Any>>

    @GET("physis/snapshot")
    suspend fun getPhysisSnapshot(): Response<Map<String, Any>>

    @GET("physis/rhythms")
    suspend fun getPhysisRhythms(): Response<List<Map<String, Any>>>

    @GET("physis/coherence-history")
    suspend fun getPhysisCoherenceHistory(): Response<List<Map<String, Any>>>

    @GET("physis/social-ties")
    suspend fun getPhysisSocialTies(): Response<List<Map<String, Any>>>

    @GET("physis/places")
    suspend fun getPhysisPlaces(): Response<List<Map<String, Any>>>

    @GET("physis/training-balance")
    suspend fun getPhysisTrainingBalance(): Response<Map<String, Any>>

    @GET("physis/transitions")
    suspend fun getPhysisTransitions(): Response<List<Map<String, Any>>>

    @GET("lattice")
    suspend fun getLattice(): Response<Map<String, Any>>

    @GET("lattice/devices")
    suspend fun getLatticeDevices(): Response<List<Map<String, Any>>>

    @GET("lattice/jobs")
    suspend fun getLatticeJobs(): Response<List<Map<String, Any>>>

    @GET("marketplace")
    suspend fun getMarketplace(): Response<List<Map<String, Any>>>

    @GET("marketplace/items")
    suspend fun getMarketplaceItems(): Response<List<Map<String, Any>>>

    @POST("marketplace/purchase")
    suspend fun purchaseMarketplaceItem(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("mcp/message")
    suspend fun sendMcpMessage(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("mcp/sse")
    suspend fun getMcpSse(): Response<Map<String, Any>>

    @GET("wiki")
    suspend fun getWiki(): Response<Map<String, Any>>

    @GET("uberwiki/graph")
    suspend fun getUberWikiGraph(): Response<Map<String, Any>>

    @GET("uberwiki/status")
    suspend fun getUberWikiStatus(): Response<Map<String, Any>>

    @GET("uberwiki/query")
    suspend fun queryUberWiki(@Query("q") query: String): Response<Map<String, Any>>

    @GET("uberwiki/cells")
    suspend fun getUberWikiCells(): Response<List<Map<String, Any>>>

    @GET("wiki-index")
    suspend fun getWikiIndex(): Response<List<Map<String, Any>>>

    @GET("wiki/pages")
    suspend fun getWikiPages(): Response<List<Map<String, Any>>>

    @GET("wiki/search")
    suspend fun searchWiki(@Query("q") query: String): Response<List<Map<String, Any>>>

    @GET("ontology")
    suspend fun getOntology(): Response<Map<String, Any>>

    @GET("aura-web")
    suspend fun getAuraWeb(): Response<Map<String, Any>>

    @GET("gantt/plan")
    suspend fun getGanttPlan(): Response<List<Map<String, Any>>>

    @GET("plan/items")
    suspend fun getPlanItems(): Response<List<Map<String, Any>>>

    @GET("coherence/gantt")
    suspend fun getCoherenceGantt(): Response<Map<String, Any>>

    @GET("coherence/timeline")
    suspend fun getCoherenceTimeline(): Response<List<Map<String, Any>>>

    @GET("coherence/dashboard")
    suspend fun getCoherenceDashboard(): Response<Map<String, Any>>

    @GET("camera")
    suspend fun getCameraConfig(): Response<Map<String, Any>>

    @GET("words/frequency")
    suspend fun getWordsFrequency(): Response<List<Map<String, Any>>>

    @GET("seasonal-events/my-progress")
    suspend fun getMySeasonalProgress(): Response<List<Map<String, Any>>>

    @POST("seasonal-events/sponsor")
    suspend fun sponsorSeasonalEvent(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("trackers/activity-feed")
    suspend fun getTrackerActivityFeed(@Query("days") days: Int = 30, @Query("limit") limit: Int = 80): Response<List<Map<String, Any>>>

    @POST("trackers/log")
    suspend fun logTracker(@Body request: Map<String, Any>): Response<Map<String, Any>>

    @POST("notebook/ai-scan")
    suspend fun aiScanNote(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("trackers/monthly-by-goal")
    suspend fun getTrackerMonthlyByGoal(@Query("days") days: Int = 30): Response<List<Map<String, Any>>>

    @GET("trackers/custom/schemas")
    suspend fun getTrackerCustomSchemas(): Response<List<CustomSchema>>

    @POST("trackers")
    suspend fun createTracker(@Body request: Map<String, Any>): Response<Map<String, Any>>

    @PUT("trackers/{trackerId}")
    suspend fun updateTracker(@Path("trackerId") trackerId: String, @Body request: Map<String, Any>): Response<Map<String, Any>>

    @DELETE("trackers/{trackerId}")
    suspend fun deleteTracker(@Path("trackerId") trackerId: String): Response<Unit>

    @POST("oracle/detox/commit")
    suspend fun oracleDetoxCommit(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("oracle/github/auth")
    suspend fun oracleGithubAuth(@Body request: Map<String, String>): Response<OracleConnection>

    @POST("oracle/strava/auth")
    suspend fun oracleStravaAuth(@Body request: Map<String, String>): Response<OracleConnection>

    @POST("oracle/health-sample")
    suspend fun submitHealthSample(@Body body: Map<String, Double>): Response<Map<String, Any>>

    @GET("calendar/google/status")
    suspend fun getGoogleCalendarStatus(): Response<Map<String, Any>>

    @GET("calendar/google/events")
    suspend fun getGoogleEvents(@Query("days") days: Int = 7): Response<List<Map<String, Any>>>

    @GET("oracle/usage-sample")
    suspend fun getOracleUsageSample(): Response<Map<String, Any>>

    @POST("oracle/verify")
    suspend fun verifyOracle(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("checkins/mutual")
    suspend fun getMutualCheckins(@Query("user1Id") user1Id: String, @Query("user2Id") user2Id: String): Response<List<CheckInResponse>>

    @GET("gamification/combos")
    suspend fun getCombos(): Response<List<Map<String, Any>>>

    @POST("gamification/social/track")
    suspend fun trackSocialGamification(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("notebook/capture")
    suspend fun captureNotebookEntry(@Body request: Map<String, Any>): Response<NotebookEntry>

    @POST("notebook/axiom-query")
    suspend fun axiomQueryNotebook(@Body request: Map<String, String>): Response<List<NotebookEntry>>

    @GET("notebook/rhymes/digest")
    suspend fun getRhymesDigest(@Query("userId") userId: String): Response<List<NotebookEntry>>

    @POST("notebook/extract-place")
    suspend fun extractPlaceFromEntry(@Body request: Map<String, String>): Response<Place>

    @POST("notebook/geocode")
    suspend fun geocodeNotebookEntry(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("bioenergetics/checkin")
    suspend fun bioenergeticsCheckin(@Body request: Map<String, Any>): Response<Map<String, Any>>

    @GET("bioenergetics/checkin/history")
    suspend fun getBioenergeticsCheckinHistory(@Query("days") days: Int = 30): Response<List<Map<String, Any>>>

    @GET("bioenergetics/streak")
    suspend fun getBioenergeticsStreak(): Response<Map<String, Any>>

    @GET("bioenergetics/stats/weekly")
    suspend fun getBioenergeticsStatsWeekly(): Response<Map<String, Any>>

    @GET("bioenergetics/stats/monthly")
    suspend fun getBioenergeticsStatsMonthly(): Response<Map<String, Any>>

    @GET("bioenergetics/5r/history")
    suspend fun getBioenergetics5rHistory(): Response<List<Map<String, Any>>>

    @GET("bioenergetics/5r/log")
    suspend fun getBioenergetics5rLog(): Response<List<Map<String, Any>>>

    @GET("bioenergetics/metabolic")
    suspend fun getBioenergeticsMetabolic(): Response<Map<String, Any>>

    @GET("bioenergetics/metabolic/trend")
    suspend fun getBioenergeticsMetabolicTrend(): Response<List<Map<String, Any>>>

    @GET("bets/real/checkout")
    suspend fun getBetsRealCheckout(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("authoring/start")
    suspend fun startAuthoring(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("authoring/suggest-topics")
    suspend fun suggestAuthoringTopics(@Body request: Map<String, String>): Response<List<Map<String, Any>>>

    @GET("admin/axiom/check-providers")
    suspend fun adminAxiomCheckProviders(): Response<Map<String, Any>>

    @POST("admin/axiom/force-push")
    suspend fun adminAxiomForcePush(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("admin/axiom/key-usage")
    suspend fun adminAxiomKeyUsage(): Response<List<Map<String, Any>>>

    @GET("admin/axiom/providers")
    suspend fun adminAxiomProviders(): Response<List<Map<String, Any>>>

    @GET("admin/axiom/stats")
    suspend fun adminAxiomStats(): Response<Map<String, Any>>

    @POST("admin/axiom/trigger-scan")
    suspend fun adminAxiomTriggerScan(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("admin/challenges")
    suspend fun adminChallenges(): Response<List<Map<String, Any>>>

    @POST("admin/cli/execute")
    suspend fun adminCliExecute(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("admin/cli/status")
    suspend fun adminCliStatus(): Response<Map<String, Any>>

    @GET("admin/coaches")
    suspend fun adminCoaches(): Response<List<Map<String, Any>>>

    @GET("admin/config")
    suspend fun adminConfig(): Response<Map<String, Any>>

    @POST("admin/config/axiom_key_strategy")
    suspend fun adminConfigAxiomKeyStrategy(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("admin/config/axiom_prompt")
    suspend fun adminConfigAxiomPrompt(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("admin/config/clear-seen-messages")
    suspend fun adminConfigClearSeenMessages(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("admin/config/global_login_message")
    suspend fun adminConfigGlobalLoginMessage(): Response<Map<String, Any>>

    @GET("admin/config/global_login_message_id")
    suspend fun adminConfigGlobalLoginMessageId(): Response<Map<String, Any>>

    @GET("admin/debug/errors")
    suspend fun adminDebugErrors(@Query("limit") limit: Int = 20): Response<List<Map<String, Any>>>

    @GET("admin/debug/health")
    suspend fun adminDebugHealth(): Response<Map<String, Any>>

    @GET("admin/debug/test/auth")
    suspend fun adminDebugTestAuth(): Response<Map<String, Any>>

    @GET("admin/debug/test/cache")
    suspend fun adminDebugTestCache(): Response<Map<String, Any>>

    @GET("admin/debug/test/db")
    suspend fun adminDebugTestDb(): Response<Map<String, Any>>

    @GET("admin/debug/test/email")
    suspend fun adminDebugTestEmail(): Response<Map<String, Any>>

    @GET("admin/debug/test/storage")
    suspend fun adminDebugTestStorage(): Response<Map<String, Any>>

    @GET("admin/demo-users")
    suspend fun adminDemoUsers(): Response<List<Map<String, Any>>>

    @GET("admin/economy")
    suspend fun adminEconomy(): Response<Map<String, Any>>

    @POST("admin/import-osm-places")
    suspend fun adminImportOsmPlaces(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("admin/metrics")
    suspend fun adminMetrics(): Response<Map<String, Any>>

    @GET("admin/network")
    suspend fun adminNetwork(): Response<Map<String, Any>>

    @POST("admin/seed")
    suspend fun adminSeed(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("admin/services")
    suspend fun adminServices(): Response<Map<String, Any>>

    @GET("admin/stats")
    suspend fun adminStats(): Response<Map<String, Any>>

    @GET("admin/users")
    suspend fun adminUsers(): Response<List<Map<String, Any>>>

    @POST("admin/users/grant-points-all")
    suspend fun adminGrantPointsAll(@Body request: Map<String, String>): Response<Map<String, Any>>
}
