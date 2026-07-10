// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.data

import androidx.room.TypeConverter

class HalterConverters {
    @TypeConverter fun appCategoryToString(value: AppCategory): String = value.name
    @TypeConverter fun stringToAppCategory(value: String): AppCategory = enumValueOf(value)
    @TypeConverter fun contentTypeToString(value: ContentType): String = value.name
    @TypeConverter fun stringToContentType(value: String): ContentType = enumValueOf(value)
    @TypeConverter fun limitTypeToString(value: LimitType): String = value.name
    @TypeConverter fun stringToLimitType(value: String): LimitType = enumValueOf(value)
    @TypeConverter fun journalReasonToString(value: JournalReason): String = value.name
    @TypeConverter fun stringToJournalReason(value: String): JournalReason = enumValueOf(value)
}
