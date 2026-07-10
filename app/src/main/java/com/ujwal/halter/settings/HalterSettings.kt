// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.settings

import com.ujwal.halter.data.LimitType

data class HalterSettings(
    val breathingTotalDurationSeconds: Int = 15,
    val breathingInhaleSeconds: Int = 4,
    val breathingHoldInSeconds: Int = 2,
    val breathingExhaleSeconds: Int = 4,
    val breathingHoldOutSeconds: Int = 2,
    val allowSkipBreathing: Boolean = false,
    val allowCustomSessionLimit: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reflectionPromptEnabled: Boolean = true,
    val defaultSessionLimitPresetsMinutes: List<Int> = listOf(5, 10, 15, 30),
    val defaultSessionLimitPresetsScrolls: List<Int> = listOf(10, 25, 50, 100),
    val defaultLimitType: LimitType = LimitType.SCROLL_COUNT,
    val scrollDebounceMillis: Int = 400,
    val averageScrollDwellSeconds: Int = 6,
    val strictModeGlobalDefault: Boolean = false,
    val breathingGateGlobalDefault: Boolean = true,
    val blockShortVideoGlobalDefault: Boolean = false,
    val excludeFromFocusGlobalDefault: Boolean = false,
    val defaultDailyTimeLimitMinutes: Int? = null,
    val defaultSessionTimeLimitMinutes: Int? = null,
    val defaultScrollLimitPerSession: Int? = null,
    val defaultHoldToOpenSeconds: Int? = null,

    val useDynamicWallpaperColor: Boolean = true,
    val customSeedColorArgb: Int? = null,
    val colorIntensity: ColorIntensity = ColorIntensity.NEUTRAL,
    val darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
    val blurEffectsEnabled: Boolean = true,
    val cornerRadiusScale: Float = 1.0f,
    val defaultFocusSessionMinutes: Int = 25,
    val journalPromptEnabled: Boolean = false,
    val journalPromptFrequency: JournalFrequency = JournalFrequency.ONCE_DAILY,
    val requirePasswordForSettingsChanges: Boolean = false,
    val settingsPasswordHash: String? = null,
    val customScrollPackages: String = "",  // comma-separated package names
    val sessionCooldownEnabled: Boolean = true,
    val sessionCooldownMinutes: Int = 5,

    // StayFree feature parity
    val siteBlockingEnabled: Boolean = false,
    val siteBlockedList: String = "",
    val keywordBlockingEnabled: Boolean = false,
    val keywordBlockedList: String = "",
    val bedtimeEnabled: Boolean = false,
    val bedtimeStartHour: Int = 22,
    val bedtimeStartMinute: Int = 0,
    val bedtimeEndHour: Int = 6,
    val bedtimeEndMinute: Int = 0,
    val greyscaleEnabled: Boolean = false
)

enum class ColorIntensity { NEUTRAL, SOFT, LIGHT, EXPRESSIVE }
enum class DarkModePreference { SYSTEM, LIGHT, DARK }
enum class JournalFrequency { EVERY_OPEN, ONCE_DAILY, OFF }
