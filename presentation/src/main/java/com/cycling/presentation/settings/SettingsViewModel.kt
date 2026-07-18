package com.cycling.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.ExcludedFolder
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.ScanResult
import com.cycling.domain.model.ScanStep
import com.cycling.domain.model.ThemeMode
import com.cycling.domain.repository.AiRepository
import com.cycling.domain.repository.ExcludedFolderRepository
import com.cycling.domain.repository.ThemeRepository
import com.cycling.domain.usecase.GetLibraryStatsUseCase
import com.cycling.domain.usecase.GetScanProgressUseCase
import com.cycling.domain.usecase.ResetScanProgressUseCase
import com.cycling.domain.usecase.ScanMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class SettingsDestination {
    MAIN, SCAN, EXCLUDE_FOLDERS, API_KEY_CONFIG, LIBRARY_STATS
}

data class SettingsUiState(
    val currentDestination: SettingsDestination = SettingsDestination.MAIN,
    val isScanning: Boolean = false,
    val scanStep: SettingsScanStep = SettingsScanStep.Idle,
    val scanProgress: Float = 0f,
    val songsProcessed: Int = 0,
    val totalSongs: Int = 0,
    val scanResult: ScanResult? = null,
    val scanError: String? = null,
    val hasStoragePermission: Boolean = false,
    val shouldRequestPermission: Boolean = false,
    val excludedFolders: List<ExcludedFolder> = emptyList(),
    val isLoadingFolders: Boolean = true,
    val apiKeyInput: String = "",
    val apiKeySaved: Boolean = false,
    val libraryStats: LibraryStats? = null,
    val isLoadingStats: Boolean = false,
    val statsError: String? = null
)

sealed interface SettingsScanStep {
    data object Idle : SettingsScanStep
    data object QueryingMediaStore : SettingsScanStep
    data object SavingToDatabase : SettingsScanStep
    data object Completed : SettingsScanStep
    data object Error : SettingsScanStep
}

sealed interface SettingsIntent {
    data class NavigateTo(val destination: SettingsDestination) : SettingsIntent
    data object NavigateBack : SettingsIntent
    data object StartScan : SettingsIntent
    data object ResetScan : SettingsIntent
    data object RequestPermission : SettingsIntent
    data class PermissionResult(val granted: Boolean) : SettingsIntent
    data class AddExcludedFolder(val path: String) : SettingsIntent
    data class RemoveExcludedFolder(val path: String) : SettingsIntent
    data class UpdateApiKeyInput(val apiKey: String) : SettingsIntent
    data object SaveApiKey : SettingsIntent
    data object ClearApiKey : SettingsIntent
    data object LoadLibraryStats : SettingsIntent
    data class SetThemeMode(val mode: ThemeMode) : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect
    data object RequestStoragePermission : SettingsEffect
    data object NavigateBackFromSettings : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val aiRepository: AiRepository,
    private val scanMusicUseCase: ScanMusicUseCase,
    private val getScanProgressUseCase: GetScanProgressUseCase,
    private val resetScanProgressUseCase: ResetScanProgressUseCase,
    private val excludedFolderRepository: ExcludedFolderRepository,
    private val getLibraryStatsUseCase: GetLibraryStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SettingsEffect>()
    val uiEffect: SharedFlow<SettingsEffect> = _uiEffect.asSharedFlow()

