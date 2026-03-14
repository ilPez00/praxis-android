package com.praxis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.praxis.app.data.model.Domain
import com.praxis.app.ui.screens.*
import com.praxis.app.ui.theme.PraxisTheme
import com.praxis.app.ui.viewmodel.*

class MainActivity : ComponentActivity() {

    private val praxisViewModel: PraxisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PraxisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PraxisApp(praxisViewModel)
                }
            }
        }
    }
}

@Composable
fun PraxisApp(viewModel: PraxisViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val bets by viewModel.bets.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is PraxisUiState.Onboarding -> OnboardingScreen(
            onGoogleSignIn = { userId, token, displayName ->
                viewModel.signInWithSupabaseUser(userId, token, displayName)
            },
            onManualContinue = { name, age, bio ->
                viewModel.createUser(name, age, bio)
            },
        )

        is PraxisUiState.GoalSelection -> GoalSelectionScreen(
            goalTemplates = viewModel.goalTemplates,
            onComplete = { goals -> viewModel.completeGoalSelection(goals) },
        )

        is PraxisUiState.Main -> Scaffold(
            bottomBar = {
                NavigationBar {
                    MainTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = state.tab == tab,
                            onClick = { viewModel.navigateTo(tab) },
                            icon = { Text(tab.emoji) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (state.tab) {
                    MainTab.DASHBOARD -> DashboardScreen(
                        user = currentUser,
                        matches = matches,
                        achievements = achievements,
                        onNavigate = { viewModel.navigateTo(it) },
                        onUpvoteAchievement = { viewModel.upvoteAchievement(it) },
                        onNavigateToUpgrade = { viewModel.navigateToUpgrade() },
                        onNavigateToAnalytics = { viewModel.navigateToAnalytics() },
                        onNavigateToIdentityVerification = { viewModel.navigateToIdentityVerification() },
                    )
                    MainTab.GOALS -> HomeScreenWithTree(
                        goals = currentUser?.goalTree ?: emptyList(),
                        onGoalClick = {},
                        onAddProgress = { goal ->
                            viewModel.updateGoalProgress(goal.id, (goal.progress + 5).coerceAtMost(100))
                        },
                    )
                    MainTab.MATCHES -> MatchesTab(
                        matches = matches,
                        onFindMatches = { viewModel.findMatches() },
                        onMatchClick = { match -> viewModel.openChat(match.userId) },
                    )
                    MainTab.GROUPS -> GroupsScreen(
                        groups = groups,
                        onJoinGroup = { viewModel.joinGroup(it) },
                        onEnterGroup = { id, name, domain -> viewModel.openGroupChat(id, name, domain) },
                        onCreateGroup = { name, desc, domain -> viewModel.createGroup(name, desc, domain) },
                    )
                    MainTab.PROFILE -> currentUser?.let { ProfileTab(user = it) }
                }
            }
        }

        is PraxisUiState.Chat -> {
            val match = viewModel.getMatch(state.matchId)
            ChatScreen(
                matchName = match?.userName ?: "Match",
                sharedGoalName = match?.sharedGoals?.firstOrNull()?.name ?: "your goal",
                onBack = { viewModel.navigateToMain() },
                onCompleteCollaboration = { viewModel.navigateToMain() },
            )
        }

        is PraxisUiState.GroupChat -> GroupChatRoomScreen(
            groupId = state.groupId,
            groupName = state.groupName,
            domain = state.domain,
            currentUserName = currentUser?.name ?: "You",
            onBack = { viewModel.navigateToMain() },
        )

        is PraxisUiState.Analytics -> AnalyticsScreen(
            user = currentUser,
            isPremium = currentUser?.isPremium ?: false,
            onBack = { viewModel.navigateToMain() },
            onNavigateToUpgrade = { viewModel.navigateToUpgrade() },
        )

        is PraxisUiState.Upgrade -> UpgradeScreen(
            onBack = { viewModel.navigateToMain() },
            onUpgradeSuccess = { viewModel.activatePremium() },
        )

        is PraxisUiState.IdentityVerification -> IdentityVerificationScreen(
            onBack = { viewModel.navigateToMain() },
            onVerified = { viewModel.markIdentityVerified() },
        )
    }
}
