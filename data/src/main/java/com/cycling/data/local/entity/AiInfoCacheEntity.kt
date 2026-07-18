package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_info_cache")
data class AiInfoCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val type: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
