package com.cycling.data.online.lx

import android.content.Context
import android.net.Uri
import com.cycling.data.online.PlayUrlProbe
import com.cycling.data.online.store.OnlineStore
import com.cycling.data.online.store.toDomain
import com.cycling.data.online.store.toStored
import com.cycling.domain.online.model.AutoBlockResult
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.OnlinePlugin
import com.cycling.domain.online.model.OnlinePluginSource
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.model.PlaySourceResolution
import com.cycling.domain.online.model.PluginImportResult
import com.cycling.domain.online.model.PluginPriority
import com.cycling.domain.online.model.PluginSortMode
import com.cycling.domain.online.model.PluginTestReport
import com.cycling.domain.online.repository.OnlinePluginRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 洛雪（LX）音源插件管理器：装、排序、屏蔽、执行、取链。
 *
 * 只保留本地脚本这一条链路
 * （不依赖任何官方数据服务）：
 *
 * - **插件生命周期**：导入（文件/URL/文本）→ 校验 → 写脚本文件 → 探测 `inited` → 落库；
 * - **排序**：手动 / 平台数 / 速度 / 可用性四种；
 * - **自动屏蔽**：探测失败的平台自动加入 `autoBlockedPlatforms`，恢复后自动移除；
 * - **取链**：按排序依次尝试插件 → 档位降级 → 失败换下一个插件/下一个候选平台；
 *   `lxkey` 额度/到期/停用/无效类错误立即终止（换平台也没用）。
 */
