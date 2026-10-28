package com.cycling.domain.online.repository

import com.cycling.domain.online.model.AutoBlockResult
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.OnlineLyric
import com.cycling.domain.online.model.OnlinePlugin
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.model.PlaySourceResolution
import com.cycling.domain.online.model.PluginImportResult
import com.cycling.domain.online.model.PluginPriority
import com.cycling.domain.online.model.PluginSortMode
import com.cycling.domain.online.model.PluginTestReport
import kotlinx.coroutines.flow.StateFlow

/**
 * 洛雪（LX）式音源插件的安装、排序、屏蔽与执行。
 *
 * 脚本在 QuickJS 里跑：`inited` 声明支持的平台/档位，取链时宿主发
 * `{source, action: 'musicUrl', info: {type, musicInfo}}`，脚本返回 URL（可带 headers）。
 * 上层只看到「插件列表 + 取链结果」。
 */
interface OnlinePluginRepository {

    val plugins: StateFlow<List<OnlinePlugin>>

    val sortMode: StateFlow<PluginSortMode>

    val priority: StateFlow<PluginPriority>

    /** 读取持久化的插件与设置；幂等，可重复调用。 */
    suspend fun initialize()

    /** 从脚本文本导入（文件内容 / 剪贴板 / 远程内容）。 */
    suspend fun importFromContent(
        content: String,
        fileName: String? = null,
        originUrl: String? = null
    ): PluginImportResult

    /** 从 URL 拉取脚本再导入。 */
    suspend fun importFromUrl(url: String): PluginImportResult

    /** 从 `content://` / `file://` URI 读取脚本导入。 */
    suspend fun importFromFile(uri: String): PluginImportResult

    suspend fun setEnabled(pluginId: String, enabled: Boolean)

    suspend fun setBlockedPlatforms(pluginId: String, platforms: Set<OnlineSource>)

    suspend fun setAutoBlockEnabled(pluginId: String, enabled: Boolean)

    suspend fun setSortMode(mode: PluginSortMode)

    suspend fun setPriority(priority: PluginPriority)

    /** 手动排序：把插件移到 [targetIndex]（只在 [PluginSortMode.MANUAL] 下生效）。 */
    suspend fun movePlugin(pluginId: String, targetIndex: Int)

    suspend fun removePlugin(pluginId: String)

    /**
     * 把随 App 打包的内置音源装回来（用户删过的话）。
     *
     * 返回本次实际新增的插件数；启动时已经自动装上的那些不算。
     */
    suspend fun restoreBundledPlugins(): Int

    /**
     * 把一轮「各平台是否可用」的探测结果落到插件的自动屏蔽列表。
     *
     * 探测失败且在 LX 平台键集合内、又没有被用户手动屏蔽的平台，会被自动屏蔽；
     * 探测恢复的平台会从自动屏蔽里移除。
     */
    suspend fun applyAutoBlockDetection(
        pluginId: String,
        probe: Map<OnlineSource, Boolean>
    ): AutoBlockResult

    /** 试运行插件并确认 `inited` 声明了哪些音源。 */
    suspend fun testPlugin(pluginId: String): PluginTestReport

    /**
     * 按当前优先级与排序取链。
     *
     * [candidates] 是同一首歌在若干平台的 [MusicInfo]：第一个是歌曲自身平台，
     * 其余是换源候选（按尝试顺序）。`PLUGIN_ONLY` 只试第一个。
     *
     * 实现内部负责：插件排序、失效平台跳过、档位降级、连续超时隔离，
     * 以及失败时的 lxkey 错误归类。
     */
    suspend fun fetchPlaySource(
        candidates: List<MusicInfo>,
        quality: MusicQuality
    ): PlaySourceResolution

    /**
     * 用脚本取歌词（LX `action = 'lyric'`）。
     *
     * 与取链不同，歌词只有「有没有」两种结果，没有可播性探测，也没有跨平台换源：
     * 只按插件排序问一遍所有声明了该 action、且没屏蔽这个平台的插件，第一个答出来的赢。
     * 全部失败或无插件支持时返回 null。
     */
    suspend fun fetchLyric(track: MusicInfo): OnlineLyric?

    /**
     * 用脚本取封面地址（LX `action = 'pic'`）。
     *
     * 返回可直接加载的 http(s) 地址；没有插件能给出时返回 null。
     */
    suspend fun fetchCover(track: MusicInfo): String?

    /** 当前至少有一个已启用插件声明支持的平台。 */
    suspend fun availableSources(): List<OnlineSource>
}
