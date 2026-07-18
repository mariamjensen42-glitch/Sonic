package com.cycling.domain.repository

import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.TagUpdate

interface AudioMetadataRepository {
    suspend fun getAudioMetadata(filePath: String): AudioMetadata?

    suspend fun updateAudioTags(filePath: String, tagUpdate: TagUpdate): Result<Boolean>
}
