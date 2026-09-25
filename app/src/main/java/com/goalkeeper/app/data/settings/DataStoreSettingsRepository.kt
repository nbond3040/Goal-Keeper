package com.goalkeeper.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** One DataStore per process: the delegate must stay top-level and single. */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val store = context.applicationContext.settingsDataStore

    override val settings: Flow<AppSettings> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setColorway(colorway: Colorway) {
        store.edit { it[Keys.colorway] = colorway.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.themeMode] = mode.name }
    }

    override suspend fun setDefaultReminderMinute(minute: Int) {
        store.edit { it[Keys.defaultReminderMinute] = minute.coerceIn(0, LAST_MINUTE) }
    }

    override suspend fun setBriefing(enabled: Boolean, minute: Int) {
        store.edit {
            it[Keys.briefingEnabled] = enabled
            it[Keys.briefingMinute] = minute.coerceIn(0, LAST_MINUTE)
        }
    }

    override suspend fun setNotificationsPrompted(prompted: Boolean) {
        store.edit { it[Keys.notificationsPrompted] = prompted }
    }

    override suspend fun setExactAlarmHintDismissed(dismissed: Boolean) {
        store.edit { it[Keys.exactAlarmHintDismissed] = dismissed }
    }

    override suspend fun exportMap(): Map<String, String> = current().toExportMap()

    override suspend fun importMap(values: Map<String, String>) {
        val parsed = parseSettingsMap(values)
        store.edit { prefs ->
            parsed.colorway?.let { prefs[Keys.colorway] = it.name }
            parsed.themeMode?.let { prefs[Keys.themeMode] = it.name }
            parsed.defaultReminderMinute?.let { prefs[Keys.defaultReminderMinute] = it }
            parsed.briefingEnabled?.let { prefs[Keys.briefingEnabled] = it }
            parsed.briefingMinute?.let { prefs[Keys.briefingMinute] = it }
        }
    }

    private object Keys {
        val colorway = stringPreferencesKey(SettingsKeys.COLORWAY)
        val themeMode = stringPreferencesKey(SettingsKeys.THEME_MODE)
        val defaultReminderMinute = intPreferencesKey(SettingsKeys.DEFAULT_REMINDER_MINUTE)
        val briefingEnabled = booleanPreferencesKey(SettingsKeys.BRIEFING_ENABLED)
        val briefingMinute = intPreferencesKey(SettingsKeys.BRIEFING_MINUTE)
        val notificationsPrompted = booleanPreferencesKey("notifications_prompted")
        val exactAlarmHintDismissed = booleanPreferencesKey("exact_alarm_hint_dismissed")
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            colorway = this[Keys.colorway]?.let(::colorwayOrNull) ?: defaults.colorway,
            themeMode = this[Keys.themeMode]?.let(::themeModeOrNull) ?: defaults.themeMode,
            defaultReminderMinute = this[Keys.defaultReminderMinute]?.coerceIn(0, LAST_MINUTE) ?: defaults.defaultReminderMinute,
            briefingEnabled = this[Keys.briefingEnabled] ?: defaults.briefingEnabled,
            briefingMinute = this[Keys.briefingMinute]?.coerceIn(0, LAST_MINUTE) ?: defaults.briefingMinute,
            notificationsPrompted = this[Keys.notificationsPrompted] ?: defaults.notificationsPrompted,
            exactAlarmHintDismissed = this[Keys.exactAlarmHintDismissed] ?: defaults.exactAlarmHintDismissed,
        )
    }
}

private const val LAST_MINUTE = 24 * 60 - 1

/** Stable keys shared by DataStore and the backup file's settings map. */
internal object SettingsKeys {
    const val COLORWAY = "colorway"
    const val THEME_MODE = "theme_mode"
    const val DEFAULT_REMINDER_MINUTE = "default_reminder_minute"
    const val BRIEFING_ENABLED = "briefing_enabled"
    const val BRIEFING_MINUTE = "briefing_minute"
}

/** Settings values read from a backup; null means absent or invalid, i.e. leave the current value alone. */
internal data class ImportedSettings(
    val colorway: Colorway?,
    val themeMode: ThemeMode?,
    val defaultReminderMinute: Int?,
    val briefingEnabled: Boolean?,
    val briefingMinute: Int?,
)

internal fun parseSettingsMap(values: Map<String, String>): ImportedSettings = ImportedSettings(
    colorway = values[SettingsKeys.COLORWAY]?.let(::colorwayOrNull),
    themeMode = values[SettingsKeys.THEME_MODE]?.let(::themeModeOrNull),
    defaultReminderMinute = values[SettingsKeys.DEFAULT_REMINDER_MINUTE]?.toIntOrNull()?.takeIf { it in 0..LAST_MINUTE },
    briefingEnabled = values[SettingsKeys.BRIEFING_ENABLED]?.toBooleanStrictOrNull(),
    briefingMinute = values[SettingsKeys.BRIEFING_MINUTE]?.toIntOrNull()?.takeIf { it in 0..LAST_MINUTE },
)

/** The user-facing preferences worth carrying to another phone (one-time prompt flags stay behind). */
internal fun AppSettings.toExportMap(): Map<String, String> = mapOf(
    SettingsKeys.COLORWAY to colorway.name,
    SettingsKeys.THEME_MODE to themeMode.name,
    SettingsKeys.DEFAULT_REMINDER_MINUTE to defaultReminderMinute.toString(),
    SettingsKeys.BRIEFING_ENABLED to briefingEnabled.toString(),
    SettingsKeys.BRIEFING_MINUTE to briefingMinute.toString(),
)

private fun colorwayOrNull(name: String): Colorway? = Colorway.entries.firstOrNull { it.name == name }

private fun themeModeOrNull(name: String): ThemeMode? = ThemeMode.entries.firstOrNull { it.name == name }
