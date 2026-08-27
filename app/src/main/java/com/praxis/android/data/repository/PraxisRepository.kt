package com.praxis.android.data.repository

import android.content.Context
import com.praxis.android.auth.AuthManager
import com.praxis.android.data.api.PraxisApi
import com.praxis.android.data.api.RetrofitClient
import com.praxis.android.data.local.CachedPost
import com.praxis.android.data.local.PraxisDatabase
import com.praxis.android.data.model.AuthRequest
import com.praxis.android.data.model.CheckInResponse
import com.praxis.android.data.model.Post
import com.praxis.android.data.model.CreatePostRequest
import com.praxis.android.data.model.Comment
import com.praxis.android.data.model.AddCommentRequest
import com.praxis.android.data.model.NotebookEntry
import com.praxis.android.data.model.NotebookEntriesResponse
import com.praxis.android.data.model.UpdateEntryRequest
import com.praxis.android.data.model.NotebookStatsResponse
import com.praxis.android.data.model.CreateEntryRequest
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
import com.praxis.android.data.model.LeaderboardEntry
import com.praxis.android.data.model.GamificationProfile
import com.praxis.android.data.model.Achievement
import com.praxis.android.data.model.Challenge
import com.praxis.android.data.model.Event
import com.praxis.android.data.model.Place
import com.praxis.android.data.model.SearchResponse
import com.praxis.android.data.model.Bet
import com.praxis.android.data.model.CreateBetRequest
import com.praxis.android.data.model.Duel
import com.praxis.android.data.model.Cohort
import com.praxis.android.data.model.SeasonalEvent
import com.praxis.android.data.model.FriendRequest
import com.praxis.android.data.model.Notification
import com.praxis.android.data.model.Tracker
import com.praxis.android.data.model.OracleConnection
import com.praxis.android.data.model.CustomSchema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class PraxisRepository(private val context: Context) {
    private val api = RetrofitClient.api
    private val db = PraxisDatabase.getInstance(context)

    init {
        RetrofitClient.init(context)
    }

    // Auth
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val res = api.login(AuthRequest(email, password))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                AuthManager.saveAuth(context, body.accessToken, body.user?.id ?: "", body.user?.email ?: email)
                RetrofitClient.setAuthToken(body.accessToken)
                saveWidgetSession(body)
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(email: String, password: String): Result<Unit> {
        return try {
            val res = api.signup(AuthRequest(email, password))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                AuthManager.saveAuth(context, body.accessToken, body.user?.id ?: "", body.user?.email ?: email)
                RetrofitClient.setAuthToken(body.accessToken)
                saveWidgetSession(body)
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mirror the session into the widget store so home-screen widgets and the
     * background refresh worker can talk to the API while the app is dead.
     * Expiry comes from the JWT's own `exp` claim; a token without one is
     * treated as hour-long, which matches the Supabase default.
     */
    private fun saveWidgetSession(body: com.praxis.android.data.model.AuthResponse) {
        val expiresAt = jwtExpiryMs(body.accessToken) ?: (System.currentTimeMillis() + 3_600_000L)
        app.praxisweb.xyz.WidgetStore.get(context).saveSession(
            body.accessToken,
            body.refreshToken,
            expiresAt,
            "https://praxisweb.xyz/api",
            app.praxisweb.xyz.BuildConfig.SUPABASE_URL,
            app.praxisweb.xyz.BuildConfig.SUPABASE_ANON_KEY
        )
        app.praxisweb.xyz.WidgetRefreshWorker.refreshNow(context)
    }

    private fun jwtExpiryMs(token: String): Long? = runCatching {
        val payload = String(android.util.Base64.decode(
            token.split(".")[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        ))
        org.json.JSONObject(payload).optLong("exp", 0L) * 1000L
    }.getOrNull()?.takeIf { it > 0L }

    // Check-ins
    suspend fun getTodayCheckin(userId: String): Result<CheckInResponse?> {
        return try {
            val res = api.getTodayCheckin(userId)
            if (res.isSuccessful) {
                res.body()?.let {
                    db.cachedCheckInDao().insertCheckIn(com.praxis.android.data.local.CachedCheckIn(
                        id = userId,
                        userId = userId,
                        date = "",
                        completed = it.checkedIn
                    ))
                }
                Result.success(res.body())
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            val cached = db.cachedCheckInDao().getLatestCheckIn(userId).firstOrNull()
            Result.success(cached?.let { CheckInResponse(checkedIn = it.completed, streak = 0, totalPoints = 0) })
        }
    }

    suspend fun checkIn(userId: String): Result<CheckInResponse> {
        val idempotencyKey = java.util.UUID.randomUUID().toString()
        return try {
            val res = api.checkIn(userId, idempotencyKey)
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                db.cachedCheckInDao().insertCheckIn(com.praxis.android.data.local.CachedCheckIn(
                    id = userId,
                    userId = userId,
                    date = "",
                    completed = body.checkedIn
                ))
                Result.success(body)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            queuePendingMutation("checkIn", "POST", userId, idempotencyKey)
            Result.failure(e)
        }
    }

    // Posts / Feed
    suspend fun getPosts(contextParam: String = "general", userId: String? = null): Result<List<Post>> {
        return try {
            val res = api.getPosts(contextParam, userId)
            if (res.isSuccessful) {
                val posts = res.body() ?: emptyList()
                val cached = posts.map { CachedPost(it.id, it.userId, it.userName, it.userAvatarUrl, it.title, it.content, it.context, it.createdAt) }
                db.cachedPostDao().insertPosts(cached)
                Result.success(posts)
            } else {
                Result.failure(Exception(res.message()))
            }
        } catch (e: Exception) {
            val cached = db.cachedPostDao().getPosts().firstOrNull() ?: emptyList()
            if (cached.isNotEmpty()) Result.success(cached.map { Post(it.id, it.userId, it.userName, it.userAvatarUrl, it.title, it.content, null, it.context, it.createdAt) }) else Result.failure(e)
        }
    }

    suspend fun getUserPosts(userId: String): Result<List<Post>> {
        return try {
            val res = api.getUserPosts(userId)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Notebook
    suspend fun getNotebookEntries(userId: String, limit: Int = 50, entryType: String? = null, domain: String? = null, tag: String? = null, search: String? = null): Result<NotebookEntriesResponse> {
        return try {
            val res = api.getNotebookEntries(userId, limit, entryType, domain, tag, search)
            if (res.isSuccessful) {
                val body = res.body() ?: NotebookEntriesResponse(emptyList(), 0)
                val cached = body.entries.map { com.praxis.android.data.local.CachedNotebookEntry(it.id, userId, it.content, it.entryType, it.domain, it.tags?.joinToString(","), it.createdAt) }
                db.cachedNotebookEntryDao().insertEntries(cached)
                Result.success(body)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            val cached = db.cachedNotebookEntryDao().getEntries(userId).firstOrNull() ?: emptyList()
            val entries = cached.map { NotebookEntry(it.id, it.content, it.entryType ?: "", it.domain, it.tags?.split(",") ?: emptyList(), it.createdAt) }
            Result.success(NotebookEntriesResponse(entries, entries.size))
        }
    }

    suspend fun createNotebookEntry(request: CreateEntryRequest): Result<NotebookEntry> {
        val idempotencyKey = java.util.UUID.randomUUID().toString()
        return try {
            val res = api.createNotebookEntry(request, idempotencyKey)
            if (res.isSuccessful && res.body() != null) {
                val entry = res.body()!!
                val uid = AuthManager.getUserId(context) ?: ""
                db.cachedNotebookEntryDao().insertEntries(listOf(com.praxis.android.data.local.CachedNotebookEntry(entry.id, uid, entry.content, entry.entryType, entry.domain, entry.tags?.joinToString(","), entry.createdAt)))
                Result.success(entry)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            queuePendingMutation("createNotebookEntry", "POST", com.google.gson.Gson().toJson(request), idempotencyKey)
            Result.failure(e)
        }
    }

    suspend fun getNotebookStats(userId: String): Result<NotebookStatsResponse> {
        return try {
            val res = api.getNotebookStats(userId)
            if (res.isSuccessful) Result.success(res.body() ?: NotebookStatsResponse(0, 0, emptyList())) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Goals
    suspend fun getGoals(userId: String): Result<List<GoalNode>> {
        return try {
            val res = api.getGoals(userId)
            if (res.isSuccessful) {
                val goals = res.body() ?: emptyList()
                val cached = goals.map { com.praxis.android.data.local.CachedGoal(it.id, userId, it.name, it.description, it.parentId, it.domain, it.progress, it.createdAt) }
                db.cachedGoalDao().insertGoals(cached)
                Result.success(goals)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            val cached = db.cachedGoalDao().getGoals(userId).firstOrNull() ?: emptyList()
            val goals = cached.map { GoalNode(it.id, it.name, it.description, it.progress, it.parentId, it.domain, it.createdAt, "") }
            if (goals.isNotEmpty()) Result.success(goals) else Result.failure(e)
        }
    }

    suspend fun createGoal(userId: String, name: String, description: String? = null, parentId: String? = null, domain: String? = null): Result<GoalNode> {
        return try {
            val res = api.createGoal(userId, CreateGoalRequest(name, description, parentId, domain))
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoalProgress(userId: String, nodeId: String, progress: Float, note: String? = null): Result<GoalNode> {
        return try {
            val res = api.updateGoalProgress(userId, nodeId, UpdateProgressRequest(progress, note))
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            queuePendingMutation("updateGoalProgress", "POST", com.google.gson.Gson().toJson(mapOf("userId" to userId, "nodeId" to nodeId, "progress" to progress, "note" to note)))
            Result.failure(e)
        }
    }

    // Chat
    suspend fun getMessages(user1Id: String, user2Id: String): Result<List<Message>> {
        return try {
            val res = api.getMessages(user1Id, user2Id)
            if (res.isSuccessful) {
                val messages = res.body() ?: emptyList()
                val cached = messages.map { com.praxis.android.data.local.CachedMessage(it.id, it.senderId, it.receiverId, it.roomId, it.content, it.createdAt) }
                db.cachedMessageDao().insertMessages(cached)
                Result.success(messages)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            val cached = db.cachedMessageDao().getMessages(null, user1Id, user2Id).firstOrNull() ?: emptyList()
            val messages = cached.map { Message(it.id, it.senderId, it.receiverId, it.roomId, it.content, it.createdAt, false) }
            if (messages.isNotEmpty()) Result.success(messages) else Result.failure(e)
        }
    }

    suspend fun sendMessage(senderId: String, receiverId: String, content: String): Result<Message> {
        val idempotencyKey = java.util.UUID.randomUUID().toString()
        return try {
            val res = api.sendMessage(SendMessageRequest(senderId, receiverId, null, content), idempotencyKey)
            if (res.isSuccessful && res.body() != null) {
                val msg = res.body()!!
                db.cachedMessageDao().insertMessages(listOf(com.praxis.android.data.local.CachedMessage(msg.id, msg.senderId, msg.receiverId, msg.roomId, msg.content, msg.createdAt)))
                Result.success(msg)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            queuePendingMutation("sendMessage", "POST", com.google.gson.Gson().toJson(SendMessageRequest(senderId, receiverId, null, content)), idempotencyKey)
            Result.failure(e)
        }
    }

    // Groups
    suspend fun getGroups(): Result<List<Group>> {
        return try {
            val res = api.getGroups()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGroupMessages(roomId: String): Result<List<Message>> {
        return try {
            val res = api.getGroupMessages(roomId)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Friends / Matches
    suspend fun getFriends(): Result<List<Friend>> {
        return try {
            val res = api.getFriends()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatches(userId: String): Result<List<Match>> {
        return try {
            val res = api.getMatches(userId)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Profile
    suspend fun getMyProfile(): Result<UserProfile> {
        return try {
            val res = api.getMyProfile()
            if (res.isSuccessful && res.body() != null) {
                val profile = res.body()!!.user
                db.cachedProfileDao().insertProfile(com.praxis.android.data.local.CachedProfile(
                    id = profile.id,
                    email = profile.email,
                    name = profile.name,
                    bio = profile.bio,
                    avatarUrl = profile.avatarUrl,
                    streak = profile.streak
                ))
                Result.success(profile)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            val cached = db.cachedProfileDao().getProfile(AuthManager.getUserId(context) ?: "").firstOrNull()
            if (cached != null) Result.success(UserProfile(cached.id, cached.name, cached.email, cached.avatarUrl, cached.bio, null, false, false, cached.streak, 0, 0f, 0f, false, "")) else Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val res = api.getUserProfile(userId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!.user) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<UserProfile> {
        val userId = AuthManager.getUserId(context) ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val res = api.updateProfile(userId, request)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!.user) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Leaderboard
    suspend fun getLeaderboard(limit: Int = 100): Result<List<LeaderboardEntry>> {
        return try {
            val res = api.getLeaderboard(limit)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gamification
    suspend fun getGamificationProfile(): Result<GamificationProfile> {
        return try {
            val res = api.getGamificationProfile()
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAchievements(): Result<List<Achievement>> {
        return try {
            val res = api.getAchievements()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Challenges
    suspend fun getChallenges(): Result<List<Challenge>> {
        return try {
            val res = api.getChallenges()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Events
    suspend fun getEvents(): Result<List<Event>> {
        return try {
            val res = api.getEvents()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Places
    suspend fun getPlaces(): Result<List<Place>> {
        return try {
            val res = api.getPlaces()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Search
    suspend fun search(query: String): Result<SearchResponse> {
        return try {
            val res = api.search(query)
            if (res.isSuccessful) Result.success(res.body() ?: SearchResponse(emptyList(), emptyList(), emptyList())) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queuePendingMutation(endpoint: String, method: String, bodyJson: String, idempotencyKey: String = java.util.UUID.randomUUID().toString()) {
        db.pendingMutationDao().insert(com.praxis.android.data.local.PendingMutation(endpoint = endpoint, method = method, bodyJson = bodyJson, idempotencyKey = idempotencyKey))
    }

    fun observeCachedPosts() = db.cachedPostDao().getPosts().map { cached ->
        cached.map { Post(it.id, it.userId, it.userName, it.userAvatarUrl, it.title, it.content, null, it.context, it.createdAt) }
    }

    fun observeCachedGoals(userId: String) = db.cachedGoalDao().getGoals(userId).map { cached ->
        cached.map { GoalNode(it.id, it.name, it.description, it.progress, it.parentId, it.domain, it.createdAt, "") }
    }

    fun observeCachedNotebookEntries(userId: String) = db.cachedNotebookEntryDao().getEntries(userId).map { cached ->
        cached.map { NotebookEntry(it.id, it.content, it.entryType ?: "", it.domain, it.tags?.split(",") ?: emptyList(), it.createdAt) }
    }

    fun observeCachedMessages(roomId: String?, user1: String, user2: String) = db.cachedMessageDao().getMessages(roomId, user1, user2).map { cached ->
        cached.map { Message(it.id, it.senderId, it.receiverId, it.roomId, it.content, it.createdAt, false) }
    }

    fun observeCachedProfile(userId: String) = db.cachedProfileDao().getProfile(userId).map { cached ->
        cached?.let { UserProfile(it.id, it.name, it.email, it.avatarUrl, it.bio, null, false, false, it.streak, 0, 0f, 0f, false, "") }
    }

    suspend fun processPendingMutations(): Result<Int> {
        val pending = db.pendingMutationDao().getAll().firstOrNull() ?: emptyList()
        var processed = 0
        for (mutation in pending) {
            // Rows queued before idempotency keys existed carry a blank key;
            // generate one so a replay still cannot duplicate on the next retry.
            val idempotencyKey = mutation.idempotencyKey.ifBlank { java.util.UUID.randomUUID().toString() }
            try {
                when (mutation.endpoint) {
                    "checkIn" -> api.checkIn(AuthManager.getUserId(context) ?: "", idempotencyKey)
                    "createPost" -> {
                        val body = com.google.gson.Gson().fromJson(mutation.bodyJson, com.praxis.android.data.model.CreatePostRequest::class.java)
                        api.createPost(body, idempotencyKey)
                    }
                    "sendMessage" -> {
                        val body = com.google.gson.Gson().fromJson(mutation.bodyJson, com.praxis.android.data.model.SendMessageRequest::class.java)
                        api.sendMessage(body, idempotencyKey)
                    }
                    "createNotebookEntry" -> {
                        val body = com.google.gson.Gson().fromJson(mutation.bodyJson, com.praxis.android.data.model.CreateEntryRequest::class.java) as com.praxis.android.data.model.CreateEntryRequest
                        api.createNotebookEntry(body, idempotencyKey)
                    }
                    // Quick-log widget taps: the request body is stored verbatim
                    // (type + data), replayed with its original idempotency key.
                    "logTracker" -> {
                        @Suppress("UNCHECKED_CAST")
                        val body = com.google.gson.Gson().fromJson(mutation.bodyJson, Map::class.java) as Map<String, Any>
                        api.logTracker(body)
                    }
                    else -> continue
                }
                db.pendingMutationDao().delete(mutation.mutationId)
                processed++
            } catch (e: Exception) {
                break
            }
        }
        return Result.success(processed)
    }

    /**
     * Queue a media file for later upload. Called at capture time when the
     * network is gone; [processPendingUploads] drains the queue on the next
     * sync. Returns the queued row id (useful for tests/debugging only).
     */
    suspend fun queueUpload(localPath: String, storagePath: String, mimeType: String): Long =
        db.pendingUploadDao().insert(
            com.praxis.android.data.local.PendingUpload(
                localPath = localPath,
                storagePath = storagePath,
                mimeType = mimeType
            )
        )

    /**
     * Upload everything waiting, oldest first. Stops at the first failure so
     * ordering is preserved (an entry may reference an earlier capture's URL).
     */
    suspend fun processPendingUploads(): Result<Int> {
        val pending = db.pendingUploadDao().getAll()
        var uploaded = 0
        for (upload in pending) {
            try {
                val file = java.io.File(upload.localPath)
                if (!file.exists()) {
                    // Source gone (cache cleared) — nothing can ever succeed;
                    // drop it rather than blocking the queue forever.
                    db.pendingUploadDao().delete(upload.uploadId)
                    continue
                }
                com.praxis.android.data.api.SupabaseStorage.upload(context, file, upload.storagePath, upload.mimeType)
                db.pendingUploadDao().delete(upload.uploadId)
                uploaded++
            } catch (e: Exception) {
                break
            }
        }
        return Result.success(uploaded)
    }

    /**
     * One-tap tracker log that survives being offline: stored as a pending
     * mutation and replayed by the sync worker with its own idempotency key.
     */
    suspend fun queueTrackerLog(type: String, data: Map<String, Any>): Result<Unit> {
        return try {
            val body = mapOf("type" to type, "data" to data)
            val json = com.google.gson.Gson().toJson(body)
            db.pendingMutationDao().insert(
                com.praxis.android.data.local.PendingMutation(
                    endpoint = "logTracker",
                    method = "POST",
                    bodyJson = json,
                    idempotencyKey = java.util.UUID.randomUUID().toString()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Free-note analysis (Pro). Server returns 403 for free accounts. */
    suspend fun aiScanNote(content: String): Result<Map<String, Any>> {
        return try {
            val res = api.aiScanNote(mapOf("content" to content))
            @Suppress("UNCHECKED_CAST")
            val analysis = res.body()?.get("analysis") as? Map<String, Any>
            if (res.isSuccessful && analysis != null) {
                Result.success(analysis)
            } else if (res.code() == 403) {
                Result.failure(Exception("AI note analysis is a Pro feature"))
            } else {
                Result.failure(Exception(res.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPost(postId: String): Result<Post> {
        return try {
            val res = api.getPost(postId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(request: CreatePostRequest): Result<Post> {
        val idempotencyKey = java.util.UUID.randomUUID().toString()
        return try {
            val res = api.createPost(request, idempotencyKey)
            if (res.isSuccessful && res.body() != null) {
                val post = res.body()!!
                db.cachedPostDao().insertPosts(listOf(CachedPost(post.id, post.userId, post.userName, post.userAvatarUrl, post.title, post.content, post.context, post.createdAt)))
                Result.success(post)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            queuePendingMutation("createPost", "POST", com.google.gson.Gson().toJson(request), idempotencyKey)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String): Result<Post> {
        return try {
            val res = api.toggleLike(postId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(postId: String, content: String): Result<Comment> {
        return try {
            val res = api.addComment(postId, AddCommentRequest(content))
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Notebook extended
    suspend fun updateNotebookEntry(entryId: String, request: UpdateEntryRequest): Result<NotebookEntry> {
        return try {
            val res = api.updateNotebookEntry(entryId, request)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotebookEntry(entryId: String): Result<Unit> {
        return try {
            val res = api.deleteNotebookEntry(entryId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotebookTags(userId: String): Result<List<String>> {
        return try {
            val res = api.getNotebookTags(userId)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Goals extended
    suspend fun updateGoalNode(userId: String, nodeId: String, request: UpdateGoalRequest): Result<GoalNode> {
        return try {
            val res = api.updateGoalNode(userId, nodeId, request)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoalNode(userId: String, nodeId: String): Result<Unit> {
        return try {
            val res = api.deleteGoalNode(userId, nodeId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Groups extended
    suspend fun getGroupMembers(roomId: String): Result<List<UserProfile>> {
        return try {
            val res = api.getGroupMembers(roomId)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Friends extended
    suspend fun getIncomingFriendRequests(): Result<List<FriendRequest>> {
        return try {
            val res = api.getIncomingFriendRequests()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(targetUserId: String): Result<Unit> {
        return try {
            val res = api.sendFriendRequest(targetUserId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val res = api.acceptFriendRequest(requestId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            val res = api.rejectFriendRequest(requestId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Challenges extended
    suspend fun joinChallenge(challengeId: String): Result<Unit> {
        return try {
            val res = api.joinChallenge(challengeId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveChallenge(challengeId: String): Result<Unit> {
        return try {
            val res = api.leaveChallenge(challengeId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Betting
    suspend fun getBets(): Result<List<Bet>> {
        return try {
            val res = api.getBets()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBet(request: CreateBetRequest): Result<Bet> {
        return try {
            val res = api.createBet(request)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelBet(betId: String): Result<Unit> {
        return try {
            val res = api.cancelBet(betId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cohorts
    suspend fun getCohorts(): Result<List<Cohort>> {
        return try {
            val res = api.getCohorts()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinCohort(cohortId: String): Result<Unit> {
        return try {
            val res = api.joinCohort(cohortId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Seasonal
    suspend fun getActiveSeasonalEvents(): Result<List<SeasonalEvent>> {
        return try {
            val res = api.getActiveSeasonalEvents()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Notifications
    suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val res = api.getNotifications()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationsRead(): Result<Unit> {
        return try {
            val res = api.markNotificationsRead()
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fails
    suspend fun getFails(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getFails()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFailsStats(): Result<Map<String, Any>> {
        return try {
            val res = api.getFailsStats()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Trackers
    suspend fun getMyTrackers(): Result<List<Tracker>> {
        return try {
            val res = api.getMyTrackers()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrackerCalendar(days: Int = 90): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getTrackerCalendar(days)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Oracle
    suspend fun getOracleConnections(): Result<List<OracleConnection>> {
        return try {
            val res = api.getOracleConnections()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun connectOracle(provider: String, token: String): Result<OracleConnection> {
        return try {
            val res = api.connectOracle(mapOf("provider" to provider, "token" to token))
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnectOracle(provider: String): Result<Unit> {
        return try {
            val res = api.disconnectOracle(provider)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCv(username: String): Result<Map<String, Any>> {
        return try {
            val res = api.getCv(username)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyCredential(credentialId: String): Result<Map<String, Any>> {
        return try {
            val res = api.verifyCredential(credentialId)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeOnboarding(data: Map<String, Any>): Result<Unit> {
        return try {
            val res = api.completeOnboarding(data)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicStats(userId: String): Result<Map<String, Any>> {
        return try {
            val res = api.getPublicStats(userId)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadNotificationsCount(): Result<Map<String, Any>> {
        return try {
            val res = api.getUnreadNotificationsCount()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        return try {
            val res = api.markNotificationRead(notificationId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhysisToday(): Result<Map<String, Any>> {
        return try {
            val res = api.getPhysisToday()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhysisSnapshot(): Result<Map<String, Any>> {
        return try {
            val res = api.getPhysisSnapshot()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhysisRhythms(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getPhysisRhythms()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhysisCoherenceHistory(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getPhysisCoherenceHistory()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhysisSocialTies(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getPhysisSocialTies()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhysisPlaces(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getPhysisPlaces()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhysisTrainingBalance(): Result<Map<String, Any>> {
        return try {
            val res = api.getPhysisTrainingBalance()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhysisTransitions(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getPhysisTransitions()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLattice(): Result<Map<String, Any>> {
        return try {
            val res = api.getLattice()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatticeDevices(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getLatticeDevices()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatticeJobs(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getLatticeJobs()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMarketplace(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getMarketplace()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMarketplaceItems(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getMarketplaceItems()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purchaseMarketplaceItem(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.purchaseMarketplaceItem(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMcpMessage(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.sendMcpMessage(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMcpSse(): Result<Map<String, Any>> {
        return try {
            val res = api.getMcpSse()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWiki(): Result<Map<String, Any>> {
        return try {
            val res = api.getWiki()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUberWikiGraph(): Result<Pair<List<Map<String, Any>>, List<Map<String, Any>>>> {
        return try {
            val res = api.getUberWikiGraph()
            if (res.isSuccessful && res.body() != null) {
                @Suppress("UNCHECKED_CAST")
                val nodes = (res.body()!!["nodes"] as? List<Map<String, Any>>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val edges = (res.body()!!["edges"] as? List<Map<String, Any>>) ?: emptyList()
                Result.success(nodes to edges)
            } else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUberWikiStatus(): Result<Map<String, Any>> {
        return try {
            val res = api.getUberWikiStatus()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queryUberWiki(query: String): Result<String> {
        return try {
            val res = api.queryUberWiki(query)
            if (res.isSuccessful) Result.success(res.body()?.get("result")?.toString() ?: "") else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUberWikiCells(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getUberWikiCells()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWikiIndex(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getWikiIndex()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWikiPages(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getWikiPages()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchWiki(query: String): Result<List<Map<String, Any>>> {
        return try {
            val res = api.searchWiki(query)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOntology(): Result<Map<String, Any>> {
        return try {
            val res = api.getOntology()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuraWeb(): Result<Map<String, Any>> {
        return try {
            val res = api.getAuraWeb()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Push today's Health Connect sample to the oracle. Body keys: steps/calories/weight_kg. */
    suspend fun submitHealthSample(steps: Long?, calories: Double?, weightKg: Double?): Result<Boolean> {
        return try {
            val body = buildMap<String, Double> {
                if (steps != null) put("steps", steps.toDouble())
                if (calories != null) put("calories", calories)
                if (weightKg != null) put("weight_kg", weightKg)
            }
            if (body.isEmpty()) return Result.success(false)
            val res = api.submitHealthSample(body)
            Result.success(res.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoogleCalendarStatus(): Result<Boolean> {
        return try {
            val res = api.getGoogleCalendarStatus()
            Result.success(res.isSuccessful && (res.body()?.get("linked") == true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoogleEvents(days: Int = 7): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getGoogleEvents(days)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGanttPlan(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getGanttPlan()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlanItems(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getPlanItems()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCoherenceGantt(): Result<Map<String, Any>> {
        return try {
            val res = api.getCoherenceGantt()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCoherenceTimeline(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getCoherenceTimeline()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCoherenceDashboard(): Result<Map<String, Any>> {
        return try {
            val res = api.getCoherenceDashboard()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCameraConfig(): Result<Map<String, Any>> {
        return try {
            val res = api.getCameraConfig()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWordsFrequency(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getWordsFrequency()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMySeasonalProgress(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getMySeasonalProgress()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sponsorSeasonalEvent(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.sponsorSeasonalEvent(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrackerActivityFeed(days: Int = 30, limit: Int = 80): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getTrackerActivityFeed(days, limit)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logTracker(request: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val res = api.logTracker(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrackerMonthlyByGoal(days: Int = 30): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getTrackerMonthlyByGoal(days)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrackerCustomSchemas(): Result<List<CustomSchema>> {
        return try {
            val res = api.getTrackerCustomSchemas()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTracker(request: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val res = api.createTracker(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTracker(trackerId: String, request: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val res = api.updateTracker(trackerId, request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTracker(trackerId: String): Result<Unit> {
        return try {
            val res = api.deleteTracker(trackerId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun oracleDetoxCommit(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.oracleDetoxCommit(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun oracleGithubAuth(request: Map<String, String>): Result<OracleConnection> {
        return try {
            val res = api.oracleGithubAuth(request)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun oracleStravaAuth(request: Map<String, String>): Result<OracleConnection> {
        return try {
            val res = api.oracleStravaAuth(request)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOracleUsageSample(): Result<Map<String, Any>> {
        return try {
            val res = api.getOracleUsageSample()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOracle(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.verifyOracle(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMutualCheckins(user1Id: String, user2Id: String): Result<List<CheckInResponse>> {
        return try {
            val res = api.getMutualCheckins(user1Id, user2Id)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCombos(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getCombos()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trackSocialGamification(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.trackSocialGamification(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun captureNotebookEntry(request: Map<String, Any>): Result<NotebookEntry> {
        return try {
            val res = api.captureNotebookEntry(request)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun axiomQueryNotebook(request: Map<String, String>): Result<List<NotebookEntry>> {
        return try {
            val res = api.axiomQueryNotebook(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRhymesDigest(userId: String): Result<List<NotebookEntry>> {
        return try {
            val res = api.getRhymesDigest(userId)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractPlaceFromEntry(request: Map<String, String>): Result<Place> {
        return try {
            val res = api.extractPlaceFromEntry(request)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun geocodeNotebookEntry(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.geocodeNotebookEntry(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bioenergeticsCheckin(request: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val res = api.bioenergeticsCheckin(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBioenergeticsCheckinHistory(days: Int = 30): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getBioenergeticsCheckinHistory(days)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBioenergeticsStreak(): Result<Map<String, Any>> {
        return try {
            val res = api.getBioenergeticsStreak()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBioenergeticsStatsWeekly(): Result<Map<String, Any>> {
        return try {
            val res = api.getBioenergeticsStatsWeekly()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBioenergeticsStatsMonthly(): Result<Map<String, Any>> {
        return try {
            val res = api.getBioenergeticsStatsMonthly()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBioenergetics5rHistory(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getBioenergetics5rHistory()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBioenergetics5rLog(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getBioenergetics5rLog()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBioenergeticsMetabolic(): Result<Map<String, Any>> {
        return try {
            val res = api.getBioenergeticsMetabolic()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBioenergeticsMetabolicTrend(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.getBioenergeticsMetabolicTrend()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBetsRealCheckout(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.getBetsRealCheckout(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startAuthoring(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.startAuthoring(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun suggestAuthoringTopics(request: Map<String, String>): Result<List<Map<String, Any>>> {
        return try {
            val res = api.suggestAuthoringTopics(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminAxiomCheckProviders(): Result<Map<String, Any>> {
        return try {
            val res = api.adminAxiomCheckProviders()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminAxiomForcePush(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminAxiomForcePush(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminAxiomKeyUsage(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.adminAxiomKeyUsage()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminAxiomProviders(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.adminAxiomProviders()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminAxiomStats(): Result<Map<String, Any>> {
        return try {
            val res = api.adminAxiomStats()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminAxiomTriggerScan(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminAxiomTriggerScan(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminChallenges(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.adminChallenges()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminCliExecute(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminCliExecute(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminCliStatus(): Result<Map<String, Any>> {
        return try {
            val res = api.adminCliStatus()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminCoaches(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.adminCoaches()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminConfig(): Result<Map<String, Any>> {
        return try {
            val res = api.adminConfig()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminConfigAxiomKeyStrategy(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminConfigAxiomKeyStrategy(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminConfigAxiomPrompt(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminConfigAxiomPrompt(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminConfigClearSeenMessages(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminConfigClearSeenMessages(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminConfigGlobalLoginMessage(): Result<Map<String, Any>> {
        return try {
            val res = api.adminConfigGlobalLoginMessage()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminConfigGlobalLoginMessageId(): Result<Map<String, Any>> {
        return try {
            val res = api.adminConfigGlobalLoginMessageId()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDebugErrors(limit: Int = 20): Result<List<Map<String, Any>>> {
        return try {
            val res = api.adminDebugErrors(limit)
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDebugHealth(): Result<Map<String, Any>> {
        return try {
            val res = api.adminDebugHealth()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDebugTestAuth(): Result<Map<String, Any>> {
        return try {
            val res = api.adminDebugTestAuth()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDebugTestCache(): Result<Map<String, Any>> {
        return try {
            val res = api.adminDebugTestCache()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDebugTestDb(): Result<Map<String, Any>> {
        return try {
            val res = api.adminDebugTestDb()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDebugTestEmail(): Result<Map<String, Any>> {
        return try {
            val res = api.adminDebugTestEmail()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDebugTestStorage(): Result<Map<String, Any>> {
        return try {
            val res = api.adminDebugTestStorage()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDemoUsers(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.adminDemoUsers()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminEconomy(): Result<Map<String, Any>> {
        return try {
            val res = api.adminEconomy()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminImportOsmPlaces(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminImportOsmPlaces(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminMetrics(): Result<Map<String, Any>> {
        return try {
            val res = api.adminMetrics()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminNetwork(): Result<Map<String, Any>> {
        return try {
            val res = api.adminNetwork()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminSeed(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminSeed(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminServices(): Result<Map<String, Any>> {
        return try {
            val res = api.adminServices()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminStats(): Result<Map<String, Any>> {
        return try {
            val res = api.adminStats()
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminUsers(): Result<List<Map<String, Any>>> {
        return try {
            val res = api.adminUsers()
            if (res.isSuccessful) Result.success(res.body() ?: emptyList()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminGrantPointsAll(request: Map<String, String>): Result<Map<String, Any>> {
        return try {
            val res = api.adminGrantPointsAll(request)
            if (res.isSuccessful) Result.success(res.body() ?: emptyMap()) else Result.failure(Exception(res.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

