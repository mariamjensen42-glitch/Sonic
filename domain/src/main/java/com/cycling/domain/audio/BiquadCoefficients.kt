package com.cycling.domain.audio

import com.cycling.domain.model.EqFilterSpec
import com.cycling.domain.model.EqFilterType
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * RBJ 风格的 biquad 系数计算。
 *
 * 输出 `{b0, b1, b2, a1, a2}` 且已按 a0 归一化，可以直接喂给转置直接 II 型（TDF-II）结构。
 *
 * 两个实现细节：
 * - 搁架滤波器固定 S = 1（alpha 不随 Q 走），所以低频/高频搁架的 Q 参数被忽略；
 * - 增益为 0 或未启用时直接返回直通系数 `1, 0, 0, 0, 0`，省掉一次无意义卷积。
 *
 * 放在 domain 是为了让"界面画响应曲线"和"数据层跑 DSP"共用同一份实现，曲线不会和声音对不上。
 */
object BiquadCoefficients {

    private const val TWO_PI = 2.0 * Math.PI

    /** 直通：H(z) = 1。 */
    val BYPASS: DoubleArray = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0)

    const val COEFFICIENT_COUNT = 5

    fun compute(spec: EqFilterSpec, sampleRate: Double): DoubleArray {
        if (!spec.enabled || spec.gainDb == 0.0 || sampleRate <= 0.0) return BYPASS

        // 频率夹在可实现的范围内：贴到 0 会让系数发散，越过奈奎斯特会混叠。
        val frequency = spec.frequencyHz.coerceIn(1.0, sampleRate * 0.49)
        val amplitude = 10.0.pow(spec.gainDb / 40.0)
        val w0 = TWO_PI * frequency / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val q = spec.q.coerceAtLeast(MIN_Q)
        val alpha = sinW / (2.0 * q)

        val b0: Double
        val b1: Double
        val b2: Double
        val a0: Double
        val a1: Double
        val a2: Double
        when (spec.type) {
            EqFilterType.PEAK -> {
                b0 = 1.0 + alpha * amplitude
                b1 = -2.0 * cosW
                b2 = 1.0 - alpha * amplitude
                a0 = 1.0 + alpha / amplitude
                a1 = -2.0 * cosW
                a2 = 1.0 - alpha / amplitude
            }

            EqFilterType.LOW_SHELF -> {
                val alphaShelf = shelfAlpha(amplitude, sinW)
                val plus = amplitude + 1.0
                val minus = amplitude - 1.0
                val minusCos = minus * cosW
                val plusCos = plus * cosW
                val lowBase = plus - minusCos
                val highBase = plus + minusCos
                b0 = (lowBase + alphaShelf) * amplitude
                b1 = 2.0 * amplitude * (minus - plusCos)
                b2 = amplitude * (lowBase - alphaShelf)
                a0 = highBase + alphaShelf
                a1 = (minus + plusCos) * -2.0
                a2 = highBase - alphaShelf
            }

            EqFilterType.HIGH_SHELF -> {
                val alphaShelf = shelfAlpha(amplitude, sinW)
                val plus = amplitude + 1.0
                val minus = amplitude - 1.0
                val minusCos = minus * cosW
                val plusCos = plus * cosW
                val highBase = plus + minusCos
                val lowBase = plus - minusCos
                b0 = (highBase + alphaShelf) * amplitude
                b1 = -2.0 * amplitude * (minus + plusCos)
                b2 = amplitude * (highBase - alphaShelf)
                a0 = lowBase + alphaShelf
                a1 = (minus - plusCos) * 2.0
                a2 = lowBase - alphaShelf
            }

            EqFilterType.LOW_PASS -> {
                val half = (1.0 - cosW) / 2.0
                b0 = half
                b1 = 1.0 - cosW
                b2 = half
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW
                a2 = 1.0 - alpha
            }

            EqFilterType.HIGH_PASS -> {
                val half = (1.0 + cosW) / 2.0
                b0 = half
                b1 = -(1.0 + cosW)
                b2 = half
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW
                a2 = 1.0 - alpha
            }

            EqFilterType.NOTCH -> {
                b0 = 1.0
                b1 = -2.0 * cosW
                b2 = 1.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW
                a2 = 1.0 - alpha
            }
        }

        return doubleArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    /**
     * 级联滤波器在 [frequencyHz] 处的幅频响应，单位 dB（含 [preampDb]）。
     *
     * 界面用它画响应曲线，不参与音频处理。
     */
    fun magnitudeDb(
        filters: List<EqFilterSpec>,
        frequencyHz: Double,
        sampleRate: Double,
        preampDb: Float = 0f
    ): Double {
        if (sampleRate <= 0.0) return preampDb.toDouble()
        val w = TWO_PI * frequencyHz.coerceIn(1.0, sampleRate * 0.49) / sampleRate
        val cosW = cos(w)
        val cos2W = cos(2.0 * w)
        val sinW = sin(w)
        val sin2W = sin(2.0 * w)

        var magnitude = amplitudeToLinear(preampDb.toDouble())
        for (filter in filters) {
            if (!filter.enabled || filter.gainDb == 0.0) continue
            val c = compute(filter, sampleRate)
            val numeratorRe = c[0] + c[1] * cosW + c[2] * cos2W
            val numeratorIm = c[1] * sinW + c[2] * sin2W
            val denominatorRe = 1.0 + c[3] * cosW + c[4] * cos2W
            val denominatorIm = c[3] * sinW + c[4] * sin2W
            val numerator = sqrt(numeratorRe * numeratorRe + numeratorIm * numeratorIm)
            val denominator = sqrt(denominatorRe * denominatorRe + denominatorIm * denominatorIm)
            if (denominator > 1e-12) magnitude *= numerator / denominator
        }
        return 20.0 * ln(magnitude.coerceAtLeast(1e-9)) / LN_10
    }

    /** dB → 线性倍数。 */
    fun amplitudeToLinear(db: Double): Double = 10.0.pow(db / 20.0)

    private fun shelfAlpha(amplitude: Double, sinW: Double): Double =
        sqrt(amplitude) * 2.0 * sqrt(2.0) * (sinW / 2.0)

    private const val MIN_Q = 0.1
    private const val LN_10 = 2.302585092994046
}
