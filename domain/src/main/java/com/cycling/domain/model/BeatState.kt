package com.cycling.domain.model

/**
 * 音频节拍分析的一帧快照。
 *
 * 三个量都归一化到 `0f..1f`，由 `BeatAnalyzerProcessor` 在音频线程每 128 个采样算一次
 * （约 344Hz @44.1kHz），再按帧率节流后交给界面。
 *
 * 三个量各自的分析过程：
 *
 * - [level]：整体响度（块内 RMS × 2.4 后截断）。
 * - [pulse]：低频段能量相对其缓慢跟随者的比值 —— 鼓点的"攻击"落在这里，是律动最灵敏的一路。
 * - [beat]：瞬态尖峰包络。响度突然超过自身慢包络 1.3 倍时抬升，按 0.93/块的系数衰减，
 *   适合驱动一次性的"弹一下"。
 */
data class BeatState(
    val level: Float = 0f,
    val pulse: Float = 0f,
    val beat: Float = 0f
) {

    /** 安静到没有可视化价值（暂停、静音段、未开始播放）。界面据此冻结动画。 */
    val isSilent: Boolean
        get() = level < SILENT_THRESHOLD && pulse < SILENT_THRESHOLD && beat < SILENT_THRESHOLD

    companion object {
        val Idle = BeatState()

        private const val SILENT_THRESHOLD = 0.01f
    }
}
