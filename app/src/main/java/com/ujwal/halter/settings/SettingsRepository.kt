// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ujwal.halter.data.LimitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.halterDataStore by preferencesDataStore("halter_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<HalterSettings> = context.halterDataStore.data.map { preferences ->
        HalterSettings(
            breathingTotalDurationSeconds = preferences[Keys.breathingTotalDurationSeconds] ?: 15,
            breathingInhaleSeconds = preferences[Keys.breathingInhaleSeconds] ?: 4,
            breathingHoldInSeconds = preferences[Keys.breathingHoldInSeconds] ?: 2,
            breathingExhaleSeconds = preferences[Keys.breathingExhaleSeconds] ?: 4,
            breathingHoldOutSeconds = preferences[Keys.breathingHoldOutSeconds] ?: 2,
            allowSkipBreathing = preferences[Keys.allowSkipBreathing] ?: false,
            allowCustomSessionLimit = preferences[Keys.allowCustomSessionLimit] ?: false,
            hapticsEnabled = preferences[Keys.hapticsEnabled] ?: true,
            reflectionPromptEnabled = preferences[Keys.reflectionPromptEnabled] ?: true,
            defaultSessionLimitPresetsMinutes = parseIntList(preferences[Keys.defaultSessionLimitPresetsMinutes], listOf(5, 10, 15, 30)),
            defaultSessionLimitPresetsScrolls = parseIntList(preferences[Keys.defaultSessionLimitPresetsScrolls], listOf(10, 25, 50, 100)),
            defaultLimitType = enumPreference(preferences[Keys.defaultLimitType], LimitType.SCROLL_COUNT),
            scrollDebounceMillis = preferences[Keys.scrollDebounceMillis] ?: 400,
            averageScrollDwellSeconds = preferences[Keys.averageScrollDwellSeconds] ?: 6,
            strictModeGlobalDefault = preferences[Keys.strictModeGlobalDefault] ?: false,
            breathingGateGlobalDefault = preferences[Keys.breathingGateGlobalDefault] ?: true,
            blockShortVideoGlobalDefault = preferences[Keys.blockShortVideoGlobalDefault] ?: false,
            excludeFromFocusGlobalDefault = preferences[Keys.excludeFromFocusGlobalDefault] ?: false,
            defaultDailyTimeLimitMinutes = preferences[Keys.defaultDailyTimeLimitMinutes]?.takeIf { it > 0 },
            defaultSessionTimeLimitMinutes = preferences[Keys.defaultSessionTimeLimitMinutes]?.takeIf { it > 0 },
            defaultScrollLimitPerSession = preferences[Keys.defaultScrollLimitPerSession]?.takeIf { it > 0 },
            defaultHoldToOpenSeconds = preferences[Keys.defaultHoldToOpenSeconds]?.takeIf { it > 0 },
    
            useDynamicWallpaperColor = preferences[Keys.useDynamicWallpaperColor] ?: true,
            customSeedColorArgb = preferences[Keys.customSeedColorArgb],
            colorIntensity = enumPreference(preferences[Keys.colorIntensity], ColorIntensity.NEUTRAL),
            darkModePreference = enumPreference(preferences[Keys.darkModePreference], DarkModePreference.SYSTEM),
            blurEffectsEnabled = preferences[Keys.blurEffectsEnabled] ?: true,
            cornerRadiusScale = preferences[Keys.cornerRadiusScale] ?: 1.0f,
            defaultFocusSessionMinutes = preferences[Keys.defaultFocusSessionMinutes] ?: 25,
            journalPromptEnabled = preferences[Keys.journalPromptEnabled] ?: false,
            journalPromptFrequency = enumPreference(preferences[Keys.journalPromptFrequency], JournalFrequency.ONCE_DAILY),
            requirePasswordForSettingsChanges = preferences[Keys.requirePasswordForSettingsChanges] ?: false,
            settingsPasswordHash = preferences[Keys.settingsPasswordHash],
            customScrollPackages = preferences[Keys.customScrollPackages] ?: "",
            sessionCooldownEnabled = preferences[Keys.sessionCooldownEnabled] ?: true,
            sessionCooldownMinutes = preferences[Keys.sessionCooldownMinutes] ?: 5,

            siteBlockingEnabled = preferences[Keys.siteBlockingEnabled] ?: false,
            siteBlockedList = preferences[Keys.siteBlockedList] ?: "",
            keywordBlockingEnabled = preferences[Keys.keywordBlockingEnabled] ?: false,
            keywordBlockedList = preferences[Keys.keywordBlockedList] ?: "",
            bedtimeEnabled = preferences[Keys.bedtimeEnabled] ?: false,
            bedtimeStartHour = preferences[Keys.bedtimeStartHour] ?: 22,
            bedtimeStartMinute = preferences[Keys.bedtimeStartMinute] ?: 0,
            bedtimeEndHour = preferences[Keys.bedtimeEndHour] ?: 6,
            bedtimeEndMinute = preferences[Keys.bedtimeEndMinute] ?: 0,
            greyscaleEnabled = preferences[Keys.greyscaleEnabled] ?: false
        )
    }

    suspend fun updateInt(key: String, value: Int) {
        context.halterDataStore.edit { it[intPreferencesKey(key)] = value }
    }

    suspend fun updateBoolean(key: String, value: Boolean) {
        context.halterDataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    suspend fun updateFloat(key: String, value: Float) {
        context.halterDataStore.edit { it[floatPreferencesKey(key)] = value }
    }

    suspend fun updateString(key: String, value: String?) {
        context.halterDataStore.edit { preferences ->
            val preferenceKey = stringPreferencesKey(key)
            if (value == null) preferences.remove(preferenceKey) else preferences[preferenceKey] = value
        }
    }

    suspend fun updateLong(key: String, value: Long) {
        context.halterDataStore.edit { it[longPreferencesKey(key)] = value }
    }

    suspend fun getLong(key: String, fallback: Long): Long {
        return context.halterDataStore.data.map { it[longPreferencesKey(key)] ?: fallback }.first()
    }

    /** Returns the epoch-millis of the very first app launch, initializing it if not set. */
    suspend fun firstInstallEpochMillis(): Long {
        val existing = context.halterDataStore.data.map { it[Keys.firstInstallEpochMillis] }.first()
        if (existing != null) return existing
        val now = System.currentTimeMillis()
        context.halterDataStore.edit { it[Keys.firstInstallEpochMillis] = now }
        return now
    }

    private fun parseIntList(value: String?, fallback: List<Int>): List<Int> =
        value?.split(",")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.isNotEmpty() } ?: fallback

    private inline fun <reified T : Enum<T>> enumPreference(value: String?, fallback: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    object Names {
        const val firstInstallEpochMillis = "first_install_epoch_millis"
        const val breathingTotalDurationSeconds = "breathing_total_duration_seconds"
        const val breathingInhaleSeconds = "breathing_inhale_seconds"
        const val breathingHoldInSeconds = "breathing_hold_in_seconds"
        const val breathingExhaleSeconds = "breathing_exhale_seconds"
        const val breathingHoldOutSeconds = "breathing_hold_out_seconds"
        const val allowSkipBreathing = "allow_skip_breathing"
        const val allowCustomSessionLimit = "allow_custom_session_limit"
        const val hapticsEnabled = "haptics_enabled"
        const val reflectionPromptEnabled = "reflection_prompt_enabled"
        const val scrollDebounceMillis = "scroll_debounce_millis"
        const val averageScrollDwellSeconds = "average_scroll_dwell_seconds"
        const val strictModeGlobalDefault = "strict_mode_global_default"
        const val breathingGateGlobalDefault = "breathing_gate_global_default"
        const val blockShortVideoGlobalDefault = "block_short_video_global_default"
        const val excludeFromFocusGlobalDefault = "exclude_from_focus_global_default"
        const val defaultDailyTimeLimitMinutes = "default_daily_time_limit_minutes"
        const val defaultSessionTimeLimitMinutes = "default_session_time_limit_minutes"
        const val defaultScrollLimitPerSession = "default_scroll_limit_per_session"
        const val defaultHoldToOpenSeconds = "default_hold_to_open_seconds"

        const val useDynamicWallpaperColor = "use_dynamic_wallpaper_color"
        const val customSeedColorArgb = "custom_seed_color_argb"
        const val colorIntensity = "color_intensity"
        const val darkModePreference = "dark_mode_preference"
        const val blurEffectsEnabled = "blur_effects_enabled"
        const val cornerRadiusScale = "corner_radius_scale"
        const val defaultFocusSessionMinutes = "default_focus_session_minutes"
        const val journalPromptEnabled = "journal_prompt_enabled"
        const val journalPromptFrequency = "journal_prompt_frequency"
        const val requirePasswordForSettingsChanges = "require_password_for_settings_changes"
        const val settingsPasswordHash = "settings_password_hash"
        const val customScrollPackages = "custom_scroll_packages"
        const val sessionCooldownEnabled = "session_cooldown_enabled"
        const val sessionCooldownMinutes = "session_cooldown_minutes"

        const val siteBlockingEnabled = "site_blocking_enabled"
        const val siteBlockedList = "site_blocked_list"
        const val keywordBlockingEnabled = "keyword_blocking_enabled"
        const val keywordBlockedList = "keyword_blocked_list"
        const val bedtimeEnabled = "bedtime_enabled"
        const val bedtimeStartHour = "bedtime_start_hour"
        const val bedtimeStartMinute = "bedtime_start_minute"
        const val bedtimeEndHour = "bedtime_end_hour"
        const val bedtimeEndMinute = "bedtime_end_minute"
        const val greyscaleEnabled = "greyscale_enabled"
        const val donate_shown_timestamps = "donate_shown_timestamps"
        const val donate_dismissed_until_epoch_ms = "donate_dismissed_until_epoch_ms"
        const val app_open_count = "app_open_count"
    }

    private object Keys {
        val firstInstallEpochMillis = longPreferencesKey(Names.firstInstallEpochMillis)
        val breathingTotalDurationSeconds = intPreferencesKey(Names.breathingTotalDurationSeconds)
        val breathingInhaleSeconds = intPreferencesKey(Names.breathingInhaleSeconds)
        val breathingHoldInSeconds = intPreferencesKey(Names.breathingHoldInSeconds)
        val breathingExhaleSeconds = intPreferencesKey(Names.breathingExhaleSeconds)
        val breathingHoldOutSeconds = intPreferencesKey(Names.breathingHoldOutSeconds)
        val allowSkipBreathing = booleanPreferencesKey(Names.allowSkipBreathing)
        val allowCustomSessionLimit = booleanPreferencesKey(Names.allowCustomSessionLimit)
        val hapticsEnabled = booleanPreferencesKey(Names.hapticsEnabled)
        val reflectionPromptEnabled = booleanPreferencesKey(Names.reflectionPromptEnabled)
        val defaultSessionLimitPresetsMinutes = stringPreferencesKey("default_session_limit_presets_minutes")
        val defaultSessionLimitPresetsScrolls = stringPreferencesKey("default_session_limit_presets_scrolls")
        val defaultLimitType = stringPreferencesKey("default_limit_type")
        val scrollDebounceMillis = intPreferencesKey(Names.scrollDebounceMillis)
        val averageScrollDwellSeconds = intPreferencesKey(Names.averageScrollDwellSeconds)
        val strictModeGlobalDefault = booleanPreferencesKey(Names.strictModeGlobalDefault)
        val breathingGateGlobalDefault = booleanPreferencesKey(Names.breathingGateGlobalDefault)
        val blockShortVideoGlobalDefault = booleanPreferencesKey(Names.blockShortVideoGlobalDefault)
        val excludeFromFocusGlobalDefault = booleanPreferencesKey(Names.excludeFromFocusGlobalDefault)
        val defaultDailyTimeLimitMinutes = intPreferencesKey(Names.defaultDailyTimeLimitMinutes)
        val defaultSessionTimeLimitMinutes = intPreferencesKey(Names.defaultSessionTimeLimitMinutes)
        val defaultScrollLimitPerSession = intPreferencesKey(Names.defaultScrollLimitPerSession)
        val defaultHoldToOpenSeconds = intPreferencesKey(Names.defaultHoldToOpenSeconds)

        val useDynamicWallpaperColor = booleanPreferencesKey(Names.useDynamicWallpaperColor)
        val customSeedColorArgb = intPreferencesKey(Names.customSeedColorArgb)
        val colorIntensity = stringPreferencesKey(Names.colorIntensity)
        val darkModePreference = stringPreferencesKey(Names.darkModePreference)
        val blurEffectsEnabled = booleanPreferencesKey(Names.blurEffectsEnabled)
        val cornerRadiusScale = floatPreferencesKey(Names.cornerRadiusScale)
        val defaultFocusSessionMinutes = intPreferencesKey(Names.defaultFocusSessionMinutes)
        val journalPromptEnabled = booleanPreferencesKey(Names.journalPromptEnabled)
        val journalPromptFrequency = stringPreferencesKey(Names.journalPromptFrequency)
        val requirePasswordForSettingsChanges = booleanPreferencesKey(Names.requirePasswordForSettingsChanges)
        val settingsPasswordHash = stringPreferencesKey(Names.settingsPasswordHash)
        val customScrollPackages = stringPreferencesKey(Names.customScrollPackages)
        val sessionCooldownEnabled = booleanPreferencesKey(Names.sessionCooldownEnabled)
        val sessionCooldownMinutes = intPreferencesKey(Names.sessionCooldownMinutes)

        val siteBlockingEnabled = booleanPreferencesKey(Names.siteBlockingEnabled)
        val siteBlockedList = stringPreferencesKey(Names.siteBlockedList)
        val keywordBlockingEnabled = booleanPreferencesKey(Names.keywordBlockingEnabled)
        val keywordBlockedList = stringPreferencesKey(Names.keywordBlockedList)
        val bedtimeEnabled = booleanPreferencesKey(Names.bedtimeEnabled)
        val bedtimeStartHour = intPreferencesKey(Names.bedtimeStartHour)
        val bedtimeStartMinute = intPreferencesKey(Names.bedtimeStartMinute)
        val bedtimeEndHour = intPreferencesKey(Names.bedtimeEndHour)
        val bedtimeEndMinute = intPreferencesKey(Names.bedtimeEndMinute)
        val greyscaleEnabled = booleanPreferencesKey(Names.greyscaleEnabled)
        val donate_shown_timestamps = stringPreferencesKey(Names.donate_shown_timestamps)
        val donate_dismissed_until_epoch_ms = longPreferencesKey(Names.donate_dismissed_until_epoch_ms)
        val app_open_count = longPreferencesKey(Names.app_open_count)
    }
    
    suspend fun getString(key: String, fallback: String): String {
        return context.halterDataStore.data.map { it[stringPreferencesKey(key)] ?: fallback }.first()
    }

}
