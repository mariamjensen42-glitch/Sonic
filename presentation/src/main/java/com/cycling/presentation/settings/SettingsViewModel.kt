package com.cycling.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.ExcludedFolder
import com.cycling.domain.model.CacheUsage
import com.cycling.domain.model.HiResBadge
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.QQMusicAccount
import com.cycling.domain.model.ScanResult
import com.cycling.domain.model.ScanStep
import com.cycling.domain.repository.AiRepository
import com.cycling.domain.repository.CacheRepository
import com.cycling.domain.repository.ExcludedFolderRepository
import com.cycling.domain.repository.LibraryTransferRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.QQMusicAccountRepository
import com.cycling.domain.repository.RecordingFilterRepository
import com.cycling.domain.repository.SettingsRepository
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_CACHE_LIMIT_BYTES
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_LYRICS_PROVIDER_ORDER
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_SLEEP_TIMER_PRESET_MINUTES
import com.cycling.domain.usecase.GetLibraryStatsUseCase
import com.cycling.domain.usecase.ScanMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.cycling.presentation.R
import timber.log.Timber
import javax.inject.Inject

enum class SettingsDestination {
    MAIN,
    SCAN,
    EXCLUDE_FOLDERS,
    API_KEY_CONFIG,
    LIBRARY_STATS,
    EQUALIZER,
    ONLINE_SOURCES,
    QQ_MUSIC_ACCOUNT
}

