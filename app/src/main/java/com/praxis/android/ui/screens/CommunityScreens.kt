package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import kotlinx.coroutines.launch

/**
 * Community commitments: seasonal events, cohorts and personal trackers.
 */
object CommunityScreens {

    @Composable
    fun SeasonalEventsScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var events by remember { mutableStateOf<List<com.praxis.android.data.model.SeasonalEvent>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getActiveSeasonalEvents().onSuccess { events = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Seasonal events", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                events.isEmpty() && error == null -> DataUi.Loading()
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (event in events) {
                        PraxisCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = event.name, style = MaterialTheme.typography.titleMedium)
                                event.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                val bits = listOfNotNull(
                                    event.startDate.take(10).let { "from $it" },
                                    event.endDate.take(10).let { "to $it" },
                                    event.reward?.let { "reward: $it" }
                                ).joinToString(" · ")
                                Text(bits, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CohortsScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var cohorts by remember { mutableStateOf<List<com.praxis.android.data.model.Cohort>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }

        fun load() {
            scope.launch {
                repo.getCohorts().onSuccess { cohorts = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Cohorts", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                cohorts.isEmpty() && error == null -> DataUi.Loading()
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (cohort in cohorts) {
                        PraxisCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = cohort.name, style = MaterialTheme.typography.titleMedium)
                                cohort.description?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${cohort.memberCount} members", style = MaterialTheme.typography.bodySmall)
                                PraxisButton(onClick = {
                                    scope.launch {
                                        repo.joinCohort(cohort.id)
                                            .onSuccess { snackbar.showSnackbar("Joined") }
                                            .onFailure { snackbar.showSnackbar(it.message ?: "Failed to join") }
                                    }
                                }, modifier = Modifier.fillMaxWidth(), enabled = !cohort.joined) {
                                    Text(if (cohort.joined) "Joined" else "Join")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TrackersScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var trackers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var calendar by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                try {
                    // Trackers come back as typed models plus raw calendar rows.
                    val trackerList = repo.getMyTrackers()
                    if (trackerList.isSuccess) {
                        trackers = trackerList.getOrDefault(emptyList()).map { t ->
                            mapOf<String, Any>(
                                "name" to t.label,
                                "type" to t.type,
                                "value" to t.value,
                                "id" to t.id
                            )
                        }
                    } else {
                        error = trackerList.exceptionOrNull()?.message
                    }
                    repo.getTrackerCalendar().onSuccess { calendar = it }
                } catch (e: Exception) {
                    error = e.message
                }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Trackers", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${trackers.size} trackers · ${calendar.size} logged days", style = MaterialTheme.typography.titleMedium)
                    if (trackers.isNotEmpty()) {
                        DataUi.MapsList(trackers, "", titleKey = "name", maxRows = 4)
                    }
                    if (calendar.isNotEmpty()) {
                        Text("Recent activity", style = MaterialTheme.typography.titleSmall)
                        DataUi.MapsList(calendar.take(14), "", titleKey = "date", maxRows = 3)
                    }
                }
            }
        }
    }
}
