package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AchievementsViewModel(private val repository: PraxisRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<AchievementsUiState>(AchievementsUiState.Loading)
    val uiState: StateFlow<AchievementsUiState> = _uiState

    init {
        loadAchievements()
    }

    fun loadAchievements() {
        viewModelScope.launch {
            val res = repository.getAchievements()
            _uiState.value = if (res.isSuccess) {
                AchievementsUiState.Success(res.getOrNull() ?: emptyList())
            } else {
                AchievementsUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load achievements")
            }
        }
    }
}

sealed class AchievementsUiState {
    object Loading : AchievementsUiState()
    data class Success(val achievements: List<com.praxis.android.data.model.Achievement>) : AchievementsUiState()
    data class Error(val message: String) : AchievementsUiState()
}
