package com.cycling.data.repository

import com.cycling.data.local.dao.PlayUrlCacheDao
import com.cycling.data.local.entity.PlayUrlCacheEntity
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.PlaySource
import com.cycling.domain.repository.PlayUrlCacheRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class PlayUrlCacheRepositoryImpl @Inject constructor(
    private val dao: PlayUrlCacheDao
) : PlayUrlCacheRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val headersSerializer = MapSerializer(String.serializer(), String.serializer())

    override suspend fun get(songId: Long, quality: MusicQuality): PlaySource? {
        val entry = dao.get(songId, quality.name) ?: return null
        if (entry.expiresAt <= System.currentTimeMillis()) {
            dao.delete(songId, quality.name)
            return null
        }
        return PlaySource(
            url = entry.url,
            headers = entry.headersJson?.let(::decodeHeaders).orEmpty()
        )
    }

    override suspend fun put(
        songId: Long,
        quality: MusicQuality,
        source: PlaySource,
        ttlMs: Long
    ) {
        val now = System.currentTimeMillis()
        dao.put(
            PlayUrlCacheEntity(
                songId = songId,
                quality = quality.name,
                url = source.url,
                headersJson = source.headers.takeIf { it.isNotEmpty() }?.let {
                    runCatching { json.encodeToString(headersSerializer, it) }.getOrNull()
                },
                createdAt = now,
                expiresAt = now + ttlMs
            )
        )
    }

    override suspend fun invalidate(songId: Long, quality: MusicQuality) {
        dao.delete(songId, quality.name)
        Timber.d("PlayUrlCache: 作废缓存 song=%d quality=%s", songId, quality.name)
    }

    override suspend fun clear() {
        dao.clear()
    }

    override suspend fun purgeExpired() {
        val removed = dao.purgeExpired(System.currentTimeMillis())
        if (removed > 0) Timber.d("PlayUrlCache: 清理 %d 条过期取链", removed)
    }

    private fun decodeHeaders(raw: String): Map<String, String> = runCatching {
        json.decodeFromString(headersSerializer, raw)
    }.getOrElse {
        Timber.w(it, "PlayUrlCache: 请求头解析失败，按无头处理")
        emptyMap()
    }
}
