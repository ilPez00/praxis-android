package com.praxis.android.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "praxis_settings")

data class SettingsState(
    val notificationsEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val matchesEnabled: Boolean = true,
    val privacyMode: String = "friends",
    val language: String = "en",
    val analyticsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    /** Consent gate for the generic phone/app monitor (screen-time + Health Connect). */
    val sharePhoneUsageEnabled: Boolean = false
)

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState

    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            notificationsEnabled = prefs[NOTIFICATIONS_KEY] ?: true,
            messagesEnabled = prefs[MESSAGES_KEY] ?: true,
            matchesEnabled = prefs[MATCHES_KEY] ?: true,
            privacyMode = prefs[PRIVACY_KEY] ?: "friends",
            language = prefs[LANGUAGE_KEY] ?: "en",
            analyticsEnabled = prefs[ANALYTICS_KEY] ?: true,
            biometricEnabled = prefs[BIOMETRIC_KEY] ?: false,
            sharePhoneUsageEnabled = prefs[SHARE_PHONE_USAGE_KEY] ?: false
        )
    }

    init {
        viewModelScope.launch {
            settings.collect { _uiState.value = SettingsUiState.Success(it) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[NOTIFICATIONS_KEY] = enabled }
        }
    }

    fun setMessagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[MESSAGES_KEY] = enabled }
        }
    }

    fun setMatchesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[MATCHES_KEY] = enabled }
        }
    }

    fun setPrivacyMode(mode: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[PRIVACY_KEY] = mode }
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[LANGUAGE_KEY] = lang }
        }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[ANALYTICS_KEY] = enabled }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[BIOMETRIC_KEY] = enabled }
        }
    }

    fun setSharePhoneUsageEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[SHARE_PHONE_USAGE_KEY] = enabled }
        }
    }

    companion object {
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        private val MESSAGES_KEY = booleanPreferencesKey("messages_enabled")
        private val MATCHES_KEY = booleanPreferencesKey("matches_enabled")
        private val PRIVACY_KEY = stringPreferencesKey("privacy_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val ANALYTICS_KEY = booleanPreferencesKey("analytics_enabled")
        private val BIOMETRIC_KEY = booleanPreferencesKey("biometric_enabled")
        private val SHARE_PHONE_USAGE_KEY = booleanPreferencesKey("share_phone_usage_enabled")
    }
}

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(val settings: SettingsState) : SettingsUiState()
}
