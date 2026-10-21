package com.cycling.domain.online.usecase

import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.repository.OnlinePluginRepository
import com.cycling.domain.online.repository.OnlineSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索页「在线音乐」一次查询的结果。
 *
 * [hasSources] 用来区分两种空：**没有可用的音源**（该去设置页导入脚本）和
 * **有音源但没搜到**（换个关键词）。两者的引导完全不同，不能让 UI 自己猜。
 */
data class OnlineSearchOutcome(
    val tracks: List<MusicInfo> = emptyList(),
    val hasSources: Boolean = true
)

/**
 * 按关键词搜在线歌曲，只搜「已启用插件声明支持」的平台。
 *
 * 多搜一个平台不会让结果更好，只会让用户点到一条取不了链的条目：
 * 插件没声明 `kg`，酷狗的搜索结果就永远播不了。
 */
@Singleton
class SearchOnlineMusicUseCase @Inject constructor(
    private val searchRepository: OnlineSearchRepository,
    private val pluginRepository: OnlinePluginRepository
) {

    suspend operator fun invoke(
        keyword: String,
        page: Int = 1,
        limitPerSource: Int = OnlineSearchRepository.DEFAULT_LIMIT_PER_SOURCE
    ): OnlineSearchOutcome {
        if (keyword.isBlank()) return OnlineSearchOutcome()
        // `availableSources()` 不像 `fetchPlaySource()` 那样自初始化：不先加载，
        // 冷启动直接搜会得到空平台表，看起来像「没装音源」。
        pluginRepository.initialize()
        val sources = pluginRepository.availableSources()
        if (sources.isEmpty()) return OnlineSearchOutcome(hasSources = false)
        return OnlineSearchOutcome(
            tracks = searchRepository.search(
                keyword = keyword.trim(),
                sources = sources,
                page = page.coerceAtLeast(1),
                limitPerSource = limitPerSource
            )
        )
    }
}
