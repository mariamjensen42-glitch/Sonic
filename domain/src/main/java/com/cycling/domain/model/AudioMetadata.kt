package com.cycling.domain.model

data class AudioMetadata(
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
