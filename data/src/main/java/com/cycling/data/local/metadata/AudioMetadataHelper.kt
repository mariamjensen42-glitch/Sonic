package com.cycling.data.local.metadata

import android.media.MediaMetadataRetriever
import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.TagUpdate
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import timber.log.Timber

/**
 * 读写本地音频文件的标签，全部交给 jaudiotagger（`net.jthink:jaudiotagger`，LGPL-2.1）。
 *
 * 之前这里是一套自己维护的多格式解析器（`:taglib`，MP3/FLAC/OGG/MP4 各写一遍 reader + writer），
 * 加上只认 MP3 的 mp3agic。现在统一成一个库：它覆盖 MP3 / FLAC / OGG-Vorbis / MP4-M4A / WAV /
 * AIFF / WMA / DSF（DFF 只读），读和写走同一套 `Tag` 抽象，字段名用 `FieldKey` 而不是各容器
 * 各自的键名。
 *
 * 例外是 Opus 与裸 AAC：jaudiotagger 不认识这两个容器，退到 `MediaMetadataRetriever` 拿基础字段
 * （码率、采样率、标题、艺术家、专辑、年份、曲号），拿不到的值留空 —— 总比详情页整片空白好。
 *
 * 写入仍是尽力而为：应用只拿到媒体库的读授权，写标签在现代设备上可能失败。这不能是编辑的唯一
 * 去处 —— 详情页同时把改动存进 `song_metadata`，并在写文件失败时如实提示。
 */
@Singleton
class AudioMetadataHelper @Inject constructor() {

    suspend fun extractMetadata(filePath: String): AudioMetadata? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            val extension = file.extension.lowercase()

