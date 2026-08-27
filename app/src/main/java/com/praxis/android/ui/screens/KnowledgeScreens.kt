package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import kotlinx.coroutines.launch

/**
 * The UberWiki family — knowledge graph, cells, wiki browser and index — plus
 * the ontology and aura-web viewers. All of these read live from the API.
 */
object KnowledgeScreens {

    @Composable
    fun UberWikiHubScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var status by remember { mutableStateOf<Map<String, Any>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getUberWikiStatus()
                    .onSuccess { status = it; error = null }
                    .onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "UberWiki", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                status == null -> DataUi.Loading()
                else -> DataUi.MapCard(status!!)
            }
            Text(
                text = "The semantic layer: entities, relationships and the query engine behind the notebook.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    @Composable
    fun WikiBrowserScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var searched by remember { mutableStateOf(false) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        DataUi.ScreenScaffold(title = "Wiki", onBack = onBack) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Semantic search") },
                modifier = Modifier.fillMaxWidth()
            )
            PraxisButton(
                onClick = {
                    scope.launch {
                        loading = true
                        repo.searchWiki(query)
                            .onSuccess { results = it; error = null }
                            .onFailure { error = it.message }
                        searched = true
                        loading = false
                    }
                },
                enabled = query.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "Searching…" else "Search") }

            if (error != null) DataUi.Error(error)
            if (searched && !loading && error == null) {
                DataUi.MapsList(results, emptyText = "No results for \"$query\".", titleKey = "title")
            }
        }
    }

    @Composable
    fun WikiIndexScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var items by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getWikiIndex()
                    .onSuccess { items = it; error = null }
                    .onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Wiki Index", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                items.isEmpty() && error == null -> DataUi.Loading()
                else -> DataUi.MapsList(items, emptyText = "Index is empty.", titleKey = "title")
            }
        }
    }

    @Composable
    fun UberWikiGraphScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var nodes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var edgeCount by remember { mutableStateOf(0) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getUberWikiGraph()
                    .onSuccess { (n, e) -> nodes = n; edgeCount = e.size; error = null }
                    .onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Knowledge graph", onBack = onBack) {
            Text(text = "${nodes.size} nodes · $edgeCount edges", style = MaterialTheme.typography.titleMedium)
            when {
                error != null -> DataUi.Error(error) { load() }
                nodes.isEmpty() && error == null -> DataUi.Loading()
                else -> DataUi.MapsList(nodes, emptyText = "No nodes yet.", titleKey = "label")
            }
        }
    }

    @Composable
    fun UberWikiCellsScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var cells by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getUberWikiCells()
                    .onSuccess { cells = it; error = null }
                    .onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Wiki cells", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                cells.isEmpty() && error == null -> DataUi.Loading()
                else -> DataUi.MapsList(cells, emptyText = "No cells yet.", titleKey = "label")
            }
        }
    }

    @Composable
    fun OntologyScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var payload by remember { mutableStateOf<Map<String, Any>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getOntology().onSuccess { payload = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Ontology", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                payload == null -> DataUi.Loading()
                else -> DataUi.MapCard(payload!!)
            }
        }
    }

    @Composable
    fun AuraWebScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var payload by remember { mutableStateOf<Map<String, Any>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getAuraWeb().onSuccess { payload = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Aura web", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                payload == null -> DataUi.Loading()
                else -> DataUi.MapCard(payload!!)
            }
        }
    }
}
