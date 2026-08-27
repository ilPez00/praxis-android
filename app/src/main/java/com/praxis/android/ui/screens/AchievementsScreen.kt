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
import app.praxisweb.xyz.R
import com.praxis.android.data.model.Achievement

@Composable
fun AchievementsScreen(achievements: List<Achievement>, loading: Boolean) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (achievements.isEmpty()) {
            item {
                Text(text = "No achievements yet. Keep going!", modifier = Modifier.padding(16.dp))
            }
        }
        items(achievements) { achievement ->
            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = achievement.title, style = MaterialTheme.typography.titleMedium)
                    if (!achievement.description.isNullOrBlank()) {
                        Text(text = achievement.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    achievement.unlockedAt?.let { unlockedAt ->
                        Text(text = "Unlocked: ${unlockedAt.take(10)}", style = MaterialTheme.typography.bodySmall)
                    } ?: Text(text = "Locked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
