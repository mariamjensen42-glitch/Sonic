package com.cycling.data.local.metadata

import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.TagUpdate
import com.cycling.salt.audiotag.AudioTag
import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.data.AudioTagData
import com.cycling.salt.audiotag.rw.data.Metadata
import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import timber.log.Timber

/**
 * Reads and writes the tags of local audio files.
 *
 * MP3 stays on mp3agic (ID3v2, plus the USLT lyrics and ReplayGain readers). Everything else goes
 * through the vendored SaltAudioTag library in `:taglib`: FLAC (VorbisComment), OGG-Vorbis,
 * MP4/M4A (ilst) are read **and** written, Opus is read-only. That closes the old "edit tags on
 * MP3 only" gap without one hand-written parser per container.
 *
 * Writes remain best-effort on purpose: the app only holds a read grant on the media library, so a
 * tag write can fail on a modern device. That must never be the only place an edit lives — the
 * detail page also stores user edits in `song_metadata`, and reports when the file was not written.
 */
@Singleton
class AudioMetadataHelper @Inject constructor() {

    suspend fun extractMetadata(filePath: String): AudioMetadata? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null

            val extension = file.extension.lowercase()

            when {
                extension == "mp3" -> extractMp3Metadata(filePath)
                extension in MULTI_FORMAT_EXTENSIONS -> extractMultiFormatMetadata(file, extension)
                else -> null
            }
        } catch (e: Exception) {
            Timber.w(e, "extractMetadata: could not read tags of %s", filePath)
            null
        }
    }

    /**
     * Reads FLAC / OGG / MP4 tags through SaltAudioTag.
     *
     * Pictures are skipped: the detail page edits text fields, and loading a multi-megabyte cover
     * into memory for every page open would be wasteful. The write path re-reads with pictures so
     * they survive a tag edit.
     */
    @OptIn(UnstableAudioTagApi::class)
    private fun extractMultiFormatMetadata(file: File, extension: String): AudioMetadata? {
        val data = AudioTag.read(
            Path(file.path),
            tagLibExtensionFor(extension),
            ReadStrategy.IgnorePicture
        )
            .onFailure { Timber.w(it, "extractMetadata: %s is not readable by SaltAudioTag", file.path) }
            .getOrNull()
            ?: return null
        return audioMetadataFromTagData(data, extension, file.length())
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

    private fun extractMp3Metadata(filePath: String): AudioMetadata {
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

        return AudioMetadata(
            bitrate = mp3File.bitrate,
            sampleRate = mp3File.sampleRate,
            channels = 2,
            encodingType = encodingType,
            year = id3v2Tag?.year,
            genre = id3v2Tag?.genreDescription,
            composer = id3v2Tag?.composer,
            albumArtist = id3v2Tag?.albumArtist,
            trackNumber = id3v2Tag?.track,
            discNumber = id3v2Tag?.partOfSet,
            publisher = id3v2Tag?.publisher,
            comment = id3v2Tag?.comment,
            copyright = id3v2Tag?.copyright
        )
    }

    /**
     * Writes [tagUpdate] into the file's tags.
     *
     * MP3 goes through mp3agic's ID3v2 writer; FLAC/OGG/MP4 go through SaltAudioTag. Formats not
     * in either path return `false`.
     *
     * @return `false` when the file is missing, the container is unsupported, or the write failed
     *   — for example because the app only has a read grant on it. Callers must surface that
     *   rather than pretend the edit was applied.
     */
    suspend fun updateTags(filePath: String, tagUpdate: TagUpdate): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            Timber.w("updateTags: file does not exist: $filePath")
            return@withContext false
        }

        when (val extension = file.extension.lowercase()) {
            "mp3" -> updateMp3Tags(filePath, tagUpdate)
            in MULTI_FORMAT_WRITE_EXTENSIONS -> updateMultiFormatTags(file, extension, tagUpdate)
            else -> {
                Timber.w("updateTags: unsupported tag container '%s' for %s", extension, filePath)
                false
            }
        }
    }

    /**
     * True when [filePath]'s container has a writer wired up here.
     *
     * The detail page uses this to decide whether "saved to the database only" is a warning or the
     * expected outcome. It is intentionally synchronous: it is a pure extension check.
     */
    fun isTagWriteSupported(filePath: String): Boolean {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return extension == "mp3" || extension in MULTI_FORMAT_WRITE_EXTENSIONS
    }

    /**
     * Writes [tagUpdate] into a FLAC / OGG / MP4 file through SaltAudioTag.
     *
     * SaltAudioTag's write API replaces the whole metadata set, so the existing one is read first
     * and only the fields in [tagUpdate] are swapped out; unknown entries (ReplayGain, lyrics,
     * cover-art comments) and pictures are handed back untouched. The result is written to a
     * cache temp file and then copied over the original, because replacing the file in place is
     * the only way the change becomes visible to MediaStore and to other players.
     */
    @OptIn(UnstableAudioTagApi::class)
    private fun updateMultiFormatTags(file: File, extension: String, tagUpdate: TagUpdate): Boolean {
        val style = tagStyleFor(extension) ?: return false
        if (tagWriterBuffersWholeFile(extension) && file.length() > MAX_BUFFERED_TAG_WRITE_BYTES) {
            Timber.w(
                "updateTags: %s is too large for the whole-file writer (%d bytes), keeping DB tags only",
                file.path,
                file.length()
            )
            return false
        }
        val tagLibExtension = tagLibExtensionFor(extension)
        return try {
            val sourcePath = Path(file.path)
            val existing = AudioTag.read(sourcePath, tagLibExtension, ReadStrategy.All)
                .getOrElse { throw it }

            val merged = mergeTagUpdate(existing.metadatas.orEmpty(), tagUpdate, style)
            val tempPath = Path(
                SystemTemporaryDirectory,
                "sonic-tag-${System.nanoTime()}.$tagLibExtension"
            )

            try {
                AudioTag.write(
                    sourcePath,
                    tempPath,
                    tagLibExtension,
                    WriteOperation.AllMetadata.create(
                        metadatas = merged,
                        pictures = existing.pictures.orEmpty()
                    )
                ).getOrElse { throw it }

                copyFile(tempPath, sourcePath)
            } finally {
                runCatching {
                    if (SystemFileSystem.exists(tempPath)) SystemFileSystem.delete(tempPath)
                }
            }

            Timber.i("updateTags: wrote %s tags for %s", extension, file.path)
            true
        } catch (e: Exception) {
            Timber.e(e, "updateTags: failed to write %s tags for %s", extension, file.path)
            false
        }
    }

    /** Streams [sourcePath] over [targetPath]; the audio file is never held in memory whole. */
    private fun copyFile(sourcePath: Path, targetPath: Path) {
        SystemFileSystem.source(sourcePath).buffered().use { source ->
            SystemFileSystem.sink(targetPath).buffered().use { sink ->
                sink.transferFrom(source)
            }
        }
    }

    /**
     * Writes [tagUpdate] into the file's ID3v2 tag, creating the tag when the file has none.
     *
     * @return `false` when the file is missing, is not an MP3, or could not be written — for
     *   example because the app only has a read grant on it. Callers must surface that rather
     *   than pretend the edit was applied.
     */
    private fun updateMp3Tags(filePath: String, tagUpdate: TagUpdate): Boolean {
        return try {
            Timber.d("Updating MP3 tags for: $filePath")

            val mp3File = Mp3File(filePath)

            val id3v2Tag: ID3v2 = if (mp3File.hasId3v2Tag()) {
                mp3File.id3v2Tag
            } else {
                Timber.d("Creating new ID3v2 tag")
                ID3v24Tag().also { mp3File.id3v2Tag = it }
            }

            tagUpdate.title?.let { id3v2Tag.title = it }
            tagUpdate.artist?.let { id3v2Tag.artist = it }
            tagUpdate.album?.let { id3v2Tag.album = it }
            tagUpdate.albumArtist?.let { id3v2Tag.albumArtist = it }
            tagUpdate.year?.let { id3v2Tag.year = it }
            tagUpdate.composer?.let { id3v2Tag.composer = it }
            tagUpdate.publisher?.let { id3v2Tag.publisher = it }
            tagUpdate.copyright?.let { id3v2Tag.copyright = it }
            tagUpdate.comment?.let { id3v2Tag.comment = it }
            // TRCK / TPOS are strings in ID3 on purpose: `3/12` is as valid as `3`.
            tagUpdate.trackNumber?.let { id3v2Tag.track = it }
            tagUpdate.discNumber?.let { id3v2Tag.partOfSet = it }
            tagUpdate.genre?.let { genre ->
                // mp3agic only understands a fixed genre list; a custom genre name would throw
                // and take every other tag edit down with it, so it is skipped instead.
                try {
                    id3v2Tag.setGenreDescription(genre)
                } catch (e: IllegalArgumentException) {
                    Timber.w("updateMp3Tags: unknown genre '%s', leaving the tag untouched", genre)
                }
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

// ---------------------------------------------------------------------------
// 多格式标签的读写规则（纯函数，便于 JVM 单测）
// ---------------------------------------------------------------------------

/** 容器格式 -> 写标签时的键名习惯。MP3 走 mp3agic，不经过这里。 */
internal enum class TagStyle {
    /** FLAC 与 OGG 共用 VorbisComment，键名按 Vorbis I 规范（ALBUMARTIST / ORGANIZATION）。 */
    VORBIS,

    /** MP4/M4A 的 ilst 原子，键名用 SaltAudioTag 的内部键（ALBUM_ARTIST 等）。 */
    MP4
}

/** SaltAudioTag 能读的容器。Opus 只能读，不能写。 */
internal val MULTI_FORMAT_EXTENSIONS: Set<String> =
    setOf("flac", "ogg", "oga", "opus", "m4a", "m4b", "mp4", "aac")

/** SaltAudioTag 能写的容器。Opus 没有 writer；裸 .aac(ADTS) 也不是 MP4 容器。 */
internal val MULTI_FORMAT_WRITE_EXTENSIONS: Set<String> =
    setOf("flac", "ogg", "oga", "m4a", "m4b", "mp4")

/**
 * SaltAudioTag 按扩展名分派 reader/writer，它不认识 `.m4b`。
 *
 * 有声书的 `.m4b` 其实就是 MP4 容器，映射成 `m4a` 交给它处理；对外仍显示 M4B。
 */
internal fun tagLibExtensionFor(extension: String): String =
    if (extension.lowercase() == "m4b") "m4a" else extension.lowercase()

internal fun tagStyleFor(extension: String): TagStyle? = when (extension.lowercase()) {
    "flac", "ogg", "oga" -> TagStyle.VORBIS
    "m4a", "mp4", "m4b" -> TagStyle.MP4
    else -> null
}

/**
 * SaltAudioTag 的 OGG / MP4 writer 会把整份文件读进内存再重组（FLAC 是流式的）。
 *
 * 超过这个尺寸就不写文件标签了：与其 OOM，不如让改动只落在数据库覆盖表里，UI 会提示。
 */
internal const val MAX_BUFFERED_TAG_WRITE_BYTES = 80L * 1024 * 1024

/** [extension] 的 writer 是否会把整份文件读进内存。 */
internal fun tagWriterBuffersWholeFile(extension: String): Boolean =
    extension.lowercase() != "flac"

/** SaltAudioTag 的标签数据 -> 详情页的领域模型。 */
internal fun audioMetadataFromTagData(
    data: AudioTagData,
    extension: String,
    fileSize: Long
): AudioMetadata {
    val values = tagValues(data.metadatas.orEmpty())
    val streaminfo = data.streaminfo

    val seconds = if (streaminfo != null && streaminfo.sampleRate > 0) {
        streaminfo.sampleCount.toDouble() / streaminfo.sampleRate
    } else {
        0.0
    }
    // 这些容器没有现成的平均码率字段，用文件大小和时长估一个即可（详情页只是展示）。
    val bitrateKbps = if (seconds > 0.0 && fileSize > 0L) {
        ((fileSize * 8.0 / seconds) / 1000.0).toInt()
    } else {
        0
    }

    return AudioMetadata(
        bitrate = bitrateKbps,
        sampleRate = streaminfo?.sampleRate ?: 0,
        channels = streaminfo?.channelCount ?: 0,
        encodingType = encodingTypeFor(extension),
        year = firstTagValue(values, "DATE", "YEAR")?.let(::yearFromTagDate),
        genre = firstTagValue(values, "GENRE"),
        composer = firstTagValue(values, "COMPOSER"),
        albumArtist = firstTagValue(values, "ALBUMARTIST", "ALBUM_ARTIST"),
        trackNumber = combineOrdinal(
            firstTagValue(values, "TRACKNUMBER"),
            firstTagValue(values, "TRACKTOTAL")
        ),
        discNumber = combineOrdinal(
            firstTagValue(values, "DISCNUMBER", "DISCID"),
            firstTagValue(values, "DISCTOTAL")
        ),
        publisher = firstTagValue(values, "PUBLISHER", "ORGANIZATION", "RECORD_LABEL"),
        comment = firstTagValue(values, "COMMENT", "DESCRIPTION"),
        copyright = firstTagValue(values, "COPYRIGHT", "LICENSE")
    )
}

/**
 * 把 [update] 合并进已读出的 [existing]，只替换本次改动到的键。
 *
 * 保留其余条目是硬要求：ReplayGain、歌词、封面注释和本表不认识的标签都靠 `existing` 原样
 * 带回去；SaltAudioTag 的写接口是「整份元数据替换」，漏传就等于删掉。
 */
internal fun mergeTagUpdate(
    existing: List<Metadata>,
    update: TagUpdate,
    style: TagStyle
): List<Metadata> {
    val replacedKeys = mutableSetOf<String>()
    val additions = mutableListOf<Metadata>()

    fun apply(value: String?, aliases: Set<String>, vorbisKey: String, mp4Key: String?) {
        if (value.isNullOrBlank()) return
        replacedKeys += aliases
        val key = when (style) {
            TagStyle.VORBIS -> vorbisKey
            TagStyle.MP4 -> mp4Key ?: return
        }
        additions += Metadata(key, value.trim())
    }

    /** Vorbis splits `3/12` into TRACKNUMBER + TRACKTOTAL; MP4 keeps it in the `trkn`/`disk` atom. */
    fun applyOrdinal(
        value: String?,
        aliases: Set<String>,
        vorbisKey: String,
        vorbisTotalKey: String,
        mp4Key: String
    ) {
        if (value.isNullOrBlank()) return
        replacedKeys += aliases
        when (style) {
            TagStyle.VORBIS -> {
                val parts = value.split('/', limit = 2).map { it.trim() }
                val number = parts[0]
                val total = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                if (number.isNotEmpty()) additions += Metadata(vorbisKey, number)
                if (total != null) additions += Metadata(vorbisTotalKey, total)
            }
            TagStyle.MP4 -> additions += Metadata(mp4Key, value.trim())
        }
    }

    apply(update.title, setOf("TITLE"), vorbisKey = "TITLE", mp4Key = "TITLE")
    apply(update.artist, setOf("ARTIST", "PERFORMER"), vorbisKey = "ARTIST", mp4Key = "ARTIST")
    apply(update.album, setOf("ALBUM"), vorbisKey = "ALBUM", mp4Key = "ALBUM")
    apply(
        update.albumArtist,
        setOf("ALBUMARTIST", "ALBUM_ARTIST"),
        vorbisKey = "ALBUMARTIST",
        mp4Key = "ALBUM_ARTIST"
    )
    apply(update.year, setOf("DATE", "YEAR"), vorbisKey = "DATE", mp4Key = "DATE")
    apply(update.genre, setOf("GENRE"), vorbisKey = "GENRE", mp4Key = "GENRE")
    applyOrdinal(
        update.trackNumber,
        aliases = setOf("TRACKNUMBER", "TRACK", "TRACKTOTAL"),
        vorbisKey = "TRACKNUMBER",
        vorbisTotalKey = "TRACKTOTAL",
        mp4Key = "TRACKNUMBER"
    )
    applyOrdinal(
        update.discNumber,
        aliases = setOf("DISCNUMBER", "DISC", "DISCID", "DISCTOTAL"),
        vorbisKey = "DISCNUMBER",
        vorbisTotalKey = "DISCTOTAL",
        mp4Key = "DISCNUMBER"
    )
    apply(update.composer, setOf("COMPOSER"), vorbisKey = "COMPOSER", mp4Key = "COMPOSER")
    // MP4 没有发行方原子，写不进去就交给数据库覆盖表，不硬塞成别的字段。
    apply(
        update.publisher,
        setOf("PUBLISHER", "ORGANIZATION", "RECORD_LABEL"),
        vorbisKey = "ORGANIZATION",
        mp4Key = null
    )
    apply(update.copyright, setOf("COPYRIGHT", "LICENSE"), vorbisKey = "COPYRIGHT", mp4Key = "COPYRIGHT")
    apply(update.comment, setOf("COMMENT", "DESCRIPTION"), vorbisKey = "COMMENT", mp4Key = "COMMENT")

    val kept = existing.filterNot { entry -> entry.key.trim().uppercase() in replacedKeys }
    return kept + additions
}

internal fun tagValues(metadatas: List<Metadata>): Map<String, String> {
    val values = LinkedHashMap<String, String>()
    for (entry in metadatas) {
        val key = entry.key.trim().uppercase()
        val value = entry.value.trim()
        if (key.isNotEmpty() && value.isNotEmpty()) {
            values.putIfAbsent(key, value)
        }
    }
    return values
}

internal fun firstTagValue(values: Map<String, String>, vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> values[key]?.takeIf { it.isNotBlank() } }

/** `2003` / `2003-07-31` / `2003-07-31T07:00:00Z` -> `2003`；没有年份时返回 null。 */
internal fun yearFromTagDate(raw: String): String? {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length >= 4) digits.take(4) else null
}

/** `3` + total `12` -> `3/12`；已经带 `/` 的值原样保留。 */
internal fun combineOrdinal(number: String?, total: String?): String? {
    val primary = number?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (primary.contains('/')) return primary
    val totalValue = total?.trim()?.takeIf { it.isNotEmpty() } ?: return primary
    return "$primary/$totalValue"
}

private fun encodingTypeFor(extension: String): String = when (extension.lowercase()) {
    "flac" -> "FLAC"
    "ogg", "oga" -> "OGG"
    "opus" -> "OPUS"
    "m4a" -> "M4A"
    "m4b" -> "M4B"
    "mp4" -> "MP4"
    else -> extension.uppercase()
}
