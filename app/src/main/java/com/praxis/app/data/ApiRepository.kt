package com.praxis.app.data

import com.praxis.app.data.model.*
import com.praxis.app.data.remote.ApiGoalNode
import com.praxis.app.data.remote.CompleteOnboardingRequest
import com.praxis.app.data.remote.GoalTreeRequest
import com.praxis.app.data.remote.RetrofitClient
import com.praxis.app.data.remote.SendMessageRequest
import com.praxis.app.data.remote.UpdateProgressRequest

class ApiRepository {
    private val api = RetrofitClient.api

    suspend fun getProfile(userId: String): Result<User> = runCatching {
        val resp = api.getProfile(userId)
        if (!resp.isSuccessful) error("Profile fetch failed: ${resp.code()}")
        val p = resp.body() ?: error("Empty profile response")
        User(
            id = p.id,
            name = p.name,
            bio = p.bio,
            age = p.age,
            isPremium = p.isPremium,
            isVerified = p.isVerified,
            currentStreak = p.currentStreak,
            praxisPoints = p.praxisPoints,
            goalTree = mutableListOf(),
        )
    }

    suspend fun getGoalTree(userId: String): Result<List<GoalNode>> = runCatching {
        val resp = api.getGoalTree(userId)
        if (!resp.isSuccessful) return@runCatching emptyList()
        val tree = resp.body() ?: return@runCatching emptyList()
        // Prefer rootNodes (already nested) over flat nodes list
        val source = if (tree.rootNodes.isNotEmpty()) tree.rootNodes else tree.nodes
        source.map { it.toGoalNode() }
    }

    suspend fun getMatches(userId: String): Result<List<Match>> = runCatching {
        val resp = api.getMatches(userId)
        if (!resp.isSuccessful) return@runCatching emptyList()
        resp.body()?.map { m ->
            Match(
                userId = m.userId,
                userName = m.userName,
                compatibilityScore = m.compatibilityScore.toDouble(),
                sharedGoals = m.sharedGoals.map { it.toGoalNode() },
            )
        } ?: emptyList()
    }

    suspend fun getAchievements(): Result<List<Achievement>> = runCatching {
        val resp = api.getAchievements()
        if (!resp.isSuccessful) return@runCatching emptyList()
        resp.body()?.map { a ->
            Achievement(
                id = a.id,
                userId = "",
                userName = a.userName,
                goalNodeId = "",
                title = a.title,
                description = a.description,
                domain = Domain.fromString(a.domain),
                totalUpvotes = a.totalUpvotes,
                comments = emptyList(),
            )
        } ?: emptyList()
    }

    suspend fun completeOnboarding(userId: String): Result<Unit> = runCatching {
        api.completeOnboarding(CompleteOnboardingRequest(userId))
    }

    suspend fun sendMessage(senderId: String, receiverId: String, content: String): Result<Unit> = runCatching {
        api.sendMessage(SendMessageRequest(senderId, receiverId, content))
    }

    suspend fun updateGoalTree(userId: String, nodes: List<GoalNode>): Result<Unit> = runCatching {
        val apiNodes = mutableListOf<ApiGoalNode>()
        fun flatten(node: GoalNode, parentId: String? = null) {
            apiNodes.add(
                ApiGoalNode(
                    id = node.id,
                    name = node.name,
                    domain = node.domain.name,
                    progress = node.progress,
                    weight = node.weight.toFloat(),
                    customDetails = node.details,
                    parentId = parentId,
                    children = emptyList(),
                )
            )
            node.subGoals.forEach { flatten(it, node.id) }
        }
        nodes.forEach { flatten(it) }
        val resp = api.createOrUpdateGoalTree(GoalTreeRequest(userId = userId, nodes = apiNodes))
        if (!resp.isSuccessful) error("Goal tree update failed: ${resp.code()}")
    }

    suspend fun updateNodeProgress(userId: String, nodeId: String, progress: Int): Result<Unit> = runCatching {
        val resp = api.updateNodeProgress(userId, nodeId, UpdateProgressRequest(progress))
        if (!resp.isSuccessful) error("Progress update failed: ${resp.code()}")
    }

    // Requires a valid Bearer token in AuthTokenHolder
    suspend fun checkIn(): Result<Boolean> = runCatching {
        val resp = api.checkIn()
        if (!resp.isSuccessful) error("Check-in failed: ${resp.code()}")
        true
    }

    suspend fun getTodayCheckin(): Result<Boolean> = runCatching {
        val resp = api.getTodayCheckin()
        if (!resp.isSuccessful) return@runCatching false
        resp.body()?.checkedIn ?: false
    }
}

// Extension to map ApiGoalNode → GoalNode (recursive)
private fun ApiGoalNode.toGoalNode(): GoalNode = GoalNode(
    id = id,
    name = name,
    domain = Domain.fromString(domain),
    progress = progress,
    weight = weight.toDouble(),
    details = customDetails ?: "",
    subGoals = children.map { it.toGoalNode() }.toMutableList(),
)
