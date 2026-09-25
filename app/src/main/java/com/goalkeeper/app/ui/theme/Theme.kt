package com.goalkeeper.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.data.settings.ThemeMode

private val LocalGkColors = staticCompositionLocalOf { gkColors(Colorway.BOLD_BLUE, dark = true) }
private val LocalGkType = staticCompositionLocalOf { GkMonoType }

/** Access to the Midnight Console tokens that Material's ColorScheme doesn't cover. */
object GkTheme {
    val colors: GkColors
        @Composable @ReadOnlyComposable get() = LocalGkColors.current

    val mono: GkTypeExtras
        @Composable @ReadOnlyComposable get() = LocalGkType.current
}

val GkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun resolveDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun GoalKeeperTheme(
    colorway: Colorway = Colorway.BOLD_BLUE,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = remember(colorway, darkTheme) { gkColors(colorway, darkTheme) }
    val scheme = remember(colors) { colors.toColorScheme() }
    CompositionLocalProvider(
        LocalGkColors provides colors,
        LocalGkType provides GkMonoType,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = GkTypography,
            shapes = GkShapes,
            content = content,
        )
    }
}

private fun GkColors.toColorScheme(): ColorScheme {
    val errorContainer = if (isDark) Color(0xFF4A1C1C) else Color(0xFFFFDAD6)
    val onErrorContainer = if (isDark) Color(0xFFFFDAD6) else Color(0xFF410002)
    return if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accentContainer,
            onPrimaryContainer = onAccentContainer,
            inversePrimary = accentText,
            secondary = subtle,
            onSecondary = background,
            secondaryContainer = raised,
            onSecondaryContainer = text,
            tertiary = accentText,
            onTertiary = background,
            tertiaryContainer = accentContainer,
            onTertiaryContainer = onAccentContainer,
            background = background,
            onBackground = text,
            surface = background,
            onSurface = text,
            surfaceVariant = raised,
            onSurfaceVariant = muted,
            surfaceTint = accent,
            inverseSurface = text,
            inverseOnSurface = background,
            error = danger,
            onError = Color.Black,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = border,
            outlineVariant = border,
            scrim = Color.Black,
            surfaceBright = raised,
            surfaceDim = background,
            surfaceContainerLowest = background,
            surfaceContainerLow = card,
            surfaceContainer = card,
            surfaceContainerHigh = raised,
            surfaceContainerHighest = raised,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accentContainer,
            onPrimaryContainer = onAccentContainer,
            inversePrimary = accentContainer,
            secondary = subtle,
            onSecondary = card,
            secondaryContainer = raised,
            onSecondaryContainer = text,
            tertiary = accentText,
            onTertiary = card,
            tertiaryContainer = accentContainer,
            onTertiaryContainer = onAccentContainer,
            background = background,
            onBackground = text,
            surface = background,
            onSurface = text,
            surfaceVariant = raised,
            onSurfaceVariant = muted,
            surfaceTint = accent,
            inverseSurface = text,
            inverseOnSurface = background,
            error = danger,
            onError = Color.White,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = border,
            outlineVariant = border,
            scrim = Color.Black,
            surfaceBright = card,
            surfaceDim = raised,
            surfaceContainerLowest = card,
            surfaceContainerLow = card,
            surfaceContainer = card,
            surfaceContainerHigh = raised,
            surfaceContainerHighest = raised,
        )
    }
}
