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
import com.praxis.android.data.model.UserProfile

@Composable
fun SearchScreen(users: List<UserProfile>, loading: Boolean, onUserClick: (String) -> Unit) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (users.isEmpty()) {
            item {
                Text(text = "No users found.", modifier = Modifier.padding(16.dp))
            }
        }
        items(users) { user ->
            PraxisCard(modifier = Modifier.fillMaxWidth(), onClick = { onUserClick(user.id) }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = user.name, style = MaterialTheme.typography.titleMedium)
                    if (!user.bio.isNullOrBlank()) {
                        Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = "Streak: ${user.streak} | Points: ${user.praxisPoints}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
