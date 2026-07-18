package com.cycling.domain.model

data class LibraryStats(
    val totalSongs: Int,
    val hrCount: Int,
    val sqCount: Int,
    val hqCount: Int,
    val othersCount: Int
) {
    val hrPercentage: Float get() = if (totalSongs > 0) hrCount.toFloat() / totalSongs * 100 else 0f
    val sqPercentage: Float get() = if (totalSongs > 0) sqCount.toFloat() / totalSongs * 100 else 0f
    val hqPercentage: Float get() = if (totalSongs > 0) hqCount.toFloat() / totalSongs * 100 else 0f
    val othersPercentage: Float get() = if (totalSongs > 0) othersCount.toFloat() / totalSongs * 100 else 0f
}

enum class AudioQuality {
    HR,
    SQ,
    HQ,
    OTHERS
}

data class QualityStatItem(
    val quality: AudioQuality,
    val count: Int,
    val percentage: Float
)