            when {
                extension in TAG_READABLE_EXTENSIONS -> readWithJaudiotagger(file, extension)
                extension in RETRIEVER_EXTENSIONS -> readWithRetriever(file, extension)
                else -> null
            }
        } catch (e: Exception) {
            Timber.w(e, "extractMetadata: could not read tags of %s", filePath)
            null
        }
    }

    /** jaudiotagger 的读路径：技术参数来自 header，文字字段来自 `Tag`。 */
    private fun readWithJaudiotagger(file: File, extension: String): AudioMetadata? {
        val audioFile = AudioFileIO.read(file)
        val header = audioFile.audioHeader
        val tag = audioFile.tag

        // 有些容器的 header 不给码率（或给 0），用文件大小和时长估一个 —— 详情页只是展示。
        val bitrate = header.bitRateAsNumber.toInt().takeIf { it > 0 }
            ?: estimatedBitrateKbps(file.length(), header.preciseTrackLength)

        return audioMetadataFrom(
            extension = extension,
            bitrate = bitrate,
            sampleRate = header.sampleRateAsNumber.coerceAtLeast(0),
            channels = channelCountOf(header.channels),
            variableBitrate = header.isVariableBitRate,
            first = { key -> tag.firstValueOrNull(key) }
        )
    }

    /** Opus / 裸 AAC 的兜底：`MediaMetadataRetriever` 只给基础字段。 */
    private fun readWithRetriever(file: File, extension: String): AudioMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            // 用路径而不是 content Uri：Sonic 手上永远是 MediaStore 给的 `_data` 真路径，
            // 这样不必为了一个兜底解析器再注入一个 Context。
            retriever.setDataSource(file.path)

            fun value(key: Int): String? =
                retriever.extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }

            AudioMetadata(
                // METADATA_KEY_BITRATE 是 bit/s，模型里是 kbps。
                bitrate = value(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toLongOrNull()?.div(1000)?.toInt() ?: 0,
                sampleRate = value(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull() ?: 0,
                channels = 0,
                encodingType = encodingTypeFor(extension, variableBitrate = false),
                year = yearFromTagDate(value(MediaMetadataRetriever.METADATA_KEY_YEAR)),
                genre = value(MediaMetadataRetriever.METADATA_KEY_GENRE),
                composer = value(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
                albumArtist = value(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                trackNumber = value(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER),
                discNumber = null,
                publisher = null,
                comment = null,
                copyright = null
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * 读取 ReplayGain 的曲目增益。
     *
     * 不走完整解析，而是在文件头部若干字节里找 `replaygain_track_gain` 这个键，再取它后面第一个
     * 带 `dB` 的数字。理由：ReplayGain 在 MP3 里是 ID3v2 的 TXXX 帧、在 FLAC/OGG 里是
     * Vorbis comment、在 M4A 里是 iTunes 的自定义 atom —— 播放器每换一首歌都要问一次，这个键在
     * 三种格式里都是明文的 ASCII，落在文件头部，扫一遍就够了。
     *
     * 扫描范围固定为 [REPLAY_GAIN_SCAN_BYTES]：ID3v2 头、Vorbis comment 块、以及 faststart 过的
     * M4A 的 `moov` 都在这个范围内；`moov` 在文件尾部的 M4A 读不到，返回 null，播放时按不启用处理。
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

    /**
     * 把 [tagUpdate] 写进文件的标签。
     *
     * 只写用户真正改过的字段：`null` 表示"不动"，空串与空白同样跳过（清空字段要靠先读后删，
     * 不在这个接口的语义里）。jaudiotagger 直接在原文件上重写标签块，不需要像旧实现那样先写临时
     * 文件再覆盖。
     *
     * @return `false` 当文件不存在、容器没有 writer、或写入失败（例如只有读授权）时。
     *   调用方必须如实提示，而不是假装写成功了。
     */
    suspend fun updateTags(filePath: String, tagUpdate: TagUpdate): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            Timber.w("updateTags: file does not exist: $filePath")
            return@withContext false
        }
        if (!isTagWriteSupported(filePath)) {
            Timber.w("updateTags: no tag writer for %s", filePath)
            return@withContext false
        }

        try {
            val audioFile = AudioFileIO.read(file)
            // 没有标签的文件（裸 MP3 常见）先建一份默认标签，否则 setField 无处可写。
            val tag = audioFile.tag ?: audioFile.createDefaultTag().also { audioFile.tag = it }

            for ((key, value) in tagFieldWrites(tagUpdate, ordinalStyleFor(file.extension))) {
                // 单个字段写不进去（该容器没有对应的键，例如 MP4 没有发行方原子）只跳过这一个，
                // 不能让它带走整次保存。
                runCatching { tag.setField(key, value) }
                    .onFailure { Timber.w(it, "updateTags: %s does not support %s", file.extension, key) }
            }

            AudioFileIO.write(audioFile)
            Timber.i("updateTags: wrote tags for %s", file.path)
            true
        } catch (e: Exception) {
            Timber.e(e, "updateTags: failed to write tags for %s", file.path)
            false
        }
    }

    /**
     * 把封面写进音频文件，替换原有的封面字段。
     *
     * 下载完成时必须做这一步：曲库里的封面来自 MediaStore 的专辑封面缓存，而那个缓存是
     * **扫描时从文件内嵌图片里生成的**。文件里没有图，缩略图就是空的 —— 与音源给的封面无关。
     *
     * @param imageBytes 原始图片字节（JPEG / PNG / WebP / GIF）。
     * @return `false` 当文件不存在、容器不支持、或图片格式/写入失败时。
     */
    suspend fun writeCoverArtwork(filePath: String, imageBytes: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists() || imageBytes.isEmpty()) return@withContext false
            if (!isTagWriteSupported(filePath)) {
                Timber.w("writeCoverArtwork: no tag writer for %s", filePath)
                return@withContext false
            }
            val extension = sniffImageExtension(imageBytes) ?: run {
                Timber.w("writeCoverArtwork: %s 不是可识别的图片", filePath)
                return@withContext false
            }

            // ArtworkFactory 只吃 File（它要读宽高写进图片信息块），所以先落一个临时图。
            // 放在同目录：跨分区临时目录可能写不进去，也没必要。
            var temp: File? = null
            try {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tag ?: audioFile.createDefaultTag().also { audioFile.tag = it }
                temp = File.createTempFile("cover-", ".$extension", file.parentFile)
                temp.writeBytes(imageBytes)
                val artwork = ArtworkFactory.createArtworkFromFile(temp)
                // 先清旧封面：有的容器（MP4）允许多张图，直接 set 会累加成相册。
                runCatching { tag.deleteArtworkField() }
                tag.setField(artwork)
                AudioFileIO.write(audioFile)
                Timber.i("writeCoverArtwork: wrote %d bytes cover for %s", imageBytes.size, file.name)
                true
            } catch (e: Exception) {
                Timber.w(e, "writeCoverArtwork: failed for %s", filePath)
                false
            } finally {
                temp?.delete()
            }
        }

    /**
     * True when [filePath]'s container has a writer wired up here.
     *
     * 详情页据此决定"只存进数据库"是警告还是该格式的正常结果。纯扩展名判断，可以同步调。
     */
    fun isTagWriteSupported(filePath: String): Boolean {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return extension in TAG_WRITABLE_EXTENSIONS
    }

    private companion object {
        const val REPLAY_GAIN_KEY = "replaygain_track_gain"

        /** 头部扫描范围：够覆盖 ID3v2 头与 Vorbis comment，又不会为每首歌读进整张专辑封面。 */
        const val REPLAY_GAIN_SCAN_BYTES = 128 * 1024

        /** 键后面允许出现的字符数。真实标签形如 `-7.53 dB`，40 个字符足够宽松。 */
        const val REPLAY_GAIN_VALUE_WINDOW = 40

        /** 只认带 `dB` 单位的数字，避免把后面的 `replaygain_track_peak` 之类的值当成增益。 */
        val REPLAY_GAIN_VALUE_REGEX = Regex("""(-?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)

        init {
            // jaudiotagger 走 java.util.logging，默认会把每个文件的解析过程打到 logcat；
            // 只留警告，免得整库扫描时淹没自己的 Timber 输出。
            Logger.getLogger("org.jaudiotagger").level = Level.WARNING
        }
    }
}

// ---------------------------------------------------------------------------
// 标签字段映射（纯函数，便于 JVM 单测）
// ---------------------------------------------------------------------------

/**
 * 按 magic number 判图片格式。
 *
 * 不按 URL 后缀猜：音源的封面地址经常是 `?...&x-oss-process=image/resize` 这类没有后缀的，
 * 而 jaudiotagger 要靠扩展名/mime 决定写进容器里的是 `image/jpeg` 还是 `image/png`。
 * 认不出就返回 null —— 宁可没有封面，也不要塞一串不是图片的字节进标签。
 */
internal fun sniffImageExtension(bytes: ByteArray): String? {
    fun at(i: Int): Int = if (i < bytes.size) bytes[i].toInt() and 0xFF else -1
    return when {
        at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF -> "jpg"
        at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47 -> "png"
        at(0) == 0x47 && at(1) == 0x49 && at(2) == 0x46 -> "gif"
        at(0) == 0x52 && at(1) == 0x49 && at(2) == 0x46 && at(3) == 0x46 &&
            at(8) == 0x57 && at(9) == 0x45 && at(10) == 0x42 && at(11) == 0x50 -> "webp"
        else -> null
    }
}

/** jaudiotagger 能读的容器。Opus 与裸 AAC 不在其中，走 `MediaMetadataRetriever` 兜底。 */
internal val TAG_READABLE_EXTENSIONS: Set<String> = setOf(
    "mp3", "flac", "ogg", "oga", "mp4", "m4a", "m4b", "m4p",
    "wav", "aiff", "aif", "wma", "dsf", "dff"
)

/** jaudiotagger 有 writer 的容器（DFF 只有 reader）。 */
internal val TAG_WRITABLE_EXTENSIONS: Set<String> = setOf(
    "mp3", "flac", "ogg", "oga", "m4a", "m4b", "mp4", "m4p",
    "wav", "aiff", "aif", "wma", "dsf"
)

/** jaudiotagger 不认识的容器，退回系统解析器。 */
internal val RETRIEVER_EXTENSIONS: Set<String> = setOf("opus", "aac")

/**
 * 轨号 / 碟号的写法分两派。
 *
 * Vorbis comment 是 `TRACKNUMBER` + `TRACKTOTAL` 两个键，`3/12` 要拆开写；ID3 的 `TRCK` 与 MP4 的
 * `trkn` 本来就是「序号 + 总数」一体的，写 `3/12` 原样交给库自己拆。
 */
internal enum class OrdinalStyle {
    /** ID3 `TRCK` / `TPOS`、MP4 `trkn` / `disk`：一个键带总数。 */
    COMBINED,

    /** Vorbis comment：`TRACKNUMBER` / `TRACKTOTAL` 分开。 */
    SPLIT
}

internal fun ordinalStyleFor(extension: String): OrdinalStyle =
    when (extension.lowercase()) {
        "flac", "ogg", "oga" -> OrdinalStyle.SPLIT
        else -> OrdinalStyle.COMBINED
    }

/** [tagUpdate] 里真正要写入的 (键, 值) 列表；空白值按「没改」处理。 */
internal fun tagFieldWrites(update: TagUpdate, style: OrdinalStyle): List<Pair<FieldKey, String>> {
    val writes = mutableListOf<Pair<FieldKey, String>>()

    fun put(key: FieldKey, value: String?) {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return
        writes += key to trimmed
    }

    put(FieldKey.TITLE, update.title)
    put(FieldKey.ARTIST, update.artist)
    put(FieldKey.ALBUM, update.album)
    put(FieldKey.ALBUM_ARTIST, update.albumArtist)
    put(FieldKey.YEAR, update.year)
    put(FieldKey.GENRE, update.genre)
    put(FieldKey.COMPOSER, update.composer)
    put(FieldKey.RECORD_LABEL, update.publisher)
    put(FieldKey.COPYRIGHT, update.copyright)
    put(FieldKey.COMMENT, update.comment)

    fun putOrdinal(value: String?, numberKey: FieldKey, totalKey: FieldKey) {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return
        when (style) {
            // 容器自己认 `3/12`，原样写回去，读回来才是同一个字符串。
            OrdinalStyle.COMBINED -> writes += numberKey to trimmed
            OrdinalStyle.SPLIT -> {
                val parts = trimmed.split('/', limit = 2).map { it.trim() }
                parts[0].takeIf { it.isNotEmpty() }?.let { writes += numberKey to it }
                parts.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { writes += totalKey to it }
            }
        }
    }

    putOrdinal(update.trackNumber, FieldKey.TRACK, FieldKey.TRACK_TOTAL)
    putOrdinal(update.discNumber, FieldKey.DISC_NO, FieldKey.DISC_TOTAL)

    return writes
}

/** 标签字段 + header 参数 -> 详情页模型。 */
internal fun audioMetadataFrom(
    extension: String,
    bitrate: Int,
    sampleRate: Int,
    channels: Int,
    variableBitrate: Boolean,
    first: (FieldKey) -> String?
): AudioMetadata = AudioMetadata(
    bitrate = bitrate,
    sampleRate = sampleRate,
    channels = channels,
    encodingType = encodingTypeFor(extension, variableBitrate),
    year = yearFromTagDate(first(FieldKey.YEAR)),
    genre = first(FieldKey.GENRE),
    composer = first(FieldKey.COMPOSER),
    albumArtist = first(FieldKey.ALBUM_ARTIST),
    trackNumber = combineOrdinal(first(FieldKey.TRACK), first(FieldKey.TRACK_TOTAL)),
    discNumber = combineOrdinal(first(FieldKey.DISC_NO), first(FieldKey.DISC_TOTAL)),
    publisher = first(FieldKey.RECORD_LABEL),
    comment = first(FieldKey.COMMENT),
    copyright = first(FieldKey.COPYRIGHT)
)

/** 取一个字段，缺这个键（容器不支持）或取不到时返回 null，不往上抛。 */
internal fun Tag?.firstValueOrNull(key: FieldKey): String? =
    this?.let { runCatching { it.getFirst(key) }.getOrNull() }
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/** `2003` / `2003-07-31` / `2003-07-31T07:00:00Z` -> `2003`；没有年份时返回 null。 */
internal fun yearFromTagDate(raw: String?): String? {
    val digits = raw.orEmpty().filter { it.isDigit() }
    return if (digits.length >= 4) digits.take(4) else null
}

/** `3` + total `12` -> `3/12`；已经带 `/` 的值原样保留。 */
internal fun combineOrdinal(number: String?, total: String?): String? {
    val primary = number?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (primary.contains('/')) return primary
    val totalValue = total?.trim()?.takeIf { it.isNotEmpty() } ?: return primary
    return "$primary/$totalValue"
}

/** jaudiotagger 的声道数是字符串（`"2"`、偶尔带说明文字），只取前导数字。 */
internal fun channelCountOf(raw: String?): Int =
    raw.orEmpty().trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 0

/** header 没给码率时（文件大小 / 时长）估一个 kbps。 */
internal fun estimatedBitrateKbps(fileSizeBytes: Long, seconds: Double): Int {
    if (seconds <= 0.0 || fileSizeBytes <= 0L) return 0
    return ((fileSizeBytes * 8.0 / seconds) / 1000.0).toInt()
}

private fun encodingTypeFor(extension: String, variableBitrate: Boolean): String {
    val base = when (extension.lowercase()) {
        "flac" -> "FLAC"
        "ogg", "oga" -> "OGG"
        "opus" -> "OPUS"
        "m4a" -> "M4A"
        "m4b" -> "M4B"
        "mp4", "m4p" -> "MP4"
        "aiff", "aif" -> "AIFF"
        "dsf" -> "DSF"
        "dff" -> "DFF"
        "wma" -> "WMA"
        else -> extension.uppercase()
    }
    // VBR/CBR 只有 MP3 的 header 会明确区分，其它容器的这个标志没有意义。
    return if (base == "MP3") base + if (variableBitrate) " (VBR)" else " (CBR)" else base
}
