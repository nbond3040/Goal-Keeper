package com.goalkeeper.app.data.settings

import kotlinx.coroutines.flow.Flow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** Accent color themes. The enum name is persisted — never rename, only append. */
enum class Colorway(val displayName: String) {
    BOLD_BLUE("Bold Blue"),
    MIDNIGHT_NAVY("Midnight Navy"),
    GLACIER_TEAL("Glacier Teal"),
    EMBER_ORANGE("Ember Orange"),
    CRIMSON("Crimson"),
    EMERALD("Emerald"),
    ROYAL_VIOLET("Royal Violet"),
    GRAPHITE("Graphite"),
}

data class AppSettings(
    val colorway: Colorway = Colorway.BOLD_BLUE,
    /** Midnight Console is dark-first. */
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Pre-filled reminder start time for new goals, minutes after midnight. */
    val defaultReminderMinute: Int = 18 * 60,
    /** Optional morning summary notification. */
    val briefingEnabled: Boolean = false,
    val briefingMinute: Int = 8 * 60,
    /** Whether the app already asked for the notification permission once. */
    val notificationsPrompted: Boolean = false,
    /** Whether the user dismissed the "allow exact alarms" hint. */
    val exactAlarmHintDismissed: Boolean = false,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings

    suspend fun setColorway(colorway: Colorway)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDefaultReminderMinute(minute: Int)

    suspend fun setBriefing(enabled: Boolean, minute: Int)

    suspend fun setNotificationsPrompted(prompted: Boolean)

    suspend fun setExactAlarmHintDismissed(dismissed: Boolean)

    /** Settings as plain strings for backups (keys are stable). */
    suspend fun exportMap(): Map<String, String>

    /** Restores what [exportMap] produced; unknown keys and bad values are ignored. */
    suspend fun importMap(values: Map<String, String>)
}
