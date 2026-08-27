package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: PraxisRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCachedPosts().collect { cached ->
                if (cached.isNotEmpty() && _uiState.value is FeedUiState.Loading) {
                    _uiState.value = FeedUiState.Success(cached)
                }
            }
        }
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading
            val res = repository.getPosts("general")
            _uiState.value = if (res.isSuccess) {
                FeedUiState.Success(res.getOrNull() ?: emptyList())
            } else {
                FeedUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load feed")
            }
        }
    }
}

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val posts: List<com.praxis.android.data.model.Post>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}
