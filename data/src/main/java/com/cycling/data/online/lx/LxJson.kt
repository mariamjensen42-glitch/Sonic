package com.cycling.data.online.lx

import com.cycling.domain.online.model.PlaySource
import org.json.JSONArray
import org.json.JSONObject

/**
 * 脚本返回值 / 服务端 `resultJson` 的统一解析。
 *
 * 两层返回都是「字符串包 JSON」：脚本侧是 `JSON.stringify`，服务端侧是 `resultJson` 字段。
 */
object LxJson {

    fun parseValue(text: String?): Any? {
        if (text == null) return null
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed == "null") return null
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                runCatching { JSONObject("{\"v\":$trimmed}").getString("v") }.getOrDefault(trimmed)
            } else {
                trimmed
            }
        }
        return runCatching {
            if (trimmed.startsWith("{")) jsonToAny(JSONObject(trimmed)) else jsonToAny(JSONArray(trimmed))
        }.getOrNull()
    }

    fun jsonToAny(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> {
            val map = LinkedHashMap<String, Any?>()
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonToAny(value.opt(key))
            }
            map
        }

        is JSONArray -> (0 until value.length()).map { jsonToAny(value.opt(it)) }
        else -> value
    }

    /**
     * 取链结果的容错解析：字符串 URL / `{ url, headers, userAgent }` 都接受。
     */
    fun extractPlaySource(value: Any?): PlaySource? {
        val url = when (value) {
            null -> null
            is String -> value.takeIf { it.startsWith("http") }
            is Map<*, *> -> value["url"]?.toString()?.takeIf { it.startsWith("http") }
            else -> value.toString().takeIf { it.startsWith("http") }
        } ?: return null

        val headers = LinkedHashMap<String, String>()
        if (value is Map<*, *>) {
            (value["headers"] as? Map<*, *>)?.forEach { (key, headerValue) ->
                val name = key?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
                val text = headerValue?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
                headers[name] = text
            }
            (value["userAgent"] as? String)?.takeIf { it.isNotBlank() }?.let {
                headers["User-Agent"] = it
            }
        }
        return PlaySource(url, headers)
    }
}
