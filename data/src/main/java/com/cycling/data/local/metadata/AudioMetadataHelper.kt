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

    /**
     * 读取 ReplayGain 的曲目增益。
     *
     * 不去做完整的标签解析，而是在文件头部若干字节里找 `replaygain_track_gain` 这个键，再取它
     * 后面第一个带 `dB` 的数字。理由：ReplayGain 在 MP3 里是 ID3v2 的 TXXX 帧、在 FLAC/OGG 里
     * 是 Vorbis comment、在 M4A 里是 iTunes 的自定义 atom —— 三种格式三套解析器，而播放器每换
     * 一首歌就要问一次。这个键在三种格式里都是明文的 ASCII，落在文件头部，扫一遍就够了。
     *
     * 扫描范围固定为 [REPLAY_GAIN_SCAN_BYTES]：ID3v2 头、Vorbis comment 块、以及
     * faststart 过的 M4A 的 `moov` 都在这个范围内；`moov` 在文件尾部的 M4A 读不到，返回 null，
     * 播放时按不启用处理，不会出错。
     */
    suspend fun extractReplayGainDb(filePath: String): Float? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null

            val head = ByteArray(REPLAY_GAIN_SCAN_BYTES)
            val read = file.inputStream().use { it.read(head) }
            if (read <= 0) return@withContext null

            parseReplayGainTrackGain(String(head, 0, read, Charsets.ISO_8859_1))
        } catch (e: Exception) {
            Timber.w(e, "extractReplayGainDb failed for: $filePath")
            null
        }
    }

    private fun parseReplayGainTrackGain(rawText: String): Float? {
        // ID3v2 的 TXXX 值可能是 UTF-16，按单字节解码后会变成夹着 NUL 的字符流；
        // 去掉 NUL 就还原成可搜索的明文，对本来就是单字节的 Vorbis comment 无影响。
        val text = rawText.replace("\u0000", "")

        var searchFrom = 0
        while (true) {
            val keyAt = text.indexOf(REPLAY_GAIN_KEY, searchFrom, ignoreCase = true)
            if (keyAt < 0) return null

            val valueFrom = keyAt + REPLAY_GAIN_KEY.length
            val windowEnd = (valueFrom + REPLAY_GAIN_VALUE_WINDOW).coerceAtMost(text.length)
            if (valueFrom < windowEnd) {
                val match = REPLAY_GAIN_VALUE_REGEX.find(text.substring(valueFrom, windowEnd))
                val gain = match?.groupValues?.get(1)?.toFloatOrNull()
                if (gain != null) return gain
            }
            searchFrom = valueFrom
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

    private companion object {
        const val REPLAY_GAIN_KEY = "replaygain_track_gain"

        /** 头部扫描范围：够覆盖 ID3v2 头与 Vorbis comment，又不会为每首歌读进整张专辑封面。 */
        const val REPLAY_GAIN_SCAN_BYTES = 128 * 1024

        /** 键后面允许出现的字符数。真实标签形如 `-7.53 dB`，40 个字符足够宽松。 */
        const val REPLAY_GAIN_VALUE_WINDOW = 40

        /** 只认带 `dB` 单位的数字，避免把后面的 `replaygain_track_peak` 之类的值当成增益。 */
        val REPLAY_GAIN_VALUE_REGEX = Regex("""(-?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)
    }
}
