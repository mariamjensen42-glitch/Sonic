package com.cycling.presentation.onlinesources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.online.model.PluginImportResult
import com.cycling.domain.online.repository.OnlinePluginRepository
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 在线音源设置页的 ViewModel。
 *
 * 把仓库的插件/排序/优先级状态收进一个 UiState；把用户意图翻译成仓库调用；
 * 用一次性 Effect 把结果吐成 Toast。同步等待的操作（导入、探测）用
 * `importing` / `testingPluginId` 驱动 UI 的 loading。
 */
@HiltViewModel
class OnlineSourcesViewModel @Inject constructor(
    private val pluginRepository: OnlinePluginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineSourcesUiState())
    val uiState: StateFlow<OnlineSourcesUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<OnlineSourcesEffect>(extraBufferCapacity = 8)
    val uiEffect: SharedFlow<OnlineSourcesEffect> = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch { pluginRepository.initialize() }
        viewModelScope.launch {
            pluginRepository.plugins.collect { plugins ->
                _uiState.update { it.copy(plugins = plugins) }
            }
        }
        viewModelScope.launch {
            pluginRepository.sortMode.collect { mode ->
                _uiState.update { it.copy(sortMode = mode) }
            }
        }
        viewModelScope.launch {
            pluginRepository.priority.collect { priority ->
                _uiState.update { it.copy(priority = priority) }
            }
        }
    }

    fun handleIntent(intent: OnlineSourcesIntent) {
        when (intent) {
            is OnlineSourcesIntent.ImportFile -> importFile(intent.uri)
            is OnlineSourcesIntent.ImportText -> importContent(intent.content)
            is OnlineSourcesIntent.ImportUrl -> importUrl(intent.url)
            is OnlineSourcesIntent.ToggleEnabled -> viewModelScope.launch {
                pluginRepository.setEnabled(intent.pluginId, intent.enabled)
            }

            is OnlineSourcesIntent.SetSortMode -> viewModelScope.launch {
                pluginRepository.setSortMode(intent.mode)
            }

            is OnlineSourcesIntent.SetPriority -> viewModelScope.launch {
                pluginRepository.setPriority(intent.priority)
            }

            is OnlineSourcesIntent.MovePlugin -> movePlugin(intent.pluginId, intent.offset)
            is OnlineSourcesIntent.DeletePlugin -> deletePlugin(intent.pluginId)
            is OnlineSourcesIntent.TestPlugin -> testPlugin(intent.pluginId)
            is OnlineSourcesIntent.SetAutoBlock -> viewModelScope.launch {
                pluginRepository.setAutoBlockEnabled(intent.pluginId, intent.enabled)
            }

            is OnlineSourcesIntent.ClearBlockedPlatforms -> viewModelScope.launch {
                pluginRepository.setBlockedPlatforms(intent.pluginId, emptySet())
            }

            OnlineSourcesIntent.RestoreBuiltin -> restoreBuiltin()
        }
    }

    private fun restoreBuiltin() {
        viewModelScope.launch {
            val restored = runCatching { pluginRepository.restoreBundledPlugins() }.getOrDefault(0)
            _uiEffect.emit(
                OnlineSourcesEffect.ShowToast(
                    R.string.online_sources_restore_builtin_done,
                    restored.toString()
                )
            )
        }
    }

    private fun importFile(uri: String) {
        if (_uiState.value.importing) return
        _uiState.update { it.copy(importing = true) }
        viewModelScope.launch {
            val result = runCatching { pluginRepository.importFromFile(uri) }
                .getOrElse { PluginImportResult(error = it.message ?: "读取失败") }
            _uiState.update { it.copy(importing = false) }
            emitImportResult(result)
        }
    }

    private fun importContent(content: String) {
        if (_uiState.value.importing) return
        _uiState.update { it.copy(importing = true) }
        viewModelScope.launch {
            val result = runCatching { pluginRepository.importFromContent(content) }
                .getOrElse { PluginImportResult(error = it.message ?: "导入失败") }
            _uiState.update { it.copy(importing = false) }
            emitImportResult(result)
        }
    }

    private fun importUrl(url: String) {
        if (url.isBlank() || _uiState.value.importing) return
        _uiState.update { it.copy(importing = true) }
        viewModelScope.launch {
            val result = runCatching { pluginRepository.importFromUrl(url.trim()) }
                .getOrElse { PluginImportResult(error = it.message ?: "下载失败") }
            _uiState.update { it.copy(importing = false) }
            emitImportResult(result)
        }
    }

    private fun deletePlugin(pluginId: String) {
        viewModelScope.launch {
            pluginRepository.removePlugin(pluginId)
            _uiEffect.emit(OnlineSourcesEffect.ShowToast(R.string.online_sources_deleted))
        }
    }

    private fun movePlugin(pluginId: String, offset: Int) {
        val plugins = _uiState.value.plugins
        val index = plugins.indexOfFirst { it.id == pluginId }
        if (index < 0) return
        val target = (index + offset).coerceIn(0, plugins.lastIndex)
        if (target == index) return
        viewModelScope.launch { pluginRepository.movePlugin(pluginId, target) }
    }

    private fun testPlugin(pluginId: String) {
        if (_uiState.value.testingPluginId != null) return
        _uiState.update { it.copy(testingPluginId = pluginId) }
        viewModelScope.launch {
            val report = runCatching { pluginRepository.testPlugin(pluginId) }.getOrNull()
            _uiState.update { it.copy(testingPluginId = null) }
            val effect = if (report != null && report.isOk) {
                OnlineSourcesEffect.ShowToast(
                    R.string.online_sources_test_ok,
                    report.sources.size.toString()
                )
            } else {
                OnlineSourcesEffect.ShowToast(
                    R.string.online_sources_test_failed,
                    report?.error ?: "未知错误"
                )
            }
            _uiEffect.emit(effect)
        }
    }

    private suspend fun emitImportResult(result: PluginImportResult) {
        _uiEffect.emit(
            if (result.isSuccess) {
                OnlineSourcesEffect.ShowToast(
                    R.string.online_sources_import_ok,
                    result.plugin?.name.orEmpty()
                )
            } else {
                OnlineSourcesEffect.ShowToast(
                    R.string.online_sources_import_failed,
                    result.error ?: "未知错误"
                )
            }
        )
    }
}
