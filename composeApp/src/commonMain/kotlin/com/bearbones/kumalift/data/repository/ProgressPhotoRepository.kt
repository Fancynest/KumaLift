package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.ProgressPhotoEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProgressPhotoRepository(
    initialPhotos: List<ProgressPhotoEntry> = emptyList(),
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val deleteLocalFile: (String) -> Unit = {}
) {
    private val _photos = MutableStateFlow(
        initialPhotos.sortedByDescending { it.capturedDateEpochMs }
    )
    val photos: StateFlow<List<ProgressPhotoEntry>> = _photos.asStateFlow()

    fun reloadFromDatabase() {
        val db = databaseRepo ?: return
        _photos.value = db.getAllProgressPhotos().sortedByDescending { it.capturedDateEpochMs }
    }

    fun addPhoto(entry: ProgressPhotoEntry) {
        val sanitized = entry.copy(isPrivate = true)
        _photos.update { list ->
            (listOf(sanitized) + list.filterNot { it.id == sanitized.id })
                .sortedByDescending { it.capturedDateEpochMs }
        }
        databaseRepo?.saveProgressPhoto(sanitized)
    }

    fun updatePhoto(entry: ProgressPhotoEntry) {
        val sanitized = entry.copy(isPrivate = true)
        _photos.update { list ->
            list.map { if (it.id == sanitized.id) sanitized else it }
                .sortedByDescending { it.capturedDateEpochMs }
        }
        databaseRepo?.saveProgressPhoto(sanitized)
    }

    fun deletePhoto(photoId: String) {
        val existing = _photos.value.firstOrNull { it.id == photoId }
        _photos.update { list -> list.filterNot { it.id == photoId } }
        databaseRepo?.deleteProgressPhoto(photoId)
        existing?.imagePath?.takeIf { it.isNotBlank() }?.let { deleteLocalFile(it) }
    }
}
