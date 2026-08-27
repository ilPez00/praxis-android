package com.praxis.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.praxis.android.ui.MainViewModel
import com.praxis.android.ui.components.PraxisBottomNav
import com.praxis.android.ui.screens.ChatScreen
import com.praxis.android.ui.screens.CheckInScreen
import com.praxis.android.ui.screens.FeedScreen
import com.praxis.android.ui.screens.GoalsScreen
import com.praxis.android.ui.screens.GroupsScreen
import com.praxis.android.ui.screens.LeaderboardScreen
import com.praxis.android.ui.screens.MatchesScreen
import com.praxis.android.ui.screens.NotebookScreen
import com.praxis.android.ui.screens.ProfileScreen
import com.praxis.android.ui.screens.SearchScreen
import com.praxis.android.ui.screens.SettingsScreen
import com.praxis.android.ui.screens.SocialScreen
import com.praxis.android.ui.screens.EventsScreen
import com.praxis.android.ui.screens.ChallengesScreen
import com.praxis.android.ui.screens.PlacesScreen
import com.praxis.android.ui.screens.GroupChatScreen
import com.praxis.android.ui.screens.AchievementsScreen
import com.praxis.android.ui.screens.FailsScreen
import com.praxis.android.ui.screens.AnalyticsScreen
import com.praxis.android.ui.screens.OnboardingScreen
import com.praxis.android.ui.screens.ChatListScreen
import com.praxis.android.ui.screens.FriendsScreen
import com.praxis.android.ui.screens.PostThreadScreen
import com.praxis.android.ui.screens.ProfileEditScreen
import com.praxis.android.ui.screens.BettingScreen
import com.praxis.android.ui.screens.ConnectPraxisScreen
import com.praxis.android.ui.screens.OpenBetsScreen
import com.praxis.android.ui.screens.GoalSelectionScreen
import com.praxis.android.ui.screens.NotesScreen
import com.praxis.android.ui.screens.PublicNotebookScreen
import com.praxis.android.ui.screens.UpgradeScreen
import com.praxis.android.ui.screens.BoardsScreen
import com.praxis.android.ui.screens.AdditionalScreens
import com.praxis.android.ui.screens.NotificationsScreen
import com.praxis.android.ui.screens.CommunityScreens
import com.praxis.android.ui.screens.CalendarScreens
import com.praxis.android.ui.screens.HealthScreens
import com.praxis.android.ui.screens.KnowledgeScreens
import com.praxis.android.ui.screens.PlanningScreens
import com.praxis.android.ui.screens.SessionScreens
import com.praxis.android.ui.screens.ToolScreens
import com.praxis.android.ui.screens.MediaCaptureScreen
import com.praxis.android.ui.viewmodel.ChatViewModel
import com.praxis.android.ui.viewmodel.FeedViewModel
import com.praxis.android.ui.viewmodel.GroupsViewModel
import com.praxis.android.ui.viewmodel.GoalsViewModel
import com.praxis.android.ui.viewmodel.LeaderboardViewModel
import com.praxis.android.ui.viewmodel.NotebookViewModel
import com.praxis.android.ui.viewmodel.ProfileViewModel
import com.praxis.android.ui.viewmodel.SocialViewModel
import com.praxis.android.ui.viewmodel.AnalyticsViewModel
import com.praxis.android.ui.viewmodel.AchievementsViewModel
import com.praxis.android.ui.viewmodel.FailsViewModel
import com.praxis.android.ui.viewmodel.NotificationsViewModel

