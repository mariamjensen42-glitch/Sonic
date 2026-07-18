package com.cycling.data.local.lyrics

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LyricsFileHelper @Inject constructor() {

    private val supportedExtensions = listOf("lrc", "ttml", "xml")

    fun findLyricsFile(audioFilePath: String): File? {
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) return null

        val parentDir = audioFile.parentFile ?: return null
        val fileNameWithoutExt = audioFile.nameWithoutExtension

        for (ext in supportedExtensions) {
            val lyricsFile = File(parentDir, "$fileNameWithoutExt.$ext")
            if (lyricsFile.exists()) {
                return lyricsFile
            }
        }

        return null
    }

    suspend fun readLyricsContent(lyricsFile: File): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!lyricsFile.exists()) return@withContext null
            lyricsFile.readText()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLyricsContent(audioFilePath: String): String? = withContext(Dispatchers.IO) {
        val lyricsFile = findLyricsFile(audioFilePath) ?: return@withContext null
        readLyricsContent(lyricsFile)
    }
}
