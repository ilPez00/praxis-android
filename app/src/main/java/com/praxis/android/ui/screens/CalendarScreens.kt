package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Calendar screen: upcoming Google Calendar events (when linked via Settings on
 * the web) merged with tracker-calendar activity. Read-only by design — plans
 * are created from the web SpeedDial / notebook, this is the pocket view.
 */
object CalendarScreens {

    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    @Composable
    fun CalendarScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var events by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var trackerDays by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var googleLinked by remember { mutableStateOf<Boolean?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        fun load() {
            error = null
            scope.launch {
                repo.getGoogleCalendarStatus().onSuccess { googleLinked = it }
                repo.getGoogleEvents(14).onSuccess { events = it }.onFailure { if (googleLinked == true) error = it.message }
                repo.getTrackerCalendar(30).onSuccess { trackerDays = it }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Calendar", onBack = onBack) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    error != null -> DataUi.Error(error) { load() }
                    else -> {
                        // Google link status line
                        PraxisCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = when (googleLinked) {
                                    null -> "Checking Google Calendar link…"
                                    true -> "Google Calendar linked — next 14 days shown."
                                    false -> "Google Calendar not linked. Connect it on the web: praxisweb.xyz → Settings → Auto-proof providers."
                                    else -> ""
                                },
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (events.isEmpty() && trackerDays.isEmpty() && (googleLinked != null)) {
                            Text(text = "Nothing scheduled in the next two weeks.", style = MaterialTheme.typography.bodyMedium)
                        }

                        for (event in events.take(20)) {
                            EventRow(event)
                        }

                        // Recent tracked activity under the events.
                        val recent = trackerDays.filter { day ->
                            val d = (day["date"] as? String)?.takeIf { it.isNotBlank() } ?: return@filter false
                            d >= LocalDate.now(ZoneId.systemDefault()).minusDays(7).toString()
                        }
                        if (recent.isNotEmpty()) {
                            Text(text = "Last 7 days of tracked activity", style = MaterialTheme.typography.titleSmall)
                            for (day in recent.take(7)) {
                                PraxisCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = (day["date"] as? String ?: "").take(10), style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "${day["count"] ?: 0} logged", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EventRow(event: Map<String, Any>) {
        PraxisCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = (event["title"] as? String) ?: "Untitled", style = MaterialTheme.typography.titleSmall)
                val start = event["start"]?.toString()
                if (!start.isNullOrBlank()) {
                    Text(text = prettyWhen(start, event["allDay"] == true), style = MaterialTheme.typography.bodySmall)
                }
                val location = event["location"]?.toString()
                if (!location.isNullOrBlank()) {
                    Text(text = "📍 $location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    private fun prettyWhen(iso: String, allDay: Boolean): String = try {
        val instant = java.time.Instant.parse(iso)
        if (allDay) {
            instant.atZone(ZoneId.systemDefault()).toLocalDate().format(dayFormat) + " · all day"
        } else {
            val zoned = instant.atZone(ZoneId.systemDefault())
            "${zoned.format(dayFormat)} · ${zoned.format(timeFormat)}"
        }
    } catch (_: Exception) {
        iso.take(16)
    }
}