sealed class Screen(val route: String) {
    object Notebook : Screen("notebook?capture={capture}&share={share}")
    object Social : Screen("social")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object CheckIn : Screen("checkin")
    object Goals : Screen("goals")
    object Feed : Screen("feed")
    object Leaderboard : Screen("leaderboard")
    object Chat : Screen("chat/{userId}")
    object ChatRoom : Screen("chat/{user1Id}/{user2Id}")
    object Groups : Screen("groups")
    object GroupRoom : Screen("groups/{roomId}")
    object Matches : Screen("matches")
    object MatchDetail : Screen("matches/{matchId}")
    object Search : Screen("search")
    object Events : Screen("events")
    object Challenges : Screen("challenges")
    object Places : Screen("places")
    object GroupChat : Screen("groupchat/{roomId}")
    object Achievements : Screen("achievements")
    object Fails : Screen("fails")
    object Analytics : Screen("analytics")
    object Onboarding : Screen("onboarding")
    object ChatList : Screen("chatlist")
    object Friends : Screen("friends")
    object PostThread : Screen("posts/{postId}")
    object ProfileEdit : Screen("profile/edit")
    object Betting : Screen("betting")
    object Connect : Screen("connect")
    object OpenBets : Screen("open-bets")
    object GoalSelection : Screen("goal-selection")
    object Notes : Screen("notes")
    object PublicNotebook : Screen("notes/{userId}")
    object Upgrade : Screen("upgrade")
    object Boards : Screen("boards")
    object BoardRoom : Screen("boards/{roomId}")
    object Wiki : Screen("wiki")
    object WikiIndex : Screen("wiki-index")
    object UberWikiGraph : Screen("uberwiki-graph")
    object Ontology : Screen("ontology")
    object AuraWeb : Screen("aura-web")
    object Gantt : Screen("gantt")
    object Plan : Screen("plan")
    object Calendar : Screen("calendar")
    object Health : Screen("health")
    object GanttTrajectory : Screen("holarchy-gantt")
    object GanttCoherence : Screen("coherence/gantt")
    object CoherenceTimeline : Screen("coherence/timeline")
    object CoherenceDashboard : Screen("coherence/dashboard")
    object Lattice : Screen("lattice")
    object Marketplace : Screen("marketplace")
    object Mcp : Screen("mcp")
    object Camera : Screen("camera")
    object Admin : Screen("admin")
    object Words : Screen("words")
    object Credential : Screen("verify/{credentialId}")
    object Cv : Screen("cv/{username}")
    object Physis : Screen("physis")
    object Privacy : Screen("privacy")
    object Terms : Screen("terms")
    object Dashboard : Screen("dashboard")
    object Today : Screen("today")
    object Commitments : Screen("commitments")
    object Notifications : Screen("notifications")
}

