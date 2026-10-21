package com.cycling.data.online.lx

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.JsObject
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * 一个音源脚本的 QuickJS 运行时。
 *
 * 几条硬性约束：
 * - **内存 64 MiB、栈 1 MiB**：脚本不可信，无限内存会直接 OOM 掉整个播放器；
 * - **单线程 JS 调度器**（`limitedParallelism(1)`）：QuickJS 不是线程安全的；
 * - 绑定 `lxHttpRequest` / `__lxSleep` 两个 async 函数 + `lxHostInvoke` 同步函数；
 * - `init` 时先 eval [LxBootstrap.js]（`lx` 全局 API），再 eval 脚本自己的代码；
 * - 宿主通道：`__lxConsole` / `__lxEvent` / `__lxUtils` / `__lxReturn`。
 */
class LxJsRuntime(
    private val httpBridge: LxHttpBridge
) {

    private val jsDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + jsDispatcher)
    private val nativeHost = LxNativeHost()

    var onConsole: ((level: String, text: String) -> Unit)? = null
    var onEvent: ((event: String, data: Any?) -> Unit)? = null
    var onReturn: ((callId: Int, ok: String?, err: String?) -> Unit)? = null

    var proToken: String = ""

    private var quickJs: QuickJs? = null

    @Volatile
    private var disposed = false

    suspend fun init() {
        val qjs = QuickJs.create(jsDispatcher)
        // quickjs-kt 的限流是属性（var memoryLimit / var maxStackSize），不是 setXxx(...) 方法。
        qjs.memoryLimit = MAX_JS_MEMORY_BYTES
        qjs.maxStackSize = MAX_JS_STACK_BYTES

        nativeHost.handler = ::handleHost
        qjs.function("lxHostInvoke") { args ->
            nativeHost.invoke(
                args.getOrNull(0)?.toString().orEmpty(),
                args.getOrNull(1)?.toString().orEmpty()
            )
        }
        qjs.asyncFunction("lxHttpRequest") { args ->
            val url = args.getOrNull(0)?.toString().orEmpty()
            require(url.isNotBlank()) { "lxHttpRequest: url 为空" }
            val options = coerceOptions(args.getOrNull(1))
            toJsValue(httpBridge.request(url, options))
        }
        qjs.asyncFunction("__lxSleep") { args ->
            val millis = (args.getOrNull(0) as? Number)?.toLong() ?: 0L
            delay(millis.coerceIn(0L, 60_000L))
            null
        }

        quickJs = qjs
        evalRaw(LxBootstrap.js(proToken))
    }

    /**
     * 在 JS 线程上执行一段代码，返回 JS 的字符串表示；异常被折叠成 `ERROR: …` 字符串，
     * 与原实现一致（调用方靠前缀判断失败，而不是靠异常）。
     */
    suspend fun evalRaw(code: String): String {
        if (disposed) return "ERROR: runtime 已释放"
        val qjs = quickJs ?: return "ERROR: runtime 未初始化"
        return try {
            withContext(jsDispatcher) {
                val result: Any? = qjs.evaluate(code, "main.js")
                result?.toString() ?: "undefined"
            }
        } catch (e: Exception) {
            Timber.w(e, "LxJsRuntime.evalRaw failed")
            "ERROR: ${e.message}"
        }
    }

    /** 把一次取链请求丢给脚本的 `request` 处理器；结果通过 [onReturn] 回来。 */
    fun dispatchRequestAsync(callId: Int, payloadJson: String) {
        scope.launch {
            evalRaw("__lxDispatch($callId, ${JSONObject.quote(payloadJson)});")
        }
    }

    /** 让出式执行（原实现里 `dispatchRequest` 的 suspend 版本）。 */
    suspend fun dispatchRequest(callId: Int, payloadJson: String): String =
        evalRaw("__lxDispatch($callId, ${JSONObject.quote(payloadJson)});")

    fun dispose() {
        disposed = true
        scope.cancel()
        runCatching { quickJs?.close() }
        quickJs = null
    }

    // region host channels

    private fun handleHost(channel: String, payload: String): String? = try {
        when (channel) {
            "__lxEvent" -> {
                val json = JSONObject(payload)
                val event = json.optString("event", "")
                val data = if (json.isNull("data")) null else jsonToAny(json.opt("data"))
                onEvent?.invoke(event, data)
                null
            }

            "__lxUtils" -> {
                val json = JSONObject(payload)
                val name = json.getString("name")
                val argsJson = json.optJSONArray("args") ?: JSONArray()
                val args = (0 until argsJson.length()).map { jsonToAny(argsJson.opt(it)) }
                runCatching { LxJsUtils.execute(name, args) }
                    .fold(
                        onSuccess = { result ->
                            val wrapped = JSONObject.wrap(result)
                            wrapped?.toString() ?: JSONObject.quote(result?.toString() ?: "")
                        },
                        onFailure = { error ->
                            JSONObject()
                                .put("__error", error.message ?: "error")
                                .toString()
                        }
                    )
            }

            "__lxConsole" -> {
                val json = JSONObject(payload)
                onConsole?.invoke(
                    json.optString("level", "log"),
                    json.optString("text", "")
                )
                null
            }

            "__lxReturn" -> {
                val json = JSONObject(payload)
                val callId = json.getInt("callId")
                val ok = if (json.isNull("ok")) null else json.optString("ok", null)
                val err = if (json.isNull("err")) null else json.optString("err", null)
                onReturn?.invoke(callId, ok, err)
                null
            }

            else -> null
        }
    } catch (e: Exception) {
        Timber.w(e, "LxJsRuntime host channel %s failed", channel)
        if (channel == "__lxUtils") null
        else JSONObject().put("__error", e.message ?: "error").toString()
    }

    // endregion

    private fun coerceOptions(value: Any?): Map<String, Any?> {
        when (value) {
            null -> return emptyMap()
            is Map<*, *> -> return value.entries.associate { (key, item) ->
                key.toString() to normalizeJsValue(item)
            }

            is String -> {
                val text = value.trim()
                if (text.startsWith("{")) {
                    runCatching { return jsonObjectToMap(JSONObject(text)) }
                }
            }
        }
        return emptyMap()
    }

    private fun normalizeJsValue(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries.associate { (key, item) -> key.toString() to normalizeJsValue(item) }
        is List<*> -> value.map(::normalizeJsValue)
        else -> value
    }

    private fun toJsValue(value: Any?): Any? = when (value) {
        null -> null
        is Map<*, *> -> JsObject(
            value.entries.associate { (key, item) -> key.toString() to toJsValue(item) }
        )

        is List<*> -> value.map(::toJsValue)
        else -> value
    }

    private fun jsonToAny(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> jsonObjectToMap(value)
        is JSONArray -> (0 until value.length()).map { jsonToAny(value.opt(it)) }
        else -> value
    }

    private fun jsonObjectToMap(json: JSONObject): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = jsonToAny(json.opt(key))
        }
        return result
    }

    companion object {
        private const val TAG = "LxJsRuntime"

        /** 脚本堆上限 64 MiB：脚本不可信，无限内存会把整个播放器 OOM 掉。 */
        const val MAX_JS_MEMORY_BYTES = 64L * 1024 * 1024

        /** 与 `LxJsRuntime.MAX_JS_STACK_BYTES` 相同：1 MiB。 */
        const val MAX_JS_STACK_BYTES = 1024L * 1024
    }
}
