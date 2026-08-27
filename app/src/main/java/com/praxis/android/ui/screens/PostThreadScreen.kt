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
import com.praxis.android.data.model.Post

@Composable
fun PostThreadScreen(post: Post?, loading: Boolean, comments: List<com.praxis.android.data.model.Comment>, onBack: () -> Unit) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    if (post == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = "Post not found.", modifier = Modifier.padding(16.dp))
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = post.userName, style = MaterialTheme.typography.titleMedium)
                    Text(text = post.content, style = MaterialTheme.typography.bodyLarge)
                    Text(text = "${post.likeCount} likes · ${post.commentCount} comments", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Text(text = "Comments", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        }

        if (comments.isEmpty()) {
            item {
                Text(text = "No comments yet.", modifier = Modifier.padding(16.dp))
            }
        }
        items(comments) { comment ->
            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = comment.userName, style = MaterialTheme.typography.titleSmall)
                    Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
                    Text(text = comment.createdAt.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
