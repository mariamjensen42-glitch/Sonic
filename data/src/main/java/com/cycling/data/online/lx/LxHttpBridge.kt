package com.cycling.data.online.lx

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LX 音源脚本唯一的出网通道。
 *
 * 行为要点：
 * - OkHttpClient 的 connect/read/write 与 Cookie 记忆都是进程级共享的；
 * - 每次请求可以带 `timeout`（毫秒，默认 15s）；
 * - 支持 `headers` / `form` / `body`（Map/List 自动 JSON 化）/ `binary`；
 * - 响应体上限 [MAX_BODY_BYTES]（8 MiB），超了直接失败——脚本不是可信代码，
 *   不能让它把整个 APK 的内存吃光；
 * - 响应统一成 `{ statusCode, statusMessage, headers, body, bodyBase64 }`，
 *   `body` 是解析后的 Map/List（JSON 对象/数组）或字符串，
 *   `bodyBase64` 只在声明 binary 或解码出现替换字符时填充。
 */
@Singleton
class LxHttpBridge @Inject constructor(
    baseClient: OkHttpClient
) {

    private val client: OkHttpClient = baseClient.newBuilder()
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // 单次调用的超时由请求里的 timeout 选项决定，这里不能钉死。
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    /** host -> ["name=value", …]，脚本之间共享同一个域名下的 Cookie。 */
    private val cookieJar = ConcurrentHashMap<String, MutableList<String>>()

    /** 网络切换后清空连接池，和原实现的 `onNetworkSwitched` 等价。 */
    fun onNetworkSwitched() {
        client.connectionPool.evictAll()
    }

    suspend fun request(url: String, options: Map<String, Any?>): Map<String, Any?> =
        withContext(Dispatchers.IO) {
            val method = (options["method"] as? String)?.uppercase() ?: "GET"
            val timeoutMs = (options["timeout"] as? Number)?.toLong()?.takeIf { it > 0 }
                ?: DEFAULT_TIMEOUT_MS

            val headers = LinkedHashMap<String, String>()
            (options["headers"] as? Map<*, *>)?.forEach { (key, value) ->
                if (key != null && value != null) {
                    headers[key.toString()] = latin1Safe(value.toString())
                }
            }

            val host = runCatching { URI(url).host }.getOrNull().orEmpty()
            if (headers.keys.none { it.equals("cookie", ignoreCase = true) }) {
                val cookies = cookieJar[host]
                if (!cookies.isNullOrEmpty()) {
                    headers["Cookie"] = cookies.joinToString("; ")
                }
            }

            val builder = Request.Builder().url(url)
            headers.forEach { (name, value) -> builder.header(name, value) }

            val form = options["form"]
            val bodyValue = options["body"]
            val mediaTypeJson = "application/json".toMediaType()
            val requestBody = when {
                form is Map<*, *> -> {
                    val formBuilder = FormBody.Builder()
                    form.forEach { (key, value) -> formBuilder.add(key.toString(), value?.toString() ?: "") }
                    formBuilder.build()
                }

                bodyValue != null -> {
                    val text = when (bodyValue) {
                        is Map<*, *>, is List<*> ->
                            JSONObject.wrap(bodyValue)?.toString() ?: bodyValue.toString()

                        else -> bodyValue.toString()
                    }
                    if (headers.keys.none { it.equals("content-type", ignoreCase = true) }) {
                        builder.header("Content-Type", "application/json")
                    }
                    text.toRequestBody(mediaTypeJson)
                }

                method == "GET" || method == "HEAD" -> null

                else -> ByteArray(0).toRequestBody(mediaTypeJson)
            }
            builder.method(method, requestBody)

            val call = client.newCall(builder.build())
            call.timeout().timeout(timeoutMs, TimeUnit.MILLISECONDS)
            call.execute().use { response ->
                val body = response.body
                val bytes = body?.byteStream()?.use { stream ->
                    val buffer = java.io.ByteArrayOutputStream(MAX_BODY_BYTES.coerceAtMost(16 * 1024))
                    val chunk = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val read = stream.read(chunk)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BODY_BYTES) {
                            throw IllegalStateException("响应体过大(>8MB)")
                        }
                        buffer.write(chunk, 0, read)
                    }
                    buffer.toByteArray()
                } ?: ByteArray(0)

                val rawText = String(bytes, StandardCharsets.UTF_8)
                val binary = options["binary"] == true || rawText.indexOf('\uFFFD') >= 0
                val bodyBase64 = if (binary) Base64.encodeToString(bytes, Base64.NO_WRAP) else null

                val parsedBody: Any = when {
                    rawText.startsWith("{") -> runCatching { jsonObjectToMap(JSONObject(rawText)) }
                        .getOrDefault(rawText)

                    rawText.startsWith("[") -> runCatching { jsonArrayToList(JSONArray(rawText)) }
                        .getOrDefault(rawText)

                    else -> rawText
                }

                rememberCookies(host, response.headers.values("Set-Cookie"))

                linkedMapOf(
                    "statusCode" to response.code,
                    "statusMessage" to response.message,
                    "headers" to linkedMapOf<String, String>().apply {
                        for (i in 0 until response.headers.size) {
                            put(response.headers.name(i), response.headers.value(i))
                        }
                    },
                    "body" to parsedBody,
                    "bodyBase64" to bodyBase64
                )
            }
        }

    /**
     * OkHttp 的 header 只允许 ISO-8859-1（HTTP 规范）。脚本里常出现中文 header
     * （网易云某些接口要 `X-Real-IP` 之外的签名参数），原实现的做法是：
     * 只要出现非 Latin-1 字符，就按 UTF-8 取字节再按 ISO-8859-1 重新解码。
     */
    private fun latin1Safe(value: String): String {
        if (value.all { it.code <= 0xFF }) return value
        return String(value.toByteArray(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)
    }

    private fun rememberCookies(host: String, setCookieHeaders: List<String>) {
        if (host.isBlank() || setCookieHeaders.isEmpty()) return
        val store = cookieJar.getOrPut(host) { mutableListOf() }
        synchronized(store) {
            for (header in setCookieHeaders) {
                val pair = header.substringBefore(';').trim()
                if (!pair.contains('=')) continue
                val name = pair.substringBefore('=').trim()
                if (name.isEmpty()) continue
                val index = store.indexOfFirst { it.startsWith("$name=") }
                if (index >= 0) store[index] = pair else store.add(pair)
            }
        }
    }

    private fun jsonObjectToMap(json: JSONObject): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = wrapJson(json.opt(key))
        }
        return result
    }

    private fun jsonArrayToList(json: JSONArray): List<Any?> =
        (0 until json.length()).map { wrapJson(json.opt(it)) }

    private fun wrapJson(value: Any?): Any? = when {
        value == null || value == JSONObject.NULL -> null
        value is JSONObject -> jsonObjectToMap(value)
        value is JSONArray -> jsonArrayToList(value)
        else -> value
    }

    companion object {
        /** 响应体上限 8 MiB：脚本不是可信代码，不能让它把进程内存吃光。 */
        const val MAX_BODY_BYTES = 8 * 1024 * 1024

        private const val DEFAULT_TIMEOUT_MS = 15_000L
    }
}
