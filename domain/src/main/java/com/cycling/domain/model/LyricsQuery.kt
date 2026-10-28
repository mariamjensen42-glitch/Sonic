package com.cycling.domain.model

import com.cycling.domain.lyrics.utils.TrackText

/**
 * What an online provider needs to identify a track.
 *
 * Deliberately not a [Song]: the file path, id and artwork are meaningless to a remote service,
 * and keeping them out means a cached result stays valid when the file moves.
 */
data class LyricsQuery(
    val title: String,
    val artist: String,
    val album: String,
    /** Track length in milliseconds, as MediaStore reports it. */
    val durationMs: Long,
    /**
     * 在线曲目的平台键（[com.cycling.domain.online.model.OnlineSource.lxKey]），本地歌曲为 null。
     *
     * 只有音源脚本这一路来源用得到：它按平台 id 取词，不能按歌名搜。
     * 不参与 [cacheKey] —— 同一首录音不该因为本地/在线两种存在形式缓存成两行。
     */
    val onlineSourceKey: String? = null,
    /** 在线曲目的平台内 id，本地歌曲为 null。 */
    val onlineSongId: String? = null
) {
    /**
     * Stable identity for the `lyrics_cache` row.
     *
     * Normalised because the same track is described slightly differently across sources — casing
     * and padding vary freely, and duration is quantised to whole seconds so that a one- or
     * two-second difference between a streamed and a local copy still hits the same row.
     */
    val cacheKey: String
        get() = buildString {
            append(title.normalised())
            append('|')
            append(artist.normalised())
            append('|')
            append(durationMs / 1000)
        }

    /**
     * 搜索用的曲名：去掉 `(Live)`、`- Remastered 2011`、`feat. X` 这类噪音。
     *
     * 平台的检索是按规范曲名建的索引，把本地标签原样拼进去会明显降低命中率；
     * 而比较「是不是同一首」时用的仍是原始 [title]，两者不能混。
     */
    val searchTitle: String get() = TrackText.baseTitle(title)

    /** 搜索用的歌手：多歌手标签只取第一个 —— 平台侧按一个名字检索命中率更高。 */
    val searchArtist: String get() = TrackText.artistNames(artist).firstOrNull().orEmpty()

    private fun String.normalised(): String = lowercase().trim()

    companion object {
        fun of(song: Song): LyricsQuery = LyricsQuery(
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.duration,
            onlineSourceKey = song.onlineSourceKey,
            onlineSongId = song.onlineSongId
        )
    }
}
