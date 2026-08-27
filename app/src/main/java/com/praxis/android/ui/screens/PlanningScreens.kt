package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import kotlinx.coroutines.launch

/**
 * Planning surfaces: the planner, the gantt views and the coherence trio.
 * Timelines render as proportional bars rather than a canvas chart — a list
 * with real data beats a pretty empty grid, and it scrolls.
 */
object PlanningScreens {

    @Composable
    fun PlannerScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var items by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getPlanItems().onSuccess { items = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Planner", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                items.isEmpty() && error == null -> DataUi.Loading()
                else -> DataUi.MapsList(items, emptyText = "Nothing planned yet.", titleKey = "title")
            }
        }
    }

    @Composable
    fun GanttScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var plan by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getGanttPlan().onSuccess { plan = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Project timeline", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                plan.isEmpty() && error == null -> DataUi.Loading()
                else -> GanttBars(plan)
            }
        }
    }

    /** Rows of [label][bar] where bar width is progress fraction. */
    @Composable
    private fun GanttBars(plan: List<Map<String, Any>>) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (item in plan) {
                val label = (item["title"] ?: item["name"] ?: item["goal"] ?: item["id"] ?: "?").toString()
                val progress = (item["progress"] as? Number)?.toDouble()?.coerceIn(0.0, 1.0)
                PraxisCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = label.take(80), style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(
                            progress = { (progress ?: 0.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        val detail = listOfNotNull(
                            item["start"]?.toString()?.let { "start $it" },
                            item["end"] ?: item["due"]?.toString()?.let { "due ${it}" },
                            progress?.let { "${(it * 100).toInt()}%" }
                        ).joinToString(" · ")
                        if (detail.isNotBlank()) {
                            Text(text = detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PlanScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var items by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getGanttPlan().onSuccess { items = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Plan", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                items.isEmpty() && error == null -> DataUi.Loading()
                else -> DataUi.MapsList(items, emptyText = "No plan yet.", titleKey = "title")
            }
        }
    }

    @Composable
    fun GanttTrajectoryScreen(repo: PraxisRepository, onBack: () -> Unit) {
        // The trajectory view reads the same plan but orders by date so the eye
        // walks forward through time.
        var plan by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getGanttPlan().onSuccess { plan = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Trajectory", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                plan.isEmpty() && error == null -> DataUi.Loading()
                else -> DataUi.MapsList(plan.sortedBy { it["start"]?.toString() ?: "" }, emptyText = "No trajectory yet.", titleKey = "title")
            }
        }
    }

    @Composable
    fun GanttCoherenceScreen(repo: PraxisRepository, onBack: () -> Unit) {
        SingleMapScreen(repo = repo, title = "Coherence analysis", loader = { repo.getCoherenceGantt() }, onBack = onBack)
    }

    @Composable
    fun CoherenceTimelineScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var items by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getCoherenceTimeline().onSuccess { items = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Coherence timeline", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                items.isEmpty() && error == null -> DataUi.Loading()
                else -> DataUi.MapsList(items, emptyText = "Timeline is empty.", titleKey = "date")
            }
        }
    }

    @Composable
    fun CoherenceDashboardScreen(repo: PraxisRepository, onBack: () -> Unit) {
        SingleMapScreen(repo = repo, title = "Coherence dashboard", loader = { repo.getCoherenceDashboard() }, onBack = onBack)
    }

    @Composable
    fun SingleMapScreen(
        repo: PraxisRepository,
        title: String,
        loader: suspend (PraxisRepository) -> Result<Map<String, Any>>,
        onBack: () -> Unit
    ) {
        var payload by remember { mutableStateOf<Map<String, Any>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                loader(repo).onSuccess { payload = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = title, onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                payload == null -> DataUi.Loading()
                else -> DataUi.MapCard(payload!!)
            }
        }
    }
}
