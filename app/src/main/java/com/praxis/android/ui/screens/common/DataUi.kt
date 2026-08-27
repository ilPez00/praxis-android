package com.praxis.android.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard
import com.praxis.android.ui.components.design.PraxisEmptyState
import com.praxis.android.ui.components.design.PraxisTopBar
import com.praxis.android.ui.components.design.PraxisVariant

/**
 * Shared plumbing for screens that read JSON-shaped API payloads.
 *
 * Most Praxis endpoints return `Map<String, Any>` / lists of them; these
 * helpers give every such screen the same loading/error/back chrome and a
 * legible key-value rendering instead of each screen reinventing it.
 */
object DataUi {

    /** Keys that are noise when rendered row by row inside a card. */
    val HIDDEN_KEYS = setOf("id", "user_id", "userId", "owner_id", "created_at", "updated_at")

    @Composable
    fun ScreenScaffold(
        title: String,
        onBack: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Web TopBar parity: sticky 48dp header with 1px bottom border.
            PraxisTopBar(title = title, onBack = onBack)
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }

    @Composable
    fun Loading() {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
    }

    @Composable
    fun Error(message: String?, onRetry: (() -> Unit)? = null) {
        PraxisCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = message ?: "Could not load.", color = MaterialTheme.colorScheme.error)
                if (onRetry != null) {
                    PraxisButton(onClick = onRetry, variant = PraxisVariant.OUTLINED) { Text("Retry") }
                }
            }
        }
    }

    /** One card per map entry, rendering scalar values as label/value rows. */
    @Composable
    fun MapsList(
        items: List<Map<String, Any>>,
        emptyText: String,
        titleKey: String? = null,
        maxRows: Int = 8
    ) {
        if (items.isEmpty()) {
            PraxisEmptyState(text = emptyText)
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                PraxisCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val headline = titleKey?.let { item[it]?.toString() }
                        if (!headline.isNullOrBlank()) {
                            Text(text = headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        }
                        var shown = 0
                        for ((k, v) in item) {
                            if (k in HIDDEN_KEYS || v == null) continue
                            if (shown >= maxRows) {
                                Text(text = "…", style = MaterialTheme.typography.bodySmall)
                                break
                            }
                            val value = v.toString().take(220)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = k.replace('_', ' '), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                                Text(text = value, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                            }
                            shown++
                        }
                    }
                }
            }
        }
    }

    /** Render a single map payload as one card of key/value rows. */
    @Composable
    fun MapCard(payload: Map<String, Any>, maxRows: Int = 14) {
        PraxisCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                var shown = 0
                for ((k, v) in payload) {
                    if (k in HIDDEN_KEYS || v == null) continue
                    if (shown >= maxRows) {
                        Text(text = "…", style = MaterialTheme.typography.bodySmall)
                        break
                    }
                    val value = v.toString().take(300)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = k.replace('_', ' '), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                        Text(text = value, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                    }
                    shown++
                }
            }
        }
    }
}
