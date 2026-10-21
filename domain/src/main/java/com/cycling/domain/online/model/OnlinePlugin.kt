package com.cycling.domain.online.model

/**
 * 取链优先级策略。
 *
 * [FALLBACK] 是默认：本平台取不到时，允许沿调用方给出的其它平台候选继续试（成功率优先）；
 * [PLUGIN_ONLY] 只用歌曲自身所在平台，不跨平台换源。
 */
enum class PluginPriority {
    FALLBACK,
    PLUGIN_ONLY;

    companion object {
        fun fromStorage(value: String?): PluginPriority = when (value) {
            "pluginOnly" -> PLUGIN_ONLY
            else -> FALLBACK
        }

        fun storage(priority: PluginPriority): String = when (priority) {
            FALLBACK -> "prefer"
            PLUGIN_ONLY -> "pluginOnly"
        }
    }
}

/** 插件列表的排序方式。 */
enum class PluginSortMode(val storage: String) {
    MANUAL("manual"),
    PLATFORM_COUNT("platform_count"),
    SPEED("speed"),
    AVAILABLE("available");

    companion object {
        fun fromStorage(value: String?): PluginSortMode =
            entries.firstOrNull { it.storage == value } ?: MANUAL
    }
}

/** 脚本在 `inited` 事件里声明的单个音源能力。 */
data class OnlinePluginSource(
    val key: OnlineSource,
    val name: String,
    val actions: List<String> = emptyList(),
    val qualities: List<String> = emptyList(),
    val homeCategories: List<String> = emptyList()
) {
    fun supports(action: String): Boolean = actions.isEmpty() || action in actions
}

/** 脚本头部块注释里 `@name …` 解析出来的元信息。 */
data class PluginMeta(
    val name: String = "",
    val description: String = "",
    val version: String = "",
    val author: String = "",
    val homepage: String = ""
)

/**
 * 一个已安装的 LX 音源插件。
 *
 * 字段描述一个已安装的脚本音源；
 * 不含官方数据服务独占的 tier/access 字段 —— 本工程只保留洛雪式脚本取链。
 */
data class OnlinePlugin(
    val id: String,
    val name: String,
    val version: String = "",
    val author: String = "",
    val description: String = "",
    val homepage: String = "",
    val scriptFileName: String = "",
    val origin: String = "",
    val originUrl: String = "",
    val enabled: Boolean = true,
    /** 脚本声明 `@x-pro-only`：只有（外部约定的）赞助/授权用户能启用。 */
    val proOnly: Boolean = false,
    /** 脚本头部 `@key =` 声明的可配置项名字（不含值）。 */
    val configFields: List<String> = emptyList(),
    val sources: List<OnlinePluginSource> = emptyList(),
    val manualOrder: Int = 0,
    val addedAt: Long = 0L,
    val blockedPlatforms: Set<OnlineSource> = emptySet(),
    val autoBlockInvalid: Boolean = true,
    val autoBlockedPlatforms: Set<OnlineSource> = emptySet(),
    /** 导入时没识别到 `lx.` 标记（被压缩/混淆），仅提示，不影响执行。 */
    val opaque: Boolean = false,
    val error: String? = null,
    val status: String = ""
) {
    val musicPlatformCount: Int get() = sources.size

    fun supports(source: OnlineSource, action: String): Boolean =
        sources.any { it.key == source && it.supports(action) }

    fun isPlatformBlocked(source: OnlineSource): Boolean =
        source in blockedPlatforms || source in autoBlockedPlatforms

    companion object {
        /** 取链动作名，与 LX 协议一致。 */
        const val ACTION_MUSIC_URL = "musicUrl"
    }
}

/**
 * lxkey 授权失败的原因。
 *
 * 服务端（或脚本自身）把 lxkey 的错误以中文文案回传（额度用完、到期、停用、平台/音质不开放……），
 * 这里按 `LxKeyIssues` 的关键字表归类，供 UI 给出可操作的提示。
 */
enum class KeyIssueKind {
    QUOTA,
    EXPIRED,
    DISABLED,
    INVALID,
    NOT_ALLOWED,
    PLATFORM,
    QUALITY
}

data class KeyIssue(val kind: KeyIssueKind, val rawMessage: String)

/** 导入一个脚本的结果。 */
data class PluginImportResult(
    val plugin: OnlinePlugin? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = plugin != null
}

/** 失效平台自动屏蔽的结果：本轮新屏蔽几个、又恢复了几个。 */
data class AutoBlockResult(
    val newlyBlocked: Int = 0,
    val recovered: Int = 0
)

/** 插件探测（启动 + `inited` 握手）的结果。 */
data class PluginTestReport(
    val status: String,
    val sources: List<OnlinePluginSource> = emptyList(),
    val error: String? = null
) {
    val isOk: Boolean get() = error == null && status == "ok"
}

/** 一次取链尝试的结果。 */
sealed interface PlaySourceResolution {
    data class Success(
        val source: PlaySource,
        val pluginName: String,
        val quality: MusicQuality
    ) : PlaySourceResolution

    data class Failure(
        val message: String,
        val keyIssue: KeyIssue? = null
    ) : PlaySourceResolution

    data object NotConfigured : PlaySourceResolution
}
