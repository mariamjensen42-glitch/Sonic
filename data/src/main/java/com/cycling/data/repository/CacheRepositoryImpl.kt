package com.cycling.data.repository

import android.content.Context
import com.cycling.data.local.dao.AiInfoCacheDao
import com.cycling.data.local.dao.LyricsCacheDao
import com.cycling.domain.model.CacheUsage
import com.cycling.domain.model.DiskCache
import com.cycling.domain.repository.CacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 缓存用量统计与清理。
 *
 * 只动 `cacheDir` 下的**整目录**：OkHttp 的 journal 跟目录一起消失时会自己重建，
 * 而按文件挑着删会把 journal 和数据删得不一致 —— 下一次请求会读到「索引里有、文件没了」
 * 的条目，然后在缓存读取出错。整目录删掉就没有这个中间态。
 *
 * `filesDir` 里的 `covers`（用户换的封面）和 `online_scripts`（导入的音源脚本）是用户数据，
 * 清掉之后重拉不回来，所以这里一律不碰。
 */
@Singleton
class CacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsCacheDao: LyricsCacheDao,
    private val aiInfoCacheDao: AiInfoCacheDao
) : CacheRepository {

    override suspend fun usage(): CacheUsage = withContext(Dispatchers.IO) {
        CacheUsage(
            diskCaches = cacheDirs().map { DiskCache(id = it.name, bytes = it.sizeOnDisk()) },
            lyricsEntries = lyricsCacheDao.count(),
            aiInfoEntries = aiInfoCacheDao.count()
        )
    }

    override suspend fun clearDiskCache(id: String): Long = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, id)
        // id 来自 usage()，只可能是 cacheDir 的直接子目录；这里再挡一次拼接出来的路径。
        if (dir.parentFile?.canonicalPath != context.cacheDir.canonicalPath) {
            Timber.w("CacheRepository: 拒绝清理 cacheDir 之外的路径 %s", id)
            return@withContext 0L
        }
        val freed = dir.sizeOnDisk()
        if (!dir.deleteRecursively() && dir.exists()) {
            Timber.w("CacheRepository: %s 未能完全删除", id)
        }
        freed
    }

    override suspend fun clearLyricsCache(): Int = withContext(Dispatchers.IO) {
        val count = lyricsCacheDao.count()
        lyricsCacheDao.clear()
        count
    }

    override suspend fun clearAiInfoCache(): Int = withContext(Dispatchers.IO) {
        val count = aiInfoCacheDao.count()
        aiInfoCacheDao.clear()
        count
    }

    /** cacheDir 的直接子目录：Coil 的 `image_cache`、OkHttp 的 `http_cache` 都在这一层。 */
    private fun cacheDirs(): List<File> =
        context.cacheDir.listFiles()?.filter { it.isDirectory }.orEmpty()
}

/** 目录树里所有普通文件的字节数之和；不存在或为空目录都是 0。 */
internal fun File.sizeOnDisk(): Long =
    walkBottomUp().filter { it.isFile }.sumOf { it.length() }
