package com.cycling.data.player.audio

/**
 * 一只双二阶滤波器的工作状态。
 *
 * 两个关键点：
 *
 * 1. **转置直接 II 型（TDF-II）**：每通道只需两个状态量（`double[2]`），
 *    增益变化时不容易产生内部溢出。
 * 2. **系数斜坡**：目标系数按 `1/rampFrames` 线性逼近，每个采样走一步。直接跳变会产生 "zipper"
 *    噪声，这也是均衡器拖滑杆时最容易听出来的瑕疵。
 */
internal class BiquadStage(maxChannels: Int) {

    private val channels = maxChannels.coerceIn(1, MAX_CHANNELS)

    /** 当前生效系数 `{b0, b1, b2, a1, a2}`，直通初值。 */
    private val current = DoubleArray(COEFFICIENT_COUNT).also { it[0] = 1.0 }

    /** 每采样的系数增量。 */
    private val step = DoubleArray(COEFFICIENT_COUNT)

    private var rampRemaining = 0

    /** TDF-II 的两个状态量，按通道分开。 */
    private val state1 = DoubleArray(this.channels)
    private val state2 = DoubleArray(this.channels)

    fun setCoefficients(coefficients: DoubleArray, rampFrames: Int) {
        for (index in 0 until COEFFICIENT_COUNT) {
            val target = coefficients[index]
            if (rampFrames > 0) {
                step[index] = (target - current[index]) / rampFrames
            } else {
                current[index] = target
                step[index] = 0.0
            }
        }
        rampRemaining = if (rampFrames > 0) rampFrames else 0
    }

    fun resetState() {
        state1.fill(0.0)
        state2.fill(0.0)
    }

    fun process(channel: Int, input: Double): Double {
        if (rampRemaining > 0) {
            for (index in 0 until COEFFICIENT_COUNT) {
                current[index] += step[index]
            }
            rampRemaining--
        }

        val slot = if (channel in 0 until channels) channel else 0
        val b0 = current[0]
        val b1 = current[1]
        val b2 = current[2]
        val a1 = current[3]
        val a2 = current[4]

        // TDF-II：y = b0*x + s1; s1 = b1*x - a1*y + s2; s2 = b2*x - a2*y
        val output = b0 * input + state1[slot]
        state1[slot] = b1 * input - a1 * output + state2[slot]
        state2[slot] = b2 * input - a2 * output
        return output
    }

    private companion object {
        const val COEFFICIENT_COUNT = 5

        /** 最多按 8 通道建状态数组（channelCount 夹在 1..8）。 */
        const val MAX_CHANNELS = 8
    }
}
