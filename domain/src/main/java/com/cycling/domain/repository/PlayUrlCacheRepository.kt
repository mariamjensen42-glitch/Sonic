package com.cycling.domain.repository

import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.PlaySource

/**
 * 取链结果的短期缓存。
 *
 * 为什么要缓存：一次取链 = QuickJS 里跑脚本 + 一次可播性探测，是「点下一首」里最慢的一环；
 * 而音源地址通常有 5~30 分钟有效期，短时间内重复解析纯属浪费。
 *
 * 键是 `songId + quality`，所以同一首歌切档位不会命中错的地址；条目自带过期时间，
 * 过期即视为未命中（实现负责顺手删掉）。
 */
interface PlayUrlCacheRepository {

    suspend fun get(songId: Long, quality: MusicQuality): PlaySource?

    suspend fun put(
        songId: Long,
        quality: MusicQuality,
        source: PlaySource,
        ttlMs: Long = DEFAULT_TTL_MS
    )

    /** 丢掉一首歌的缓存。播放失败时调用：音源直链常带短期签名，过期后必须重新取。 */
    suspend fun invalidate(songId: Long, quality: MusicQuality)

    suspend fun clear()

    /** 清理过期条目；启动时跑一次就够。 */
    suspend fun purgeExpired()

    companion object {
        /** 20 分钟：比常见音源签名有效期短，又足够覆盖「连续听同一张专辑」。 */
        const val DEFAULT_TTL_MS: Long = 20 * 60 * 1000L
    }
}
