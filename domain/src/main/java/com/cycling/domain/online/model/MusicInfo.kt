package com.cycling.domain.online.model

import java.util.Locale

/**
 * 交给音源脚本的歌曲信息，等价于 LX 协议里 `info.musicInfo` 的内容。
 *
 * [id] 是平台内的歌曲 id（网易云 song_id、QQ songmid、酷狗 hash …）；
 * [extras] 放平台侧的附加标识（`mid` / `rid` / `copyright_id` / `track_id`），
 * 取链时必须原样交给脚本，否则脚本拼不出请求。
 */
data class MusicInfo(
    val source: OnlineSource,
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Long = 0L,
    val coverUrl: String? = null,
    val extras: Map<String, String> = emptyMap()
) {

    /** 脚本侧 `musicInfo` 的字段名与优先级，与 `LxMusicInfoBuilder.build` 完全一致。 */
    fun toLxPayload(): Map<String, Any?> {
        val payload = LinkedHashMap<String, Any?>()
        put(payload, "name", title)
        put(payload, "singer", artist)
        if (artist.isNotBlank()) {
            val artists = artist.split(ARTIST_SEPARATOR).filter { it.isNotBlank() }
            if (artists.isNotEmpty()) put(payload, "artists", artists)
        }
        put(payload, "albumName", album)
        if (durationSeconds > 0L) {
            put(
                payload,
                "interval",
                String.format(
                    Locale.ROOT,
                    "%02d:%02d",
                    durationSeconds / 60,
                    durationSeconds % 60
                )
            )
            put(payload, "duration", durationSeconds)
        }
        put(payload, "img", coverUrl)

        when (source) {
            OnlineSource.QQ -> {
                val songmid = extras["mid"] ?: id
                put(payload, "songmid", songmid)
                put(payload, "hash", songmid)
                put(payload, "songId", extras["songId"])
            }

            OnlineSource.KUWO -> {
                val rid = extras["rid"] ?: id
                put(payload, "songmid", rid)
                put(payload, "hash", rid)
            }

            OnlineSource.MIGU -> {
                val contentId = extras["content_id"] ?: extras["contentId"] ?: id
                val copyrightId = extras["copyright_id"] ?: extras["copyrightId"]
                put(payload, "copyrightId", copyrightId)
                put(payload, "songmid", contentId)
                put(payload, "hash", copyrightId ?: contentId)
            }

            OnlineSource.SODA -> {
                val trackId = extras["track_id"] ?: extras["trackId"] ?: id
                put(payload, "songmid", trackId)
                put(payload, "hash", trackId)
            }

            OnlineSource.KUGOU -> put(payload, "hash", extras["hash"] ?: id)

            OnlineSource.NETEASE -> {
                val songId = extras["song_id"] ?: extras["songId"] ?: id
                put(payload, "songmid", songId)
                put(payload, "hash", songId)
            }
        }
        return payload
    }

    private fun put(target: MutableMap<String, Any?>, key: String, value: Any?) {
        when {
            value == null -> Unit
            value is String && value.isEmpty() -> Unit
            else -> target[key] = value
        }
    }

    companion object {
        /** 艺人协作分隔符。 */
        private val ARTIST_SEPARATOR = Regex("[/、,，]\\s*")
    }
}
