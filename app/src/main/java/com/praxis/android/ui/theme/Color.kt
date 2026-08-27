package com.praxis.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Webapp palette parity — client/src/index.tsx "Premium Dark Theme".
 *   bg #080808  <  surface #111111  <  raised #1a1a1a   (border #222222)
 * primary amber #F59E0B (light #FCD34D, dark #D97706), secondary electric
 * violet #8B5CF6, text #EEEEEE/#AAAAAA. Every M3 slot that the Compose
 * baseline would otherwise fill with off-brand purple-grays is pinned here:
 * filled cards resolve to surfaceContainerHighest, chips/pills to *Container,
 * dividers and text-field outlines to outlineVariant.
 */
object PraxisColors {
    val Bg = Color(0xFF080808)
    val Surface = Color(0xFF111111)
    val Raised = Color(0xFF1A1A1A)
    val Border = Color(0xFF222222)
    val BorderSoft = Color(0xFF2A2A2A)

    val Amber = Color(0xFFF59E0B)
    val AmberLight = Color(0xFFFCD34D)
    val AmberDark = Color(0xFFD97706)
    val Violet = Color(0xFF8B5CF6)
    val VioletLight = Color(0xFFA78BFA)
    val VioletDark = Color(0xFF6D28D9)

    val Success = Color(0xFF10B981) // web MUI success
    val Info = Color(0xFF3B82F6)    // web MUI info
    val ErrorRed = Color(0xFFEF4444)
    val TextPrimary = Color(0xFFEEEEEE)
    val TextSecondary = Color(0xFFAAAAAA)
    val TextDisabled = Color(0xFF6B7280)

    /** Webapp default-avatar gradient: linear-gradient(135deg, amber→violet). */
    val AvatarGradient = listOf(Amber, Violet)
}

private val DarkColorScheme = darkColorScheme(
    primary = PraxisColors.Amber,
    onPrimary = PraxisColors.Bg,                       // web contrastText #080808
    primaryContainer = Color(0xFF3D2F18),              // rgba(245,158,11,.16) on raised
    onPrimaryContainer = PraxisColors.AmberLight,
    inversePrimary = PraxisColors.AmberDark,

    secondary = PraxisColors.Violet,
    onSecondary = Color.White,                         // web contrastText #ffffff
    secondaryContainer = Color(0xFF2C2544),            // rgba(139,92,246,.16) on raised
    onSecondaryContainer = PraxisColors.VioletLight,

    tertiary = PraxisColors.Violet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2C2544),
    onTertiaryContainer = PraxisColors.VioletLight,

    background = PraxisColors.Bg,
    onBackground = PraxisColors.TextPrimary,
    surface = PraxisColors.Surface,
    onSurface = PraxisColors.TextPrimary,
    surfaceVariant = PraxisColors.Raised,
    onSurfaceVariant = PraxisColors.TextSecondary,
    surfaceDim = PraxisColors.Bg,
    surfaceBright = PraxisColors.Raised,
    surfaceContainerLowest = PraxisColors.Bg,
    surfaceContainerLow = Color(0xFF0F0F0F),           // web input bg
    surfaceContainer = PraxisColors.Surface,
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = PraxisColors.Surface,    // filled Card == web Paper

    outline = PraxisColors.Border,                     // 1px card border
    outlineVariant = PraxisColors.BorderSoft,          // text-field fieldset / dividers

    error = PraxisColors.ErrorRed,
    onError = PraxisColors.Bg,
    errorContainer = Color(0xFF3B1515),
    onErrorContainer = Color(0xFFFCA5A5),

    inverseSurface = PraxisColors.TextPrimary,
    inverseOnSurface = PraxisColors.Bg,
    scrim = Color.Black,
)

/**
 * The shell is always dark (PraxisApp sets MODE_NIGHT_YES; the WebView shell
 * forces algorithmic darkening too), so darkTheme defaults to true instead of
 * following the system.
 */
@Composable
fun PraxisTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
