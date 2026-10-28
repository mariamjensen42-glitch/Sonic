package com.cycling.data.online.lx

import android.content.Context
import com.cycling.domain.online.model.OnlinePlugin
import com.cycling.domain.online.model.OnlinePluginSource
import com.cycling.domain.online.model.OnlineSource
import timber.log.Timber

/**
 * 随 App 打包的洛雪（LX）音源脚本。
 *
 * 脚本正文放在 `assets/lx_sources/`，首次启动时写进 `filesDir/online_scripts/` 并登记成插件，
 * 之后和用户自己导入的插件走完全相同的链路（启停、排序、屏蔽、取链）。脚本来源与许可见
 * `assets/lx_sources/NOTICE.md`。
 *
 * 这里额外声明了每个脚本 `inited` 时会报的平台：没有它，首次启动要等脚本跑一遍才知道有哪些
 * 音源，界面会先显示「0 个平台」。首次探测后宿主的真实结果会覆盖它。
 */
internal object LxBundledSources {

    /** 一个内置脚本。 */
    data class Bundled(
        val key: String,
        val assetName: String,
        val fallbackName: String,
        val sources: List<OnlinePluginSource>
    ) {
        val pluginId: String get() = "$ID_PREFIX$key"

        val scriptFileName: String get() = "$key.js"
    }

    private val MUSIC_URL = listOf(OnlinePlugin.ACTION_MUSIC_URL)
    private val MUSIC_URL_LYRIC_PIC = listOf(
        OnlinePlugin.ACTION_MUSIC_URL,
        OnlinePlugin.ACTION_LYRIC,
        OnlinePlugin.ACTION_PIC
    )

    val ALL: List<Bundled> = listOf(
        Bundled(
            key = "xinghai",
            assetName = "lx_sources/xinghai.js",
            fallbackName = "星海音乐源",
            sources = listOf(
                source(OnlineSource.NETEASE, MUSIC_URL_LYRIC_PIC, "128k", "320k", "flac", "hires"),
                source(OnlineSource.QQ, MUSIC_URL_LYRIC_PIC, "128k", "320k", "flac", "hires"),
                source(OnlineSource.KUGOU, MUSIC_URL_LYRIC_PIC, "128k", "320k", "flac"),
                source(OnlineSource.KUWO, MUSIC_URL_LYRIC_PIC, "128k", "320k", "flac"),
                source(OnlineSource.MIGU, MUSIC_URL_LYRIC_PIC, "128k", "320k", "flac")
            )
        ),
        Bundled(
            key = "gdstudio",
            assetName = "lx_sources/gdstudio.js",
            fallbackName = "统一音乐源",
            sources = listOf(
                source(OnlineSource.NETEASE, MUSIC_URL, "128k", "320k", "flac"),
                source(OnlineSource.KUWO, MUSIC_URL, "128k", "320k", "flac")
            )
        ),
        Bundled(
            key = "meting",
            assetName = "lx_sources/meting.js",
            fallbackName = "稳定版音源",
            sources = listOf(
                source(OnlineSource.NETEASE, MUSIC_URL, "128k", "320k", "flac"),
                source(OnlineSource.QQ, MUSIC_URL, "128k", "320k", "flac"),
                source(OnlineSource.KUGOU, MUSIC_URL, "128k", "320k", "flac")
            )
        )
    )

    /** 读脚本正文；asset 缺失（打包出错）时返回 null，调用方跳过。 */
    fun read(context: Context, bundled: Bundled): String? = runCatching {
        context.assets.open(bundled.assetName).bufferedReader().use { it.readText() }
    }.onFailure {
        Timber.w(it, "LxBundledSources: 读取 %s 失败", bundled.assetName)
    }.getOrNull()

    private fun source(
        key: OnlineSource,
        actions: List<String>,
        vararg qualities: String
    ): OnlinePluginSource = OnlinePluginSource(
        key = key,
        name = key.displayName,
        actions = actions,
        qualities = qualities.toList()
    )

    private const val ID_PREFIX = "builtin:"
}
