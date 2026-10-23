package com.cycling.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.model.PlayerEvent
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SettingsRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.GetLyricsUseCase
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
import com.cycling.presentation.R
import timber.log.Timber
import javax.inject.Inject
import com.cycling.domain.model.BeatState
import com.cycling.domain.repository.AudioReactiveRepository

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val getLyricsUseCase: GetLyricsUseCase,
    private val audioReactiveRepository: AudioReactiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlayerEffect>()
    val uiEffect: SharedFlow<PlayerEffect> = _uiEffect.asSharedFlow()

    /**
     * 点歌起播时是否跳转到播放页。单独一个 Flow 而不是塞进 uiState：AppNavGraph 收集它来
     * 决定要不要 navigate，收 uiState 会让整个导航图跟着播放位置一秒一跳地重组。
     */
    val openPlayerOnPlay: StateFlow<Boolean> = settingsRepository.openPlayerOnPlay
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /**
     * 播放页律动用的节拍快照。不放进 uiState：约 30Hz 的更新会让整页跟着重组。
     */
    val beat: StateFlow<BeatState> = audioReactiveRepository.beat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BeatState.Idle)

    private var lastLyricsSongPath: String? = null

    /**
     * 上一次查到的候选版本。带上内容，所以不放进 UI state —— 界面只需要版本描述，选中时用 id 回查。
     */
    private var lyricsCandidates: List<LyricsCandidate> = emptyList()

    init {
        observePlayerState()
        observePlaybackSettings()
        observePlaybackEvents()
    }

    /**
     * 播放失败只提示一次。是否已经跳到下一首由播放实现层决定（见 `PlayerEvent.PlaybackFailed`），
     * 这里只负责把话说清楚 —— 文件被删/损坏时界面若无反应，用户会以为播放器卡死了。
     */
    private fun observePlaybackEvents() {
        viewModelScope.launch {
            playerRepository.events.collect { event ->
                when (event) {
                    is PlayerEvent.PlaybackFailed -> _uiEffect.emit(
                        PlayerEffect.ShowToast(
                            if (event.skipped) {
                                R.string.player_playback_failed_skipped
                            } else {
                                R.string.player_playback_failed
                            }
                        )
                    )
                }
            }
        }
    }

    /**
     * 倍速与定时关闭都由播放器直接持有，这里读回来只是为了面板显示的值和真正在跑的值是同一个。
     */
    private fun observePlaybackSettings() {
        viewModelScope.launch {
            settingsRepository.playbackSpeed.collect { speed ->
                _uiState.update { it.copy(playbackSpeed = speed) }
            }
        }
        viewModelScope.launch {
            settingsRepository.sleepTimerPresetMinutes.collect { minutes ->
                _uiState.update { it.copy(sleepTimerMinutes = minutes) }
            }
        }
        viewModelScope.launch {
            playerRepository.sleepTimerRemainingMs.collect { remaining ->
                _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
            }
        }
        viewModelScope.launch {
            settingsRepository.playerSpectrumEnabled.collect { enabled ->
                _uiState.update { it.copy(playerSpectrumEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.hiResBadge.collect { badge ->
                _uiState.update { it.copy(hiResBadge = badge) }
            }
        }
        viewModelScope.launch {
            settingsRepository.hiResBadgeCustomUri.collect { uri ->
                _uiState.update { it.copy(hiResBadgeCustomUri = uri) }
            }
        }
        viewModelScope.launch {
            settingsRepository.lyricsTextSizeSp.collect { sp ->
                _uiState.update { it.copy(lyricsTextSizeSp = sp) }
            }
        }
        viewModelScope.launch {
            settingsRepository.lyricsAlignCenter.collect { enabled ->
                _uiState.update { it.copy(lyricsAlignCenter = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.lyricsGlowEnabled.collect { enabled ->
                _uiState.update { it.copy(lyricsGlowEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.lyricsBlurEnabled.collect { enabled ->
                _uiState.update { it.copy(lyricsBlurEnabled = enabled) }
            }
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            playerRepository.playerState.collect { state ->
                val currentSong = state.currentSong
                val songChanged = currentSong?.path != null && currentSong.path != lastLyricsSongPath
                _uiState.update { prev ->
                    state.toUiState().copy(
                        viewMode = prev.viewMode,
                        // 面板与倍速/定时这几个字段不属于 PlayerState，每次发射都会被 toUiState()
                        // 的默认值抹掉，必须像 viewMode 一样显式带过来。
                        showPlaybackSettings = prev.showPlaybackSettings,
                        playbackSpeed = prev.playbackSpeed,
                        playerSpectrumEnabled = prev.playerSpectrumEnabled,
                        sleepTimerRemainingMs = prev.sleepTimerRemainingMs,
                        sleepTimerMinutes = prev.sleepTimerMinutes,
                        hiResBadge = prev.hiResBadge,
                        hiResBadgeCustomUri = prev.hiResBadgeCustomUri,
                        lyricsTextSizeSp = prev.lyricsTextSizeSp,
                        lyricsAlignCenter = prev.lyricsAlignCenter,
                        lyricsGlowEnabled = prev.lyricsGlowEnabled,
                        lyricsBlurEnabled = prev.lyricsBlurEnabled,
                        lyrics = if (songChanged) null else prev.lyrics,
                        isLoadingLyrics = if (songChanged) false else prev.isLoadingLyrics,
                        hasLyrics = if (songChanged) false else prev.hasLyrics,
                        lyricsSource = if (songChanged) LyricsSource.NONE else prev.lyricsSource,
                        // 候选列表属于上一首歌，换歌时必须一起丢掉，否则会给出对不上的版本
                        lyricsCandidates = if (songChanged) emptyList() else prev.lyricsCandidates,
                        showLyricsCandidates = if (songChanged) false else prev.showLyricsCandidates,
                        isLoadingLyricsCandidates = if (songChanged) false else prev.isLoadingLyricsCandidates,
                        // 换歌时先归零：上一首的偏移在这首歌上会显示成歌词跑偏，直到读完新值
                        lyricsOffsetMs = if (songChanged) 0L else prev.lyricsOffsetMs,
                        isFavorite = if (currentSong?.id != null && currentSong.id == prev.currentSong?.id) prev.isFavorite else currentSong?.isFavorite ?: false
                    )
                }

                if (currentSong != null && currentSong.path != lastLyricsSongPath) {
                    lastLyricsSongPath = currentSong.path
                    loadLyrics(currentSong)
                    loadLyricsOffset(currentSong.id)
                }
            }
        }
    }

    private fun loadLyrics(song: Song, refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLyrics = true) }

            // The whole song, not its path: a cache miss sends title/artist/duration to the online
            // providers, and only the path would not identify the track to a remote service.
            val result = if (refresh) getLyricsUseCase.refresh(song) else getLyricsUseCase(song)
            val syncedLyrics = result.syncedLyrics
            val hasLyrics = syncedLyrics != null && syncedLyrics.lines.isNotEmpty()

            _uiState.update {
                it.copy(
                    lyrics = syncedLyrics,
                    isLoadingLyrics = false,
                    hasLyrics = hasLyrics,
                    lyricsSource = result.source
                )
            }

            // A tap has to say how it went — the lyrics already on screen do not change on their
            // own, so without this the button looks as though it did nothing at all.
            if (refresh) {
                _uiEffect.emit(
                    PlayerEffect.ShowToast(
                        when {
                            !hasLyrics -> R.string.player_lyrics_unavailable
                            syncedLyrics?.isWordByWord == true -> R.string.player_lyrics_refreshed_word_by_word
                            else -> R.string.player_lyrics_refreshed
                        }
                    )
                )
            }
        }
    }

    private fun refreshLyrics() {
        _uiState.value.currentSong?.let { loadLyrics(it, refresh = true) }
    }

    /**
     * 拉一份版本列表给「选择歌词」用。
     *
     * 每次都重新查而不是复用上一份：用户点这个按钮，多半就是因为自动挑的那份不对，那么刚才那次
     * 自动挑选的结果正是他想绕开的东西。查的过程中面板立刻打开并显示加载中，不让点击看起来没反应。
     */
    private fun openLyricsCandidates() {
        val song = _uiState.value.currentSong ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(showLyricsCandidates = true, isLoadingLyricsCandidates = true) }

            val found = getLyricsUseCase.candidates(song)
            lyricsCandidates = found

            _uiState.update {
                it.copy(lyricsCandidates = found, isLoadingLyricsCandidates = false)
            }

            if (found.isEmpty()) {
                _uiEffect.emit(PlayerEffect.ShowToast(R.string.lyrics_candidates_empty))
            }
        }
    }

    private fun dismissLyricsCandidates() {
        _uiState.update { it.copy(showLyricsCandidates = false) }
    }

    private fun selectLyricsCandidate(candidateId: String) {
        val song = _uiState.value.currentSong ?: return
        val candidate = lyricsCandidates.firstOrNull { it.id == candidateId } ?: return

        viewModelScope.launch {
            val result = getLyricsUseCase.selectCandidate(song, candidate)
            val syncedLyrics = result.syncedLyrics
            val hasLyrics = syncedLyrics != null && syncedLyrics.lines.isNotEmpty()

            _uiState.update {
                it.copy(
                    lyrics = syncedLyrics,
                    hasLyrics = hasLyrics,
                    lyricsSource = result.source,
                    showLyricsCandidates = false,
                    lyricsCandidates = emptyList()
                )
            }

            _uiEffect.emit(PlayerEffect.ShowToast(R.string.lyrics_candidate_selected))
        }
    }

    private fun loadLyricsOffset(songId: Long) {
        viewModelScope.launch {
            val offset = songRepository.getLyricsOffset(songId)
            _uiState.update { it.copy(lyricsOffsetMs = offset) }
        }
    }

    /**
     * 调整立刻生效并写回数据库：这个动作就是用户一边听一边对轴，等到确认才生效的话，他没法听出
     * 调对了没有。
     */
    private fun adjustLyricsOffset(deltaMs: Long) {
        val currentSong = _uiState.value.currentSong ?: return
        val offset = (_uiState.value.lyricsOffsetMs + deltaMs)
            .coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS)
        _uiState.update { it.copy(lyricsOffsetMs = offset) }
        viewModelScope.launch { songRepository.setLyricsOffset(currentSong.id, offset) }
    }

    private fun resetLyricsOffset() {
        val currentSong = _uiState.value.currentSong ?: return
        _uiState.update { it.copy(lyricsOffsetMs = 0L) }
        viewModelScope.launch { songRepository.setLyricsOffset(currentSong.id, 0L) }
    }

    fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.PlaySong -> {
                Timber.d("handleIntent: PlaySong - ${intent.song.title}")
                playSong(intent.song, intent.queue)
            }
            is PlayerIntent.PlayFromQueue -> {
                Timber.d("handleIntent: PlayFromQueue - index=${intent.index}")
                playFromQueue(intent.index)
            }
            is PlayerIntent.PlayPause -> {
                Timber.d("handleIntent: PlayPause")
                playPause()
            }
            is PlayerIntent.SeekTo -> {
                Timber.d("handleIntent: SeekTo - position=${intent.position}")
                seekTo(intent.position)
            }
            is PlayerIntent.SkipToNext -> {
                Timber.d("handleIntent: SkipToNext")
                skipToNext()
            }
            is PlayerIntent.SkipToPrevious -> {
                Timber.d("handleIntent: SkipToPrevious")
                skipToPrevious()
            }
            is PlayerIntent.ToggleRepeatMode -> {
                Timber.d("handleIntent: ToggleRepeatMode")
                toggleRepeatMode()
            }
            is PlayerIntent.ToggleShuffleMode -> {
                Timber.d("handleIntent: ToggleShuffleMode")
                toggleShuffleMode()
            }
            is PlayerIntent.AddToQueue -> {
                Timber.d("handleIntent: AddToQueue - ${intent.song.title}")
                addToQueue(intent.song)
            }
            is PlayerIntent.PlayNext -> {
                Timber.d("handleIntent: PlayNext - ${intent.song.title}")
                playNext(intent.song)
            }
            is PlayerIntent.MoveQueueItem -> {
                Timber.d("handleIntent: MoveQueueItem - from=${intent.fromIndex} to=${intent.toIndex}")
                moveQueueItem(intent.fromIndex, intent.toIndex)
            }
            is PlayerIntent.RemoveFromQueue -> {
                Timber.d("handleIntent: RemoveFromQueue - index=${intent.index}")
                removeFromQueue(intent.index)
            }
            is PlayerIntent.ClearQueue -> {
                Timber.d("handleIntent: ClearQueue")
                clearQueue()
            }
            is PlayerIntent.UpdateProgress -> { }
            is PlayerIntent.ToggleFavorite -> {
                Timber.d("handleIntent: ToggleFavorite")
                toggleFavorite()
            }
            is PlayerIntent.ToggleViewMode -> {
                Timber.d("handleIntent: ToggleViewMode")
                toggleViewMode()
            }

            is PlayerIntent.SetViewMode -> {
                _uiState.update { it.copy(viewMode = intent.mode) }
            }
            is PlayerIntent.RefreshLyrics -> {
                Timber.d("handleIntent: RefreshLyrics")
                refreshLyrics()
            }
            is PlayerIntent.OpenLyricsCandidates -> {
                Timber.d("handleIntent: OpenLyricsCandidates")
                openLyricsCandidates()
            }
            is PlayerIntent.DismissLyricsCandidates -> {
                Timber.d("handleIntent: DismissLyricsCandidates")
                dismissLyricsCandidates()
            }
            is PlayerIntent.SelectLyricsCandidate -> {
                Timber.d("handleIntent: SelectLyricsCandidate id=${intent.candidateId}")
                selectLyricsCandidate(intent.candidateId)
            }
            is PlayerIntent.AdjustLyricsOffset -> {
                Timber.d("handleIntent: AdjustLyricsOffset delta=${intent.deltaMs}")
                adjustLyricsOffset(intent.deltaMs)
            }
            is PlayerIntent.ResetLyricsOffset -> {
                Timber.d("handleIntent: ResetLyricsOffset")
                resetLyricsOffset()
            }
            is PlayerIntent.TogglePlaybackSettings -> {
                _uiState.update { it.copy(showPlaybackSettings = !it.showPlaybackSettings) }
            }
            is PlayerIntent.SetPlaybackSpeed -> {
                Timber.d("handleIntent: SetPlaybackSpeed - ${intent.speed}")
                viewModelScope.launch { settingsRepository.setPlaybackSpeed(intent.speed) }
            }
            is PlayerIntent.SetSleepTimer -> {
                Timber.d("handleIntent: SetSleepTimer - ${intent.minutes}min")
                playerRepository.setSleepTimer(intent.minutes)
                viewModelScope.launch {
                    settingsRepository.setSleepTimerPresetMinutes(intent.minutes)
                }
            }
            is PlayerIntent.CancelSleepTimer -> {
                Timber.d("handleIntent: CancelSleepTimer")
                playerRepository.cancelSleepTimer()
            }
            is PlayerIntent.SetPlayerSpectrumEnabled -> {
                Timber.d("handleIntent: SetPlayerSpectrumEnabled - ${intent.enabled}")
                viewModelScope.launch {
                    settingsRepository.setPlayerSpectrumEnabled(intent.enabled)
                }
            }
            is PlayerIntent.SetLyricsTextSizeSp -> {
                viewModelScope.launch { settingsRepository.setLyricsTextSizeSp(intent.sp) }
            }
            is PlayerIntent.SetLyricsAlignCenter -> {
                viewModelScope.launch { settingsRepository.setLyricsAlignCenter(intent.enabled) }
            }
            is PlayerIntent.SetLyricsGlowEnabled -> {
                viewModelScope.launch { settingsRepository.setLyricsGlowEnabled(intent.enabled) }
            }
            is PlayerIntent.SetLyricsBlurEnabled -> {
                viewModelScope.launch { settingsRepository.setLyricsBlurEnabled(intent.enabled) }
            }
        }
    }

    private fun playSong(song: com.cycling.domain.model.Song, queue: List<com.cycling.domain.model.Song>) {
        playerRepository.playSong(song, queue)
    }

    private fun playFromQueue(index: Int) {
        val queue = _uiState.value.playbackQueue
        if (index in queue.indices) {
            playerRepository.playSong(queue[index], queue)
        }
    }

    private fun playPause() {
        playerRepository.playPause()
    }

    private fun seekTo(position: Long) {
        playerRepository.seekTo(position)
    }

    private fun skipToNext() {
        playerRepository.skipToNext()
    }

    private fun skipToPrevious() {
        playerRepository.skipToPrevious()
    }

    private fun toggleRepeatMode() {
        val currentMode = _uiState.value.repeatMode
        val newMode = when (currentMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerRepository.setRepeatMode(newMode)
    }

    private fun toggleShuffleMode() {
        val newShuffleMode = !_uiState.value.shuffleMode
        playerRepository.setShuffleMode(newShuffleMode)
    }

    private fun addToQueue(song: com.cycling.domain.model.Song) {
        playerRepository.addToQueue(song)
        viewModelScope.launch {
            _uiEffect.emit(PlayerEffect.ShowToast(R.string.player_added_to_queue))
        }
    }

    private fun playNext(song: com.cycling.domain.model.Song) {
        playerRepository.playNext(song)
        viewModelScope.launch {
            _uiEffect.emit(PlayerEffect.ShowToast(R.string.player_play_next))
        }
    }

    private fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playerRepository.moveQueueItem(fromIndex, toIndex)
    }

    private fun removeFromQueue(index: Int) {
        playerRepository.removeFromQueue(index)
    }

    private fun clearQueue() {
        playerRepository.clearQueue()
    }

    private fun toggleFavorite() {
        val currentSong = _uiState.value.currentSong ?: return
        viewModelScope.launch {
            val newFavoriteStatus = songRepository.toggleFavorite(currentSong.id)
            playerRepository.setSongFavorite(currentSong.id, newFavoriteStatus)
            _uiState.update { it.copy(isFavorite = newFavoriteStatus) }
            val message = if (newFavoriteStatus) R.string.player_added_to_favorites else R.string.player_removed_from_favorites
            _uiEffect.emit(PlayerEffect.ShowToast(message))
        }
    }

    private fun toggleViewMode() {
        _uiState.update { state ->
            val newViewMode = when (state.viewMode) {
                PlayerViewMode.COVER -> PlayerViewMode.LYRICS
                PlayerViewMode.LYRICS -> PlayerViewMode.COVER
                PlayerViewMode.QUEUE -> PlayerViewMode.COVER
            }
            state.copy(viewMode = newViewMode)
        }
    }
}
