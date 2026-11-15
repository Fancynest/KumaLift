package com.bearbones.kumalift.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

fun formatEpochToDateString(epochMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val tz = TimeZone.currentSystemDefault()
    val ldt = instant.toLocalDateTime(tz)
    val hour = ldt.hour.toString().padStart(2, '0')
    val minute = ldt.minute.toString().padStart(2, '0')
    val day = ldt.dayOfMonth.toString().padStart(2, '0')
    val month = ldt.monthNumber.toString().padStart(2, '0')
    val year = ldt.year
    return "$year-$month-$day $hour:$minute"
}
