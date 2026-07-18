package com.cycling.domain.model

data class AiInfo(
    val type: AiInfoType,
    val title: String,
    val content: String
)

enum class AiInfoType {
    SONG,
    ARTIST,
    ALBUM
}
