package com.praxis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.app.data.remote.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    /**
     * Exchange a Google ID token (from CredentialManager) for a Supabase session.
     * Supabase verifies the token with Google and returns a Supabase JWT.
     */
    fun signInWithGoogle(googleIdToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                supabase.auth.signInWith(IDToken) {
                    provider = Google
                    idToken = googleIdToken
                }
                val session = supabase.auth.currentSessionOrNull()
                val user = supabase.auth.currentUserOrNull()
                if (session != null && user != null) {
                    val displayName = user.userMetadata
                        ?.get("full_name")?.toString()?.trim('"')
                        ?: user.userMetadata?.get("name")?.toString()?.trim('"')
                        ?: ""
                    _authState.value = AuthState.Success(
                        userId = user.id,
                        accessToken = session.accessToken,
                        displayName = displayName,
                    )
                } else {
                    _authState.value = AuthState.Error("Sign-in completed but no session found")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    fun reset() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(
        val userId: String,
        val accessToken: String,
        val displayName: String,
    ) : AuthState()
    data class Error(val message: String?) : AuthState()
}
