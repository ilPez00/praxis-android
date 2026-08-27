package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.praxisweb.xyz.R
import com.praxis.android.data.model.Group

@Composable
fun GroupsScreen(groups: List<Group>, loading: Boolean, onGroupClick: (String) -> Unit) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (groups.isEmpty()) {
            item {
                Text(text = stringResource(id = R.string.no_goals), modifier = Modifier.padding(16.dp))
            }
        }
        items(groups) { group ->
            PraxisCard(modifier = Modifier.fillMaxWidth(), onClick = { onGroupClick(group.id) }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = group.name, style = MaterialTheme.typography.titleMedium)
                    if (!group.description.isNullOrBlank()) {
                        Text(text = group.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = "${group.memberCount} members", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