    val themeMode = themeRepository.themeMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ThemeMode.SYSTEM
        )

    val hasApiKey = kotlinx.coroutines.flow.flow {
        emit(aiRepository.hasApiKey())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    init {
        Timber.d("SettingsViewModel: initialized")
        observeScanProgress()
        loadExcludedFolders()
        loadLibraryStats()
    }

    fun handleIntent(intent: SettingsIntent) {
        Timber.d("handleIntent: $intent")
        when (intent) {
            is SettingsIntent.NavigateTo -> navigateTo(intent.destination)
            is SettingsIntent.NavigateBack -> navigateBack()
            is SettingsIntent.StartScan -> startScan()
            is SettingsIntent.ResetScan -> resetScan()
            is SettingsIntent.RequestPermission -> requestPermission()
            is SettingsIntent.PermissionResult -> handlePermissionResult(intent.granted)
            is SettingsIntent.AddExcludedFolder -> addExcludedFolder(intent.path)
            is SettingsIntent.RemoveExcludedFolder -> removeExcludedFolder(intent.path)
            is SettingsIntent.UpdateApiKeyInput -> updateApiKeyInput(intent.apiKey)
            is SettingsIntent.SaveApiKey -> saveApiKey()
            is SettingsIntent.ClearApiKey -> clearApiKey()
            is SettingsIntent.LoadLibraryStats -> loadLibraryStats()
            is SettingsIntent.SetThemeMode -> setThemeMode(intent.mode)
        }
    }

    fun checkAndRequestPermission(hasPermission: Boolean) {
        Timber.d("checkAndRequestPermission: hasPermission = $hasPermission")
        if (hasPermission) {
            _uiState.update { it.copy(hasStoragePermission = true, shouldRequestPermission = false) }
        } else {
            _uiState.update { it.copy(hasStoragePermission = false, shouldRequestPermission = true) }
        }
    }

    private fun navigateTo(destination: SettingsDestination) {
        Timber.d("navigateTo: $destination")
        _uiState.update { it.copy(currentDestination = destination) }
        if (destination == SettingsDestination.LIBRARY_STATS) {
            loadLibraryStats()
        }
    }

    private fun navigateBack() {
        val currentDestination = _uiState.value.currentDestination
        Timber.d("navigateBack: currentDestination = $currentDestination")
        if (currentDestination == SettingsDestination.MAIN) {
            viewModelScope.launch {
                _uiEffect.emit(SettingsEffect.NavigateBackFromSettings)
            }
        } else {
            _uiState.update { it.copy(currentDestination = SettingsDestination.MAIN) }
        }
    }

    private fun requestPermission() {
        Timber.d("requestPermission: requesting storage permission")
        viewModelScope.launch {
            _uiEffect.emit(SettingsEffect.RequestStoragePermission)
        }
    }

    private fun handlePermissionResult(granted: Boolean) {
        Timber.d("handlePermissionResult: granted = $granted")
        if (granted) {
            _uiState.update { it.copy(hasStoragePermission = true, shouldRequestPermission = false) }
        } else {
            Timber.w("handlePermissionResult: permission denied")
            _uiState.update { 
                it.copy(
                    hasStoragePermission = false, 
                    shouldRequestPermission = false,
                    scanError = "需要存储权限才能扫描音乐文件"
                )
            }
        }
    }

    private fun observeScanProgress() {
        Timber.d("observeScanProgress: starting to observe scan progress")
        viewModelScope.launch {
            getScanProgressUseCase().collect { progress ->
                Timber.d("observeScanProgress: progress = ${progress.songsProcessed}/${progress.totalSongs}, step = ${progress.currentStep}")
                _uiState.update { state ->
                    state.copy(
                        isScanning = progress.isScanning,
                        scanStep = progress.currentStep.toSettingsScanStep(),
                        songsProcessed = progress.songsProcessed,
                        totalSongs = progress.totalSongs,
                        scanProgress = if (progress.totalSongs > 0) {
                            progress.songsProcessed.toFloat() / progress.totalSongs
                        } else 0f
                    )
                }
            }
        }
    }

    private fun startScan() {
        Timber.d("startScan: starting music scan")
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanError = null) }
            
            scanMusicUseCase()
                .onSuccess { result ->
                    Timber.d("startScan: scan completed successfully, found ${result.songsFound} songs")
                    _uiState.update { 
                        it.copy(
                            isScanning = false,
                            scanResult = result,
                            scanStep = SettingsScanStep.Completed
                        )
                    }
                    _uiEffect.emit(SettingsEffect.ShowToast("扫描完成: 发现 ${result.songsFound} 首歌曲"))
                }
                .onFailure { error ->
                    Timber.e(error, "startScan: scan failed")
                    _uiState.update { 
                        it.copy(
                            isScanning = false,
                            scanStep = SettingsScanStep.Error,
                            scanError = error.message
                        )
                    }
                    _uiEffect.emit(SettingsEffect.ShowToast("扫描失败: ${error.message}"))
                }
        }
    }

    private fun resetScan() {
        Timber.d("resetScan: resetting scan state")
        resetScanProgressUseCase()
        _uiState.update { 
            it.copy(
                isScanning = false,
                scanStep = SettingsScanStep.Idle,
                scanProgress = 0f,
                songsProcessed = 0,
                totalSongs = 0,
                scanResult = null,
                scanError = null
            )
        }
    }

    private fun loadExcludedFolders() {
        Timber.d("loadExcludedFolders: starting")
        viewModelScope.launch {
            excludedFolderRepository.excludedFolders.collect { folders ->
                Timber.d("loadExcludedFolders: loaded ${folders.size} folders")
                _uiState.update { it.copy(excludedFolders = folders, isLoadingFolders = false) }
            }
        }
    }

    private fun addExcludedFolder(path: String) {
        viewModelScope.launch {
            val exists = _uiState.value.excludedFolders.any { it.path == path }
            if (exists) {
                _uiEffect.emit(SettingsEffect.ShowToast("该文件夹已在排除列表中"))
                return@launch
            }
            excludedFolderRepository.addExcludedFolder(ExcludedFolder(path = path))
            _uiEffect.emit(SettingsEffect.ShowToast("已添加到排除列表"))
        }
    }

    private fun removeExcludedFolder(path: String) {
        viewModelScope.launch {
            excludedFolderRepository.removeExcludedFolder(path)
            _uiEffect.emit(SettingsEffect.ShowToast("已从排除列表移除"))
        }
    }

    private fun updateApiKeyInput(apiKey: String) {
        _uiState.update { it.copy(apiKeyInput = apiKey) }
    }

    private fun saveApiKey() {
        Timber.d("saveApiKey: saving API key")
        val apiKey = _uiState.value.apiKeyInput
        if (apiKey.isBlank()) {
            viewModelScope.launch {
                _uiEffect.emit(SettingsEffect.ShowToast("请输入 API Key"))
            }
            return
        }
        viewModelScope.launch {
            aiRepository.setApiKey(apiKey)
            _uiState.update { it.copy(apiKeySaved = true, apiKeyInput = "") }
            _uiEffect.emit(SettingsEffect.ShowToast("API Key 已保存"))
            Timber.d("saveApiKey: API key saved successfully")
        }
    }

    private fun clearApiKey() {
        Timber.d("clearApiKey: clearing API key")
        viewModelScope.launch {
            aiRepository.setApiKey("")
            _uiState.update { it.copy(apiKeySaved = true) }
            _uiEffect.emit(SettingsEffect.ShowToast("API Key 已清除"))
            Timber.d("clearApiKey: API key cleared successfully")
        }
    }

    private fun loadLibraryStats() {
        Timber.d("loadLibraryStats: starting")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true, statsError = null) }
            try {
                val stats = getLibraryStatsUseCase()
                Timber.d("loadLibraryStats: loaded stats successfully")
                _uiState.update { it.copy(
                    libraryStats = stats,
                    isLoadingStats = false
                )}
            } catch (e: Exception) {
                Timber.e(e, "loadLibraryStats: error loading stats")
                _uiState.update { it.copy(
                    isLoadingStats = false,
                    statsError = e.message
                )}
            }
        }
    }

    private fun setThemeMode(mode: ThemeMode) {
        Timber.d("setThemeMode: mode=$mode")
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    private fun ScanStep.toSettingsScanStep(): SettingsScanStep {
        return when (this) {
            ScanStep.IDLE -> SettingsScanStep.Idle
            ScanStep.QUERYING_MEDIASTORE -> SettingsScanStep.QueryingMediaStore
            ScanStep.SAVING_TO_DATABASE -> SettingsScanStep.SavingToDatabase
            ScanStep.COMPLETED -> SettingsScanStep.Completed
            ScanStep.ERROR -> SettingsScanStep.Error
        }
    }
}
