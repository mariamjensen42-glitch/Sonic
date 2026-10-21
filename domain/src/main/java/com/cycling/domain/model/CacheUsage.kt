package com.cycling.domain.model

/**
 * `cacheDir` 下的一个缓存目录。
 *
 * [id] 就是目录名（`image_cache`、`http_cache`）：目录名由各自的库决定，这里不枚举，
 * 界面对认识的 id 给出中文名，其余原样显示。
 */
data class DiskCache(val id: String, val bytes: Long)

/**
 * 缓存面板的一屏用量。
 *
 * 只涵盖**可再生的缓存**。自定义封面、音源脚本这些是用户数据，不在统计范围里 ——
 * 它们被清掉之后重拉不回来。
 */
data class CacheUsage(
    val diskCaches: List<DiskCache> = emptyList(),
    val lyricsEntries: Int = 0,
    val aiInfoEntries: Int = 0
) {
    val diskBytes: Long get() = diskCaches.sumOf { it.bytes }
}
