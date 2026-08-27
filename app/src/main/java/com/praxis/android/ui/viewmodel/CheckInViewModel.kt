package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CheckInViewModel(private val repository: PraxisRepository, private val userId: String) : ViewModel() {
    private val _uiState = MutableStateFlow<CheckInUiState>(CheckInUiState.Loading)
    val uiState: StateFlow<CheckInUiState> = _uiState

    init {
        loadTodayCheckin()
    }

    fun loadTodayCheckin() {
        viewModelScope.launch {
            val res = repository.getTodayCheckin(userId)
            _uiState.value = if (res.isSuccess) {
                val checkin = res.getOrNull()
                CheckInUiState.Success(checkin?.checkedIn ?: false, checkin?.streak ?: 0, checkin?.totalPoints ?: 0)
            } else {
                CheckInUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load check-in")
            }
        }
    }

    fun checkIn(mood: String, energy: Int, winOfTheDay: String) {
        viewModelScope.launch {
            _uiState.value = CheckInUiState.Loading
            val res = repository.checkIn(userId)
            _uiState.value = if (res.isSuccess) {
                val data = res.getOrNull()!!
                CheckInUiState.CheckedIn(data)
            } else {
                CheckInUiState.Error(res.exceptionOrNull()?.message ?: "Check-in failed")
            }
        }
    }
}

sealed class CheckInUiState {
    object Loading : CheckInUiState()
    data class Success(val checkedIn: Boolean, val streak: Int, val totalPoints: Int) : CheckInUiState()
    data class CheckedIn(val response: com.praxis.android.data.model.CheckInResponse) : CheckInUiState()
    data class Error(val message: String) : CheckInUiState()
}
