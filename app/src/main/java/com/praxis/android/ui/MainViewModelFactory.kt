package com.praxis.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.praxis.android.data.repository.PraxisRepository

class MainViewModelFactory(private val repository: PraxisRepository, private val context: android.content.Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, context) as T
    }
}
