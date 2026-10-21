package com.cycling.presentation.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.AudioReactiveRepository
import com.cycling.domain.repository.AutoEqRepository
import com.cycling.domain.repository.EqualizerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 均衡器页面。所有写入都直接落到仓库，界面状态再从仓库的 Flow 回流 ——
 * 这样"实际在跑的滤波器"和"页面显示的值"永远是同一份，不会出现滑杆回了但声音没变的错觉。
 *
 * 目录搜索按输入节流：8850 条虽然查得快，但每敲一个字符查一次会让输入框在高负载机型上发顿。
 */
@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerRepository: EqualizerRepository,
    private val audioReactiveRepository: AudioReactiveRepository,
    private val autoEqRepository: AutoEqRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        equalizerRepository.enabled
            .onEach { enabled -> _uiState.update { it.copy(enabled = enabled) } }
            .launchIn(viewModelScope)
        equalizerRepository.preset
            .onEach { preset -> _uiState.update { it.copy(preset = preset) } }
            .launchIn(viewModelScope)
        equalizerRepository.bandGains
            .onEach { gains -> _uiState.update { it.copy(bandGains = gains) } }
            .launchIn(viewModelScope)
        equalizerRepository.preampDb
            .onEach { preamp -> _uiState.update { it.copy(preampDb = preamp) } }
            .launchIn(viewModelScope)
        audioReactiveRepository.visualizationEnabled
            .onEach { enabled -> _uiState.update { it.copy(visualizationEnabled = enabled) } }
            .launchIn(viewModelScope)
        autoEqRepository.applied
            .onEach { applied -> _uiState.update { it.copy(autoEq = applied) } }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: EqualizerIntent) {
        when (intent) {
            is EqualizerIntent.SetEnabled ->
                viewModelScope.launch { equalizerRepository.setEnabled(intent.enabled) }

            is EqualizerIntent.SetPreset ->
                viewModelScope.launch { equalizerRepository.setPreset(intent.preset) }

            is EqualizerIntent.SetBandGain ->
                viewModelScope.launch {
                    equalizerRepository.setBandGain(intent.index, intent.gainDb)
                }

            is EqualizerIntent.SetPreamp ->
                viewModelScope.launch { equalizerRepository.setPreampDb(intent.preampDb) }

            EqualizerIntent.Reset ->
                viewModelScope.launch { equalizerRepository.reset() }

            is EqualizerIntent.SetVisualizationEnabled ->
                viewModelScope.launch {
                    audioReactiveRepository.setVisualizationEnabled(intent.enabled)
                }

            EqualizerIntent.OpenAutoEqPicker -> {
                _uiState.update { it.copy(autoEqPickerVisible = true) }
                loadCatalogSize()
                runSearch()
            }

            EqualizerIntent.DismissAutoEqPicker -> {
                searchJob?.cancel()
                _uiState.update { it.copy(autoEqPickerVisible = false) }
            }

            is EqualizerIntent.SetAutoEqQuery -> {
                _uiState.update { it.copy(autoEqQuery = intent.query) }
                scheduleSearch()
            }

            is EqualizerIntent.SetAutoEqFormFactor -> {
                _uiState.update { it.copy(autoEqFormFactor = intent.formFactor) }
                scheduleSearch()
            }

            is EqualizerIntent.ApplyAutoEq -> viewModelScope.launch {
                autoEqRepository.apply(intent.id)
                searchJob?.cancel()
                _uiState.update { it.copy(autoEqPickerVisible = false) }
            }

            EqualizerIntent.ClearAutoEq ->
                viewModelScope.launch { autoEqRepository.clear() }
        }
    }

    private fun loadCatalogSize() {
        viewModelScope.launch {
            val size = autoEqRepository.totalCount()
            _uiState.update { it.copy(autoEqCatalogSize = size) }
        }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch()
        }
    }

    private fun runSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch() }
    }

    private suspend fun performSearch() {
        val state = _uiState.value
        _uiState.update { it.copy(autoEqSearching = true) }
        val results = autoEqRepository.search(
            query = state.autoEqQuery,
            formFactor = state.autoEqFormFactor,
            limit = AutoEqRepository.DEFAULT_LIMIT
        )
        _uiState.update { it.copy(autoEqResults = results, autoEqSearching = false) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 180L
    }
}
