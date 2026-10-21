package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 歌单 ↔ 歌曲的关联行。
 *
 * `(playlistId, songId)` 上有唯一索引：一首歌在一张歌单里只能占一行。这不只是洁癖 ——
 * 详情页用 `song.id` 当列表 key，重复行会让 Compose 直接抛 "Key ... was already used"
 * 闪退；`removeSongFromPlaylist` / `updateSongPosition` / 多选的 id 集合也都按 songId
 * 定位，重复行只会让它们各自踩到其中一行。
 */
@Entity(
    tableName = "playlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["songId"]),
        Index(value = ["playlistId", "songId"], unique = true)
    ]
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val position: Int
)
