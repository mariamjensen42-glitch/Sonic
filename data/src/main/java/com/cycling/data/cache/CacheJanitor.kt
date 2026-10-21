package com.cycling.data.cache

import com.cycling.domain.repository.CacheRepository
import com.cycling.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * 缓存上限的执行者。
 *
 * 超限时按占用**从大到小整目录清空**，回到上限内就停。整目录是唯一安全的粒度：
 * OkHttp 与 Coil 的缓存目录里各有一份 journal，挑文件删会让索引和数据对不上，
 * 下一次读取直接抛异常。代价是清一次退得比 LRU 狠 —— 对这些可再生的小缓存可以接受。
 */
@Singleton
class CacheJanitor @Inject constructor(
    private val cacheRepository: CacheRepository,
    private val settingsRepository: SettingsRepository
) {

    /** 检查并清理；返回释放的字节数，没做事就是 0。 */
    suspend fun cleanIfNeeded(): Long {
        if (!settingsRepository.autoCleanCacheEnabled.first()) return 0L
        val limit = settingsRepository.cacheLimitBytes.first()
        val caches = cacheRepository.usage().diskCaches
        var over = caches.sumOf { it.bytes } - limit
        if (over <= 0L) return 0L

        var freed = 0L
        for (cache in caches.sortedByDescending { it.bytes }) {
            if (over <= 0L) break
            val released = cacheRepository.clearDiskCache(cache.id)
            freed += released
            over -= released
            Timber.i("CacheJanitor: 清空 %s，释放 %d 字节", cache.id, released)
        }
        return freed
    }
}
