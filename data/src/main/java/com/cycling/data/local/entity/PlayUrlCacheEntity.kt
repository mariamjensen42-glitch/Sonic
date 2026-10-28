package com.cycling.data.local.entity

import androidx.room.Entity

/**
 * 取链缓存表：`songId + quality` 是主键，同一首歌的不同档位各存一行。
 *
 * 存 [headersJson] 是因为不少音源要求 Referer/UA 才能播，丢掉头就等于丢掉地址。
 */
@Entity(tableName = "play_url_cache", primaryKeys = ["songId", "quality"])
data class PlayUrlCacheEntity(
    val songId: Long,
    val quality: String,
    val url: String,
    val headersJson: String?,
    val createdAt: Long,
    val expiresAt: Long
)
