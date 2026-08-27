package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SocialViewModel(private val repository: PraxisRepository, private val userId: String) : ViewModel() {
    private val _feedState = MutableStateFlow<SocialUiState>(SocialUiState.Loading)
    val feedState: StateFlow<SocialUiState> = _feedState

    private val _matchesState = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val matchesState: StateFlow<MatchesUiState> = _matchesState

    init {
        loadFeed()
        loadMatches()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _feedState.value = SocialUiState.Loading
            val res = repository.getPosts("general")
            _feedState.value = if (res.isSuccess) {
                SocialUiState.Success(res.getOrNull()!!)
            } else {
                SocialUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load feed")
            }
        }
    }

    fun loadMatches() {
        viewModelScope.launch {
            _matchesState.value = MatchesUiState.Loading
            val res = repository.getMatches(userId)
            _matchesState.value = if (res.isSuccess) {
                MatchesUiState.Success(res.getOrNull() ?: emptyList())
            } else {
                MatchesUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load matches")
            }
        }
    }
}

sealed class SocialUiState {
    object Loading : SocialUiState()
    data class Success(val posts: List<com.praxis.android.data.model.Post>) : SocialUiState()
    data class Error(val message: String) : SocialUiState()
}

sealed class MatchesUiState {
    object Loading : MatchesUiState()
    data class Success(val matches: List<com.praxis.android.data.model.Match>) : MatchesUiState()
    data class Error(val message: String) : MatchesUiState()
}