@Singleton
class LxPluginManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: OnlineStore,
    private val httpBridge: LxHttpBridge,
    private val playUrlProbe: PlayUrlProbe
) : OnlinePluginRepository {

    private val scriptMeta = LxScriptMeta()
    private val mutex = Mutex()

    private val _plugins = MutableStateFlow<List<OnlinePlugin>>(emptyList())
    override val plugins: StateFlow<List<OnlinePlugin>> = _plugins.asStateFlow()

    private val _sortMode = MutableStateFlow(PluginSortMode.MANUAL)
    override val sortMode: StateFlow<PluginSortMode> = _sortMode.asStateFlow()

    private val _priority = MutableStateFlow(PluginPriority.FALLBACK)
    override val priority: StateFlow<PluginPriority> = _priority.asStateFlow()

    /** 每个插件的启动耗时，给 `SPEED` 排序用。 */
    private val bootDurations = ConcurrentHashMap<String, Long>()

    /** 插件 id -> 运行中的脚本运行时。 */
    private val runners = ConcurrentHashMap<String, LxScriptRunner>()

    @Volatile
    private var initialized = false

    override suspend fun initialize() {
        mutex.withLock {
            if (initialized) return
            initialized = true
            val snapshot = store.readSnapshot()
            _sortMode.value = PluginSortMode.fromStorage(snapshot.sortMode)
            _priority.value = PluginPriority.fromStorage(snapshot.priority)
            _plugins.value = snapshot.plugins
                .map { it.toDomain() }
                .sortedBy { it.manualOrder }
            Timber.d("LxPluginManager: 已加载 %d 个插件", _plugins.value.size)
        }
    }

    // region 导入

    override suspend fun importFromContent(
        content: String,
        fileName: String?,
        originUrl: String?
    ): PluginImportResult = mutex.withLock {
        val reject = scriptMeta.validateForImport(content)
        if (reject != null) return@withLock PluginImportResult(error = reject)

        val meta = scriptMeta.parseMeta(content)
        val id = "script:" + scriptMeta.md5(content)
        val existing = _plugins.value.firstOrNull { it.id == id }
        val scriptFileName = "$id.js"
        store.writeScript(scriptFileName, content)

        val plugin = OnlinePlugin(
            id = id,
            name = meta.name.ifBlank {
                fileName?.substringAfterLast('/')?.substringBeforeLast('.') ?: "未命名音源"
            },
            version = meta.version,
            author = meta.author,
            description = meta.description,
            homepage = meta.homepage,
            scriptFileName = scriptFileName,
            origin = if (originUrl != null) "url" else "local",
            originUrl = originUrl.orEmpty(),
            enabled = existing?.enabled ?: true,
            proOnly = scriptMeta.isProOnly(content),
            configFields = scriptMeta.parseEmptyConfigFields(content),
            sources = existing?.sources.orEmpty(),
            manualOrder = existing?.manualOrder ?: nextManualOrder(),
            addedAt = System.currentTimeMillis(),
            blockedPlatforms = existing?.blockedPlatforms.orEmpty(),
            autoBlockInvalid = existing?.autoBlockInvalid ?: true,
            autoBlockedPlatforms = existing?.autoBlockedPlatforms.orEmpty(),
            opaque = scriptMeta.gateForImport(content) == LxScriptMeta.Gate.NO_LX_MARKER,
            status = "installing"
        )
        replacePlugin(plugin)
        persist()

        val probe = probePlugin(plugin)
        val installed = plugin.copy(
            sources = probe.sources.ifEmpty { plugin.sources },
            status = if (probe.error == null) "ok" else "error",
            error = probe.error
        )
        replacePlugin(installed)
        persist()
        PluginImportResult(plugin = installed, error = probe.error)
    }

    override suspend fun importFromUrl(url: String): PluginImportResult {
        val response = runCatching {
            httpBridge.request(
                url,
                mapOf(
                    "timeout" to 20_000,
                    "headers" to mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android) Sonic")
                )
            )
        }.getOrElse { error ->
            return PluginImportResult(error = "脚本下载失败: ${error.message}")
        }
        val content = response["body"] as? String
            ?: return PluginImportResult(error = "链接返回的不是脚本文本（可能是 API 响应或错误页）")
        return importFromContent(content, Uri.parse(url).lastPathSegment, url)
    }

    override suspend fun importFromFile(uri: String): PluginImportResult {
        val content = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }.getOrNull()
        } ?: return PluginImportResult(error = "无法读取所选文件")
        return importFromContent(content, Uri.parse(uri).lastPathSegment)
    }

    // endregion

    // region 管理

    override suspend fun setEnabled(pluginId: String, enabled: Boolean) = mutex.withLock {
        val plugin = _plugins.value.firstOrNull { it.id == pluginId } ?: return@withLock
        replacePlugin(plugin.copy(enabled = enabled))
        if (!enabled) disposeRunner(pluginId)
        persist()
    }

    override suspend fun setBlockedPlatforms(pluginId: String, platforms: Set<OnlineSource>) =
        mutex.withLock {
            val plugin = _plugins.value.firstOrNull { it.id == pluginId } ?: return@withLock
            replacePlugin(plugin.copy(blockedPlatforms = platforms))
            persist()
        }

    override suspend fun setAutoBlockEnabled(pluginId: String, enabled: Boolean) = mutex.withLock {
        val plugin = _plugins.value.firstOrNull { it.id == pluginId } ?: return@withLock
        replacePlugin(plugin.copy(autoBlockInvalid = enabled))
        persist()
    }

    override suspend fun setSortMode(mode: PluginSortMode) {
        _sortMode.value = mode
        store.saveSortMode(mode.storage)
    }

    override suspend fun setPriority(priority: PluginPriority) {
        _priority.value = priority
        store.savePriority(PluginPriority.storage(priority))
    }

    override suspend fun movePlugin(pluginId: String, targetIndex: Int) = mutex.withLock {
        val ordered = orderedPlugins().toMutableList()
        val from = ordered.indexOfFirst { it.id == pluginId }
        if (from < 0) return@withLock
        val target = targetIndex.coerceIn(0, ordered.lastIndex)
        if (from == target) return@withLock
        val moved = ordered.removeAt(from)
        ordered.add(target, moved)
        _plugins.value = ordered.mapIndexed { index, plugin -> plugin.copy(manualOrder = index) }
        persist()
    }

    override suspend fun removePlugin(pluginId: String) = mutex.withLock {
        val plugin = _plugins.value.firstOrNull { it.id == pluginId } ?: return@withLock
        disposeRunner(pluginId)
        store.deleteScript(plugin.scriptFileName)
        _plugins.value = _plugins.value.filterNot { it.id == pluginId }
        persist()
    }

    override suspend fun applyAutoBlockDetection(
        pluginId: String,
        probe: Map<OnlineSource, Boolean>
    ): AutoBlockResult = mutex.withLock {
        val plugin = _plugins.value.firstOrNull { it.id == pluginId } ?: return@withLock AutoBlockResult()
        if (!plugin.autoBlockInvalid || probe.isEmpty()) return@withLock AutoBlockResult()

        val previous = plugin.autoBlockedPlatforms
        val next = previous.toMutableSet()
        probe.forEach { (source, ok) ->
            when {
                ok -> next.remove(source)
                source !in plugin.blockedPlatforms -> next.add(source)
            }
        }
        val newlyBlocked = (next - previous).size
        val recovered = previous.count { probe[it] == true }
        if (next != previous) {
            replacePlugin(plugin.copy(autoBlockedPlatforms = next))
            persist()
            if (newlyBlocked > 0 || recovered > 0) {
                Timber.i(
                    "LxPluginManager: %s 自动屏蔽变更(+%d/-%d) -> %s",
                    plugin.name,
                    newlyBlocked,
                    recovered,
                    next.map { it.lxKey }
                )
            }
        }
        AutoBlockResult(newlyBlocked, recovered)
    }

    override suspend fun testPlugin(pluginId: String): PluginTestReport = mutex.withLock {
        val plugin = _plugins.value.firstOrNull { it.id == pluginId }
            ?: return@withLock PluginTestReport(status = "error", error = "插件不存在")
        disposeRunner(pluginId)
        probePlugin(plugin)
    }

    // endregion

    // region 取链

    override suspend fun fetchPlaySource(
        candidates: List<MusicInfo>,
        quality: MusicQuality
    ): PlaySourceResolution {
        initialize()
        if (candidates.isEmpty()) return PlaySourceResolution.NotConfigured
        val ordered = if (_priority.value == PluginPriority.PLUGIN_ONLY) {
            candidates.take(1)
        } else {
            candidates.distinctBy { it.source }
        }
        var failure: PlaySourceResolution.Failure? = null

        for (candidate in ordered) {
            val eligible = orderedPlugins().filter { plugin ->
                plugin.enabled &&
                    !plugin.isPlatformBlocked(candidate.source) &&
                    plugin.supports(candidate.source, OnlinePlugin.ACTION_MUSIC_URL)
            }
            for (plugin in eligible) {
                when (val result = tryLocal(plugin, candidate, quality)) {
                    is PlaySourceResolution.Success -> {
                        // 脚本只负责拼 URL，不会去访问它：后端挂了它照样「成功」。
                        // 这里探一下，不可播就当作这个插件失败，继续试下一个。
                        if (playUrlProbe.isPlayable(result.source.url)) return result
                        failure = PlaySourceResolution.Failure(
                            "「${plugin.name}」返回的地址不可播放（${result.source.url.take(96)}）"
                        )
                    }

                    is PlaySourceResolution.Failure -> {
                        failure = result
                        if (LxKeyIssues.isFatal(result.keyIssue)) return result
                    }

                    PlaySourceResolution.NotConfigured -> Unit
                }
            }
        }
        if (failure == null && orderedPlugins().none { it.enabled }) {
            return PlaySourceResolution.NotConfigured
        }
        return failure ?: PlaySourceResolution.NotConfigured
    }

    override suspend fun availableSources(): List<OnlineSource> = orderedPlugins()
        .filter { it.enabled }
        .flatMap { plugin -> plugin.sources.map { it.key } }
        .distinct()

    private suspend fun tryLocal(
        plugin: OnlinePlugin,
        musicInfo: MusicInfo,
        quality: MusicQuality
    ): PlaySourceResolution {
        val runner = ensureRunner(plugin) ?: return PlaySourceResolution.Failure(
            plugin.error ?: "插件启动失败"
        )
        val declared = plugin.sources
            .firstOrNull { it.key == musicInfo.source }
            ?.qualities
            .orEmpty()
        val candidates = qualityCandidates(declared, quality)
        var lastError: String? = null
        for (candidate in candidates) {
            try {
                val playSource = runner.musicUrl(musicInfo.source, musicInfo, candidate)
                return PlaySourceResolution.Success(
                    source = playSource,
                    pluginName = plugin.name,
                    quality = candidate
                )
            } catch (e: LxPluginQuarantinedException) {
                return PlaySourceResolution.Failure(e.message ?: "插件已隔离", null)
            } catch (e: Exception) {
                val message = e.message ?: "取链失败"
                lastError = message
                val issue = LxKeyIssues.classify(message)
                if (LxKeyIssues.isFatal(issue)) {
                    return PlaySourceResolution.Failure(message, issue)
                }
                Timber.d(
                    "LxPluginManager: %s 取链失败(%s/%s, declared=%s): %s",
                    plugin.name,
                    musicInfo.source.lxKey,
                    candidate.lxKey,
                    declared,
                    message
                )
            }
        }
        return PlaySourceResolution.Failure(lastError ?: "插件未返回可用地址")
    }

    private fun qualityCandidates(
        declared: List<String>,
        quality: MusicQuality
    ): List<MusicQuality> {
        val keys = MusicQuality.ladder(
            available = declared,
            preferred = quality,
            limit = MAX_QUALITY_ATTEMPTS,
            pluginOnlyPreferred = quality.pluginOnly
        )
        val mapped = keys.mapNotNull { key -> MusicQuality.fromLxKey(key) }
        return mapped.ifEmpty { listOf(quality) }
    }

    // endregion

    // region 运行时

    private suspend fun ensureRunner(plugin: OnlinePlugin): LxScriptRunner? {
        runners[plugin.id]?.let { existing ->
            if (existing.hasRuntime) return existing
            if (existing.isQuarantined) return existing
            existing.dispose()
            runners.remove(plugin.id)
        }
        val raw = store.readScript(plugin.scriptFileName) ?: return null
        val runner = LxScriptRunner(
            rawScript = raw,
            label = plugin.name,
            httpBridge = httpBridge,
            onLog = { message -> Timber.d("[lx:%s] %s", plugin.id, message) }
        )
        val startedAt = System.currentTimeMillis()
        val result = withTimeoutOrNull(RUNNER_BOOT_TIMEOUT_MS) { runner.init() }
        if (result == null || !result.ok) {
            runner.dispose()
            updatePluginAfterProbe(plugin.id, emptyList(), result?.error ?: "插件启动超时")
            return null
        }
        bootDurations[plugin.id] = System.currentTimeMillis() - startedAt
        if (result.sources.isNotEmpty()) {
            updatePluginAfterProbe(plugin.id, result.sources.values.toList(), null)
        }
        runners[plugin.id] = runner
        return runner
    }

    private suspend fun probePlugin(plugin: OnlinePlugin): PluginTestReport {
        val raw = store.readScript(plugin.scriptFileName)
            ?: return PluginTestReport(status = "error", error = "脚本文件缺失")
        val runner = LxScriptRunner(
            rawScript = raw,
            label = plugin.name,
            httpBridge = httpBridge,
            onLog = { message -> Timber.d("[lx:%s] %s", plugin.id, message) }
        )
        val startedAt = System.currentTimeMillis()
        val result = withTimeoutOrNull(RUNNER_BOOT_TIMEOUT_MS) { runner.init() }
        if (result == null || !result.ok) {
            runner.dispose()
            return PluginTestReport(status = "error", error = result?.error ?: "插件启动超时")
        }
        bootDurations[plugin.id] = System.currentTimeMillis() - startedAt
        disposeRunner(plugin.id)
        runners[plugin.id] = runner
        return PluginTestReport(status = "ok", sources = result.sources.values.toList())
    }

    private suspend fun updatePluginAfterProbe(
        pluginId: String,
        sources: List<OnlinePluginSource>,
        error: String?
    ) = mutex.withLock {
        val plugin = _plugins.value.firstOrNull { it.id == pluginId } ?: return@withLock
        replacePlugin(
            plugin.copy(
                sources = sources.ifEmpty { plugin.sources },
                status = if (error == null) "ok" else "error",
                error = error
            )
        )
        persist()
    }

    private fun disposeRunner(pluginId: String) {
        runners.remove(pluginId)?.dispose()
    }

    // endregion

    // region helpers

    private fun orderedPlugins(): List<OnlinePlugin> {
        val list = _plugins.value
        return when (_sortMode.value) {
            PluginSortMode.MANUAL -> list.sortedBy { it.manualOrder }
            PluginSortMode.PLATFORM_COUNT -> list.sortedWith(
                compareByDescending<OnlinePlugin> { it.musicPlatformCount }
                    .thenBy { it.manualOrder }
            )

            PluginSortMode.SPEED -> list.sortedWith(
                compareBy<OnlinePlugin> { bootDurations[it.id] ?: Long.MAX_VALUE }
                    .thenBy { it.manualOrder }
            )

            PluginSortMode.AVAILABLE -> list.sortedWith(
                compareBy<OnlinePlugin> { if (it.error != null) 1 else 0 }
                    .thenByDescending { it.musicPlatformCount }
                    .thenBy { it.manualOrder }
            )
        }
    }

    private fun nextManualOrder(): Int =
        (_plugins.value.maxOfOrNull { it.manualOrder } ?: -1) + 1

    private fun replacePlugin(plugin: OnlinePlugin) {
        val current = _plugins.value.toMutableList()
        val index = current.indexOfFirst { it.id == plugin.id }
        if (index >= 0) current[index] = plugin else current.add(plugin)
        _plugins.value = current.sortedBy { it.manualOrder }
    }

    private suspend fun persist() {
        runCatching { store.savePlugins(_plugins.value.map { it.toStored() }) }
            .onFailure { Timber.w(it, "LxPluginManager: 保存插件清单失败") }
    }

    // endregion

    companion object {
        private const val RUNNER_BOOT_TIMEOUT_MS = 20_000L
        private const val MAX_QUALITY_ATTEMPTS = 4
    }
}
