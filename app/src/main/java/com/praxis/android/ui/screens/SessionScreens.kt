package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.praxis.android.auth.AuthManager
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import kotlinx.coroutines.launch

/**
 * OAuth return landing. Supabase hands back the session in the URL fragment
 * (#access_token=…&refresh_token=…); we parse, persist through the same path a
 * password login uses, and hand the user to their notebook.
 */
object SessionScreens {

    @Composable
    fun AuthCallbackScreen(data: String?, onDone: (Boolean) -> Unit) {
        var outcome by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
        val context = LocalContext.current

        LaunchedEffect(data) {
            val parsed = parseOAuthCallback(data)
            if (parsed == null) {
                outcome = false to "No session found in the callback URL."
            } else {
                val (access, refresh) = parsed
                AuthManager.saveTokenOnly(context, access)
                // Same mirror the login flow uses so widgets keep working.
                runCatching {
                    val expiresAt = jwtExpiry(access) ?: (System.currentTimeMillis() + 3_600_000L)
                    app.praxisweb.xyz.WidgetStore.get(context).saveSession(
                        access,
                        refresh,
                        expiresAt,
                        "https://praxisweb.xyz/api",
                        app.praxisweb.xyz.BuildConfig.SUPABASE_URL,
                        app.praxisweb.xyz.BuildConfig.SUPABASE_ANON_KEY
                    )
                }
                outcome = true to "Signed in."
            }
        }

        DataUi.ScreenScaffold(title = "Signing in", onBack = null) {
            when (val o = outcome) {
                null -> DataUi.Loading()
                else -> {
                    Text(
                        text = o.second,
                        color = if (o.first) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LaunchedEffect(o.first) { onDone(o.first) }
                }
            }
        }
    }

    /** Extract access/refresh tokens from an implicit-flow callback URL. */
    internal fun parseOAuthCallback(raw: String?): Pair<String, String?>? {
        if (raw.isNullOrBlank()) return null
        val fragmentPart = raw.substringAfter('#', "").ifBlank { raw.substringAfter('?', "") }
        if (!fragmentPart.contains("access_token")) return null
        val params = fragmentPart.split('&').mapNotNull {
            val idx = it.indexOf('=')
            if (idx <= 0) null else it.substring(0, idx) to java.net.URLDecoder.decode(it.substring(idx + 1), "UTF-8")
        }.toMap()
        val access = params["access_token"] ?: return null
        return access to params["refresh_token"]
    }

    private fun jwtExpiry(token: String): Long? = runCatching {
        val payload = String(android.util.Base64.decode(
            token.split(".")[1],
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        ))
        org.json.JSONObject(payload).optLong("exp", 0L) * 1000L
    }.getOrNull()?.takeIf { it > 0L }

    @Composable
    fun ScreenTimeScreen(context: Context, repo: PraxisRepository, onBack: () -> Unit) {
        var usage by remember { mutableStateOf<com.praxis.android.util.ScreenTimeManager.Usage?>(null) }
        var posted by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun sample() {
            usage = com.praxis.android.util.ScreenTimeManager.getTodayUsage(context)
        }
        LaunchedEffect(Unit) { sample() }

        DataUi.ScreenScaffold(title = context.getString(app.praxisweb.xyz.R.string.screen_time_title), onBack = onBack) {
            val current = usage
            if (current == null) {
                DataUi.Loading()
            } else if (!current.granted) {
                Text("Usage access is needed to measure social-media minutes for detox commitments — the numbers are read from Android, not typed in.")
                PraxisButton(onClick = { com.praxis.android.util.ScreenTimeManager.openSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text(context.getString(app.praxisweb.xyz.R.string.screen_time_grant))
                }
            } else {
                DataUi.MapCard(
                    mapOf(
                        "social minutes today" to current.socialMinutes,
                        "total minutes today" to current.totalMinutes,
                        "apps counted" to current.perApp.size
                    )
                )
                PraxisButton(
                    onClick = {
                        scope.launch {
                            repo.oracleDetoxCommit(mapOf("minutes" to current.socialMinutes.toString()))
                                .onSuccess { posted = "Sent to your detox commitment." }
                                .onFailure { posted = it.message ?: "Could not reach the server." }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Send today's number to my detox commitment") }
                posted?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

                if (current.perApp.isNotEmpty()) {
                    Text("Per app", style = MaterialTheme.typography.titleSmall)
                    DataUi.MapsList(
                        current.perApp.entries.map { mapOf<String, Any>("app" to it.key, "minutes" to it.value) },
                        "",
                        titleKey = "app",
                        maxRows = 2
                    )
                }
            }
        }
    }
}
