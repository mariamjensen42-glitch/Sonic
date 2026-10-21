package com.cycling.data.online.lx

import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.OnlinePlugin
import com.cycling.domain.online.model.OnlinePluginSource
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.model.PlaySource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/** 一个脚本的启动结果。 */
data class LxScriptInitResult(
    val ok: Boolean,
    val error: String? = null,
    val sources: Map<String, OnlinePluginSource> = emptyMap()
)

/** 脚本被隔离（连续多次无响应）时抛出，调用方应跳过该插件。 */
class LxPluginQuarantinedException(val label: String) : IllegalStateException(
    "插件脚本连续无响应，已暂停使用($label)，稍后将自动恢复；如持续失败请检查或重装该插件"
)

/**
 * 一个音源脚本的完整生命周期：启动 → `inited` 握手 → 逐次请求 → 隔离/重建。
 *
 * 行为要点：
 * - 初始化 60s 超时；`inited` 事件必须在超时前到达（或脚本 eval 结束时已经声明过音源）；
 * - 每次请求用 `callId` 关联 `__lxReturn`，默认 15s 超时；
 * - **连续 3 次超时判定脚本失控**，进入 60s 隔离；隔离期满自动重建运行时再试；
 * - 脚本自身抛错按原样回传，不污染宿主线程。
 */
