package com.cycling.presentation.equalizer

import com.cycling.domain.model.AppliedAutoEq
import com.cycling.domain.model.AutoEqEntry
import com.cycling.domain.model.AutoEqFormFactor
import com.cycling.domain.model.EqualizerBands
import com.cycling.domain.model.EqualizerPreset

/**
 * 均衡器页面的状态。整组一次性给出 —— 预设、10 个增益、预放大、律动开关本来就一起变；
 * 耳机校正的档案与挑选面板状态也挂在这里，它们只服务这一个页面。
 */
data class EqualizerUiState(
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.FLAT,
    val bandGains: List<Float> = EqualizerBands.DEFAULT_GAINS,
    val preampDb: Float = 0f,
    val visualizationEnabled: Boolean = true,
    val autoEq: AppliedAutoEq? = null,
    val autoEqCatalogSize: Int = 0,
    val autoEqPickerVisible: Boolean = false,
    val autoEqQuery: String = "",
    val autoEqFormFactor: AutoEqFormFactor? = null,
    val autoEqResults: List<AutoEqEntry> = emptyList(),
    val autoEqSearching: Boolean = false
)

sealed interface EqualizerIntent {
    data class SetEnabled(val enabled: Boolean) : EqualizerIntent
    data class SetPreset(val preset: EqualizerPreset) : EqualizerIntent
    data class SetBandGain(val index: Int, val gainDb: Float) : EqualizerIntent
    data class SetPreamp(val preampDb: Float) : EqualizerIntent
    data object Reset : EqualizerIntent
    data class SetVisualizationEnabled(val enabled: Boolean) : EqualizerIntent

    data object OpenAutoEqPicker : EqualizerIntent
    data object DismissAutoEqPicker : EqualizerIntent
    data class SetAutoEqQuery(val query: String) : EqualizerIntent
    data class SetAutoEqFormFactor(val formFactor: AutoEqFormFactor?) : EqualizerIntent
    data class ApplyAutoEq(val id: String) : EqualizerIntent
    data object ClearAutoEq : EqualizerIntent
}
