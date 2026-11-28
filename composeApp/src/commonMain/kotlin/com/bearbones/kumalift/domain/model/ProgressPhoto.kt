package com.bearbones.kumalift.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ProgressPhotoEntry(
    val id: String,
    val imagePath: String,
    val capturedDateEpochMs: Long,
    val bodyweightKg: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armsCm: Double? = null,
    val note: String = "",
    val isPrivate: Boolean = true,
    val createdAtEpochMs: Long = capturedDateEpochMs
)

data class MonthlyProgressPhotoGroup(
    val year: Int,
    val monthNumber: Int,
    val photos: List<ProgressPhotoEntry>
) {
    val key: String get() = "${year}_${monthNumber.toString().padStart(2, '0')}"
}

fun groupProgressPhotosByMonth(
    photos: List<ProgressPhotoEntry>,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): List<MonthlyProgressPhotoGroup> {
    if (photos.isEmpty()) return emptyList()
    return photos
        .sortedByDescending { it.capturedDateEpochMs }
        .groupBy { entry ->
            val dt = Instant.fromEpochMilliseconds(entry.capturedDateEpochMs).toLocalDateTime(timeZone)
            dt.year to dt.monthNumber
        }
        .entries
        .map { (ym, list) ->
            MonthlyProgressPhotoGroup(
                year = ym.first,
                monthNumber = ym.second,
                photos = list
            )
        }
        .sortedWith(
            compareByDescending<MonthlyProgressPhotoGroup> { it.year }
                .thenByDescending { it.monthNumber }
        )
}
