package com.cycling.domain.lyrics.utils

import com.cycling.domain.lyrics.model.Attributes

object LrcMetadataHelper {

    private val METADATA_TAGS = setOf("ar", "ti", "al", "offset", "length", "tool")

    private val attributeParser = Regex("^\\[([a-zA-Z]+):([^]]*)]$")

    fun parse(lines: List<String>): Attributes {
        val attributesMap = lines.mapNotNull { line ->
            attributeParser.find(line)?.destructured?.let { (tag, value) ->
                if (tag in METADATA_TAGS) {
                    tag.trim() to value.trim()
                } else {
                    null
                }
            }
        }.toMap()

        return Attributes(
            artist = attributesMap["ar"],
            title = attributesMap["ti"],
            album = attributesMap["al"],
            offset = attributesMap["offset"]?.toIntOrNull() ?: 0,
            duration = attributesMap["length"]?.toIntOrNull() ?: 0
        )
    }

    fun removeAttributes(lines: List<String>): List<String> {
        return lines.filterNot { line ->
            attributeParser.find(line)?.destructured?.let { (tag, _) ->
                tag in METADATA_TAGS
            } == true
        }
    }
}
