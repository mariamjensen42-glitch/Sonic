package com.cycling.domain.repository

import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.TagUpdate

interface AudioMetadataRepository {
    suspend fun getAudioMetadata(filePath: String): AudioMetadata?

    /**
     * 写入 [tagUpdate] 到文件自身的标签。
     *
     * 支持写回的容器：MP3 (ID3v2)、FLAC / OGG-Vorbis (VorbisComment)、MP4/M4A (ilst)、
     * WAV、AIFF、WMA、DSF。DFF 只读；Opus 与裸 AAC 既不能读标签细节也不能写，先问
     * [isTagWriteSupported] 再决定提示方式，不要把 `false` 当成错误。
     *
     * @return `false` 当容器没有 writer、或写入失败（例如只有媒体库的读授权）。
     */
    suspend fun updateAudioTags(filePath: String, tagUpdate: TagUpdate): Result<Boolean>

    /**
     * 把封面图写进文件，替换已有的封面。
     *
     * 为什么不复用 [updateAudioTags]：那一套是「字段 → 字符串」的映射，而封面是二进制，
     * 还要由容器决定写 APIC / COVER / covr。分开一个方法，语义更清楚，也不会让
     * `TagUpdate` 背上一个每次都要序列化的大数组。
     *
     * 曲库里的封面是 MediaStore 在**扫描时**从文件内嵌图片生成的，所以下载完成后必须调它 ——
     * 否则文件入库了也只会显示占位图。
     *
     * @return `false` 当容器不支持、图片格式认不出或写入失败（都只是「没有封面」，不算失败）。
     */
    suspend fun writeCoverArtwork(filePath: String, imageBytes: ByteArray): Result<Boolean>

    /**
     * Whether [filePath]'s container has a tag writer.
     *
     * Used by the edit page to decide whether "stored in the app database only" is a warning or
     * simply the expected outcome for the format.
     */
    fun isTagWriteSupported(filePath: String): Boolean

    /**
     * ReplayGain 的曲目增益（dB），文件里没有这个标签时返回 `null`。
     *
     * 单独一个方法而不是塞进 [getAudioMetadata]：读增益只需要文件头部若干字节，而完整元数据
     * 解析要过一遍 jaudiotagger。播放器每换一首歌都要问一次，不能让那条路径背上完整解析的代价。
     */
    suspend fun getReplayGainDb(filePath: String): Float?

    /**
     * Tag fields the user edited in the app for [songId], or `null` when they never edited any.
     *
     * These are stored independently of the file: a tag write can fail when the app lacks write
     * access, and unsupported containers have no writer at all; a rescan would otherwise overwrite
     * a corrected value. A field that is `null` inside the result was never overridden, so the
     * file's value still applies.
     */
    suspend fun getUserTagOverrides(songId: Long): TagUpdate?

    /**
     * Persists [tagUpdate] as the user's overrides for [songId].
     *
     * Only the fields present in [tagUpdate] are changed; `null` leaves the previously stored
     * override alone.
     */
    suspend fun saveUserTagOverrides(songId: Long, tagUpdate: TagUpdate)
}
