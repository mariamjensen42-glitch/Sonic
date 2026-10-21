package com.cycling.domain.repository

import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.TagUpdate

interface AudioMetadataRepository {
    suspend fun getAudioMetadata(filePath: String): AudioMetadata?

    /**
     * Writes [tagUpdate] into the file's own tags.
     *
     * Supported containers: MP3 (ID3v2), FLAC and OGG-Vorbis (VorbisComment), MP4/M4A (ilst).
     * Opus and everything else are read-only or unsupported; check [isTagWriteSupported] first
     * rather than treating `false` as an error.
     *
     * @return `false` when the container is unsupported or the write failed (for example because
     *   the app only has a read grant on the media library).
     */
    suspend fun updateAudioTags(filePath: String, tagUpdate: TagUpdate): Result<Boolean>

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
     * 解析要过一遍 mp3agic。播放器每换一首歌都要问一次，不能让那条路径背上完整解析的代价。
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
