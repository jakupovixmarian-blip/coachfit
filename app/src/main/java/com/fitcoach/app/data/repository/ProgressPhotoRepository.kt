package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.ProgressPhotoEntity
import com.fitcoach.app.domain.model.ProgressPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Gestisce solo URI locali autorizzati: nessuna immagine viene caricata automaticamente. */
@Singleton
class ProgressPhotoRepository @Inject constructor(private val db: FitCoachDatabase) {
    val photos: Flow<List<ProgressPhoto>> = db.progressPhotoDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun add(uri: String, capturedAt: Long, pose: String, note: String, consentForAi: Boolean): Long {
        require(uri.startsWith("content://") || uri.startsWith("file://")) { "URI foto non locale" }
        return db.progressPhotoDao().insert(
            ProgressPhotoEntity(uri = uri, capturedAt = capturedAt, pose = pose, note = note, consentForAi = consentForAi)
        )
    }

    suspend fun delete(photo: ProgressPhoto) {
        db.progressPhotoDao().delete(
            ProgressPhotoEntity(
                id = photo.id, uri = photo.uri, capturedAt = photo.capturedAt,
                pose = photo.pose, note = photo.note, consentForAi = photo.consentForAi
            )
        )
    }


    suspend fun setAiConsent(photo: ProgressPhoto, enabled: Boolean) {
        db.progressPhotoDao().setConsent(photo.id, enabled)
    }

    private fun ProgressPhotoEntity.toDomain() = ProgressPhoto(
        id = id, uri = uri, capturedAt = capturedAt, pose = pose, note = note, consentForAi = consentForAi
    )
}
