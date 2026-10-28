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
import com.cycling.domain.online.model.OnlineLyric
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
import kotlin.coroutines.cancellation.CancellationException

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
 *   `lxkey` 额度/到期/停用/无效类错误立即终止（换平台也没用）；
 * - **取词 / 取封面**：同一个排序，只问声明了 `lyric` / `pic` 的插件，第一个答出来的赢；
 * - **健康度**：每次取链/取词/取封面记一次成败，连续失败 [OnlinePlugin.HEALTH_DEMOTE_THRESHOLD]
 *   次后降到队尾（仍会被尝试，成功一次即恢复）。
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

    /** 用户删掉的内置音源 id，用来阻止下次启动又装回来。 */
    private var removedBuiltins: Set<String> = emptySet()

    @Volatile
    private var initialized = false

    override suspend fun initialize() {
        mutex.withLock {
            if (initialized) return
            loadLocked()
        }
    }

    /** 已持有 [mutex] 时用这个。 */
    private suspend fun loadLocked() {
        initialized = true
        val snapshot = store.readSnapshot()
        _sortMode.value = PluginSortMode.fromStorage(snapshot.sortMode)
        _priority.value = PluginPriority.fromStorage(snapshot.priority)
        removedBuiltins = snapshot.removedBuiltins
        _plugins.value = snapshot.plugins
            .map { it.toDomain() }
            .sortedBy { it.manualOrder }
        seedBundledLocked(removedBuiltins)
        Timber.d("LxPluginManager: 已加载 %d 个插件", _plugins.value.size)
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
            status = "installing",
            // 同一个脚本重装时保留健康度：失败过的插件不该因为重装一次就假装自己是健康的。
            consecutiveFailures = existing?.consecutiveFailures ?: 0,
            lastFailureAt = existing?.lastFailureAt ?: 0L,
            lastSuccessAt = existing?.lastSuccessAt ?: 0L
        )
        replacePlugin(plugin)
        persist()

        val probe = probePlugin(plugin)
        val probeOk = probe.error == null
        val installed = plugin.copy(
            sources = probe.sources.ifEmpty { plugin.sources },
            status = if (probeOk) "ok" else "error",
            error = probe.error,
            // 刚装完就能启动，等于一次成功的健康度采样；探测失败则保留原来的失败计数。
            consecutiveFailures = if (probeOk) 0 else plugin.consecutiveFailures,
            lastSuccessAt = if (probeOk) System.currentTimeMillis() else plugin.lastSuccessAt
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
        // 内置脚本记一笔「用户不想要」，否则下次启动又回来了。
        if (plugin.isBuiltin) {
            removedBuiltins = removedBuiltins + pluginId
            store.saveRemovedBuiltins(removedBuiltins)
        }
        persist()
    }

    override suspend fun restoreBundledPlugins(): Int = mutex.withLock {
        if (!initialized) loadLocked()
        if (removedBuiltins.isNotEmpty()) {
            removedBuiltins = emptySet()
            store.saveRemovedBuiltins(emptySet())
        }
        val before = _plugins.value.size
        seedBundledLocked(emptySet())
        _plugins.value.size - before
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
        val report = probePlugin(plugin)
        // 手动测试也算一次健康度采样：测通了就把降权摘掉，免得用户点了「测试」却还排在队尾。
        applyResultLocked(pluginId, ok = report.isOk)
        report
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
            for (plugin in eligiblePlugins(candidate.source, OnlinePlugin.ACTION_MUSIC_URL)) {
                when (val result = tryLocal(plugin, candidate, quality)) {
                    is PlaySourceResolution.Success -> {
                        // 脚本只负责拼 URL，不会去访问它：后端挂了它照样「成功」。
                        // 这里探一下，不可播就当作这个插件失败，继续试下一个。
                        if (playUrlProbe.isPlayable(result.source.url)) {
                            noteResult(plugin.id, ok = true)
                            return result
                        }
                        noteResult(plugin.id, ok = false)
                        failure = PlaySourceResolution.Failure(
                            "「${plugin.name}」返回的地址不可播放（${result.source.url.take(96)}）"
                        )
                    }

                    is PlaySourceResolution.Failure -> {
                        noteResult(plugin.id, ok = false)
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

    // endregion

    // region 歌词与封面

    override suspend fun fetchLyric(track: MusicInfo): OnlineLyric? {
        initialize()
        for (plugin in eligiblePlugins(track.source, OnlinePlugin.ACTION_LYRIC)) {
            val runner = ensureRunner(plugin)
            if (runner == null) {
                noteResult(plugin.id, ok = false)
                continue
            }
            try {
                val lyric = runner.lyric(track.source, track)
                noteResult(plugin.id, ok = true)
                return lyric
            } catch (e: CancellationException) {
                throw e
            } catch (e: LxPluginQuarantinedException) {
                noteResult(plugin.id, ok = false)
            } catch (e: Exception) {
                noteResult(plugin.id, ok = false)
                Timber.d(
                    "LxPluginManager: %s 取词失败(%s): %s",
                    plugin.name,
                    track.source.lxKey,
                    e.message
                )
            }
        }
        return null
    }

    override suspend fun fetchCover(track: MusicInfo): String? {
        initialize()
        for (plugin in eligiblePlugins(track.source, OnlinePlugin.ACTION_PIC)) {
            val runner = ensureRunner(plugin)
            if (runner == null) {
                noteResult(plugin.id, ok = false)
                continue
            }
            try {
                val url = runner.pic(track.source, track)
                noteResult(plugin.id, ok = true)
                return url
            } catch (e: CancellationException) {
                throw e
            } catch (e: LxPluginQuarantinedException) {
                noteResult(plugin.id, ok = false)
            } catch (e: Exception) {
                noteResult(plugin.id, ok = false)
                Timber.d(
                    "LxPluginManager: %s 取封面失败(%s): %s",
                    plugin.name,
                    track.source.lxKey,
                    e.message
                )
            }
        }
        return null
    }

    // endregion

    // region 取链与取词共用

    /** 按当前排序，挑出能处理 [source] 的 [action] 的插件。 */
    private fun eligiblePlugins(source: OnlineSource, action: String): List<OnlinePlugin> =
        orderedPlugins().filter { plugin ->
            plugin.enabled &&
                !plugin.isPlatformBlocked(source) &&
                plugin.supports(source, action)
        }

    /**
     * 记一次取链 / 取词 / 取封面的结果。
     *
     * 成功且本来就没有失败计数时直接返回：否则每播一首歌都要写一遍 DataStore。
     */
    private suspend fun noteResult(pluginId: String, ok: Boolean) {
        val current = _plugins.value.firstOrNull { it.id == pluginId } ?: return
        if (ok && current.consecutiveFailures == 0) return
        mutex.withLock { applyResultLocked(pluginId, ok) }
    }

    /** 已经持有 [mutex] 时用这个（`Mutex` 不可重入，套用会死锁）。 */
    private suspend fun applyResultLocked(pluginId: String, ok: Boolean) {
        val plugin = _plugins.value.firstOrNull { it.id == pluginId } ?: return
        val now = System.currentTimeMillis()
        val updated = if (ok) {
            plugin.copy(consecutiveFailures = 0, lastSuccessAt = now)
        } else {
            plugin.copy(
                consecutiveFailures = plugin.consecutiveFailures + 1,
                lastFailureAt = now
            )
        }
        if (updated == plugin) return
        replacePlugin(updated)
        persist()
    }

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
            } catch (e: CancellationException) {
                throw e
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

    /**
     * 当前排序 + 健康度降权。
     *
     * 降权是**稳定排序的第二趟**：连续失败够多的插件挪到队尾，其余保持用户选的那种排序。
     * 不能把它们直接剔掉 —— 后端恢复后只有靠再被尝试一次才能把计数清零。
     */
    private fun orderedPlugins(): List<OnlinePlugin> {
        val list = _plugins.value
        val sorted = when (_sortMode.value) {
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
        return sorted.sortedBy { if (it.isDemoted) 1 else 0 }
    }

    private fun nextManualOrder(): Int =
        (_plugins.value.maxOfOrNull { it.manualOrder } ?: -1) + 1

    /**
     * 把还没装过的内置脚本写进脚本目录并登记成插件。
     *
     * [skip] 是用户手动删掉的 id。内置插件不探测：启动时跑三个 QuickJS 运行时太贵，
     * 平台声明直接用 [LxBundledSources] 里的静态值，首次取链时再校正。
     */
    private suspend fun seedBundledLocked(skip: Set<String>) {
        var order = nextManualOrder()
        val added = ArrayList<OnlinePlugin>(LxBundledSources.ALL.size)
        for (bundled in LxBundledSources.ALL) {
            if (bundled.pluginId in skip) continue
            if (_plugins.value.any { it.id == bundled.pluginId }) continue
            val content = LxBundledSources.read(context, bundled) ?: continue
            val meta = scriptMeta.parseMeta(content)
            store.writeScript(bundled.scriptFileName, content)
            added += OnlinePlugin(
                id = bundled.pluginId,
                name = meta.name.ifBlank { bundled.fallbackName },
                version = meta.version,
                author = meta.author,
                description = meta.description,
                homepage = meta.homepage,
                scriptFileName = bundled.scriptFileName,
                origin = OnlinePlugin.ORIGIN_BUILTIN,
                sources = bundled.sources,
                manualOrder = order++,
                addedAt = System.currentTimeMillis(),
                status = "ok"
            )
        }
        if (added.isEmpty()) return
        _plugins.value = (_plugins.value + added).sortedBy { it.manualOrder }
        persist()
        Timber.i("LxPluginManager: 装入 %d 个内置音源", added.size)
    }

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
