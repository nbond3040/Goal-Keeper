package com.goalkeeper.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.goalkeeper.app.data.settings.Colorway

/**
 * The Midnight Console palette. Neutrals are shared by every colorway; only the accent changes.
 *
 * - [background] screen, [card] panels, [raised] segmented controls / nav pill / chips
 * - [text] primary copy, [muted] secondary copy, [subtle] tertiary/disabled, [border] 1dp outlines
 * - [accent] fills (buttons, done cells), [onAccent] content on fills, [accentText] accent-colored text and
 *   icons on neutral surfaces, [accentContainer] tinted backgrounds, [onAccentContainer] content on them
 */
@Immutable
data class GkColors(
    val background: Color,
    val card: Color,
    val raised: Color,
    val text: Color,
    val muted: Color,
    val subtle: Color,
    val border: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val danger: Color,
    val isDark: Boolean,
)

private data class Neutrals(
    val background: Color,
    val card: Color,
    val raised: Color,
    val text: Color,
    val muted: Color,
    val subtle: Color,
    val border: Color,
    val danger: Color,
)

private data class Accent(
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
)

private val LightNeutrals = Neutrals(
    background = Color(0xFFEDF0F4),
    card = Color(0xFFFFFFFF),
    raised = Color(0xFFE2E6EC),
    text = Color(0xFF12161D),
    muted = Color(0xFF586170),
    subtle = Color(0xFF6B7585),
    border = Color(0xFFCDD3DC),
    danger = Color(0xFFC62828),
)

private val DarkNeutrals = Neutrals(
    background = Color(0xFF0D1015),
    card = Color(0xFF171B22),
    raised = Color(0xFF232934),
    text = Color(0xFFEEF1F6),
    muted = Color(0xFF9AA3B2),
    subtle = Color(0xFF7E8898),
    border = Color(0xFF2E3542),
    danger = Color(0xFFFF6B6B),
)

private fun accent(accent: Long, onAccent: Long, accentText: Long, container: Long, onContainer: Long) = Accent(
    accent = Color(accent),
    onAccent = Color(onAccent),
    accentText = Color(accentText),
    accentContainer = Color(container),
    onAccentContainer = Color(onContainer),
)

/** Light and dark accents per colorway (first = light, second = dark). */
private val Accents: Map<Colorway, Pair<Accent, Accent>> = mapOf(
    Colorway.BOLD_BLUE to (
        accent(0xFF1D4ED8, 0xFFFFFFFF, 0xFF1D4ED8, 0xFFDCE5FF, 0xFF0F2A78) to
            accent(0xFF3563F0, 0xFFFFFFFF, 0xFF8FAAFF, 0xFF1B2850, 0xFFD6E0FF)
        ),
    Colorway.MIDNIGHT_NAVY to (
        accent(0xFF1E3A8A, 0xFFFFFFFF, 0xFF1E3A8A, 0xFFDDE3F4, 0xFF152A63) to
            accent(0xFF3050B0, 0xFFFFFFFF, 0xFFA3B6EE, 0xFF18223F, 0xFFD8E0FA)
        ),
    Colorway.GLACIER_TEAL to (
        accent(0xFF0E7490, 0xFFFFFFFF, 0xFF0E7490, 0xFFD3EFF5, 0xFF08475A) to
            accent(0xFF0B7F9C, 0xFFFFFFFF, 0xFF72D2EA, 0xFF0D3440, 0xFFCDF1FA)
        ),
    Colorway.EMBER_ORANGE to (
        accent(0xFFC2410C, 0xFFFFFFFF, 0xFFC2410C, 0xFFFFE3D5, 0xFF6A2208) to
            accent(0xFFC8480C, 0xFFFFFFFF, 0xFFFF9C6B, 0xFF43210F, 0xFFFFE0D0)
        ),
    Colorway.CRIMSON to (
        accent(0xFFBE123C, 0xFFFFFFFF, 0xFFBE123C, 0xFFFFE0E7, 0xFF6B0A22) to
            accent(0xFFD11F4A, 0xFFFFFFFF, 0xFFFF8EA8, 0xFF45131F, 0xFFFFDCE4)
        ),
    Colorway.EMERALD to (
        accent(0xFF047857, 0xFFFFFFFF, 0xFF047857, 0xFFD2F4E5, 0xFF044A36) to
            accent(0xFF08825F, 0xFFFFFFFF, 0xFF5FDDB2, 0xFF0E3A2D, 0xFFCFF7E7)
        ),
    Colorway.ROYAL_VIOLET to (
        accent(0xFF6D28D9, 0xFFFFFFFF, 0xFF6D28D9, 0xFFEADFFF, 0xFF3A1580) to
            accent(0xFF7445E6, 0xFFFFFFFF, 0xFFC3AEFF, 0xFF2C1F52, 0xFFE8DEFF)
        ),
    Colorway.GRAPHITE to (
        accent(0xFF2B313A, 0xFFFFFFFF, 0xFF2B313A, 0xFFE0E3E8, 0xFF1B1F25) to
            accent(0xFFD5DAE1, 0xFF12161D, 0xFFD5DAE1, 0xFF2A3039, 0xFFE6E9ED)
        ),
)

fun gkColors(colorway: Colorway, dark: Boolean): GkColors {
    val n = if (dark) DarkNeutrals else LightNeutrals
    val pair = Accents.getValue(colorway)
    val a = if (dark) pair.second else pair.first
    return GkColors(
        background = n.background,
        card = n.card,
        raised = n.raised,
        text = n.text,
        muted = n.muted,
        subtle = n.subtle,
        border = n.border,
        accent = a.accent,
        onAccent = a.onAccent,
        accentText = a.accentText,
        accentContainer = a.accentContainer,
        onAccentContainer = a.onAccentContainer,
        danger = n.danger,
        isDark = dark,
    )
}
