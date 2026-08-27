package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(private val repository: PraxisRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            val res = repository.getLeaderboard()
            _uiState.value = if (res.isSuccess) {
                LeaderboardUiState.Success(res.getOrNull()!!)
            } else {
                LeaderboardUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load leaderboard")
            }
        }
    }
}

sealed class LeaderboardUiState {
    object Loading : LeaderboardUiState()
    data class Success(val entries: List<com.praxis.android.data.model.LeaderboardEntry>) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
}
