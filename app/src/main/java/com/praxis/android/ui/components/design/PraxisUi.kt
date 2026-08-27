package com.praxis.android.ui.components.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.praxis.android.ui.theme.LabelMonoSmall

/**
 * Webapp design-system parity primitives (client/src/components/design-system/
 * + the MUI component overrides in client/src/index.tsx). Screens should reach
 * for these instead of raw M3 widgets so the app keeps the web's look:
 * bordered cards on #111111, gradient buttons with an amber glow, translucent
 * tinted chips, and uppercase mono micro-labels.
 */

// ── Card ─────────────────────────────────────────────────────────────────────

/** Web Paper/PraxisCard: surface fill + 1px #222222 border, radius 12. */
@Composable
fun PraxisCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    cornerRadius: Int = 12,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val border = BorderStroke(1.dp, borderColor)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = border,
        ) { Column(content = content) }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = border,
        ) { Column(content = content) }
    }
}

/** Raised variant: #1a1a1a fill + border, radius 16 (web PraxisCard raised). */
@Composable
fun PraxisRaisedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    PraxisCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        cornerRadius = 16,
        content = content,
    )
}

// ── Buttons ──────────────────────────────────────────────────────────────────

/** Web MuiButton variants. */
enum class PraxisVariant { PRIMARY, SECONDARY, OUTLINED }

/**
 * Web MuiButton overrides: sentence case semibold label, radius 10, min 44dp,
 * containedPrimary = linear-gradient(135deg,#F59E0B→#D97706) + amber glow,
 * containedSecondary = violet gradient, outlinedPrimary = amber outline.
 */
@Composable
fun PraxisButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: PraxisVariant = PraxisVariant.PRIMARY,
    content: @Composable RowScope.() -> Unit,
) {
    when (variant) {
        PraxisVariant.PRIMARY ->
            GradientButton(onClick, modifier, enabled, listOf(Color(0xFFF59E0B), Color(0xFFD97706)), content)
        PraxisVariant.SECONDARY ->
            GradientButton(onClick, modifier, enabled, listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)), content)
        PraxisVariant.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 44.dp),
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = BorderStroke(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
            shape = RoundedCornerShape(10.dp),
            content = content,
        )
    }
}

@Composable
private fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    gradientColors: List<Color>,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val glow = gradientColors.first()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 44.dp)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = shape,
                ambientColor = glow.copy(alpha = 0.30f),
                spotColor = glow.copy(alpha = 0.30f),
            )
            .background(
                brush = Brush.linearGradient(
                    colors = if (enabled) gradientColors
                    else listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Web containedPrimary/Secondary contrastText is near-black.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides Color(0xFF080808),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                content = content,
            )
        }
    }
}

// ── Chip ─────────────────────────────────────────────────────────────────────

/**
 * Web PraxisChip: translucent tint of the accent — bg alpha .12, border
 * alpha .25, bold small label.
 */
@Composable
fun PraxisChip(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
    }
}

// ── Micro-labels / empty states ──────────────────────────────────────────────

/** Web chrome micro-label: uppercase bold mono with wide tracking. */
@Composable
fun PraxisSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = LabelMonoSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun PraxisEmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

/**
 * Web TopBar parity for native screens: 48dp row on bg/90 with a 1px bottom
 * border, back chevron, and a Jakarta-semibold title.
 */
@Composable
fun PraxisTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = Modifier.padding(top = 32.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = if (onBack != null) 4.dp else 12.dp, end = 8.dp),
            )
            Box(Modifier.weight(1f))
            trailing()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
    }
}
