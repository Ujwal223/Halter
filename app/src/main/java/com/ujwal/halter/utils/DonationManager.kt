// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ujwal.halter.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DonationManager {
    private const val WEEK_MS = 7L * 24 * 3600 * 1000

    suspend fun shouldShowNow(context: Context, recentUsageMillis: Long? = null): Boolean = withContext(Dispatchers.IO) {
        val repo = SettingsRepository(context)
        val opens = repo.getLong(SettingsRepository.Names.app_open_count, 0L)
        // Only show donations after the user has opened the app at least 5 times
        if (opens < 5L) return@withContext false
        val now = System.currentTimeMillis()
        val dismissedUntil = repo.getLong(SettingsRepository.Names.donate_dismissed_until_epoch_ms, 0L)
        if (now < dismissedUntil) return@withContext false

        val raw = repo.getString(SettingsRepository.Names.donate_shown_timestamps, "")
        val recent = raw.split(',').mapNotNull { it.toLongOrNull() }.filter { it >= now - WEEK_MS }
        if (recent.size >= 2) return@withContext false // already shown twice this week

        // Avoid prompting during heavy usage — if caller provides recent usage (ms), skip when over threshold
        val heavyUsageThreshold = 30L * 60L * 1000L // 30 minutes
        if (recentUsageMillis != null && recentUsageMillis >= heavyUsageThreshold) return@withContext false

        val hour = java.time.LocalDateTime.now().hour
        if (hour < 10 || hour > 21) return@withContext false // avoid early/late hours

        return@withContext true
    }

    suspend fun recordShown(context: Context) = withContext(Dispatchers.IO) {
        val repo = SettingsRepository(context)
        val now = System.currentTimeMillis()
        val raw = repo.getString(SettingsRepository.Names.donate_shown_timestamps, "")
        val list = raw.split(',').mapNotNull { it.toLongOrNull() }.filter { it >= now - WEEK_MS }.toMutableList()
        list.add(now)
        repo.updateString(SettingsRepository.Names.donate_shown_timestamps, list.joinToString(","))
    }

    suspend fun dismissForDays(context: Context, days: Int) = withContext(Dispatchers.IO) {
        val repo = SettingsRepository(context)
        val until = System.currentTimeMillis() + days * 24L * 3600L * 1000L
        repo.updateLong(SettingsRepository.Names.donate_dismissed_until_epoch_ms, until)
    }

    fun openDonateUrl(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymemomo.com/ujwal"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
