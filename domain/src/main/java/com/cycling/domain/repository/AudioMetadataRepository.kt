package com.cycling.domain.repository

import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.TagUpdate

interface AudioMetadataRepository {
    suspend fun getAudioMetadata(filePath: String): AudioMetadata?

    suspend fun updateAudioTags(filePath: String, tagUpdate: TagUpdate): Result<Boolean>

    /**
     * ReplayGain 的曲目增益（dB），文件里没有这个标签时返回 `null`。
     *
     * 单独一个方法而不是塞进 [getAudioMetadata]：读增益只需要文件头部若干字节，而完整元数据
     * 解析要过一遍 mp3agic。播放器每换一首歌都要问一次，不能让那条路径背上完整解析的代价。
     */
    suspend fun getReplayGainDb(filePath: String): Float?
}
