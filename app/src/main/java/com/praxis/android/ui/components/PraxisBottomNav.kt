package com.praxis.android.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import app.praxisweb.xyz.R
import com.praxis.android.ui.theme.LabelMonoSmall

/**
 * Bottom nav parity with client/src/layout/BottomNav.tsx: 56dp row on
 * surface/95 behind a 1px #222222 top border, stroke-style outline icons,
 * bold uppercase mono micro-labels, and an amber top-edge indicator
 * (h-0.5 w-8 rounded-full) that springs between tabs.
 */

private data class TabSpec(
    val root: String,
    val labelRes: Int,
    val icon: ImageVector,
    val matchPrefixes: List<String>,
)

private val TABS = listOf(
    TabSpec(
        root = "notebook",
        labelRes = R.string.notebook,
        icon = Icons.Outlined.MenuBook,
        // Web matchPaths: /notebook /goals /notes /goal-selection /planner
        matchPrefixes = listOf("notebook", "goals", "notes", "goal-selection", "planner"),
    ),
    TabSpec(
        root = "social",
        labelRes = R.string.social,
        icon = Icons.Outlined.Groups,
        // Web matchPaths: /discover /posts /friends /matches /chat /groups
        // /boards /challenges /cohorts /commitments /open-bets
        matchPrefixes = listOf(
            "social", "feed", "posts", "friends", "matches", "search",
            "chat", "groups", "groupchat", "boards", "challenges",
            "cohorts", "open-bets", "events",
        ),
    ),
    TabSpec(
        root = "profile",
        labelRes = R.string.me,
        icon = Icons.Outlined.Person,
        // Web matchPaths: /profile /analytics /achievements /settings /upgrade /fails
        matchPrefixes = listOf(
            "profile", "analytics", "achievements", "settings", "upgrade", "fails",
        ),
    ),
)

@Composable
fun PraxisBottomNav(navController: NavController, currentRoute: String?) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                TABS.forEach { tab ->
                    NavTab(
                        tab = tab,
                        selected = isActive(tab, currentRoute),
                        onClick = {
                            navController.navigate(tab.root) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

private fun isActive(tab: TabSpec, currentRoute: String?): Boolean {
    if (currentRoute == null) return false
    val base = currentRoute.substringBefore('?')
    return tab.matchPrefixes.any { base.startsWith(it) || base.startsWith("/$it") }
}

@Composable
private fun NavTab(
    tab: TabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 32.dp else 0.dp,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "tab-indicator-width",
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "tab-indicator-alpha",
    )
    val tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Amber top-edge indicator, centered like the web layoutId pill.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = 0.dp)
                .size(width = indicatorWidth, height = 2.dp)
                .background(accent.copy(alpha = indicatorAlpha), RoundedCornerShape(2.dp)),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = tab.icon,
                contentDescription = stringResource(id = tab.labelRes),
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(id = tab.labelRes).uppercase(),
                style = LabelMonoSmall,
                color = tint,
            )
        }
    }
}
