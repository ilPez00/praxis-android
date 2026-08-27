package com.praxis.android.data.model

data class User(
    val id: String,
    val email: String?,
    val name: String?,
    val avatarUrl: String?,
    val streak: Int = 0,
    val praxisPoints: Int = 0,
    val currentStreak: Int = 0
)

data class Post(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val title: String?,
    val content: String,
    val mediaUrl: String? = null,
    val context: String,
    val createdAt: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val userLiked: Boolean = false
)

data class CheckInResponse(
    val checkedIn: Boolean,
    val streak: Int,
    val totalPoints: Int,
    val pointsAwarded: Int = 0,
    val streakBonus: Int = 0,
    val shieldConsumed: Boolean = false,
    val xpAwarded: Int = 0,
    val leveledUp: Boolean = false,
    val newLevel: Int = 1,
    val mysteryReward: MysteryReward? = null,
    val seasonalEventCompleted: SeasonalEventUpdate? = null,
    val shieldEarned: Boolean = false,
    val isRecovery: Boolean = false
)

data class MysteryReward(val tier: String, val amount: Int, val emoji: String)
data class SeasonalEventUpdate(val eventSlug: String, val eventName: String)

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val user: User?
)

// Notebook
data class NotebookEntry(val id: String, val content: String, val entryType: String, val domain: String?, val tags: List<String>, val createdAt: String, val pinned: Boolean = false, val attachments: List<Map<String, Any?>> = emptyList())
data class NotebookEntriesResponse(val entries: List<NotebookEntry>, val total: Int)
data class CreateEntryRequest(val content: String, val entryType: String? = null, val domain: String? = null, val tags: List<String>? = null, val lat: Double? = null, val lng: Double? = null, val attachments: List<Map<String, Any?>>? = null)
data class UpdateEntryRequest(val content: String? = null, val entryType: String? = null, val domain: String? = null, val tags: List<String>? = null, val pinned: Boolean? = null)
data class NotebookStatsResponse(val totalEntries: Int, val streakDays: Int, val recentTags: List<String>)

// Goals
data class GoalNode(val id: String, val name: String, val description: String?, val progress: Float, val parentId: String?, val domain: String?, val createdAt: String, val updatedAt: String)
data class CreateGoalRequest(val name: String, val description: String? = null, val parentId: String? = null, val domain: String? = null)
data class UpdateGoalRequest(val name: String? = null, val description: String? = null, val progress: Float? = null, val domain: String? = null)
data class UpdateProgressRequest(val progress: Float, val note: String? = null)

// Chat
data class Message(val id: String, val senderId: String, val receiverId: String?, val roomId: String?, val content: String, val createdAt: String, val read: Boolean = false)
data class SendMessageRequest(val senderId: String, val receiverId: String? = null, val roomId: String? = null, val content: String, val goalNodeId: String? = null)

// Groups
data class Group(val id: String, val name: String, val description: String?, val memberCount: Int, val isJoined: Boolean = false)

// Friends / Matches
data class Friend(val id: String, val name: String, val avatarUrl: String?, val streak: Int, val lastActivity: String?)
data class Match(val id: String, val score: Float, val goalScore: Float, val textAffinity: Float, val ontologySimilarity: Float, val name: String, val avatarUrl: String?, val bio: String?, val currentStreak: Int, val lastCheckinDate: String?, val latitude: Double?, val longitude: Double?, val domains: List<String>, val sharedGoals: List<String>)

// Profile
data class UserProfile(val id: String, val name: String, val email: String, val avatarUrl: String?, val bio: String?, val city: String?, val isPremium: Boolean, val isAdmin: Boolean, val streak: Int, val praxisPoints: Int, val honorScore: Float, val reliabilityScore: Float, val onboardingCompleted: Boolean, val createdAt: String)
data class ProfileResponse(val user: UserProfile)
data class UpdateProfileRequest(val name: String? = null, val bio: String? = null, val city: String? = null, val avatarUrl: String? = null)
data class DeleteAccountRequest(val password: String)

// Leaderboard
data class LeaderboardEntry(val rank: Int, val userId: String, val name: String, val avatarUrl: String?, val streak: Int, val points: Int, val reliability: Float, val league: String, val domains: List<String>)

// Gamification
data class GamificationProfile(val level: Int, val xp: Int, val xpToNext: Int, val league: String, val streak: Int, val praxisPoints: Int, val titles: List<String>)
data class Achievement(val id: String, val title: String, val description: String, val icon: String, val unlockedAt: String?)

