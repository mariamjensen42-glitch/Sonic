package com.cycling.data.online.lx

import com.cycling.domain.online.model.PluginMeta
import java.security.MessageDigest

/**
 * LX 音源脚本的头部元信息解析与导入校验。
 *
 * 头部是脚本开头的一段块注释，示例：
 * ```
 *  * @name 示例音源
 *  * @description 支持 wy/tx
 *  * @version 1.0.0
 *  * @author someone
 *  * @homepage https://example.com
 *  * @x-pro-only true
 *  * @lxkey =
 * ```
 * 解析规则（字段名、`@x-pro-only` 的布尔词表、
 * `@key =` 形态的配置项声明、以及 4MB / NOT_JS / LX 标记三道导入闸门）。
 */
class LxScriptMeta {

    enum class Gate {
        OK,
        EMPTY,
        TOO_LARGE,
        NOT_JS,
        NO_LX_MARKER
    }

    fun gateForImport(script: String): Gate {
        val trimmed = script.trim()
        if (trimmed.isEmpty()) return Gate.EMPTY
        if (script.toByteArray(Charsets.UTF_8).size > MAX_SCRIPT_BYTES) return Gate.TOO_LARGE
        if (HTML_START.containsMatchIn(trimmed.take(200))) return Gate.NOT_JS
        return if (LX_API_PATTERN.containsMatchIn(script)) Gate.OK else Gate.NO_LX_MARKER
    }

    /** 返回 null 表示可以导入，否则是给用户看的原因。 */
    fun validateForImport(script: String): String? = when (gateForImport(script)) {
        Gate.OK -> null
        Gate.EMPTY -> "脚本内容为空"
        Gate.TOO_LARGE -> "脚本文件过大（>4MB）"
        Gate.NOT_JS -> "获取到的内容不是 JS 脚本（可能是错误页面，请检查链接是否有效）"
        Gate.NO_LX_MARKER -> "不是有效的 LX 音源脚本（未检测到 lx API 调用；重度加密的脚本可能无法识别）"
    }

    fun parseMeta(script: String): PluginMeta {
        val header = headerOf(script) ?: return PluginMeta()
        return PluginMeta(
            name = pick(header, "name"),
            description = pick(header, "description"),
            version = pick(header, "version").trimStart('v', 'V'),
            author = pick(header, "author"),
            homepage = pick(header, "homepage")
        )
    }

    fun isProOnly(script: String): Boolean {
        val value = parseHeaderValue(script, "x-pro-only").lowercase()
        return value == "true" || value == "1" || value == "yes" || value == "y"
    }

    /**
     * 头部里 `@config =` 形态的声明：名字是配置项，冒号后为空表示默认空值。
     * 标准元信息键（name/description/...）不算配置项。
     */
    fun parseEmptyConfigFields(script: String): List<String> {
        val header = headerOf(script) ?: return emptyList()
        val result = LinkedHashSet<String>()
        header.lineSequence().forEach { line ->
            val attribute = FIELD_LINE.matchEntire(line) ?: return@forEach
            val key = attribute.groupValues[1]
            if (key in RESERVED_HEADER_KEYS) return@forEach
            val rawValue = attribute.groupValues[2].trim()
            val emptyValue = CONFIG_TEMPLATE.matchEntire(rawValue)
            result += if (emptyValue != null && emptyValue.groupValues[1].isNotBlank()) {
                emptyValue.groupValues[1]
            } else {
                key
            }
        }
        return result.toList()
    }

    fun md5(script: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(script.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun parseHeaderValue(script: String, field: String): String {
        val header = headerOf(script) ?: return ""
        return pick(header, field)
    }

    private fun headerOf(script: String): String? =
        HEADER.find(script)?.groupValues?.getOrNull(1)

    private fun pick(header: String, field: String): String {
        val regex = Regex("""@$field\s+([^\n\r*]+)""")
        return regex.find(header)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    }

    companion object {
        /** 单脚本上限 4 MiB，与原实现的 `LxScriptGate.TOO_LARGE` 一致。 */
        const val MAX_SCRIPT_BYTES = 4 * 1024 * 1024

        private val HEADER = Regex("""/\*!?\s*([\s\S]*?)\*/""")
        private val HTML_START = Regex("""^\s*<""")
        private val FIELD_LINE = Regex("""^\s*\*?\s*@(\w+)\s*(.*)$""")
        private val CONFIG_TEMPLATE = Regex("""^([\w-]*)\s*=\s*;?\s*$""")

        private val RESERVED_HEADER_KEYS = setOf(
            "name", "description", "version", "author", "homepage", "license", "preserve"
        )

        /**
         * 「这是不是一个 LX 脚本」的特征：`window.lx` / `globalThis.lx`（含 `['lx']` 与
         * `\x6c\x78`、`\u006c\u0078` 转义写法），或者出现 `lx.on` / `lx.send` / `lx.request`。
         */
        private val LX_API_PATTERN = Regex(
            """(?:globalThis|window)\s*(?:\.\s*lx\b|\[\s*['"](?:lx|\\x6[cC]\\x78|\\u006[cC]\\u0078)['"]\s*\])|(?:^|[^\w${'$'}.])lx\s*\.\s*(?:on|send|request)\b""",
            setOf(RegexOption.MULTILINE)
        )
    }
}
