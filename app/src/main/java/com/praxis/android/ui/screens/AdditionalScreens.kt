package com.praxis.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import kotlinx.coroutines.launch

/**
 * Credential verification, the Praxis CV, and Physis — all reading live API
 * data — plus the legal pages.
 */
object AdditionalScreens {

    @Composable
    fun CredentialScreen(repo: PraxisRepository, credentialId: String, onBack: () -> Unit) {
        var payload by remember { mutableStateOf<Map<String, Any>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.verifyCredential(credentialId)
                    .onSuccess { payload = it; error = null }
                    .onFailure { error = it.message }
            }
        }
        LaunchedEffect(credentialId) { load() }

        DataUi.ScreenScaffold(title = "Credential", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                payload == null -> DataUi.Loading()
                else -> DataUi.MapCard(payload!!)
            }
        }
    }

    @Composable
    fun CvScreen(repo: PraxisRepository, username: String, onBack: () -> Unit) {
        var payload by remember { mutableStateOf<Map<String, Any>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                repo.getCv(username).onSuccess { payload = it; error = null }.onFailure { error = it.message }
            }
        }
        LaunchedEffect(username) { load() }

        DataUi.ScreenScaffold(title = "Praxis CV · $username", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                payload == null -> DataUi.Loading()
                else -> DataUi.MapCard(payload!!, maxRows = 24)
            }
        }
    }

    @Composable
    fun PhysisScreen(repo: PraxisRepository, onBack: () -> Unit) {
        var snapshot by remember { mutableStateOf<Map<String, Any>?>(null) }
        var rhythms by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun load() {
            scope.launch {
                try {
                    repo.getPhysisSnapshot().onSuccess { snapshot = it }
                    repo.getPhysisRhythms().onSuccess { rhythms = it }
                    error = null
                } catch (e: Exception) {
                    error = e.message
                }
            }
        }
        LaunchedEffect(Unit) { load() }

        DataUi.ScreenScaffold(title = "Physis", onBack = onBack) {
            when {
                error != null -> DataUi.Error(error) { load() }
                snapshot == null && rhythms.isEmpty() -> DataUi.Loading()
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    snapshot?.let { DataUi.MapCard(it) }
                    if (rhythms.isNotEmpty()) {
                        Text("Rhythms", style = MaterialTheme.typography.titleSmall)
                        DataUi.MapsList(rhythms, "", titleKey = "name", maxRows = 4)
                    }
                }
            }
        }
    }

    @Composable
    fun PrivacyPolicyScreen(onBack: () -> Unit) {
        LegalScreen(
            title = "Privacy Policy",
            body = "Praxis stores your journal entries, goals and check-ins on your account. " +
                "Your data is yours: export or delete it any time from Settings. " +
                "The full policy lives at praxisweb.xyz/privacy.",
            onBack = onBack
        )
    }

    @Composable
    fun TermsOfServiceScreen(onBack: () -> Unit) {
        LegalScreen(
            title = "Terms of Service",
            body = "Use Praxis honestly — bets and commitments are backed by real check-ins. " +
                "The full terms live at praxisweb.xyz/terms.",
            onBack = onBack
        )
    }

    @Composable
    private fun LegalScreen(title: String, body: String, onBack: () -> Unit) {
        DataUi.ScreenScaffold(title = title, onBack = onBack) {
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
