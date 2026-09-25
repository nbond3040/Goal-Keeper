package com.goalkeeper.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.goalkeeper.app.R

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private fun grotesk(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val GkTypography = Typography(
    displayLarge = grotesk(52, 56, FontWeight.Bold, -0.02),
    displayMedium = grotesk(44, 48, FontWeight.Bold, -0.02),
    displaySmall = grotesk(36, 40, FontWeight.Bold, -0.015),
    headlineLarge = grotesk(32, 38, FontWeight.Bold, -0.01),
    headlineMedium = grotesk(28, 34, FontWeight.Bold, -0.01),
    headlineSmall = grotesk(24, 30, FontWeight.Bold),
    titleLarge = grotesk(22, 28, FontWeight.Bold),
    titleMedium = grotesk(16, 22, FontWeight.SemiBold),
    titleSmall = grotesk(14, 20, FontWeight.SemiBold),
    bodyLarge = grotesk(16, 24, FontWeight.Normal),
    bodyMedium = grotesk(15, 22, FontWeight.Normal),
    bodySmall = grotesk(13, 18, FontWeight.Normal),
    labelLarge = grotesk(14, 20, FontWeight.SemiBold),
    labelMedium = grotesk(12, 16, FontWeight.Medium),
    labelSmall = grotesk(11, 16, FontWeight.Medium),
)

/** The monospace "console" styles used for numbers, labels and metadata. */
@Immutable
data class GkTypeExtras(
    /** Uppercase section labels like "PRIORITY QUEUE" (callers uppercase the text). */
    val label: TextStyle,
    /** Small metadata lines like "42d streak · nudge 18:00". */
    val meta: TextStyle,
    /** Tiny numbers inside rings and grids. */
    val small: TextStyle,
    /** Stat tile values like "2/5" or "118". */
    val value: TextStyle,
    /** Larger stat values. */
    val valueLarge: TextStyle,
    /** The big streak number. */
    val hero: TextStyle,
)

val GkMonoType = GkTypeExtras(
    label = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.14.em),
    meta = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 16.sp),
    small = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    value = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    valueLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    hero = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 84.sp, lineHeight = 88.sp, letterSpacing = (-0.04).em),
)
