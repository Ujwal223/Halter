// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.util

import java.util.concurrent.TimeUnit

/** Formats a duration in minutes. Uses hours + minutes when >= 60 min. */
fun formatMinutes(totalMinutes: Long): String {
    if (totalMinutes <= 0) return "0 min"
    if (totalMinutes < 60) return "$totalMinutes min"
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val hourLabel = if (hours == 1L) "1 hour" else "$hours hours"
    return if (mins == 0L) hourLabel else "$hourLabel $mins min"
}

/** Formats millis as minutes, using hours + minutes when >= 60 min. */
fun formatDurationMillis(millis: Long): String =
    formatMinutes(TimeUnit.MILLISECONDS.toMinutes(millis.coerceAtLeast(0)))
