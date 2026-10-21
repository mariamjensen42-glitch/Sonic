package com.cycling.domain.repository

import com.cycling.domain.model.EqualizerPreset
import kotlinx.coroutines.flow.Flow

/**
 * 图形均衡器的用户设置。
 *
 * 与 `SettingsRepository` 分成两个仓库：均衡器的读改写全是"整组状态"（预设 = 10 个增益一起变），
 * 混进按项拆分的设置仓库会让每次拖动滑杆都触发一整套无关设置的重组。
 *
 * 实现的约定：
 * - [bandGains] 的长度恒为 10（见 `EqualizerBands.count`），越界/损坏存盘数据按 0dB 补齐；
 * - 手动改任一频段会把 [preset] 切成 `CUSTOM`，选回预设则整体覆盖增益；
 * - 写设置的同时必须把新参数推给音频处理器 —— 处理器只在音频线程读快照。
 * - 耳机校正（`AutoEqRepository`）的滤波器叠加在这 10 段之前，预放大取两者之和。
 */
interface EqualizerRepository {

    val enabled: Flow<Boolean>

    val preset: Flow<EqualizerPreset>

    val bandGains: Flow<List<Float>>

    val preampDb: Flow<Float>

    suspend fun setEnabled(enabled: Boolean)

    /** 应用预设增益；[EqualizerPreset.CUSTOM] 不是可选预设，忽略即可。 */
    suspend fun setPreset(preset: EqualizerPreset)

    /** 改单个频段（自动吸附到 0.5dB），并切到 `CUSTOM`。 */
    suspend fun setBandGain(index: Int, gainDb: Float)

    suspend fun setPreampDb(preampDb: Float)

    /** 增益归零、预设复位为平直、预放大归零，并撤掉耳机校正；不动开关。 */
    suspend fun reset()
}
