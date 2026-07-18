package com.cycling.data.local.lyrics

import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.Mp3File
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EmbeddedLyricsHelper @Inject constructor() {

    suspend fun extractLyrics(filePath: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null

            if (!filePath.lowercase().endsWith(".mp3")) return@withContext null

            val mp3File = Mp3File(filePath)

            val id3v2Tag: ID3v2? = if (mp3File.hasId3v2Tag()) {
                mp3File.id3v2Tag
            } else {
                null
            }

            val lyrics = id3v2Tag?.lyrics

            if (lyrics.isNullOrBlank()) null else lyrics
        } catch (e: Exception) {
            null
        }
    }
}
