package com.cycling.domain.online.repository

import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.OnlineSource

/**
 * 在线目录搜索：把关键词变成各平台的 [MusicInfo]（带平台内的歌曲 id）。
 *
 * 与 [OnlinePluginRepository] 的分工是硬的：洛雪（LX）音源脚本只实现「给定平台歌曲 id →
 * 播放地址」，**不实现搜索**。所以「搜歌」必须由宿主自己打各平台的公开搜索接口，
 * 否则取链时脚本拿不到 `songmid` / `hash`，再好的音源也无从下手。
 */
interface OnlineSearchRepository {

    /**
     * 并行搜索 [sources]（空表示默认可搜的平台），每页每个平台最多取 [limitPerSource] 条。
     *
     * 单个平台失败只丢那一个平台的结果，不抛异常：搜索结果本来就是「有多少给多少」。
     * 返回顺序固定按 [OnlineSource.ORDERED]，同一平台内保持接口返回的相关度顺序。
     *
     * [page] 从 1 开始；翻页由调用方拼接（见 `SearchViewModel.loadMoreOnline`），
     * 各平台的分页参数不一样，统一在这里换算。
     */
    suspend fun search(
        keyword: String,
        sources: List<OnlineSource> = emptyList(),
        page: Int = 1,
        limitPerSource: Int = DEFAULT_LIMIT_PER_SOURCE
    ): List<MusicInfo>

    companion object {
        /** 每页每平台条数：5 个平台 × 10 = 50 条一屏，翻页再要下一批。 */
        const val DEFAULT_LIMIT_PER_SOURCE = 10
    }
}
