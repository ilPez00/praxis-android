package com.praxis.app.data.remote

import com.google.gson.annotations.SerializedName

data class ApiProfile(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val age: Int = 0,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_premium") val isPremium: Boolean = false,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("current_streak") val currentStreak: Int = 0,
    @SerializedName("praxis_points") val praxisPoints: Int = 100,
    @SerializedName("onboarding_completed") val onboardingCompleted: Boolean = false,
)

data class ApiGoalNode(
    val id: String = "",
    val name: String = "",
    val domain: String = "",
    val progress: Int = 0,
    val weight: Float = 1f,
    @SerializedName("custom_details") val customDetails: String? = null,
    @SerializedName("parent_id") val parentId: String? = null,
    val children: List<ApiGoalNode> = emptyList(),
)

data class ApiGoalTree(
    val id: String = "",
    @SerializedName("userId") val userId: String = "",
    val nodes: List<ApiGoalNode> = emptyList(),
    @SerializedName("rootNodes") val rootNodes: List<ApiGoalNode> = emptyList(),
)

data class ApiMatch(
    @SerializedName("userId") val userId: String = "",
    @SerializedName("userName") val userName: String = "",
    @SerializedName("compatibilityScore") val compatibilityScore: Float = 0f,
    @SerializedName("sharedGoals") val sharedGoals: List<ApiGoalNode> = emptyList(),
)

data class ApiAchievement(
    val id: String = "",
    @SerializedName("user_name") val userName: String = "",
    val title: String = "",
    val description: String = "",
    val domain: String = "",
    @SerializedName("total_upvotes") val totalUpvotes: Int = 0,
)

data class ApiMessage(
    val id: String = "",
    @SerializedName("sender_id") val senderId: String = "",
    @SerializedName("receiver_id") val receiverId: String = "",
    val content: String = "",
    val timestamp: String = "",
)

data class ApiRoom(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val domain: String? = null,
)

data class SendMessageRequest(
    @SerializedName("senderId") val senderId: String,
    @SerializedName("receiverId") val receiverId: String,
    val content: String,
)

data class CompleteOnboardingRequest(
    @SerializedName("userId") val userId: String,
)

data class UpdateProgressRequest(
    val progress: Int,
)

data class UpdateProfileRequest(
    val name: String? = null,
    val bio: String? = null,
    val age: Int? = null,
)

data class GoalTreeRequest(
    @SerializedName("userId") val userId: String,
    val nodes: List<ApiGoalNode>,
)

data class ApiCheckin(
    val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("checked_in") val checkedIn: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
)
