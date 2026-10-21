package com.cycling.domain.online.usecase

import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.PlaySourceResolution
import com.cycling.domain.online.repository.OnlinePluginRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 给一首在线歌曲取到可播放的地址。
 *
 * 多平台换源的输入由调用方给出：同一个歌名/歌手在其它平台的 [MusicInfo] 通常来自
 * 本地歌曲的在线匹配结果或用户手动指定。仓库内部按插件排序依次尝试：
 * 1. 第一个候选（歌曲自身平台）；
 * 2. `FALLBACK` 模式下继续试后续候选平台，`PLUGIN_ONLY` 只试第一个；
 * 3. 遇到 lxkey 额度/到期/停用/无效类错误立即停止（换平台也没用）。
 */
@Singleton
class ResolveOnlinePlaySourceUseCase @Inject constructor(
    private val pluginRepository: OnlinePluginRepository
) {

    suspend operator fun invoke(
        track: MusicInfo,
        quality: MusicQuality,
        alternatives: List<MusicInfo> = emptyList()
    ): PlaySourceResolution = pluginRepository.fetchPlaySource(
        candidates = listOf(track) + alternatives.filter { it.source != track.source },
        quality = quality
    )
}
