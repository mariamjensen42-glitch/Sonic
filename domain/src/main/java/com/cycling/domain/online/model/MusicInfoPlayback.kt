package com.cycling.domain.online.model

import com.cycling.domain.model.Song

/**
 * 在线歌曲（[MusicInfo]）→ 可交给播放器的 [Song]。
 *
 * 只做形状转换，不做取链：先由 `ResolveOnlinePlaySourceUseCase` 拿到 [PlaySource]，
 * 再调用本函数。`Song.path` 正是播放器写进 `MediaItem.uri` 的字段，`http(s)` 地址
 * 可以被 ExoPlayer 直接播放，所以在线歌曲能直接走既有
 * `PlayerRepository.playSong(song, queue)`。
 *
 * 注意两点：
 * - 合成 id 是负数，避免和 MediaStore 的歌曲 id 撞。指针（[Song.onlineSourceKey] /
 *   [Song.onlineSongId]）会一并带上，所以这首曲目可以入库、被收藏，并在下次播放时
 *   重新取链（见 `PlayerManager.resolveOnlineUri`）。
 * - 取链返回的请求头（Referer 等）目前没有进播放队列：需要带头的源要后续给
 *   `QueueItem` 加 headers，或先在解析侧把 URL 换成可直接访问的形式。
 */
fun MusicInfo.toSong(playSource: PlaySource): Song = Song(
    id = syntheticSongId(),
    title = title,
    artist = artist,
    album = album,
    duration = durationSeconds * 1000,
    path = playSource.url,
    albumId = -1L,
    artistId = -1L,
    dateAdded = 0L,
    dateModified = 0L,
    size = 0L,
    mimeType = "",
    albumArt = coverUrl,
    isFavorite = false,
    playCount = 0,
    lastPlayedAt = null,
    bitrate = 0,
    rating = 0,
    hasCustomAlbumArt = false,
    onlineSourceKey = source.lxKey,
    onlineSongId = id
)

/** 稳定的负 id：同一首在线歌曲每次播放都落到同一个队列 id。 */
fun MusicInfo.syntheticSongId(): Long {
    val hash = (source.lxKey + ":" + id).hashCode().toLong() and 0x7FFFFFFFL
    return -(hash.coerceAtLeast(1L))
}
