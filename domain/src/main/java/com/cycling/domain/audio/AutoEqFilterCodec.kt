package com.cycling.domain.audio

import com.cycling.domain.model.AutoEqEntry
import com.cycling.domain.model.AutoEqFormFactor
import com.cycling.domain.model.AutoEqProfile
import com.cycling.domain.model.EqFilterSpec
import com.cycling.domain.model.EqFilterType
import java.util.Base64

/**
 * AutoEQ 目录与滤波器链的编解码。
 *
 * 目录是 gzip 后的 TSV，每行：
 * `id <TAB> in-ear|over-ear <TAB> base64(来源) <TAB> base64(型号) <TAB> 预放大 dB <TAB> 滤波器链`
 *
 * 滤波器链用 `;` 分隔，每项 `类型,频率,增益,Q`，类型取 `L`（低架）/ `P`（峰值）/ `H`（高架）。
 * 存盘用的是同一种写法，读回来不需要第二套格式。
 */
object AutoEqFilterCodec {

    private const val FILTER_SEPARATOR = ';'
    private const val FIELD_SEPARATOR = ','
    private const val FIELD_COUNT = 6

    /** 解析整份目录；`#` 开头是元信息行。 */
    fun parseCatalog(text: String): List<AutoEqProfile> {
        val profiles = ArrayList<AutoEqProfile>(9_000)
        for (line in text.lineSequence()) {
            if (line.isEmpty() || line[0] == '#') continue
            profiles += parseLine(line) ?: continue
        }
        return profiles
    }

    fun parseLine(line: String): AutoEqProfile? {
        val parts = line.split('\t')
        if (parts.size < FIELD_COUNT) return null
        val formFactor = when (parts[1]) {
            "in-ear" -> AutoEqFormFactor.IN_EAR
            "earbud" -> AutoEqFormFactor.EARBUD
            "over-ear" -> AutoEqFormFactor.OVER_EAR
            else -> return null
        }
        val filters = decodeFilters(parts[5])
        if (filters.isEmpty()) return null
        return AutoEqProfile(
            entry = AutoEqEntry(
                id = parts[0],
                formFactor = formFactor,
                source = decodeText(parts[2]),
                name = decodeText(parts[3])
            ),
            preampDb = parts[4].toFloatOrNull() ?: 0f,
            filters = filters
        )
    }

    fun decodeFilters(encoded: String): List<EqFilterSpec> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(FILTER_SEPARATOR).mapNotNull { item ->
            val fields = item.split(FIELD_SEPARATOR)
            if (fields.size < 4) return@mapNotNull null
            val type = when (fields[0]) {
                "L" -> EqFilterType.LOW_SHELF
                "H" -> EqFilterType.HIGH_SHELF
                "P" -> EqFilterType.PEAK
                else -> return@mapNotNull null
            }
            EqFilterSpec(
                frequencyHz = fields[1].toDoubleOrNull() ?: return@mapNotNull null,
                gainDb = fields[2].toDoubleOrNull() ?: return@mapNotNull null,
                q = fields[3].toDoubleOrNull() ?: return@mapNotNull null,
                type = type
            )
        }
    }

    fun encodeFilters(filters: List<EqFilterSpec>): String = filters.joinToString(";") { spec ->
        val code = when (spec.type) {
            EqFilterType.LOW_SHELF -> "L"
            EqFilterType.HIGH_SHELF -> "H"
            else -> "P"
        }
        "$code,${spec.frequencyHz},${spec.gainDb},${spec.q}"
    }

    private fun decodeText(encoded: String): String = runCatching {
        String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
    }.getOrDefault(encoded)
}
