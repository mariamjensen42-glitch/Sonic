package com.cycling.data.local.metadata

import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class TagUpdate(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val composer: String? = null
)

data class AudioMetadataResult(
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val encodingType: String = "",
    val year: String? = null,
    val genre: String? = null,
    val composer: String? = null,
    val publisher: String? = null,
    val comment: String? = null,
    val copyright: String? = null
)

@Singleton
class AudioMetadataHelper @Inject constructor() {

    suspend fun extractMetadata(filePath: String): AudioMetadataResult? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null

            val extension = filePath.substringAfterLast('.').lowercase()
            
            when (extension) {
                "mp3" -> extractMp3Metadata(filePath)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractMp3Metadata(filePath: String): AudioMetadataResult {
        val mp3File = Mp3File(filePath)
        
        val id3v2Tag: ID3v2? = if (mp3File.hasId3v2Tag()) {
            mp3File.id3v2Tag
        } else {
            null
        }

        val encodingType = buildString {
            append("MP3")
            if (mp3File.isVbr) {
                append(" (VBR)")
            } else {
                append(" (CBR)")
            }
        }

        return AudioMetadataResult(
            bitrate = mp3File.bitrate,
            sampleRate = mp3File.sampleRate,
            channels = 2,
            encodingType = encodingType,
            year = id3v2Tag?.year,
            genre = id3v2Tag?.genreDescription,
            composer = id3v2Tag?.composer,
            publisher = id3v2Tag?.publisher,
            comment = id3v2Tag?.comment,
            copyright = id3v2Tag?.copyright
        )
    }

    suspend fun updateMp3Tags(filePath: String, tagUpdate: TagUpdate): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.w("File does not exist: $filePath")
                return@withContext false
            }

            Timber.d("Updating MP3 tags for: $filePath")
            
            val mp3File = Mp3File(filePath)
            
            val id3v2Tag: ID3v2 = if (mp3File.hasId3v2Tag()) {
                mp3File.id3v2Tag
            } else {
                Timber.d("Creating new ID3v2 tag")
                ID3v24Tag().also { mp3File.id3v2Tag = it }
            }

            tagUpdate.title?.let { 
                id3v2Tag.title = it 
                Timber.d("Updated title: $it")
            }
            tagUpdate.artist?.let { 
                id3v2Tag.artist = it 
                Timber.d("Updated artist: $it")
            }
            tagUpdate.album?.let { 
                id3v2Tag.album = it 
                Timber.d("Updated album: $it")
            }
            tagUpdate.year?.let { 
                id3v2Tag.year = it 
                Timber.d("Updated year: $it")
            }
            tagUpdate.genre?.let { 
                id3v2Tag.setGenreDescription(it) 
                Timber.d("Updated genre: $it")
            }
            tagUpdate.composer?.let { 
                id3v2Tag.composer = it 
                Timber.d("Updated composer: $it")
            }

            mp3File.save(filePath)
            Timber.i("Successfully saved MP3 tags for: $filePath")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to update MP3 tags for: $filePath")
            false
        }
    }
}