// Challenges
data class Challenge(val id: String, val title: String, val description: String, val startDate: String, val endDate: String, val joined: Boolean = false)

// Events
data class Event(val id: String, val title: String, val description: String, val startDate: String, val endDate: String, val location: String?, val attendees: Int, val isRsvped: Boolean = false)

// Places
data class Place(val id: String, val name: String, val description: String, val lat: Double, val lng: Double, val category: String)

// Search
data class SearchResponse(val users: List<UserProfile>, val posts: List<Post>, val goals: List<GoalNode>)

// Comments
data class Comment(val id: String, val userId: String, val userName: String, val content: String, val createdAt: String)

// Post requests
data class CreatePostRequest(val content: String, val title: String? = null, val context: String = "general")
data class AddCommentRequest(val content: String)

// Betting
data class Bet(val id: String, val userId: String, val goalNodeId: String?, val stake: Int, val payout: Int, val deadline: String, val status: String, val createdAt: String)
data class CreateBetRequest(val goalNodeId: String? = null, val stake: Int, val days: Int)

// Duel
data class Duel(val id: String, val challengerId: String, val challengedId: String, val stake: Int, val status: String, val createdAt: String)

// Groups / Boards
data class Board(val id: String, val name: String, val description: String?, val roomId: String, val columns: Int = 3, val createdAt: String)

// Cohorts
data class Cohort(val id: String, val name: String, val description: String?, val startDate: String, val endDate: String, val memberCount: Int, val joined: Boolean = false)

// Seasonal
data class SeasonalEvent(val id: String, val name: String, val description: String?, val startDate: String, val endDate: String, val reward: String?, val progress: Int = 0, val joined: Boolean = false)

// Friend request
data class FriendRequest(val id: String, val fromUserId: String, val toUserId: String, val status: String, val createdAt: String)

// Upgrade / Plans
data class UpgradePlan(val id: String, val name: String, val price: String, val features: List<String>, val period: String)

// Notifications
data class Notification(val id: String, val title: String, val body: String, val type: String, val read: Boolean, val createdAt: String)

// Trackers
data class Tracker(val id: String, val type: String, val label: String, val unit: String, val value: String, val goalNodeId: String?, val createdAt: String)
data class CustomSchema(val id: String, val name: String, val fields: List<String>, val unit: String)

// Oracle
data class OracleConnection(val provider: String, val connected: Boolean, val verified: Boolean, val label: String?)

// Fails
data class FailItem(val id: String, val type: String, val details: String, val retriedAt: String?, val createdAt: String)

// Credentials
data class Credential(val id: String, val requesterName: String?, val requesterUsername: String?, val requesterId: String, val verifierName: String?, val goalName: String, val goalNodeId: String, val evidenceUrl: String?, val verifierReliability: Float?, val trustWeight: Float?, val lowTrust: Boolean?, val source: String?, val oracleProvider: String?, val verifiedAt: String, val keyId: String, val alg: String, val payload: Map<String, Any>, val signature: String)

// CV
data class CvData(val username: String, val sections: List<Map<String, Any>>)

// Physis
data class PhysisSnapshot(val date: String, val rhythmScore: Float, val coherence: Float, val trainingBalance: Float)

// Lattice
data class LatticeDevice(val id: String, val name: String, val type: String, val status: String, val lastSync: String)
data class LatticeJob(val id: String, val deviceId: String, val type: String, val status: String, val progress: Float, val createdAt: String)

// Marketplace
data class MarketplaceItem(val id: String, val name: String, val description: String, val price: Int, val imageUrl: String?, val category: String)

// MCP
data class McpMessage(val id: String, val role: String, val content: String, val createdAt: String)

// Wiki
data class WikiPage(val id: String, val title: String, val content: String, val path: String, val updatedAt: String)

// Gantt
data class GanttPlanItem(val id: String, val title: String, val startDate: String, val endDate: String, val progress: Float, val assignee: String?)

// Coherence
data class CoherenceGantt(val projectId: String, val phases: List<Map<String, Any>>)
data class CoherenceTimeline(val events: List<Map<String, Any>>)
data class CoherenceDashboard(val score: Float, val trends: List<Float>, val alerts: List<String>)

// Words
data class WordFrequency(val word: String, val count: Int, val sentiment: Float?)
