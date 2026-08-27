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
import androidx.compose.ui.res.stringResource
import com.praxis.android.data.model.Message

@Composable
fun ChatListScreen(messages: List<Message>, loading: Boolean, onChatClick: (String, String) -> Unit) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    val conversations = messages.groupBy { it.roomId ?: it.senderId }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (conversations.isEmpty()) {
            item {
                Text(text = stringResource(id = R.string.no_conversations), modifier = Modifier.padding(16.dp))
            }
        }
        items(conversations.toList()) { (roomId, msgs) ->
            val lastMsg = msgs.maxByOrNull { it.createdAt }
            PraxisCard(modifier = Modifier.fillMaxWidth(), onClick = { onChatClick(roomId, lastMsg?.content ?: "") }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Room: ${roomId.take(24)}", style = MaterialTheme.typography.titleMedium)
                    Text(text = lastMsg?.content?.take(100) ?: "", style = MaterialTheme.typography.bodyMedium)
                    Text(text = lastMsg?.createdAt?.take(16) ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