@Composable
fun PraxisNavHost(navController: NavHostController, vm: MainViewModel, context: android.content.Context, modifier: androidx.compose.ui.Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "notebook", modifier = modifier) {
        composable(
            Screen.Notebook.route,
            arguments = listOf(
                androidx.navigation.navArgument("capture") { defaultValue = "" },
                androidx.navigation.navArgument("share") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val capture = backStackEntry.arguments?.getString("capture") ?: ""
            val share = backStackEntry.arguments?.getString("share")?.takeIf { it.isNotBlank() }?.let {
                android.net.Uri.decode(it)
            }
            val notebookVm = NotebookViewModel(vm.repo, userId = "me")
            NotebookScreen(
                viewModel = notebookVm,
                onNavigateToCheckIn = { navController.navigate("checkin") },
                onNavigateToGoals = { navController.navigate("goals") },
                onNavigateToFeed = { navController.navigate("feed") },
                prefill = share,
                openComposer = capture == "1",
                onOpenCalendar = { navController.navigate(Screen.Calendar.route) },
                onOpenHealth = { navController.navigate(Screen.Health.route) }
            )
        }
        composable(Screen.Social.route) {
            val socialVm = SocialViewModel(vm.repo, userId = "me")
            SocialScreen(viewModel = socialVm, onNavigateToProfile = { navController.navigate("profile") })
        }
        composable(Screen.Profile.route) {
            val profileVm = ProfileViewModel(vm.repo, userId = "me")
            ProfileScreen(viewModel = profileVm, onLogout = {
                vm.logout()
                navController.navigate(Screen.Notebook.route) { popUpTo(0) }
            }, onNavigateToCheckIn = { navController.navigate("checkin") })
        }
        composable(Screen.Settings.route) {
            val settingsVm = com.praxis.android.ui.settings.SettingsViewModel(context)
            SettingsScreen(viewModel = settingsVm, onBack = { navController.popBackStack() }, onOpenScreenTime = {
                navController.navigate("screen-time")
            }, onOpenContacts = {
                navController.navigate("contacts")
            })
        }
        composable("screen-time") {
            SessionScreens.ScreenTimeScreen(context = context, repo = vm.repo, onBack = { navController.popBackStack() })
        }
        composable(Screen.CheckIn.route) {
            val checkInVm = com.praxis.android.ui.viewmodel.CheckInViewModel(vm.repo, userId = "me")
            CheckInScreen(viewModel = checkInVm, onBack = { navController.popBackStack() })
        }
        composable(Screen.Goals.route) {
            val goalsVm = GoalsViewModel(vm.repo, userId = "me")
            GoalsScreen(viewModel = goalsVm)
        }
        composable(Screen.Feed.route) {
            val feedVm = FeedViewModel(vm.repo)
            FeedScreen(viewModel = feedVm, onNavigateToProfile = {})
        }
        composable(Screen.Leaderboard.route) {
            val leaderboardVm = LeaderboardViewModel(vm.repo)
            LeaderboardScreen(viewModel = leaderboardVm, onNavigateToProfile = {})
        }
        composable("chat/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: "me"
            val chatVm = ChatViewModel(vm.repo, userId = "me")
            ChatScreen(viewModel = chatVm, partnerId = userId, partnerName = "User")
        }
        composable(Screen.Groups.route) {
            val groupsVm = com.praxis.android.ui.viewmodel.GroupsViewModel(vm.repo)
            val groupsState = groupsVm.uiState.value
            val groups = (groupsState as? com.praxis.android.ui.viewmodel.GroupsUiState.Success)?.groups ?: emptyList()
            val loading = groupsState is com.praxis.android.ui.viewmodel.GroupsUiState.Loading
            GroupsScreen(groups = groups, loading = loading, onGroupClick = { navController.navigate("groupchat/$it") })
        }
        composable("groupchat/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "me"
            val chatVm = ChatViewModel(vm.repo, userId = "me")
            GroupChatScreen(roomId = roomId, roomName = "Group", viewModel = chatVm)
        }
        composable(Screen.Matches.route) {
            val socialVm = SocialViewModel(vm.repo, userId = "me")
            val matchesState = socialVm.matchesState.value
            val matches = (matchesState as? com.praxis.android.ui.viewmodel.MatchesUiState.Success)?.matches ?: emptyList()
            val loading = matchesState is com.praxis.android.ui.viewmodel.MatchesUiState.Loading
            MatchesScreen(matches = matches, loading = loading, onMatchClick = { navController.navigate("chat/$it") })
        }
        composable(Screen.Search.route) {
            val profileVm = ProfileViewModel(vm.repo, userId = "me")
            val profileState = profileVm.uiState.value
            val users = (profileState as? com.praxis.android.ui.viewmodel.ProfileUiState.Success)?.let {
                listOf(it.profile)
            } ?: emptyList()
            SearchScreen(users = users, loading = false, onUserClick = { navController.navigate("profile/$it") })
        }
        composable(Screen.Events.route) {
            val events = remember { mutableStateOf(emptyList<com.praxis.android.data.model.Event>()) }
            val loading = remember { mutableStateOf(true) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                val res = vm.repo.getEvents()
                if (res.isSuccess) {
                    events.value = res.getOrNull() ?: emptyList()
                }
                loading.value = false
            }
            EventsScreen(events = events.value, loading = loading.value, onEventClick = {})
        }
        composable(Screen.Challenges.route) {
            val challenges = remember { mutableStateOf(emptyList<com.praxis.android.data.model.Challenge>()) }
            val loading = remember { mutableStateOf(true) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                val res = vm.repo.getChallenges()
                if (res.isSuccess) {
                    challenges.value = res.getOrNull() ?: emptyList()
                }
                loading.value = false
            }
            ChallengesScreen(challenges = challenges.value, loading = loading.value, onChallengeClick = {})
        }
        composable(Screen.Places.route) {
            val places = remember { mutableStateOf(emptyList<com.praxis.android.data.model.Place>()) }
            val loading = remember { mutableStateOf(true) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                val res = vm.repo.getPlaces()
                if (res.isSuccess) {
                    places.value = res.getOrNull() ?: emptyList()
                }
                loading.value = false
            }
            PlacesScreen(places = places.value, loading = loading.value, onPlaceClick = {})
        }
        composable(Screen.Achievements.route) {
            val achievementsVm = AchievementsViewModel(vm.repo)
            val achievementsState = achievementsVm.uiState.value
            val achievements = (achievementsState as? com.praxis.android.ui.viewmodel.AchievementsUiState.Success)?.achievements ?: emptyList()
            val loading = achievementsState is com.praxis.android.ui.viewmodel.AchievementsUiState.Loading
            AchievementsScreen(achievements = achievements, loading = loading)
        }
        composable(Screen.Fails.route) {
            val failsVm = FailsViewModel(vm.repo)
            val failsState = failsVm.uiState.value
            FailsScreen(viewModel = failsVm)
        }
        composable(Screen.Analytics.route) {
            val analyticsVm = AnalyticsViewModel(vm.repo)
            AnalyticsScreen(viewModel = analyticsVm, onBack = { navController.popBackStack() })
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = { name, goal ->
                navController.navigate(Screen.Notebook.route) { popUpTo(0) }
            })
        }
        composable(Screen.ChatList.route) {
            val chatVm = ChatViewModel(vm.repo, userId = "me")
            val chatState = chatVm.uiState.value
            val messages = (chatState as? com.praxis.android.ui.viewmodel.ChatUiState.Success)?.messages ?: emptyList()
            ChatListScreen(messages = messages, loading = false, onChatClick = { userId, _ -> navController.navigate("chat/$userId") })
        }
        composable(Screen.Friends.route) {
            val chatVm = ChatViewModel(vm.repo, userId = "me")
            FriendsScreen(friends = emptyList(), requests = emptyList(), loading = false, onAccept = { /* TODO */ }, onSend = { /* TODO */ })
        }
        composable("posts/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val feedVm = FeedViewModel(vm.repo)
            val feedState = feedVm.uiState.value
            val post = (feedState as? com.praxis.android.ui.viewmodel.FeedUiState.Success)?.posts?.find { it.id == postId }
            PostThreadScreen(post = post, loading = false, comments = emptyList(), onBack = { navController.popBackStack() })
        }
        composable(Screen.ProfileEdit.route) {
            val profileVm = ProfileViewModel(vm.repo, userId = "me")
            val profileState = profileVm.uiState.value
            val profile = (profileState as? com.praxis.android.ui.viewmodel.ProfileUiState.Success)?.profile
            ProfileEditScreen(name = profile?.name ?: "", bio = profile?.bio ?: "", onSave = { name, bio ->
                navController.popBackStack()
            }, onBack = { navController.popBackStack() })
        }
        composable(Screen.Betting.route) {
            val betsVm = com.praxis.android.ui.viewmodel.SocialViewModel(vm.repo, userId = "me")
            BettingScreen(bets = emptyList(), loading = false, onPlaceBet = { _, _, _ -> })
        }
        composable(Screen.Connect.route) {
            ConnectPraxisScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.OpenBets.route) {
            OpenBetsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.GoalSelection.route) {
            GoalSelectionScreen(onGoalSelected = { goal ->
                navController.navigate(Screen.Goals.route) { popUpTo(Screen.Goals.route) { inclusive = false } }
            }, onBack = { navController.popBackStack() })
        }
        composable(Screen.Notes.route) {
            val notebookVm = NotebookViewModel(vm.repo, userId = "me")
            val notebookState = notebookVm.uiState.value
            val entries = (notebookState as? com.praxis.android.ui.viewmodel.NotebookUiState.Success)?.entries ?: emptyList()
            NotesScreen(entries = entries, loading = false, onEntryClick = { navController.navigate("posts/$it") })
        }
        composable("notes/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: "me"
            PublicNotebookScreen(userId = userId, entries = emptyList(), loading = false)
        }
        composable(Screen.Upgrade.route) {
            UpgradeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Boards.route) {
            val boards = remember { mutableStateOf(emptyList<com.praxis.android.data.model.Board>()) }
            val loading = remember { mutableStateOf(true) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                val res = vm.repo.getGroups()
                if (res.isSuccess) {
                    boards.value = res.getOrNull()?.map { g -> com.praxis.android.data.model.Board(g.id, g.name, g.description, g.id, 3, "") } ?: emptyList()
                }
                loading.value = false
            }
            BoardsScreen(boards = boards.value, loading = loading.value, onBoardClick = { navController.navigate("groupchat/$it") })
        }
        composable(Screen.Wiki.route) { KnowledgeScreens.WikiBrowserScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.WikiIndex.route) { KnowledgeScreens.WikiIndexScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.UberWikiGraph.route) { KnowledgeScreens.UberWikiGraphScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable("uberwiki") { KnowledgeScreens.UberWikiHubScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable("uberwiki-cells") { KnowledgeScreens.UberWikiCellsScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Ontology.route) { KnowledgeScreens.OntologyScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.AuraWeb.route) { KnowledgeScreens.AuraWebScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Gantt.route) { PlanningScreens.GanttScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Plan.route) { PlanningScreens.PlanScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable("planner") { PlanningScreens.PlannerScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Calendar.route) { CalendarScreens.CalendarScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Health.route) { HealthScreens.HealthConnectScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable("trackers") { CommunityScreens.TrackersScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable("seasonal") { CommunityScreens.SeasonalEventsScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable("cohorts") { CommunityScreens.CohortsScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable("auth-callback?data={data}") { backStackEntry ->
            val data = backStackEntry.arguments?.getString("data")
            SessionScreens.AuthCallbackScreen(data = data, onDone = { ok ->
                navController.navigate("notebook") { popUpTo(0) }
            })
        }
        composable(Screen.GanttTrajectory.route) { PlanningScreens.GanttTrajectoryScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.GanttCoherence.route) { PlanningScreens.GanttCoherenceScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.CoherenceTimeline.route) { PlanningScreens.CoherenceTimelineScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.CoherenceDashboard.route) { PlanningScreens.CoherenceDashboardScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Lattice.route) { ToolScreens.LatticeScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Marketplace.route) { ToolScreens.MarketplaceScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Mcp.route) { ToolScreens.McpScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Camera.route) {
            com.praxis.android.ui.screens.MediaCaptureScreen(repo = vm.repo, mode = "photo", onBack = { navController.popBackStack() })
        }
        composable(
            "capture/{mode}?auto={auto}",
            arguments = listOf(androidx.navigation.navArgument("mode") { defaultValue = "photo" })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "photo"
            val auto = backStackEntry.arguments?.getString("auto") == "1"
            com.praxis.android.ui.screens.MediaCaptureScreen(repo = vm.repo, mode = mode, autoStart = auto, onBack = { navController.popBackStack() })
        }
        composable("contacts") {
            com.praxis.android.ui.screens.ContactsScreens.ContactsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Admin.route) { ToolScreens.AdminScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Words.route) { ToolScreens.WordsScreen(repo = vm.repo, onBack = { navController.popBackStack() }) }
        composable(Screen.Dashboard.route) {
            navController.navigate(Screen.Notebook.route) { popUpTo(Screen.Notebook.route) { inclusive = false } }
        }
        composable(Screen.Today.route) {
            navController.navigate(Screen.Notebook.route) { popUpTo(Screen.Notebook.route) { inclusive = false } }
        }
        composable(Screen.Commitments.route) {
            val betsVm = com.praxis.android.ui.viewmodel.SocialViewModel(vm.repo, userId = "me")
            BettingScreen(bets = emptyList(), loading = false, onPlaceBet = { _, _, _ -> })
        }
        composable("goals/{goalId}") { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
            val goalsVm = GoalsViewModel(vm.repo, userId = "me")
            GoalsScreen(viewModel = goalsVm)
        }
        composable("matches/{matchId}") { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val socialVm = SocialViewModel(vm.repo, userId = "me")
            val matchesState = socialVm.matchesState.value
            val matches = (matchesState as? com.praxis.android.ui.viewmodel.MatchesUiState.Success)?.matches ?: emptyList()
            val loading = matchesState is com.praxis.android.ui.viewmodel.MatchesUiState.Loading
            MatchesScreen(matches = matches, loading = loading, onMatchClick = { navController.navigate("chat/$it") })
        }
        composable("chat/{user1Id}/{user2Id}") { backStackEntry ->
            val user1Id = backStackEntry.arguments?.getString("user1Id") ?: "me"
            val user2Id = backStackEntry.arguments?.getString("user2Id") ?: "me"
            val chatVm = ChatViewModel(vm.repo, userId = user1Id)
            ChatScreen(viewModel = chatVm, partnerId = user2Id, partnerName = "User")
        }
        composable("groups/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "me"
            val chatVm = ChatViewModel(vm.repo, userId = "me")
            GroupChatScreen(roomId = roomId, roomName = "Group", viewModel = chatVm)
        }
        composable("boards/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "me"
            val chatVm = ChatViewModel(vm.repo, userId = "me")
            GroupChatScreen(roomId = roomId, roomName = "Board", viewModel = chatVm)
        }
        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: "me"
            val profileVm = ProfileViewModel(vm.repo, userId = userId)
            ProfileScreen(viewModel = profileVm, onLogout = {
                vm.logout()
                navController.navigate(Screen.Notebook.route) { popUpTo(0) }
            }, onNavigateToCheckIn = { navController.navigate("checkin") })
        }
        composable(Screen.Credential.route) { backStackEntry ->
            val credentialId = backStackEntry.arguments?.getString("credentialId") ?: ""
            AdditionalScreens.CredentialScreen(repo = vm.repo, credentialId = credentialId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Cv.route) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            AdditionalScreens.CvScreen(repo = vm.repo, username = username, onBack = { navController.popBackStack() })
        }
        composable(Screen.Physis.route) {
            AdditionalScreens.PhysisScreen(repo = vm.repo, onBack = { navController.popBackStack() })
        }
        composable(Screen.Privacy.route) {
            AdditionalScreens.PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Terms.route) {
            AdditionalScreens.TermsOfServiceScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Notifications.route) {
            val notificationsVm = NotificationsViewModel(vm.repo)
            val notificationsState = notificationsVm.uiState.value
            val notifications = (notificationsState as? com.praxis.android.ui.viewmodel.NotificationsUiState.Success)?.notifications ?: emptyList()
            val loading = notificationsState is com.praxis.android.ui.viewmodel.NotificationsUiState.Loading
            NotificationsScreen(notifications = notifications, loading = loading, onNotificationClick = { navController.popBackStack() })
        }
    }
}
