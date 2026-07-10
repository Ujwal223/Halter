// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter

import com.ujwal.halter.data.ContentType
import com.ujwal.halter.data.localDayStartMillis
import com.ujwal.halter.service.ScheduleRules
import com.ujwal.halter.service.ScrollDebouncer
import com.ujwal.halter.service.isShortVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CoreRulesTest {
    @Test
    fun scheduleWindowMatchesSameDayRanges() {
        assertTrue(ScheduleRules.isMinuteInWindow(minute = 9 * 60, startMinute = 8 * 60, endMinute = 10 * 60))
        assertFalse(ScheduleRules.isMinuteInWindow(minute = 10 * 60, startMinute = 8 * 60, endMinute = 10 * 60))
    }

    @Test
    fun scheduleWindowMatchesOvernightRanges() {
        assertTrue(ScheduleRules.isMinuteInWindow(minute = 23 * 60, startMinute = 22 * 60, endMinute = 6 * 60))
        assertTrue(ScheduleRules.isMinuteInWindow(minute = 5 * 60, startMinute = 22 * 60, endMinute = 6 * 60))
        assertFalse(ScheduleRules.isMinuteInWindow(minute = 12 * 60, startMinute = 22 * 60, endMinute = 6 * 60))
    }

    @Test
    fun mondayIsBitZeroForSchedules() {
        assertEquals(1, ScheduleRules.dayBitForIsoDay(1))
        assertEquals(64, ScheduleRules.dayBitForIsoDay(7))
    }

    @Test
    fun scrollDebouncerCountsOnlyAfterDebounceWindow() {
        val debouncer = ScrollDebouncer()

        assertTrue(debouncer.shouldCount("youtube:SHORT", nowMillis = 1_000, debounceMillis = 400))
        assertFalse(debouncer.shouldCount("youtube:SHORT", nowMillis = 1_200, debounceMillis = 400))
        assertTrue(debouncer.shouldCount("youtube:SHORT", nowMillis = 1_401, debounceMillis = 400))
    }

    @Test
    fun shortVideoTypesAreExplicit() {
        assertTrue(ContentType.REEL.isShortVideo())
        assertTrue(ContentType.SHORT.isShortVideo())
        assertFalse(ContentType.FEED.isShortVideo())
        assertFalse(ContentType.UNKNOWN.isShortVideo())
    }

    @Test
    fun localDayStartUsesDeviceTimezone() {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.of(2026, 7, 1).atTime(15, 45).atZone(zone).toInstant().toEpochMilli()
        val expected = LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(expected, localDayStartMillis(now))
        assertEquals(LocalDate.of(2026, 7, 1), Instant.ofEpochMilli(expected).atZone(zone).toLocalDate())
    }
}
