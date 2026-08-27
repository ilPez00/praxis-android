package com.praxis.android

import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import com.praxis.android.ui.MainViewModel
import com.praxis.android.ui.MainViewModelFactory
import com.praxis.android.ui.navigation.PraxisNavHost
import com.praxis.android.ui.components.PraxisBottomNav
import com.praxis.android.ui.theme.PraxisTheme

class MainActivity : ComponentActivity() {

    /** Route to navigate to once the NavHost is up; consumed by the UI. */
    private val pendingRoute: MutableState<String?> = mutableStateOf(null)

    /** web = WebView shell mirroring praxisweb.xyz; native = Compose screens. */
    private val uiMode: MutableState<String> =
        mutableStateOf(com.praxis.android.ui.web.UiMode.WEB)


    override fun onCreate(savedInstanceState: Bundle?) {
        // Installs the compat splash and swaps in postSplashScreenTheme
        // (AppTheme.NoActionBar) after the first frame — without it the
        // activity stays in the light launch theme for the whole session.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        consumeIntent(intent)
        uiMode.value = com.praxis.android.ui.web.UiMode.read(this)
        // Dark status bar — the launch theme leaves a white strip above the
        // WebView shell, and the SPA is dark anyway. #080808 = web --color-bg.
        window.statusBarColor = android.graphics.Color.rgb(8, 8, 8)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
        setContent {
            PraxisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (uiMode.value == com.praxis.android.ui.web.UiMode.WEB) {
                        com.praxis.android.ui.web.WebScreen(
                            pendingRoute = pendingRoute.value,
                            onRouteConsumed = { pendingRoute.value = null },
                            onUseNativeUi = {
                                uiMode.value = com.praxis.android.ui.web.UiMode.NATIVE
                                com.praxis.android.ui.web.UiMode.write(
                                    this, com.praxis.android.ui.web.UiMode.NATIVE,
                                )
                            },
                        )
                    } else {
                        PraxisRoot(pendingRoute = pendingRoute)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == com.praxis.android.ui.web.WebFileChooser.RESULT_CODE) {
            com.praxis.android.ui.web.WebFileChooser.handle(resultCode, data)
        }
    }

    /**
     * Every entry path — launcher, widget taps, web links, OAuth returns,
     * share-sheet sends — lands here and becomes at most one pending route.
     */
    private fun consumeIntent(intent: Intent?) {
        when {
            intent?.action == Intent.ACTION_VIEW -> pendingRoute.value = routeFromUri(intent)
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                pendingRoute.value = "notebook?share=${android.net.Uri.encode(text)}"
            }
        }
    }

    private fun routeFromUri(intent: Intent): String {
        val data = intent.data ?: return "notebook"
        val segments = data.pathSegments ?: emptyList()
        val path = segments.joinToString("/")

        // Widget routes: praxis://checkin, praxis://notebook?capture=1,
        // praxis://planner, praxis://trackers, praxis://capture-audio,
        // praxis://contacts.
        if (data.scheme == "praxis") {
            val host = data.host ?: ""
            if (host == "notebook" && data.getQueryParameter("capture") == "1") {
                return "notebook?capture=1"
            }
            return when (host) {
                "checkin" -> "checkin"
                "planner" -> "planner"
                "calendar" -> "calendar"
                "health" -> "health"
                "trackers" -> "trackers"
                "capture-audio" -> "capture/audio?auto=1"
                "capture-video" -> "capture/video?auto=1"
                "capture-photo" -> "capture/photo?auto=1"
                "contacts" -> "contacts"
                else -> "notebook"
            }
        }
        // OAuth callback: praxis-auth://callback or https://praxisweb.xyz/auth/callback.
        if (data.scheme == "praxis-auth") {
            val fragmentOrUrl = data.fragment ?: data.toString()
            return "auth-callback?data=${android.net.Uri.encode(fragmentOrUrl)}"
        }

        return when {
            path.startsWith("auth/callback") ->
                "auth-callback?data=${android.net.Uri.encode(data.toString())}"
            path.startsWith("chat/") -> "chat/${data.lastPathSegment ?: "me"}"
            path.startsWith("groups/") -> "groups/${data.lastPathSegment ?: ""}"
            path.startsWith("posts/") -> "posts/${data.lastPathSegment ?: ""}"
            path.startsWith("notes/") -> "notes/${data.lastPathSegment ?: ""}"
            path.startsWith("profile/") -> "profile/${data.lastPathSegment ?: "me"}"
            path.startsWith("goals/") -> "goals/${data.lastPathSegment ?: ""}"
            path == "notifications" -> "notifications"
            path.startsWith("verify/") || path.startsWith("cv/") || path.startsWith("planner")
                || path == "calendar" || path == "health"
                || path.startsWith("uberwiki") || path.startsWith("seasonal") || path.startsWith("cohorts") -> path
            else -> "notebook"
        }
    }
}

@Composable
private fun PraxisRoot(pendingRoute: MutableState<String?>) {
    val context = LocalContext.current
    val app = context.applicationContext as PraxisApp
    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(app.repository, context))
    val navController = androidx.navigation.compose.rememberNavController()

    LaunchedEffect(pendingRoute.value) {
        pendingRoute.value?.let { route ->
            navController.navigate(route)
            pendingRoute.value = null
        }
    }

    Scaffold(
        bottomBar = {
            // currentBackStackEntryAsState is observable; currentDestination
            // is not, and reading it here left the nav with no active tab.
            val backStackEntry by navController.currentBackStackEntryAsState()
            PraxisBottomNav(
                navController = navController,
                currentRoute = backStackEntry?.destination?.route,
            )
        }
    ) { padding ->
        PraxisNavHost(
            navController = navController,
            vm = vm,
            context = context,
            modifier = Modifier.padding(padding).swipeBack { navController.popBackStack() }
        )
    }
}

@Composable
fun Modifier.swipeBack(onSwipe: () -> Unit): Modifier {
    return this.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                if (dragAmount.x < -50) {
                    onSwipe()
                    change.consume()
                }
            }
        )
    }
}
