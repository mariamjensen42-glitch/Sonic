package com.cycling.data.player.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import androidx.annotation.OptIn

/**
 * 节拍 / 响度分析器：旁路音频的同时，每 128 个采样算一帧律动数据。
 *
 * 分析过程分两步：
 *
 * 1. 块内累计原始能量 `Σx²`，同时把 x 过一只一阶低通（系数 0.014，约 1.6kHz 以下占主导）
 *    并累计其能量 `Σlp²` —— 这一路就是"低频/鼓点"信号；
 * 2. 每 128 采样（约 2.9ms @44.1kHz）结算一次：
 *    - `level = min(1, rms × 2.4)`：整体响度；
 *    - `low = min(1, rmsLow × 5)` 经 0.15 平滑得到 `slowLow`，再跟一个 0.02 速率的慢跟随者
 *      `follower`；两者之比 `slowLow / follower` 去掉 0.6 基线后归一到 0..1 → [AudioReactiveBus.publish]
 *      的 `pulse`。这是经典的"能量比"式 onset 检测：鼓点打下来时低频能量会明显高于它的慢均值；
 *    - 响度慢包络 `levelEnvelope` 以 0.06 速率跟随 `level`；当 `level > 1.3 × levelEnvelope`
 *      时把 `(level/envelope - 1)/1.8` 记为瞬态尖峰，下限 0.15，上限 1，然后每块乘 0.93 衰减
 *      （约 40ms 时间常数）→ `beat`。
 *
 * 三个量全程不修改音频（只读采样后原样写出），所以它可以和均衡器共存于同一条处理链。
 */
@UnstableApi
@OptIn(UnstableApi::class)
@Singleton
class BeatAnalyzerProcessor @Inject constructor(
    private val bus: AudioReactiveBus
) : AudioProcessor {

    private var configured = false
    private var supported = false
    private var isFloat = false
    private var inputEnded = false
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    private var energySum = 0.0
    private var lowPassState = 0.0
    private var lowEnergySum = 0.0
    private var blockSampleCount = 0

    private var slowLow = 0.0
    private var follower = 0.0
    private var levelEnvelope = INITIAL_LEVEL_ENVELOPE
    private var transient = 0.0

    /** 用一个标志位标记"已经初始化过"，暂停后重新开始时先清状态再算。 */
    private var started = false

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        if (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) {
            configured = false
            supported = false
            return AudioProcessor.AudioFormat.NOT_SET
        }

        supported = true
        configured = true
        isFloat = encoding == C.ENCODING_PCM_FLOAT
        inputEnded = false
        started = false
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return inputAudioFormat
    }

    /** 与均衡器同理：始终激活，内部按 [AudioReactiveBus.enabled] 旁路。 */
    override fun isActive(): Boolean = configured && supported

    override fun isEnded(): Boolean = inputEnded

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer = outputBuffer

    override fun flush() {
        resetAnalysis()
        inputEnded = false
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun reset() {
        flush()
        configured = false
        supported = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (inputEnded) return
        val byteCount = inputBuffer.remaining()
        if (byteCount == 0) return

        val out = if (outputBuffer.capacity() < byteCount) {
            ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
            outputBuffer
        }
        outputBuffer = out

        if (!bus.enabled) {
            if (started) {
                started = false
                resetAnalysis()
            }
            out.put(inputBuffer)
            out.flip()
            return
        }

        if (!started) {
            resetAnalysis()
            started = true
        }

        if (isFloat) {
            val sampleCount = byteCount / 4
            for (index in 0 until sampleCount) {
                val sample = inputBuffer.getFloat()
                analyze(sample.toDouble())
                out.putFloat(sample)
            }
        } else {
            val sampleCount = byteCount / 2
            for (index in 0 until sampleCount) {
                val raw = inputBuffer.getShort()
                analyze(raw / 32768.0)
                out.putShort(raw)
            }
        }
        out.flip()
    }

    private fun resetAnalysis() {
        energySum = 0.0
        lowPassState = 0.0
        lowEnergySum = 0.0
        blockSampleCount = 0
        slowLow = 0.0
        follower = 0.0
        levelEnvelope = INITIAL_LEVEL_ENVELOPE
        transient = 0.0
        // 暂停 / 切歌时把界面那一路也归零，动画随即冻结。
        bus.reset()
    }

    private fun analyze(sample: Double) {
        energySum += sample * sample
        lowPassState += (sample - lowPassState) * LOW_PASS_COEFFICIENT
        lowEnergySum += lowPassState * lowPassState
        blockSampleCount++

        if (blockSampleCount < BLOCK_SAMPLES) return

        val rms = sqrt(energySum / blockSampleCount)
        val lowRms = sqrt(lowEnergySum / blockSampleCount)
        blockSampleCount = 0
        energySum = 0.0
        lowEnergySum = 0.0

        val level = min(1.0, rms * LEVEL_GAIN)
        val lowLevel = min(1.0, lowRms * LOW_LEVEL_GAIN)

        slowLow += (lowLevel - slowLow) * LOW_SMOOTHING
        if (slowLow > FOLLOWER_FLOOR) {
            follower += (slowLow - follower) * FOLLOWER_RATE
        }
        val ratio = slowLow / max(follower, FOLLOWER_FLOOR)
        val pulse = ((ratio - PULSE_BASELINE) / PULSE_SPAN).coerceIn(0.0, 1.0)

        levelEnvelope += (level - levelEnvelope) * LEVEL_SMOOTHING
        if (level > LEVEL_FLOOR && level > TRANSIENT_RATIO * levelEnvelope) {
            val spike = ((level / levelEnvelope) - 1.0) / TRANSIENT_SPAN
            transient = max(transient, spike.coerceIn(MIN_TRANSIENT, 1.0))
        }
        transient *= TRANSIENT_DECAY

        bus.publish(level.toFloat(), pulse.toFloat(), transient.toFloat())
    }

    private companion object {
        const val BLOCK_SAMPLES = 128

        /** 一阶低通系数：0.014。 */
        const val LOW_PASS_COEFFICIENT = 0.014

        const val LEVEL_GAIN = 2.4
        const val LOW_LEVEL_GAIN = 5.0
        const val LOW_SMOOTHING = 0.15
        const val FOLLOWER_RATE = 0.02
        const val FOLLOWER_FLOOR = 0.005
        const val PULSE_BASELINE = 0.6
        const val PULSE_SPAN = 0.9

        const val LEVEL_SMOOTHING = 0.06
        const val LEVEL_FLOOR = 0.03
        const val TRANSIENT_RATIO = 1.3
        const val TRANSIENT_SPAN = 1.8
        const val MIN_TRANSIENT = 0.15
        const val TRANSIENT_DECAY = 0.93

        /** 慢包络重置成 0.03（避免开局一次假瞬态）。 */
        const val INITIAL_LEVEL_ENVELOPE = 0.03
    }
}
