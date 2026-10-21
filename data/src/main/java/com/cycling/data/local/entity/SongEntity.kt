package com.cycling.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumId: Long,
    val artistId: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val mimeType: String,
    /** Album art URI published by MediaStore. Never user-editable. */
    val albumArt: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val bitrate: Int = 0,
    /** Star rating, 0 = unrated, 1..5. */
    @ColumnInfo(defaultValue = "0")
    val rating: Int = 0,
    /**
     * User-supplied cover, stored as a URI into app-private storage.
     *
     * Kept separate from [albumArt] rather than overwriting it: MediaStore owns [albumArt] and
     * will reset it on the next scan, whereas this column is preserved across scans by
     * [com.cycling.data.local.scanner.MusicScanner]. The two are merged by
     * [com.cycling.data.mapper.toDomain], which prefers this one.
     */
    val customAlbumArt: String? = null,

    /**
     * 在线曲目的平台键与平台 id。
     *
     * 两列都为 null 表示本地文件。扫描器的 stale 判定必须跳过非 null 的行 ——
     * 它们的 [path] 不在 MediaStore 结果里，否则每次扫描都会被当成"文件已删除"清掉。
     */
    val onlineSourceKey: String? = null,
    val onlineSongId: String? = null
)
