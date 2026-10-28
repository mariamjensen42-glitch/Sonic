package com.cycling.data.local.lyrics

import com.cycling.data.local.metadata.TAG_READABLE_EXTENSIONS
import com.cycling.data.local.metadata.firstValueOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import timber.log.Timber

/**
 * 读文件里内嵌的歌词。
 *
 * jaudiotagger 把三种容器的歌词统一到 `FieldKey.LYRICS`：MP3 是 ID3v2 的 USLT、FLAC/OGG 是
 * Vorbis comment 的 `LYRICS` 键、M4A 是 `©lyr` 原子。以前只认 MP3（mp3agic 的 `lyrics`），
 * 现在跟着标签库一起覆盖到所有它能读的容器。
 */
@Singleton
class EmbeddedLyricsHelper @Inject constructor() {

    suspend fun extractLyrics(filePath: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null

            val extension = file.extension.lowercase()
            if (extension !in TAG_READABLE_EXTENSIONS) return@withContext null

            AudioFileIO.read(file).tag.firstValueOrNull(FieldKey.LYRICS)
        } catch (e: Exception) {
            Timber.w(e, "extractLyrics: could not read embedded lyrics of %s", filePath)
            null
        }
    }
}
