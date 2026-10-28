package com.cycling.data.online.lx

import com.cycling.domain.online.model.OnlineLyric
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

    /**
     * 歌词结果的容错解析（LX `action = 'lyric'`）。
     *
     * 逐字文档（`lxlyric`）优先当主文档：它是信息量最大的一份，普通 `lyric` 往往只是它的
     * 行级裁剪。只有翻译时退化成拿翻译当主文档，此时不再重复带一份翻译。
     */
    fun extractLyric(value: Any?): OnlineLyric? {
        if (value is String) {
            return value.takeIf { it.isNotBlank() }?.let { OnlineLyric(it) }
        }
        val map = value as? Map<*, *> ?: return null

        fun pick(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            map[key]?.toString()?.takeIf { it.isNotBlank() }
        }

        val main = pick("lxlyric", "lyric") ?: pick("tlyric") ?: return null
        return OnlineLyric(
            lyric = main,
            translation = pick("tlyric")?.takeIf { it != main },
            romanization = pick("rlyric")?.takeIf { it != main }
        )
    }

    /** 封面结果的容错解析：字符串 URL 或 `{ url, pic, img }`。 */
    fun extractPicUrl(value: Any?): String? = when (value) {
        null -> null
        is String -> value.trim().takeIf { it.startsWith("http") }
        is Map<*, *> -> (value["url"] ?: value["pic"] ?: value["img"])
            ?.toString()
            ?.trim()
            ?.takeIf { it.startsWith("http") }

        else -> value.toString().trim().takeIf { it.startsWith("http") }
    }
}
