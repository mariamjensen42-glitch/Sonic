package com.cycling.domain.lyrics.utils

import com.cycling.domain.lyrics.model.Attributes

/**
 * LRC 头部标签的解析与写回。
 *
 * 只有 [METADATA_TAGS] 里的标签算元数据：用于从歌词文本里读出「这份歌词自称是哪首歌」，
 * 以及把用户在歌曲详情页改过的元数据写回文本。时间戳、TTML/YRC 标签和未知的方括号内容
 * 都原样保留 —— 这个类只碰它认识的那几行。
 */
object LrcMetadataHelper {

    private val METADATA_TAGS = setOf("ar", "ti", "al", "offset", "length", "tool", "by")

    /** 可编辑的标签，顺序即写回时缺少该标签时的插入顺序。 */
    private val EDITABLE_TAGS = listOf("ti", "ar", "al", "offset", "by")

    private val attributeParser = Regex("^\\[([a-zA-Z]+):([^]]*)]$")

    fun parse(lines: List<String>): Attributes {
        val attributesMap = lines.mapNotNull { line ->
            attributeParser.find(line.trim())?.destructured?.let { (tag, value) ->
                if (tag.lowercase() in METADATA_TAGS) {
                    tag.lowercase() to value.trim()
                } else {
                    null
                }
            }
        }.toMap()

        return Attributes(
            artist = attributesMap["ar"],
            title = attributesMap["ti"],
            album = attributesMap["al"],
            offset = attributesMap["offset"]?.toIntOrNull(),
            duration = attributesMap["length"]?.toIntOrNull(),
            by = attributesMap["by"],
        )
    }

    /** [parse] 的整篇文本重载。 */
    fun parse(content: String): Attributes = parse(content.lines())

    fun removeAttributes(lines: List<String>): List<String> {
        return lines.filterNot { line ->
            attributeParser.find(line.trim())?.destructured?.let { (tag, _) ->
                tag.lowercase() in METADATA_TAGS
            } == true
        }
    }

    /**
     * 把 [attributes] 写回 [content] 的头部标签，返回新的歌词文本。
     *
     * 已存在的可编辑标签行**就地**替换成新值（重复行只保留第一条，其余删除 —— 两份 `[ti:]`
     * 谁生效取决于解析器，写回时不该留下这种歧义）；值为 `null` 或空白表示删掉该标签；
     * 文档里还没有的标签按 [EDITABLE_TAGS] 的顺序补在最前面。`[length:]`、`[tool:]`
     * 和其他未知标签、以及所有时间轴行都不动，所以对同一份输入重复调用结果不变。
     *
     * 空白文本原样返回：没有歌词时不应该被元数据编辑凭空变成一段只有标签的文档。
     */
    fun applyAttributes(content: String, attributes: Attributes): String {
        if (content.isBlank()) return content

        val values = mapOf(
            "ti" to attributes.title,
            "ar" to attributes.artist,
            "al" to attributes.album,
            "offset" to attributes.offset?.toString(),
            "by" to attributes.by,
        ).mapValues { (_, value) -> value?.trim()?.takeIf { it.isNotEmpty() } }

        val remaining = content.lines().toMutableList()
        val missing = mutableListOf<String>()

        for (tag in EDITABLE_TAGS) {
            val value = values[tag]
            val indices = remaining.indices.filter { matchesTag(remaining[it], tag) }

            if (indices.isEmpty()) {
                if (value != null) missing += "[$tag:$value]"
                continue
            }

            if (value == null) {
                indices.sortedDescending().forEach { remaining.removeAt(it) }
            } else {
                remaining[indices.first()] = "[$tag:$value]"
                indices.drop(1).sortedDescending().forEach { remaining.removeAt(it) }
            }
        }

        return (missing + remaining).joinToString("\n")
    }

    private fun matchesTag(line: String, tag: String): Boolean {
        val found = attributeParser.find(line.trim()) ?: return false
        return found.groupValues[1].equals(tag, ignoreCase = true)
    }
}
