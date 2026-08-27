package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.praxis.android.health.HealthConnectBridge
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import kotlinx.coroutines.launch

/**
 * Health Connect screen: grant read access, see today's steps/calories/weight,
 * push a sample to the oracle. HC aggregates MyFitnessPal, Yazio, Fitbit,
 * Google Fit and Samsung Health, so this one bridge covers them all.
 *
 * Permissions use the SDK's ActivityResult contract
 * (PermissionController.Companion.createRequestPermissionResultContract) — the
 * suspend requestGrantedPermissions API is gone in 1.1.0-alpha07.
 */
object HealthScreens {

    @Composable
    fun HealthConnectScreen(repo: PraxisRepository, onBack: () -> Unit) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var health by remember { mutableStateOf(HealthConnectBridge.TodayHealth(available = false, granted = false)) }
        var message by remember { mutableStateOf<String?>(null) }
        var syncing by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            androidx.health.connect.client.PermissionController.Companion.createRequestPermissionResultContract()
        ) { _ ->
            scope.launch {
                health = HealthConnectBridge.readToday(context)
                if (health.granted && health.hasData()) {
                    push(repo, health) { message = it }
                } else {
                    message = "Read permission is needed to sync."
                }
                syncing = false
            }
        }

        LaunchedEffect(Unit) {
            syncing = true
            health = HealthConnectBridge.readToday(context)
            syncing = false
        }

        fun sync() {
            scope.launch {
                syncing = true
                message = null
                health = HealthConnectBridge.readToday(context)
                when {
                    !health.available ->
                        message = "Install or update Health Connect from the Play Store first."
                    !health.granted -> {
                        if (HealthConnectBridge.isAvailable(context)) {
                            // Launches the Health Connect permissions sheet; result lands in permissionLauncher.
                            permissionLauncher.launch(HealthConnectBridge.readPermissions)
                        } else {
                            message = "This device cannot run Health Connect (needs Android 8+)."
                            syncing = false
                        }
                    }
                    else -> push(repo, health) { message = it }
                }
                if (health.available && health.granted) syncing = false
            }
        }

        DataUi.ScreenScaffold(title = "Health", onBack = onBack) {
            when {
                !health.available && health.error != null -> DataUi.Error(health.error) { sync() }
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PraxisCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Today via Health Connect", style = MaterialTheme.typography.titleMedium)
                            if (health.hasData()) {
                                health.steps?.let { StatRow("Steps", "%,d".format(it)) }
                                health.calories?.let { StatRow("Calories", "${it.toInt()} kcal") }
                                health.weightKg?.let { StatRow("Weight", "%.1f kg".format(it)) }
                            } else {
                                Text(
                                    text = "No samples yet. Data appears here once MyFitnessPal, Yazio,\nFitbit, Google Fit or Samsung Health write through Health Connect.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    PraxisButton(onClick = { sync() }, enabled = !syncing, modifier = Modifier.fillMaxWidth()) {
                        Text(if (syncing) "Syncing…" else if (health.granted) "Sync to oracle" else "Grant access & sync")
                    }
                    message?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "One sample per user-day lands in the oracle (trust 0.7 — self-device). Goals with steps / calories / weight windows verify from it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    private suspend fun push(repo: PraxisRepository, health: HealthConnectBridge.TodayHealth, onMessage: (String) -> Unit) {
        repo.submitHealthSample(health.steps, health.calories, health.weightKg)
            .onSuccess { ok -> onMessage(if (ok) "Sample pushed — the health_connect oracle can now verify goals." else "Nothing to push (no data).") }
            .onFailure { onMessage("Push failed: ${it.message ?: "network error"}") }
    }

    @Composable
    private fun StatRow(label: String, value: String) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
