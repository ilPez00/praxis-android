package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.praxisweb.xyz.R
import com.praxis.android.data.model.NotebookEntry
import com.praxis.android.ui.viewmodel.NotebookViewModel
import com.praxis.android.ui.viewmodel.NotebookUiState
import kotlinx.coroutines.launch

@Composable
fun NotebookScreen(
    viewModel: NotebookViewModel,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToFeed: () -> Unit,
    prefill: String? = null,
    openComposer: Boolean = false,
    onOpenCalendar: () -> Unit = {},
    onOpenHealth: () -> Unit = {}
) {
    val state = viewModel.uiState.value
    val searchQuery = remember { mutableStateOf("") }
    // The quick-capture composer: opened automatically by the capture widget's
    // deep link, or by sharing text into Praxis from another app.
    val composerOpen = remember { mutableStateOf(openComposer || !prefill.isNullOrBlank()) }
    val draft = remember(prefill) { mutableStateOf(prefill ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (state) {
            is NotebookUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
            is NotebookUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    PraxisButton(onClick = { viewModel.loadEntries() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Retry")
                    }
                }
            }
            is NotebookUiState.Success -> {
                Text(text = stringResource(id = R.string.notebook), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp))

                if (composerOpen.value) {
                    OutlinedTextField(
                        value = draft.value,
                        onValueChange = { draft.value = it },
                        label = { Text("Quick note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // AI scan (Pro): what Axiom sees in the draft.
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    var scan by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
                    var scanning by remember { mutableStateOf(false) }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PraxisButton(
                            onClick = {
                                val content = draft.value.trim()
                                if (content.isNotEmpty()) {
                                    viewModel.createEntry(content)
                                    draft.value = ""
                                    composerOpen.value = false
                                    scan = null
                                }
                            },
                            enabled = draft.value.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Save") }
                        PraxisButton(
                            onClick = {
                                val content = draft.value.trim()
                                if (content.isNotEmpty()) {
                                    scanning = true
                                    scan = null
                                    scope.launch {
                                        viewModel.aiScan(content).fold(
                                            onSuccess = { m ->
                                                val summary = m["summary"]?.toString() ?: ""
                                                val actions = (m["action_items"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                                                val tags = (m["suggested_tags"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                                                scan = true to buildString {
                                                    append(summary)
                                                    if (actions.isNotEmpty()) append("\nNext: " + actions.joinToString("; "))
                                                    if (tags.isNotEmpty()) append("\n#" + tags.joinToString(" #"))
                                                }
                                            },
                                            onFailure = { scan = false to (it.message ?: "Scan failed.") }
                                        )
                                        scanning = false
                                    }
                                }
                            },
                            enabled = draft.value.isNotBlank() && !scanning,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (scanning) "Reading…" else "AI scan") }
                    }
                    scan?.let { (ok, text) ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    PraxisButton(onClick = { composerOpen.value = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Quick note")
                    }
                }

                OutlinedTextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    label = { Text("Search entries") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.entries.isEmpty()) {
                    Text(text = "No entries yet. Start journaling!", modifier = Modifier.padding(16.dp))
                } else {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.entries) { entry: NotebookEntry ->
                            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = entry.content.take(200), style = MaterialTheme.typography.bodyMedium)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = entry.createdAt.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        // Share-to: hands the entry text to any app
                                        // (WhatsApp, Telegram, mail…) via the system sheet.
                                        androidx.compose.material3.TextButton(onClick = {
                                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, entry.content)
                                            }
                                            context.startActivity(
                                                android.content.Intent.createChooser(send, "Share entry")
                                            )
                                        }) { Text("Share ⤴", style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                        }
                    }
                }

                PraxisButton(onClick = onNavigateToCheckIn, modifier = Modifier.fillMaxWidth(), enabled = true) {
                    Text(stringResource(id = R.string.check_in_label))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PraxisButton(onClick = onOpenCalendar, modifier = Modifier.weight(1f)) {
                        Text("📅 Calendar")
                    }
                    PraxisButton(onClick = onOpenHealth, modifier = Modifier.weight(1f)) {
                        Text("♥ Health")
                    }
                }
                PraxisButton(onClick = onNavigateToGoals, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.goals))
                }
                PraxisButton(onClick = onNavigateToFeed, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.feed))
                }
            }
        }
    }
}
