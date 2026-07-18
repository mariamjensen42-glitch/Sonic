package com.cycling.data.repository

import com.cycling.data.local.metadata.AudioMetadataHelper
import com.cycling.data.local.metadata.TagUpdate as DataTagUpdate
import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.TagUpdate
import com.cycling.domain.repository.AudioMetadataRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioMetadataRepositoryImpl @Inject constructor(
    private val audioMetadataHelper: AudioMetadataHelper
) : AudioMetadataRepository {

    override suspend fun getAudioMetadata(filePath: String): AudioMetadata? {
        Timber.d("getAudioMetadata: filePath=$filePath")
        val metadata = audioMetadataHelper.extractMetadata(filePath)
        if (metadata == null) {
            Timber.d("getAudioMetadata: no metadata found for filePath=$filePath")
            return null
        }
        Timber.d("getAudioMetadata: found metadata for filePath=$filePath, bitrate=${metadata.bitrate}, sampleRate=${metadata.sampleRate}")
        return AudioMetadata(
            bitrate = metadata.bitrate,
            sampleRate = metadata.sampleRate,
            channels = metadata.channels,
            encodingType = metadata.encodingType,
            year = metadata.year,
            genre = metadata.genre,
            composer = metadata.composer,
            publisher = metadata.publisher,
            comment = metadata.comment,
            copyright = metadata.copyright
        )
    }

    override suspend fun updateAudioTags(filePath: String, tagUpdate: TagUpdate): Result<Boolean> {
        Timber.d("updateAudioTags: filePath=$filePath, tagUpdate=$tagUpdate")
        return try {
            val dataTagUpdate = DataTagUpdate(
                title = tagUpdate.title,
                artist = tagUpdate.artist,
                album = tagUpdate.album,
                year = tagUpdate.year,
                genre = tagUpdate.genre,
                composer = tagUpdate.composer
            )
            val success = audioMetadataHelper.updateMp3Tags(filePath, dataTagUpdate)
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
}
