package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateAdded: Long,
    val dateModified: Long,
    /** User-supplied cover URI; falls back to the first song's art when null. */
    val customCover: String? = null,
    /** 歌单介绍，AI 创建时写入。 */
    val description: String? = null
)
