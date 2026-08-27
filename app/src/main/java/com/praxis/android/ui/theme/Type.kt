@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.praxis.android.ui.theme

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import app.praxisweb.xyz.R

/**
 * Webapp type ramp parity (client/src/index.tsx): Inter for body text,
 * Plus Jakarta Sans for display/headings, JetBrains Mono for the chrome
 * micro-labels. PJS and JB Mono ship as variable TTFs — the wght axis is
 * applied through FontVariation on API 26+; older devices fall back to
 * weight-matched defaults.
 */

private fun variableFont(resId: Int, weight: FontWeight): Font =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Font(resId, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    } else {
        Font(resId, weight = weight)
    }

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    variableFont(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    variableFont(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    variableFont(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    variableFont(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
    variableFont(R.font.inter_extrabold, FontWeight.ExtraBold),
)

val JakartaFamily = FontFamily(
    variableFont(R.font.pjs_variable, FontWeight.SemiBold),
    variableFont(R.font.pjs_variable, FontWeight.Bold),
    variableFont(R.font.pjs_variable, FontWeight.ExtraBold),
)

val MonoFamily = FontFamily(
    variableFont(R.font.jbmono_variable, FontWeight.Normal),
    variableFont(R.font.jbmono_variable, FontWeight.Medium),
    variableFont(R.font.jbmono_variable, FontWeight.Bold),
)

/** Webapp chrome micro-labels: uppercase mono, wide tracking, bold. */
val LabelMonoSmall = TextStyle(
    fontFamily = MonoFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    letterSpacing = 1.2.sp,
)

val Typography = Typography(
    // h1 — display, ExtraBold, tight tracking
    displayLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.9).sp
    ),
    // h2
    displayMedium = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.5).sp
    ),
    // h3
    headlineLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    // h4
    headlineMedium = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    // h5
    titleLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // h6
    titleMedium = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    // body1 — Inter, 1.6 line height, +0.01em tracking
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    // body2
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    // caption — +0.02em tracking
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    // button — semibold, sentence case, r10 handled in PraxisButton
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    ),
)