class LxScriptRunner(
    private val rawScript: String,
    private val label: String,
    private val httpBridge: LxHttpBridge,
    private val onLog: ((String) -> Unit)? = null,
    private val proTokenProvider: suspend () -> String = { "" }
) {

    private val mutex = Mutex()
    private val meta = LxScriptMeta()
    private val calls = LinkedHashMap<Int, CompletableDeferred<Any?>>()

    private var callSeq = 0
    private var consecutiveTimeouts = 0
    private var initDeferred: CompletableDeferred<Unit>? = null
    private var initedSources: Map<String, OnlinePluginSource> = emptyMap()
    private var runtime: LxJsRuntime? = null

    @Volatile
    private var disposed = false

    @Volatile
    private var quarantined = false

    @Volatile
    private var quarantinedAt = 0L

    /** 脚本通过 `lx.send('updateAlert', …)` 上报的更新提醒；由管理器转成 UI 提示。 */
    var lastUpdateAlert: Any? = null

    val sources: Map<String, OnlinePluginSource> get() = initedSources
    val isQuarantined: Boolean get() = quarantined
    val hasRuntime: Boolean get() = runtime != null

    suspend fun init(): LxScriptInitResult = bootRuntime()

    suspend fun bootRuntime(): LxScriptInitResult = mutex.withLock {
        if (disposed) return LxScriptInitResult(false, "脚本已释放")
        disposeRuntime()
        val created = LxJsRuntime(httpBridge)
        created.proToken = runCatching { proTokenProvider() }.getOrDefault("")
        created.onConsole = { _, text -> log(text) }
        created.onEvent = { event, data -> handleEvent(event, data) }
        created.onReturn = { callId, ok, err -> handleReturn(callId, ok, err) }
        runtime = created
        try {
            created.init()
        } catch (e: Exception) {
            Timber.w(e, "LxScriptRunner(%s): runtime init failed", label)
            disposeRuntime()
            return LxScriptInitResult(false, e.message ?: "运行时初始化失败")
        }

        val parsedMeta = meta.parseMeta(rawScript)
        runCatching { created.evalRaw(LxBootstrap.scriptInfoJs(parsedMeta, rawScript)) }
            .onFailure { Timber.w(it, "LxScriptRunner(%s): scriptInfo eval failed", label) }

        initDeferred = CompletableDeferred()
        val evalResult = try {
            withTimeout(INIT_TIMEOUT_MS) {
                created.evalRaw(LxBootstrap.wrapUserScript(rawScript))
            }
        } catch (e: TimeoutCancellationException) {
            "ERROR: 初始化超时(${INIT_TIMEOUT_MS}ms)"
        }
        val evalError = evalResult.takeIf { it.startsWith(ERROR_PREFIX) }

        val result = when {
            initedSources.isNotEmpty() -> {
                if (evalError != null) log("[lx] inited 后抛错(按官方行为忽略): $evalResult")
                LxScriptInitResult(true, null, initedSources)
            }

            evalError != null -> LxScriptInitResult(false, evalError.removePrefix(ERROR_PREFIX).trim())

            else -> {
                val handshake = withTimeoutOrNull(INIT_TIMEOUT_MS) {
                    initDeferred?.await()
                    true
                } ?: false
                if (handshake) {
                    LxScriptInitResult(true, null, initedSources)
                } else {
                    LxScriptInitResult(false, "初始化超时:未收到 inited 事件")
                }
            }
        }
        if (!result.ok) {
            disposeRuntime()
            consecutiveTimeouts = 0
            log("[lx] 启动失败: ${result.error}")
        } else {
            log("[lx] 启动成功，声明音源: ${initedSources.keys.joinToString()}")
        }
        result
    }

    /**
     * 向脚本发一次请求。`info` 会以 JSON 传给脚本的 `request` 处理器，
     * 返回值是脚本 `JSON.stringify` 后再解析回来的数据。
     */
    suspend fun request(
        source: String,
        action: String,
        info: Map<String, Any?>,
        timeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS
    ): Any? {
        check(!disposed) { "脚本已释放" }
        recoverFromQuarantineIfNeeded()
        ensureRuntimeLive()
        if (quarantined) throw LxPluginQuarantinedException(label)

        val callId = ++callSeq
        val deferred = CompletableDeferred<Any?>()
        calls[callId] = deferred
        val payload = JSONObject()
            .put("source", source)
            .put("action", action)
            .put("info", JSONObject.wrap(info) ?: JSONObject.NULL)
            .toString()
        val live = runtime
        if (live == null) {
            calls.remove(callId)
            throw IllegalStateException("插件启动失败")
        }
        live.dispatchRequestAsync(callId, payload)

        return try {
            val result = withTimeout(timeoutMs) { deferred.await() }
            consecutiveTimeouts = 0
            result
        } catch (e: TimeoutCancellationException) {
            calls.remove(callId)
            onRequestTimeout()
            throw IllegalStateException("LX 脚本请求超时(${timeoutMs}ms)")
        } catch (e: Throwable) {
            calls.remove(callId)
            throw e
        }
    }

    /** 取链：`action = musicUrl`，`info = { type: <档位>, musicInfo: <歌曲信息> }`。 */
    suspend fun musicUrl(
        source: OnlineSource,
        info: MusicInfo,
        quality: MusicQuality,
        timeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS
    ): PlaySource {
        val result = request(
            source = source.lxKey,
            action = OnlinePlugin.ACTION_MUSIC_URL,
            info = linkedMapOf(
                "type" to quality.lxKey,
                "musicInfo" to info.toLxPayload()
            ),
            timeoutMs = timeoutMs
        )
        return extractPlaySource(result) ?: throw IllegalStateException("脚本返回空 URL")
    }

    /** 定向执行一段 JS（插件自检用）。 */
    suspend fun evalRaw(code: String): String = runtime?.evalRaw(code) ?: "ERROR: runtime 未初始化"

    fun dispose() {
        disposed = true
        calls.values.forEach { it.completeExceptionally(IllegalStateException("脚本已释放")) }
        calls.clear()
        disposeRuntime()
        Timber.d("LxScriptRunner.dispose: %s", label)
    }

    // region internals

    private fun disposeRuntime() {
        runtime?.dispose()
        runtime = null
    }

    private suspend fun ensureRuntimeLive() {
        if (runtime != null) return
        val result = bootRuntime()
        if (!result.ok) throw IllegalStateException("插件启动失败: ${result.error}")
    }

    private suspend fun recoverFromQuarantineIfNeeded() {
        if (!quarantined || disposed) return
        if (System.currentTimeMillis() - quarantinedAt < QUARANTINE_RECOVERY_MS) return
        Timber.i("LxScriptRunner(%s): 隔离期满，重建运行时", label)
        val result = bootRuntime()
        if (result.ok) {
            quarantined = false
            quarantinedAt = 0L
            log("[lx] 已从隔离中恢复")
        }
    }

    private fun onRequestTimeout() {
        consecutiveTimeouts++
        if (consecutiveTimeouts < QUARANTINE_TIMEOUT_THRESHOLD || quarantined) return
        quarantined = true
        quarantinedAt = System.currentTimeMillis()
        Timber.w(
            "LxScriptRunner(%s): 连续 %d 次请求超时，判定疑似失控，隔离 %dms",
            label,
            consecutiveTimeouts,
            QUARANTINE_RECOVERY_MS
        )
    }

    private fun handleEvent(event: String, data: Any?) {
        when (event) {
            "inited" -> {
                val payload = data as? Map<*, *> ?: emptyMap<Any?, Any?>()
                val declared = payload["sources"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
                initedSources = declared.mapNotNull { (key, value) -> parseSource(key, value) }.toMap()
                val deferred = initDeferred
                if (deferred != null && !deferred.isCompleted) deferred.complete(Unit)
            }

            "updateAlert" -> {
                lastUpdateAlert = data
                log("[lx] updateAlert: $data")
            }

            else -> Unit
        }
    }

    private fun parseSource(key: Any?, value: Any?): Pair<String, OnlinePluginSource>? {
        val keyText = key?.toString() ?: return null
        val source = OnlineSource.fromLxKey(keyText) ?: return null
        val map = value as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val actions = map["actions"].asStringList()
        val qualities = map["qualitys"].asStringList().ifEmpty { map["qualities"].asStringList() }
        val homeCategories = map["homeCategories"].asStringList()
        val name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: source.displayName
        return keyText to OnlinePluginSource(
            key = source,
            name = name,
            actions = actions,
            qualities = qualities,
            homeCategories = homeCategories
        )
    }

    private fun Any?.asStringList(): List<String> = when (this) {
        is List<*> -> mapNotNull { it?.toString() }
        is JSONArray -> (0 until length()).mapNotNull { opt(it)?.toString() }
        else -> emptyList()
    }

    private fun handleReturn(callId: Int, ok: String?, err: String?) {
        val deferred = calls.remove(callId) ?: return
        if (err != null) {
            deferred.completeExceptionally(IllegalStateException(err))
            return
        }
        if (ok == null) {
            deferred.complete(null)
            return
        }
        try {
            deferred.complete(parseJsonValue(ok))
        } catch (e: Exception) {
            deferred.completeExceptionally(IllegalStateException("脚本返回解析失败: ${e.message}"))
        }
    }

    /**
     * 取链结果与脚本返回值的解析统一放在 [LxJson]：
     * 脚本返回 `JSON.stringify` 后的字符串，服务端返回 `resultJson`，两层规则一样。
     */
    private fun parseJsonValue(text: String): Any? = LxJson.parseValue(text)

    private fun extractPlaySource(value: Any?): PlaySource? = LxJson.extractPlaySource(value)

    private fun log(message: String) {
        onLog?.invoke(message) ?: Timber.d("[%s] %s", label, message)
    }

    // endregion

    companion object {
        const val INIT_TIMEOUT_MS = 60_000L
        const val DEFAULT_REQUEST_TIMEOUT_MS = 15_000L
        const val QUARANTINE_RECOVERY_MS = 60_000L
        const val QUARANTINE_TIMEOUT_THRESHOLD = 3

        private const val ERROR_PREFIX = "ERROR:"
    }
}
