package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
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
import com.praxis.android.data.model.Friend

@Composable
fun FriendsScreen(friends: List<Friend>, requests: List<com.praxis.android.data.model.FriendRequest>, loading: Boolean, onAccept: (String) -> Unit, onSend: (String) -> Unit) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (requests.isNotEmpty()) {
            item {
                Text(text = "Friend Requests", style = MaterialTheme.typography.titleMedium)
            }
            items(requests) { request ->
                PraxisCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Request from ${request.fromUserId}", style = MaterialTheme.typography.bodyMedium)
                        PraxisButton(onClick = { onAccept(request.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Accept")
                        }
                    }
                }
            }
        }

        if (friends.isEmpty() && requests.isEmpty()) {
            item {
                Text(text = "No friends yet. Send a request to get started!", modifier = Modifier.padding(16.dp))
            }
        }
        items(friends) { friend ->
            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = friend.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Streak: ${friend.streak}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
