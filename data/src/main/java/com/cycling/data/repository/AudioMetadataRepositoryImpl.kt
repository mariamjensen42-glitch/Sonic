package com.cycling.data.repository

import com.cycling.data.local.dao.SongMetadataDao
import com.cycling.data.local.metadata.AudioMetadataHelper
import com.cycling.data.mapper.mergeWith
import com.cycling.data.mapper.toSongMetadataEntity
import com.cycling.data.mapper.toTagUpdate
import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.TagUpdate
import com.cycling.domain.repository.AudioMetadataRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioMetadataRepositoryImpl @Inject constructor(
    private val audioMetadataHelper: AudioMetadataHelper,
    private val songMetadataDao: SongMetadataDao
) : AudioMetadataRepository {

    override suspend fun getAudioMetadata(filePath: String): AudioMetadata? {
        Timber.d("getAudioMetadata: filePath=$filePath")
        val metadata = audioMetadataHelper.extractMetadata(filePath)
        if (metadata == null) {
            Timber.d("getAudioMetadata: no metadata found for filePath=$filePath")
            return null
        }
        Timber.d(
            "getAudioMetadata: found metadata for filePath=$filePath, " +
                "bitrate=${metadata.bitrate}, sampleRate=${metadata.sampleRate}"
        )
        return metadata
    }

    override suspend fun getReplayGainDb(filePath: String): Float? =
        audioMetadataHelper.extractReplayGainDb(filePath)

    override suspend fun updateAudioTags(filePath: String, tagUpdate: TagUpdate): Result<Boolean> {
        Timber.d("updateAudioTags: filePath=$filePath, tagUpdate=$tagUpdate")
        return try {
            val success = audioMetadataHelper.updateTags(filePath, tagUpdate)
            if (success) {
                Timber.i("updateAudioTags: successfully updated tags for filePath=$filePath")
            } else {
                Timber.w("updateAudioTags: failed to update tags for filePath=$filePath")
            }
            Result.success(success)
        } catch (e: Exception) {
            Timber.e(e, "updateAudioTags: exception while updating tags for filePath=$filePath")
            Result.failure(e)
        }
    }

    override suspend fun writeCoverArtwork(
        filePath: String,
        imageBytes: ByteArray
    ): Result<Boolean> {
        return try {
            val success = audioMetadataHelper.writeCoverArtwork(filePath, imageBytes)
            if (success) {
                Timber.i("writeCoverArtwork: wrote cover for filePath=$filePath")
            } else {
                Timber.w("writeCoverArtwork: cover not written for filePath=$filePath")
            }
            Result.success(success)
        } catch (e: Exception) {
            Timber.e(e, "writeCoverArtwork: exception for filePath=$filePath")
            Result.failure(e)
        }
    }

    override fun isTagWriteSupported(filePath: String): Boolean =
        audioMetadataHelper.isTagWriteSupported(filePath)

    override suspend fun getUserTagOverrides(songId: Long): TagUpdate? {
        return try {
            songMetadataDao.get(songId)?.toTagUpdate()
        } catch (e: Exception) {
            // A missing override row only means "no edits yet"; failing the whole detail page
            // because the override table could not be read would be the worse trade.
            Timber.w(e, "getUserTagOverrides: could not read overrides for songId=$songId")
            null
        }
    }

    override suspend fun saveUserTagOverrides(songId: Long, tagUpdate: TagUpdate) {
        val existing = try {
            songMetadataDao.get(songId)
        } catch (e: Exception) {
            Timber.w(e, "saveUserTagOverrides: could not read existing overrides for songId=$songId")
            null
        }
        val merged = (existing?.toTagUpdate() ?: TagUpdate()).mergeWith(tagUpdate)
        songMetadataDao.put(merged.toSongMetadataEntity(songId))
        Timber.d("saveUserTagOverrides: stored overrides for songId=$songId")
    }
}
