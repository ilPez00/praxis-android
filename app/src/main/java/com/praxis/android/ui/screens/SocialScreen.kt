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
import com.praxis.android.ui.viewmodel.MatchesUiState
import com.praxis.android.ui.viewmodel.SocialUiState

@Composable
fun SocialScreen(viewModel: com.praxis.android.ui.viewmodel.SocialViewModel, onNavigateToProfile: (String) -> Unit) {
    val feedState = viewModel.feedState.value
    val matchesState = viewModel.matchesState.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (matchesState) {
            is MatchesUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
            }
            is MatchesUiState.Error -> {
                Text(text = matchesState.message, color = MaterialTheme.colorScheme.error)
            }
            is MatchesUiState.Success -> {
                if (matchesState.matches.isNotEmpty()) {
                    Text(text = "Top Matches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matchesState.matches.take(5)) { match ->
                            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = match.name, style = MaterialTheme.typography.titleSmall)
                                    Text(text = "Compatibility: ${(match.score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        when (feedState) {
            is SocialUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
            is SocialUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = feedState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is SocialUiState.Success -> {
                if (feedState.posts.isEmpty()) {
                    Text(text = "No posts yet. Be the first to share!", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(feedState.posts) { post: Post ->
                            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = post.userName, style = MaterialTheme.typography.titleMedium)
                                    Text(text = post.content, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "${post.likeCount} likes · ${post.commentCount} comments", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
