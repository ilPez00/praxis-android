package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.praxisweb.xyz.R
import com.praxis.android.ui.viewmodel.GoalTreeItem
import com.praxis.android.ui.viewmodel.GoalsViewModel
import com.praxis.android.ui.viewmodel.GoalsUiState

@Composable
fun GoalsScreen(viewModel: GoalsViewModel) {
    val state = viewModel.uiState.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (state) {
            is GoalsUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
            is GoalsUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GoalsUiState.Success -> {
                if (state.tree.isEmpty()) {
                    Text(text = stringResource(id = R.string.no_goals), modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.tree, key = { it.node.id }) { item ->
                            GoalNodeItem(item = item, depth = 0, onToggle = { viewModel.toggleExpand(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalNodeItem(item: com.praxis.android.ui.viewmodel.GoalTreeItem, depth: Int, onToggle: () -> Unit) {
    val indent = depth * 24
    PraxisCard(modifier = Modifier.fillMaxWidth().padding(start = indent.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = item.node.name, style = MaterialTheme.typography.titleMedium)
            if (!item.node.description.isNullOrBlank()) {
                Text(text = item.node.description, style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(
                progress = item.node.progress,
                modifier = Modifier.fillMaxWidth()
            )
            Text(text = "${(item.node.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}
