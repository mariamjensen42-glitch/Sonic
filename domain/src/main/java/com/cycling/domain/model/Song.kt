package com.cycling.domain.model

data class Song(
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
    /** Effective artwork: the user's own cover when set, otherwise MediaStore's. */
    val albumArt: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val bitrate: Int = 0,
    /** Star rating, 0 = unrated, 1..5. */
    val rating: Int = 0,
    /** True when [albumArt] came from the user rather than MediaStore. */
    val hasCustomAlbumArt: Boolean = false,
    /**
     * 在线曲目的平台键（网易云 `wy`、QQ `tx` …，取值见 `OnlineSource.lxKey`）。本地文件为 null。
     *
     * 这是**指针**，不是结果：真正可播的 URL 每次播放时才由音源脚本取，从不入库。
     */
    val onlineSourceKey: String? = null,
    /** 平台内的歌曲 id；与 [onlineSourceKey] 一起才能重新取链。 */
    val onlineSongId: String? = null
) {
    /** 在线曲目：没有本地文件，[path] 在解析之前是空串。 */
    val isOnline: Boolean get() = onlineSourceKey != null
}