data class SettingsUiState(
    val currentDestination: SettingsDestination = SettingsDestination.MAIN,
    val isScanning: Boolean = false,
    val scanStep: SettingsScanStep = SettingsScanStep.Idle,
    val scanProgress: Float = 0f,
    val songsProcessed: Int = 0,
    val totalSongs: Int = 0,
    val scanResult: ScanResult? = null,
    val scanErrorResId: Int? = null,
    val hasStoragePermission: Boolean = false,
    val shouldRequestPermission: Boolean = false,
    val excludedFolders: List<ExcludedFolder> = emptyList(),
    val isLoadingFolders: Boolean = true,
    val apiKeyInput: String = "",
    val apiKeySaved: Boolean = false,
    val libraryStats: LibraryStats? = null,
    val isLoadingStats: Boolean = false,
    val statsError: String? = null,
    /** 一次备份/恢复是否正在进行，用来禁用重复点击。 */
    val isTransferring: Boolean = false,
    val onlineLyricsEnabled: Boolean = true,
    val lyricsProviderOrder: List<LyricsProvider> = DEFAULT_LYRICS_PROVIDER_ORDER,
    val playbackSpeed: Float = DEFAULT_PLAYBACK_SPEED,
    val skipSilence: Boolean = false,
    /** 扫描时是否把录音 / 通话录音踢出曲库。 */
    val excludeRecordings: Boolean = true,
    val replayGainEnabled: Boolean = false,
    val audioFadeDurationMs: Long = SettingsRepository.DEFAULT_AUDIO_FADE_DURATION_MS,
    val rememberPlaybackPosition: Boolean = true,
    val openPlayerOnPlay: Boolean = true,
    /** 队列播完后是否自动续上风格相近的歌。 */
    val autoContinueEnabled: Boolean = true,
    /** 上次选过的睡眠时长，用作下次打开选择器时的默认值。 */
    val sleepTimerMinutes: Int = DEFAULT_SLEEP_TIMER_PRESET_MINUTES,
    /** 定时器剩余毫秒，没开就是 null。 */
    val sleepTimerRemainingMs: Long? = null,
    /** 缓存面板的用量；首次统计完成前是 null（界面显示「统计中」）。 */
    val cacheUsage: CacheUsage? = null,
    val cacheLimitBytes: Long = DEFAULT_CACHE_LIMIT_BYTES,
    val autoCleanCacheEnabled: Boolean = true,
    /** 播放页封面左下角的小金标样式。 */
    val hiResBadge: HiResBadge = HiResBadge.OFF,
    /** 自定义小金标图片；与 [hiResBadge] 一起决定要不要真的去加载。 */
    val hiResBadgeCustomUri: String? = null,
    /** 已登录的 QQ 音乐账号；未登录为 null。设置页那一行用它显示当前身份。 */
    val qqMusicAccount: QQMusicAccount? = null,
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
    data class SetOnlineLyricsEnabled(val enabled: Boolean) : SettingsIntent
    /** Moves [provider] [offset] places through the priority list; out-of-range moves are ignored. */
    data class MoveLyricsProvider(val provider: LyricsProvider, val offset: Int) : SettingsIntent
    data class SetPlaybackSpeed(val speed: Float) : SettingsIntent
    data class SetSkipSilence(val enabled: Boolean) : SettingsIntent
    data class SetReplayGainEnabled(val enabled: Boolean) : SettingsIntent
    data class SetAudioFadeDuration(val durationMs: Long) : SettingsIntent
    data class SetRememberPlaybackPosition(val enabled: Boolean) : SettingsIntent
    data class SetOpenPlayerOnPlay(val enabled: Boolean) : SettingsIntent
    data class SetAutoContinueEnabled(val enabled: Boolean) : SettingsIntent
    data class ExportBackup(val targetUri: String) : SettingsIntent
    data class ImportBackup(val sourceUri: String) : SettingsIntent
    data class ImportM3u(val sourceUri: String) : SettingsIntent
    data class SetSleepTimer(val minutes: Int) : SettingsIntent
    data object CancelSleepTimer : SettingsIntent
    data class ClearDiskCache(val id: String) : SettingsIntent
    data object ClearLyricsCache : SettingsIntent
    data object ClearAiInfoCache : SettingsIntent
    data object ClearAllCaches : SettingsIntent
    data class SetCacheLimit(val bytes: Long) : SettingsIntent
    data class SetAutoCleanCache(val enabled: Boolean) : SettingsIntent
    data class SetHiResBadge(val badge: HiResBadge) : SettingsIntent
    data class SetHiResBadgeCustomUri(val uri: String?) : SettingsIntent
    data class SetExcludeRecordings(val enabled: Boolean) : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowToast(val messageResId: Int, val messageArg: Int? = null) : SettingsEffect
    data object RequestStoragePermission : SettingsEffect
    data object NavigateBackFromSettings : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val scanMusicUseCase: ScanMusicUseCase,
    private val excludedFolderRepository: ExcludedFolderRepository,
    private val recordingFilterRepository: RecordingFilterRepository,
    private val getLibraryStatsUseCase: GetLibraryStatsUseCase,
    private val settingsRepository: SettingsRepository,
    private val libraryTransferRepository: LibraryTransferRepository,
    private val playerRepository: PlayerRepository,
    private val cacheRepository: CacheRepository,
    private val qqMusicAccountRepository: QQMusicAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SettingsEffect>()
    val uiEffect: SharedFlow<SettingsEffect> = _uiEffect.asSharedFlow()

    private val _hasApiKey = MutableStateFlow(false)
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    private var isClearingCache = false

    init {
        Timber.d("SettingsViewModel: initialized")
        viewModelScope.launch {
            _hasApiKey.value = aiRepository.hasApiKey()
        }
        observeScanProgress()
        loadExcludedFolders()
        loadLibraryStats()
        observeLyricsSettings()
        observePlaybackSettings()
        observeAppearanceSettings()
        observeSleepTimer()
        observeCacheSettings()
        observeQqMusicAccount()
        loadCacheUsage()
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
            is SettingsIntent.SetOnlineLyricsEnabled -> setOnlineLyricsEnabled(intent.enabled)
            is SettingsIntent.MoveLyricsProvider -> moveLyricsProvider(intent.provider, intent.offset)
            is SettingsIntent.SetPlaybackSpeed -> setPlaybackSpeed(intent.speed)
            is SettingsIntent.SetSkipSilence -> setSkipSilence(intent.enabled)
            is SettingsIntent.SetExcludeRecordings -> setExcludeRecordings(intent.enabled)
            is SettingsIntent.SetReplayGainEnabled -> setReplayGainEnabled(intent.enabled)
            is SettingsIntent.SetAudioFadeDuration -> setAudioFadeDuration(intent.durationMs)
            is SettingsIntent.SetRememberPlaybackPosition -> setRememberPlaybackPosition(intent.enabled)
            is SettingsIntent.SetOpenPlayerOnPlay -> setOpenPlayerOnPlay(intent.enabled)
            is SettingsIntent.SetAutoContinueEnabled -> setAutoContinueEnabled(intent.enabled)
            is SettingsIntent.ExportBackup -> exportBackup(intent.targetUri)
            is SettingsIntent.ImportBackup -> importBackup(intent.sourceUri)
            is SettingsIntent.ImportM3u -> importM3u(intent.sourceUri)
            is SettingsIntent.SetSleepTimer -> setSleepTimer(intent.minutes)
            is SettingsIntent.CancelSleepTimer -> playerRepository.cancelSleepTimer()
            is SettingsIntent.ClearDiskCache -> clearCache { cacheRepository.clearDiskCache(intent.id) }
            is SettingsIntent.ClearLyricsCache -> clearCache { cacheRepository.clearLyricsCache() }
            is SettingsIntent.ClearAiInfoCache -> clearCache { cacheRepository.clearAiInfoCache() }
            is SettingsIntent.ClearAllCaches -> clearCache {
                _uiState.value.cacheUsage?.diskCaches?.forEach { cacheRepository.clearDiskCache(it.id) }
                cacheRepository.clearLyricsCache()
                cacheRepository.clearAiInfoCache()
            }
            is SettingsIntent.SetCacheLimit -> viewModelScope.launch {
                settingsRepository.setCacheLimitBytes(intent.bytes)
            }
            is SettingsIntent.SetAutoCleanCache -> viewModelScope.launch {
                settingsRepository.setAutoCleanCacheEnabled(intent.enabled)
            }
            is SettingsIntent.SetHiResBadge -> viewModelScope.launch {
                settingsRepository.setHiResBadge(intent.badge)
            }
            is SettingsIntent.SetHiResBadgeCustomUri -> viewModelScope.launch {
                settingsRepository.setHiResBadgeCustomUri(intent.uri)
            }
        }
    }

    private fun loadCacheUsage() {
        viewModelScope.launch { refreshCacheUsage() }
    }

    private fun observeCacheSettings() {
        settingsRepository.cacheLimitBytes
            .onEach { bytes -> _uiState.update { it.copy(cacheLimitBytes = bytes) } }
            .launchIn(viewModelScope)
        settingsRepository.autoCleanCacheEnabled
            .onEach { enabled -> _uiState.update { it.copy(autoCleanCacheEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    /** QQ 音乐账号：设置页那一行直接显示当前身份，登录/退出立刻反映出来。 */
    private fun observeQqMusicAccount() {
        qqMusicAccountRepository.account
            .onEach { account -> _uiState.update { it.copy(qqMusicAccount = account) } }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshCacheUsage() {
        val usage = cacheRepository.usage()
        _uiState.update { it.copy(cacheUsage = usage) }
    }

    /**
     * 清理是用户点一下就跑完的一次性任务，连点没有额外收益，只会有两遍删除和两条提示。
     * 门闩做成普通字段而不是状态：界面不需要知道它，禁用按钮反而会闪一下。
     */
    private fun clearCache(block: suspend () -> Unit) {
        if (isClearingCache) return
        isClearingCache = true
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_cache_cleared))
                }
                .onFailure { Timber.e(it, "clearCache failed") }
            refreshCacheUsage()
            isClearingCache = false
        }
    }

    /**
     * 睡眠定时器跑在播放器里而不是这个 ViewModel 里：它到点要暂停的是播放器，而且用户设好之后
     * 会离开设置页。这里只负责把选择传下去、再把剩余时间读回来显示。
     */
    private fun observeSleepTimer() {
        settingsRepository.sleepTimerPresetMinutes
            .onEach { minutes -> _uiState.update { it.copy(sleepTimerMinutes = minutes) } }
            .launchIn(viewModelScope)
        playerRepository.sleepTimerRemainingMs
            .onEach { remaining -> _uiState.update { it.copy(sleepTimerRemainingMs = remaining) } }
            .launchIn(viewModelScope)
    }

    private fun setSleepTimer(minutes: Int) {
        playerRepository.setSleepTimer(minutes)
        viewModelScope.launch { settingsRepository.setSleepTimerPresetMinutes(minutes) }
    }

    /**
     * 速度与跳过静音由 PlayerManager 直接从仓库读取，所以写进仓库就已经生效 —— 这里读回来只是
     * 为了让设置页显示的值和真正在跑的值是同一个。
     */
    private fun observePlaybackSettings() {
        settingsRepository.playbackSpeed
            .onEach { speed -> _uiState.update { it.copy(playbackSpeed = speed) } }
            .launchIn(viewModelScope)
        settingsRepository.skipSilence
            .onEach { enabled -> _uiState.update { it.copy(skipSilence = enabled) } }
            .launchIn(viewModelScope)
        settingsRepository.replayGainEnabled
            .onEach { enabled -> _uiState.update { it.copy(replayGainEnabled = enabled) } }
            .launchIn(viewModelScope)
        settingsRepository.audioFadeDurationMs
            .onEach { durationMs -> _uiState.update { it.copy(audioFadeDurationMs = durationMs) } }
            .launchIn(viewModelScope)
        settingsRepository.rememberPlaybackPosition
            .onEach { enabled -> _uiState.update { it.copy(rememberPlaybackPosition = enabled) } }
            .launchIn(viewModelScope)
        settingsRepository.openPlayerOnPlay
            .onEach { enabled -> _uiState.update { it.copy(openPlayerOnPlay = enabled) } }
            .launchIn(viewModelScope)
        settingsRepository.autoContinueEnabled
            .onEach { enabled -> _uiState.update { it.copy(autoContinueEnabled = enabled) } }
            .launchIn(viewModelScope)
        // 开关只是偏好，真正的过滤发生在扫描里：这里读回来是为了让开关显示的是仓库里的值。
        recordingFilterRepository.excludeRecordings
            .onEach { enabled -> _uiState.update { it.copy(excludeRecordings = enabled) } }
            .launchIn(viewModelScope)
    }

    /**
     * 切换「排除录音」后立刻重扫一遍：只写偏好不动曲库的话，已经进库的录音要等到下次
     * 扫描才消失，看起来就像开关没生效。没有存储权限时只存偏好，重扫交给用户自己在扫描页触发。
     */
    private fun setExcludeRecordings(enabled: Boolean) {
        viewModelScope.launch {
            recordingFilterRepository.setExcludeRecordings(enabled)
            if (_uiState.value.hasStoragePermission) startScan()
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setPlaybackSpeed(speed) }
    }

    private fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSkipSilence(enabled) }
    }

    private fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReplayGainEnabled(enabled) }
    }

    private fun setAudioFadeDuration(durationMs: Long) {
        viewModelScope.launch { settingsRepository.setAudioFadeDurationMs(durationMs) }
    }

    private fun setRememberPlaybackPosition(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRememberPlaybackPosition(enabled) }
    }

    private fun setOpenPlayerOnPlay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOpenPlayerOnPlay(enabled) }
    }

    private fun setAutoContinueEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoContinueEnabled(enabled) }
    }

    /** 封面小金标：设置页里唯一的外观项（歌词排版已经挪到歌词页自己的面板）。 */
    private fun observeAppearanceSettings() {
        settingsRepository.hiResBadge
            .onEach { badge -> _uiState.update { it.copy(hiResBadge = badge) } }
            .launchIn(viewModelScope)
        settingsRepository.hiResBadgeCustomUri
            .onEach { uri -> _uiState.update { it.copy(hiResBadgeCustomUri = uri) } }
            .launchIn(viewModelScope)
    }

    /**
     * 备份/恢复都是用户挑完文件之后才发生的一次性长任务。这里统一加 [isTransferring] 门闩：
     * SAF 回调可能被连点，重复写同一个文件会相互截断。
     */
    private fun exportBackup(targetUri: String) {
        if (_uiState.value.isTransferring) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTransferring = true) }
            runCatching { libraryTransferRepository.exportBackup(targetUri) }
                .onSuccess { count ->
                    _uiEffect.emit(
                        SettingsEffect.ShowToast(R.string.settings_vm_backup_exported, count)
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "exportBackup failed")
                    _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_transfer_failed))
                }
            _uiState.update { it.copy(isTransferring = false) }
        }
    }

    private fun importBackup(sourceUri: String) {
        if (_uiState.value.isTransferring) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTransferring = true) }
            runCatching { libraryTransferRepository.importBackup(sourceUri) }
                .onSuccess { result ->
                    _uiEffect.emit(
                        SettingsEffect.ShowToast(
                            R.string.settings_vm_backup_restored,
                            result.matchedSongs
                        )
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "importBackup failed")
                    _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_transfer_failed))
                }
            _uiState.update { it.copy(isTransferring = false) }
        }
    }

    private fun importM3u(sourceUri: String) {
        if (_uiState.value.isTransferring) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTransferring = true) }
            runCatching { libraryTransferRepository.importM3u(sourceUri, playlistName = null) }
                .onSuccess { result ->
                    _uiEffect.emit(
                        SettingsEffect.ShowToast(
                            R.string.settings_vm_m3u_imported,
                            result.imported
                        )
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "importM3u failed")
                    _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_transfer_failed))
                }
            _uiState.update { it.copy(isTransferring = false) }
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
                    scanErrorResId = R.string.settings_vm_permission_needed
                )
            }
        }
    }

    private fun observeScanProgress() {
        Timber.d("observeScanProgress: starting to observe scan progress")
        viewModelScope.launch {
            scanMusicUseCase.progress().collect { progress ->
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
            _uiState.update { it.copy(isScanning = true, scanErrorResId = null) }
            
            scanMusicUseCase.scan()
                .onSuccess { result ->
                    Timber.d("startScan: scan completed successfully, found ${result.songsFound} songs")
                    _uiState.update { 
                        it.copy(
                            isScanning = false,
                            scanResult = result,
                            scanStep = SettingsScanStep.Completed
                        )
                    }
                    _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_scan_complete, result.songsFound))
                }
                .onFailure { error ->
                    Timber.e(error, "startScan: scan failed")
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            scanStep = SettingsScanStep.Error,
                            scanErrorResId = R.string.settings_vm_scan_failed
                        )
                    }
                    _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_scan_failed))
                }
        }
    }

    private fun resetScan() {
        Timber.d("resetScan: resetting scan state")
        scanMusicUseCase.resetProgress()
        _uiState.update { 
            it.copy(
                isScanning = false,
                scanStep = SettingsScanStep.Idle,
                scanProgress = 0f,
                songsProcessed = 0,
                totalSongs = 0,
                scanResult = null,
                scanErrorResId = null
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

    private fun observeLyricsSettings() {
        viewModelScope.launch {
            settingsRepository.onlineLyricsEnabled.collect { enabled ->
                _uiState.update { it.copy(onlineLyricsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.lyricsProviderOrder.collect { order ->
                _uiState.update { it.copy(lyricsProviderOrder = order) }
            }
        }
    }

    /**
     * Persists the published order and lets the repository's flow update the UI, rather than
     * writing the new list to state directly — the repository is the one place that guarantees the
     * stored order is never empty.
     */
    private fun setOnlineLyricsEnabled(enabled: Boolean) {
        Timber.d("setOnlineLyricsEnabled: enabled = $enabled")
        viewModelScope.launch { settingsRepository.setOnlineLyricsEnabled(enabled) }
    }

    private fun moveLyricsProvider(provider: LyricsProvider, offset: Int) {
        val current = _uiState.value.lyricsProviderOrder
        val from = current.indexOf(provider)
        val to = from + offset
        if (from < 0 || to !in current.indices) return

        Timber.d("moveLyricsProvider: $provider $from -> $to")
        viewModelScope.launch {
            settingsRepository.setLyricsProviderOrder(
                current.toMutableList().apply { add(to, removeAt(from)) }
            )
        }
    }

    private fun addExcludedFolder(path: String) {
        viewModelScope.launch {
            val exists = _uiState.value.excludedFolders.any { it.path == path }
            if (exists) {
                _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_folder_already_excluded))
                return@launch
            }
            excludedFolderRepository.addExcludedFolder(ExcludedFolder(path = path))
            _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_folder_excluded))
        }
    }

    private fun removeExcludedFolder(path: String) {
        viewModelScope.launch {
            excludedFolderRepository.removeExcludedFolder(path)
            _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_folder_removed))
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
                _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_enter_api_key))
            }
            return
        }
        viewModelScope.launch {
            aiRepository.setApiKey(apiKey)
            _hasApiKey.value = true
            _uiState.update { it.copy(apiKeySaved = true, apiKeyInput = "") }
            _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_api_key_saved))
            Timber.d("saveApiKey: API key saved successfully")
        }
    }

    private fun clearApiKey() {
        Timber.d("clearApiKey: clearing API key")
        viewModelScope.launch {
            aiRepository.setApiKey("")
            _hasApiKey.value = false
            _uiState.update { it.copy(apiKeySaved = true) }
            _uiEffect.emit(SettingsEffect.ShowToast(R.string.settings_vm_api_key_cleared))
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
