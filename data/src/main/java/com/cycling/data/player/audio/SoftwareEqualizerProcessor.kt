package com.cycling.data.player.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.cycling.domain.audio.BiquadCoefficients
import com.cycling.domain.model.EqFilterSpec
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh
import androidx.annotation.OptIn

/**
 * 软件图形均衡器：一串串联的峰值 biquad + 预放大 + 软限幅。
 *
 * 处理链路如下：
 *
 * - 支持 `PCM_16BIT` / `PCM_FLOAT` 两种编码，其它编码整只处理器旁路；
 * - 每 10ms 为一档参数更新周期，`rampFrames = sampleRate * 10 / 1000`；
 * - 系数与预放大都按采样线性斜坡推进，避免拖滑杆时的爆音 / zipper 噪声；
 * - 输出统一过一只 0.9 拐点的 tanh 软限幅：`|x| <= 0.9` 直通，
 *   超过后渐进压到 1.0，防止正增益把 16-bit 输出顶到硬削波。
 *
 * `isActive()` 只取决于"格式是否受支持"，不取决于开关，开关在 `queueInput` 里再判断 ——
 * 否则运行中打开/关闭均衡器要等一次音频管线重配才生效
 * （Media3 只在 flush / configure 时重新读 `isActive`）。
 */
@UnstableApi
@OptIn(UnstableApi::class)
@Singleton
class SoftwareEqualizerProcessor @Inject constructor() : AudioProcessor {

    @Volatile
    private var filters: List<EqFilterSpec> = emptyList()

    @Volatile
    private var preampDb: Float = 0f

    @Volatile
    private var enabled: Boolean = false

    /** 每次 [update] 自增，音频线程据此判断是否需要重算系数。 */
    @Volatile
    private var revision: Long = 0L

    private var configured = false
    private var supported = false
    private var isFloat = false
    private var sampleRate = 0
    private var channelCount = 2
    private var rampFrames = 1

    private var stages: Array<BiquadStage> = emptyArray()
    private var appliedRevision = -1L
    private var appliedEnabled = false

    private var preampCurrent = 0.0
    private var preampStep = 0.0
    private var preampRemaining = 0

    private var inputEnded = false
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    /** 由仓库在参数变化时调用；音频线程随后在下一个 buffer 开头看到新快照。 */
    fun update(filters: List<EqFilterSpec>, preampDb: Float, enabled: Boolean) {
        this.filters = filters
        this.preampDb = preampDb
        this.enabled = enabled
        revision++
    }

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
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount.coerceIn(1, MAX_CHANNELS)
        rampFrames = ((sampleRate.toDouble() * RAMP_WINDOW_MS) / 1000.0).toInt().coerceAtLeast(1)

        stages = emptyArray()
        appliedRevision = -1L
        appliedEnabled = false
        preampCurrent = 0.0
        preampRemaining = 0
        inputEnded = false
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return inputAudioFormat
    }

    override fun isActive(): Boolean = configured && supported

    override fun isEnded(): Boolean = inputEnded

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer = outputBuffer

    override fun flush() {
        stages.forEach { it.resetState() }
        appliedRevision = -1L
        appliedEnabled = false
        preampCurrent = 0.0
        preampRemaining = 0
        inputEnded = false
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun reset() {
        flush()
        stages = emptyArray()
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

        syncParameters()

        if (!enabled) {
            // 旁路：未启用时只是原样搬运，不做任何滤波。
            out.put(inputBuffer)
            out.flip()
            return
        }

        val activeStages = stages
        if (isFloat) {
            val sampleCount = byteCount / 4
            for (index in 0 until sampleCount) {
                advancePreamp()
                var sample = inputBuffer.getFloat().toDouble() * preampCurrent
                val channel = index % channelCount
                for (stage in activeStages) {
                    sample = stage.process(channel, sample)
                }
                out.putFloat(softClip(sample).toFloat())
            }
        } else {
            val sampleCount = byteCount / 2
            for (index in 0 until sampleCount) {
                advancePreamp()
                var sample = (inputBuffer.getShort().toDouble() / 32768.0) * preampCurrent
                val channel = index % channelCount
                for (stage in activeStages) {
                    sample = stage.process(channel, sample)
                }
                val scaled = (softClip(sample) * 32768.0).toInt().coerceIn(-32768, 32767)
                out.putShort(scaled.toShort())
            }
        }
        out.flip()
    }

    private fun advancePreamp() {
        if (preampRemaining > 0) {
            preampCurrent += preampStep
            preampRemaining--
        }
    }

    /**
     * 参数变了才重算：重建滤波器数量、写入目标系数（带 10ms 斜坡）、排好预放大斜坡。
     * 用 revision 比对实现"参数变了才重算"，避免每帧都重建系数。
     */
    private fun syncParameters() {
        val currentRevision = revision
        if (currentRevision == appliedRevision) return
        appliedRevision = currentRevision

        if (!enabled) {
            // 关闭后不必留着滤波器状态；下次打开会重建。
            stages = emptyArray()
            appliedEnabled = false
            preampRemaining = 0
            preampCurrent = 0.0
            return
        }

        val specs = filters
        if (stages.size != specs.size) {
            stages = Array(specs.size) { BiquadStage(MAX_CHANNELS) }
            appliedEnabled = false
        }

        val sampleRateDouble = sampleRate.toDouble()
        for (index in specs.indices) {
            stages[index].setCoefficients(
                BiquadCoefficients.compute(specs[index], sampleRateDouble),
                rampFrames
            )
        }

        if (!appliedEnabled) {
            // 从旁路切到生效：清掉旧残留，否则打开瞬间会有一声拖尾。
            stages.forEach { it.resetState() }
            preampCurrent = 0.0
        }

        val targetPreamp = 10.0.pow(preampDb / 20.0)
        preampStep = (targetPreamp - preampCurrent) / rampFrames
        preampRemaining = rampFrames
        appliedEnabled = true
    }

    /** 软限幅：0.9 以下直通，以上用 tanh 渐进压限。 */
    private fun softClip(sample: Double): Double {
        val magnitude = abs(sample)
        if (magnitude <= SOFT_CLIP_KNEE) return sample
        val excess = magnitude - SOFT_CLIP_KNEE
        val limited = tanh(excess / SOFT_CLIP_RANGE) * SOFT_CLIP_RANGE + SOFT_CLIP_KNEE
        return if (sample < 0.0) -limited else limited
    }

    private companion object {
        const val MAX_CHANNELS = 8
        const val RAMP_WINDOW_MS = 10.0
        const val SOFT_CLIP_KNEE = 0.9
        const val SOFT_CLIP_RANGE = 0.1
    }
}
