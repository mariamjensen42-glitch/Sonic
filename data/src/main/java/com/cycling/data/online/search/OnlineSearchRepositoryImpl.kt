package com.cycling.data.online.search

import com.cycling.data.api.search.OnlineCatalogSearchService
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.repository.OnlineSearchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

/**
 * 在线目录搜索的默认实现：各平台并行请求，单平台失败只丢那一个平台。
 *
 * 并行而不是串行，是因为三个接口互不依赖，串行只会让首屏多等两个 RTT；
 * 而「丢平台」而不是「整体失败」，是因为搜索结果的语义本来就是有多少给多少。
 */
@Singleton
class OnlineSearchRepositoryImpl @Inject constructor(
    private val searchService: OnlineCatalogSearchService
) : OnlineSearchRepository {

    override suspend fun search(
        keyword: String,
        sources: List<OnlineSource>,
        page: Int,
        limitPerSource: Int
    ): List<MusicInfo> = coroutineScope {
        val requested = sources.ifEmpty { SEARCHABLE_SOURCES }
        val targets = requested
            .filter { it in SEARCHABLE_SOURCES }
            .distinct()
            .sortedBy { OnlineSource.ORDERED.indexOf(it) }

        targets.map { source ->
            async {
                try {
                    searchService.search(source, keyword, page, limitPerSource)
                } catch (e: CancellationException) {
                    // 用户还在打字时上一个关键词的请求会被取消。这里必须原样抛出去：
                    // 用 runCatching 吞掉的话，取消会被记成「搜索失败」，而且这一路的
                    // 结果会以「空列表」参与合并，最终列表看起来就比实际少。
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "OnlineSearch: %s 搜索失败", source.lxKey)
                    emptyList()
                }
            }
        }.awaitAll().flatten().borrowMissingCovers()
    }

    /**
     * 同一首歌常常在几个平台都搜得到，但封面字段的覆盖率各不相同
     * （酷我的 `web_albumpic_short` 实测只有一半条目有值，网易云 / QQ / 酷狗基本都有）。
     *
     * 这里用**已经在手的**结果互相补：同「歌名 + 首位歌手」的条目里，只要有一条带封面，
     * 其余几条就借过来。零额外请求，也不会把不同歌的封面串味 —— 匹配键同时含歌名和歌手。
     */
    private fun List<MusicInfo>.borrowMissingCovers(): List<MusicInfo> {
        if (isEmpty()) return this
        val covers = HashMap<String, String>()
        for (track in this) {
            val cover = track.coverUrl?.takeIf { it.isNotBlank() } ?: continue
            covers.putIfAbsent(track.coverKey(), cover)
        }
        if (covers.isEmpty()) return this
        return map { track ->
            if (!track.coverUrl.isNullOrBlank()) track
            else track.copy(coverUrl = covers[track.coverKey()])
        }
    }

    /** 跨平台对齐用的键：歌名 + 首位歌手，去掉空白、括号后缀和大小写差异。 */
    private fun MusicInfo.coverKey(): String {
        val normalizedTitle = title.substringBefore('(').substringBefore('（')
            .replace(" ", "")
            .lowercase()
        val firstArtist = artist.split('/', '、', ',', '，', '&')
            .firstOrNull { it.isNotBlank() }
            ?.replace(" ", "")
            ?.lowercase()
            .orEmpty()
        return "$normalizedTitle|$firstArtist"
    }

    private companion object {
        /** 有可用公开搜索接口的平台；汽水（SODA）暂时没有。 */
        val SEARCHABLE_SOURCES = listOf(
            OnlineSource.NETEASE,
            OnlineSource.QQ,
            OnlineSource.KUGOU,
            OnlineSource.KUWO,
            OnlineSource.MIGU
        )
    }
}
