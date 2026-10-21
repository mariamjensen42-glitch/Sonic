package com.cycling.presentation.onlinesources

import androidx.annotation.StringRes
import com.cycling.domain.online.model.OnlinePlugin
import com.cycling.domain.online.model.PluginPriority
import com.cycling.domain.online.model.PluginSortMode

/** 在线音源设置页的状态。 */
data class OnlineSourcesUiState(
    val plugins: List<OnlinePlugin> = emptyList(),
    val sortMode: PluginSortMode = PluginSortMode.MANUAL,
    val priority: PluginPriority = PluginPriority.FALLBACK,
    val importing: Boolean = false,
    val testingPluginId: String? = null
) {
    val hasPlugins: Boolean get() = plugins.isNotEmpty()
}

sealed interface OnlineSourcesIntent {
    data class ImportFile(val uri: String) : OnlineSourcesIntent
    data class ImportText(val content: String) : OnlineSourcesIntent
    data class ImportUrl(val url: String) : OnlineSourcesIntent
    data class ToggleEnabled(val pluginId: String, val enabled: Boolean) : OnlineSourcesIntent
    data class SetSortMode(val mode: PluginSortMode) : OnlineSourcesIntent
    data class SetPriority(val priority: PluginPriority) : OnlineSourcesIntent
    data class MovePlugin(val pluginId: String, val offset: Int) : OnlineSourcesIntent
    data class DeletePlugin(val pluginId: String) : OnlineSourcesIntent
    data class TestPlugin(val pluginId: String) : OnlineSourcesIntent
    data class SetAutoBlock(val pluginId: String, val enabled: Boolean) : OnlineSourcesIntent
    data class ClearBlockedPlatforms(val pluginId: String) : OnlineSourcesIntent
}

sealed interface OnlineSourcesEffect {
    data class ShowToast(
        @StringRes val messageRes: Int,
        val arg: String? = null
    ) : OnlineSourcesEffect
}
