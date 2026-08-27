package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import kotlinx.coroutines.launch

/**
 * Tool surfaces: device lattice, marketplace, the MCP chat and word frequency.
 */
object ToolScreens {

    @Composable
    fun LatticeScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var overview by remember { mutableStateOf<Map<String, Any>?>(null) }
        var devices by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var jobs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                try {
                    repo.getLattice().onSuccess { overview = it }
                    repo.getLatticeDevices().onSuccess { devices = it }
                    repo.getLatticeJobs().onSuccess { jobs = it }
                    error = null
                } catch (e: Exception) {
                    error = e.message
                }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Device lattice", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                overview == null -> DataUi.Loading()
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "${devices.size} devices · ${jobs.size} jobs", style = MaterialTheme.typography.titleMedium)
                    DataUi.MapCard(overview ?: emptyMap())
                    if (devices.isNotEmpty()) {
                        Text("Devices", style = MaterialTheme.typography.titleSmall)
                        DataUi.MapsList(devices, "", titleKey = "name", maxRows = 5)
                    }
                    if (jobs.isNotEmpty()) {
                        Text("Jobs", style = MaterialTheme.typography.titleSmall)
                        DataUi.MapsList(jobs, "", titleKey = "kind", maxRows = 5)
                    }
                }
            }
        }
    }

    @Composable
    fun MarketplaceScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var items by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        var buying by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getMarketplaceItems().onSuccess { items = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Marketplace", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                items.isEmpty() && error == null -> DataUi.Loading()
                else -> DataUi.MapsList(items, emptyText = "Marketplace is empty.", titleKey = "name")
            }
        }
        // Purchase flows go through Stripe on the web; the app links out rather
        // than reimplementing checkout. Buying stays a web action for now.
        if (!buying) {
            PraxisButton(onClick = { buying = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Open store on praxisweb.xyz/marketplace")
            }
        }
    }

    @Composable
    fun McpScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var input by remember { mutableStateOf("") }
        var transcript by remember { mutableStateOf(listOf<Pair<String, String>>()) } // role → text
        var sending by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        DataUi.ScreenScaffold(title = "MCP", onBack = onBack) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Message the agent bus") },
                modifier = Modifier.fillMaxWidth()
            )
            PraxisButton(
                onClick = {
                    val message = input.trim()
                    if (message.isEmpty()) return@PraxisButton
                    input = ""
                    transcript = transcript + ("you" to message)
                    sending = true
                    scope.launch {
                        repo.sendMcpMessage(mapOf("message" to message))
                            .onSuccess { res ->
                                val reply = res["reply"]?.toString()
                                    ?: res["response"]?.toString()
                                    ?: res.toString().take(400)
                                transcript = transcript + ("mcp" to reply)
                            }
                            .onFailure { transcript = transcript + ("error" to (it.message ?: "failed")) }
                        sending = false
                    }
                },
                enabled = !sending,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (sending) "Sending…" else "Send") }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((role, text) in transcript.asReversed()) {
                    PraxisCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = role.uppercase(), style = MaterialTheme.typography.labelSmall)
                            Text(text = text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun WordsScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var words by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getWordsFrequency().onSuccess { words = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Word frequency", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                words.isEmpty() && error == null -> DataUi.Loading()
                else -> WordBars(words)
            }
        }
    }

    @Composable
    private fun WordBars(words: List<Map<String, Any>>) {
        val counts = words.mapNotNull { w ->
            val term = (w["word"] ?: w["term"] ?: w["text"])?.toString() ?: return@mapNotNull null
            val count = ((w["count"] ?: w["frequency"]) as? Number)?.toDouble() ?: 0.0
            term to count
        }
        val peak = counts.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((term, count) in counts.take(30)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = term.take(18),
                        modifier = Modifier.width(110.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { (count / peak).toFloat() },
                        modifier = Modifier.weight(1f).height(10.dp)
                    )
                    Text(
                        text = count.toInt().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(36.dp).padding(start = 6.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun AdminScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var stats by remember { mutableStateOf<Map<String, Any>?>(null) }
        var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                try {
                    repo.adminStats().onSuccess { stats = it }
                    repo.adminUsers().onSuccess { users = it }
                    error = null
                } catch (e: Exception) {
                    error = e.message
                }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Admin", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                stats == null -> DataUi.Loading()
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Stats", style = MaterialTheme.typography.titleSmall)
                    DataUi.MapCard(stats ?: emptyMap())
                    Text("${users.size} users", style = MaterialTheme.typography.titleSmall)
                    if (users.isNotEmpty()) {
                        DataUi.MapsList(users.take(20), "", titleKey = "email", maxRows = 4)
                    }
                }
            }
        }
    }
}
