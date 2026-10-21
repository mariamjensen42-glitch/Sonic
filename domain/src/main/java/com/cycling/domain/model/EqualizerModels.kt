package com.cycling.domain.model

/**
 * 双二阶（biquad）滤波器类型。
 *
 * 声明顺序即类型编号：图形均衡的每个频段都用 [PEAK]，搁架 / 截止 / 陷波服务于参数均衡。
 */
enum class EqFilterType {
    PEAK,
    LOW_SHELF,
    HIGH_SHELF,
    LOW_PASS,
    HIGH_PASS,
    NOTCH
}

/**
 * 一只滤波器的不可变描述，音频线程按快照读取。
 *
 * 字段：频率、增益、Q、类型、是否启用。
 */
data class EqFilterSpec(
    val frequencyHz: Double,
    val gainDb: Double,
    val q: Double = EqualizerBands.Q,
    val type: EqFilterType = EqFilterType.PEAK,
    val enabled: Boolean = true
)

/**
 * 内置音色预设。增益表下标与 [EqualizerBands.frequenciesHz] 一一对应。
 *
 * 内置风格预设：低音增强 / 人声 / 摇滚 / 流行 / 古典 / 爵士 / 电子 / 舞曲。
 * 增益值按等响度习惯给出：±12dB 范围内、相邻频段平滑过渡，不做极端跳变。
 */
enum class EqualizerPreset(val gains: List<Float>) {
    FLAT(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST(listOf(7f, 6f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)),
    VOCAL(listOf(-3f, -2f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f)),
    ROCK(listOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 4f)),
    POP(listOf(-1f, 0f, 2f, 3f, 3f, 2f, 0f, -1f, -1f, -1f)),
    CLASSICAL(listOf(3f, 2f, 0f, 0f, 0f, 0f, -1f, -2f, -2f, -3f)),
    JAZZ(listOf(4f, 3f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)),
    ELECTRONIC(listOf(5f, 4f, 2f, 0f, -1f, 1f, 2f, 3f, 4f, 5f)),
    DANCE(listOf(6f, 5f, 3f, 1f, 0f, 0f, -1f, -2f, -3f, -3f)),

    /** 用户手动改过某个频段，增益单独存放，这里不带表。 */
    CUSTOM(emptyList())
}

/**
 * 均衡器的固定频段定义与取值范围。
 *
 * 10 段 ISO 倍频程中心频率，Q 取 1.41（一倍频程带宽的峰值滤波器）——图形均衡按每频段
 * 一只峰值 biquad 实现；这也是几乎所有播放器 10 段 EQ 的通行档位。
 */
object EqualizerBands {

    val frequenciesHz: List<Int> = listOf(31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)

    val count: Int get() = frequenciesHz.size

    /** 一倍频程间隔对应的峰值滤波器 Q。 */
    const val Q: Double = 1.41

    const val MIN_GAIN_DB: Float = -12f
    const val MAX_GAIN_DB: Float = 12f
    const val MIN_PREAMP_DB: Float = -12f
    const val MAX_PREAMP_DB: Float = 12f
    /** 滑杆吸附步长：0.5dB。 */
    const val GAIN_STEP_DB: Float = 0.5f

    val DEFAULT_GAINS: List<Float> = List(count) { 0f }

    /** 把"每频段增益"翻译成音频线程要的滤波器列表；长度不足时按 0dB 补齐。 */
    fun filtersFor(gains: List<Float>): List<EqFilterSpec> = frequenciesHz.mapIndexed { index, frequency ->
        EqFilterSpec(
            frequencyHz = frequency.toDouble(),
            gainDb = clampGain(gains.getOrElse(index) { 0f }).toDouble()
        )
    }

    fun clampGain(gainDb: Float): Float = gainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)

    /** 吸附到 [GAIN_STEP_DB] 并夹到合法范围。 */
    fun snapGain(gainDb: Float): Float {
        val snapped = kotlin.math.round(gainDb / GAIN_STEP_DB) * GAIN_STEP_DB
        return clampGain(snapped)
    }

    /** 把存盘的逗号分隔串还原成固定长度的增益表。 */
    fun decodeGains(encoded: String?): List<Float> {
        if (encoded.isNullOrBlank()) return DEFAULT_GAINS
        val parsed = encoded.split(',')
            .mapNotNull { it.trim().toFloatOrNull() }
            .map(::clampGain)
        if (parsed.isEmpty()) return DEFAULT_GAINS
        return List(count) { index -> parsed.getOrElse(index) { 0f } }
    }

    fun encodeGains(gains: List<Float>): String =
        List(count) { index -> clampGain(gains.getOrElse(index) { 0f }) }.joinToString(",") { it.toString() }
}
