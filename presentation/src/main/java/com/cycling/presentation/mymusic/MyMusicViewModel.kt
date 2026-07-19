package com.cycling.presentation.mymusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import com.cycling.domain.repository.AiRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.SongQueryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MyMusicViewModel @Inject constructor(
    private val songQueryUseCase: SongQueryUseCase,
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository,
    private val playlistRepository: PlaylistRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyMusicUiState())
    val uiState: StateFlow<MyMusicUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<MyMusicEffect>()
    val uiEffect: SharedFlow<MyMusicEffect> = _uiEffect.asSharedFlow()

    init {
        loadData()
    }

    fun handleIntent(intent: MyMusicIntent) {
        when (intent) {
            // Songs
            is MyMusicIntent.SelectTab -> selectTab(intent.tab)
            is MyMusicIntent.SongClick -> handleSongClick(intent.song)
            is MyMusicIntent.PlayAll -> playAll()
            is MyMusicIntent.ShuffleAll -> shuffleAll()
            is MyMusicIntent.ToggleFavorite -> toggleFavorite(intent.song)
            // Playlists
            is MyMusicIntent.FavoriteSongsClick -> handleFavoriteSongsClick()
            is MyMusicIntent.BackToPlaylists -> handleBackToPlaylists()
            is MyMusicIntent.PlaylistClick -> handlePlaylistClick(intent.playlist)
            is MyMusicIntent.CreatePlaylist -> handleCreatePlaylist(intent.name)
            is MyMusicIntent.DeletePlaylist -> handleDeletePlaylist(intent.playlistId)
            is MyMusicIntent.ShowRenameDialog -> handleShowRenameDialog(intent.playlist)
            is MyMusicIntent.RenamePlaylist -> handleRenamePlaylist(intent.playlistId, intent.newName)
            is MyMusicIntent.DismissRenameDialog -> handleDismissRenameDialog()
            is MyMusicIntent.ShowAiCreateDialog -> handleShowAiCreateDialog()
            is MyMusicIntent.DismissAiCreateDialog -> handleDismissAiCreateDialog()
            is MyMusicIntent.SetAiCreateMode -> handleSetAiCreateMode(intent.mode)
            is MyMusicIntent.SetSelectedSongCount -> handleSetSelectedSongCount(intent.count)
            is MyMusicIntent.SetThemeInput -> handleSetThemeInput(intent.theme)
            is MyMusicIntent.GenerateAiPlaylist -> handleGenerateAiPlaylist()
            is MyMusicIntent.ConfirmAiPlaylist -> handleConfirmAiPlaylist()
        }
    }

    private fun loadData() {
        Timber.d("loadData: starting")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            launch {
                songQueryUseCase.favorites().collectLatest { songs ->
                    _uiState.update { it.copy(favoriteSongs = songs, isLoading = false) }
                }
            }

            launch {
                songQueryUseCase.recentlyPlayed().collectLatest { songs ->
                    _uiState.update { it.copy(recentlyPlayedSongs = songs) }
                }
            }

            launch {
                songQueryUseCase.mostPlayed().collectLatest { songs ->
                    _uiState.update { it.copy(mostPlayedSongs = songs) }
                }
            }

            launch {
                playlistRepository.getAllPlaylists().collectLatest { playlists ->
                    _uiState.update { it.copy(playlists = playlists, isLoading = false) }
                }
            }
        }
    }

    private fun selectTab(tab: MyMusicTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    private fun handleSongClick(song: Song) {
        viewModelScope.launch {
            val currentSongs = getCurrentSongs()
            playerRepository.playSong(song, currentSongs)
            _uiEffect.emit(MyMusicEffect.NavigateToPlayer(song.id))
        }
    }

    private fun playAll() {
        val songs = getCurrentSongs()
        if (songs.isNotEmpty()) {
            viewModelScope.launch {
                playerRepository.playSong(songs.first(), songs)
            }
        }
    }

    private fun shuffleAll() {
        val songs = getCurrentSongs().shuffled()
        if (songs.isNotEmpty()) {
            viewModelScope.launch {
                playerRepository.playSong(songs.first(), songs)
            }
        }
    }

    private fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            songRepository.toggleFavorite(song.id)
            _uiEffect.emit(MyMusicEffect.ShowToast("已从喜欢移除"))
        }
    }

    private fun getCurrentSongs(): List<Song> {
        return when (_uiState.value.currentTab) {
            MyMusicTab.RECENTLY_PLAYED -> _uiState.value.recentlyPlayedSongs
            MyMusicTab.MOST_PLAYED -> _uiState.value.mostPlayedSongs
            MyMusicTab.PLAYLISTS -> emptyList()
        }
    }

    // ═══════════════════════════════════════════
    // Playlist logic
    // ═══════════════════════════════════════════

    private fun handleFavoriteSongsClick() {
        _uiState.update { it.copy(showFavoriteSongs = true) }
    }

    private fun handleBackToPlaylists() {
        _uiState.update { it.copy(showFavoriteSongs = false) }
    }

    private fun handlePlaylistClick(playlist: Playlist) {
        viewModelScope.launch {
            _uiEffect.emit(MyMusicEffect.NavigateToPlaylistDetail(playlist.id))
        }
    }

    private fun handleCreatePlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
            _uiEffect.emit(MyMusicEffect.ShowToast("播放列表已创建"))
        }
    }

    private fun handleDeletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            _uiEffect.emit(MyMusicEffect.ShowToast("播放列表已删除"))
        }
    }

    private fun handleShowRenameDialog(playlist: Playlist) {
        _uiState.update { it.copy(playlistToRename = playlist, showRenameDialog = true) }
    }

    private fun handleRenamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, newName)
            _uiState.update { it.copy(showRenameDialog = false, playlistToRename = null) }
            _uiEffect.emit(MyMusicEffect.ShowToast("播放列表已重命名"))
        }
    }

    private fun handleDismissRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = false, playlistToRename = null) }
    }

    private fun handleShowAiCreateDialog() {
        viewModelScope.launch {
            val hasKey = aiRepository.hasApiKey()
            if (hasKey) {
                _uiState.update {
                    it.copy(showAiCreateDialog = true, hasApiKey = true)
                }
            } else {
                _uiEffect.emit(MyMusicEffect.ShowToast("请先配置 API Key"))
            }
        }
    }

    private fun handleDismissAiCreateDialog() {
        _uiState.update {
            it.copy(
                showAiCreateDialog = false,
                aiCreateMode = AiCreateMode.RANDOM,
                aiCreateStep = AiCreateStep.SELECT_MODE,
                selectedSongCount = 10,
                themeInput = "",
                isAiGenerating = false,
                aiGeneratedSongs = emptyList(),
                aiGeneratedPlaylistName = "",
                aiCreateError = null
            )
        }
    }

    private fun handleSetAiCreateMode(mode: AiCreateMode) {
        _uiState.update {
            it.copy(aiCreateMode = mode, aiCreateStep = AiCreateStep.INPUT_DETAILS, aiCreateError = null)
        }
    }

    private fun handleSetSelectedSongCount(count: Int) {
        _uiState.update { it.copy(selectedSongCount = count.coerceIn(1, 100)) }
    }

    private fun handleSetThemeInput(theme: String) {
        _uiState.update { it.copy(themeInput = theme) }
    }

    private fun handleGenerateAiPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true, aiCreateError = null) }

            try {
                val allSongs = songRepository.getAllSongs().first()
                if (allSongs.isEmpty()) {
                    _uiState.update { it.copy(isAiGenerating = false, aiCreateError = "没有可用的歌曲") }
                    return@launch
                }

                val state = _uiState.value
                when (state.aiCreateMode) {
                    AiCreateMode.RANDOM -> {
                        val selectedSongs = allSongs.shuffled().take(state.selectedSongCount)
                        _uiState.update {
                            it.copy(
                                isAiGenerating = false,
                                aiGeneratedSongs = selectedSongs,
                                aiGeneratedPlaylistName = "随机歌单",
                                aiCreateStep = AiCreateStep.PREVIEW
                            )
                        }
                    }
                    AiCreateMode.THEME -> {
                        val theme = state.themeInput.trim()
                        if (theme.isBlank()) {
                            _uiState.update { it.copy(isAiGenerating = false, aiCreateError = "请输入主题") }
                            return@launch
                        }

                        val result = aiRepository.generatePlaylistByTheme(theme, allSongs)
                        result.fold(
                            onSuccess = { songIds ->
                                val selectedSongs = allSongs.filter { it.id in songIds }
                                if (selectedSongs.isEmpty()) {
                                    _uiState.update { it.copy(isAiGenerating = false, aiCreateError = "未找到匹配的歌曲") }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            isAiGenerating = false,
                                            aiGeneratedSongs = selectedSongs,
                                            aiGeneratedPlaylistName = "${theme}歌单",
                                            aiCreateStep = AiCreateStep.PREVIEW
                                        )
                                    }
                                }
                            },
                            onFailure = { error ->
                                Timber.e(error, "generatePlaylistByTheme failed")
                                _uiState.update { it.copy(isAiGenerating = false, aiCreateError = "生成失败: ${error.message}") }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "handleGenerateAiPlaylist failed")
                _uiState.update { it.copy(isAiGenerating = false, aiCreateError = "生成失败: ${e.message}") }
            }
        }
    }

    private fun handleConfirmAiPlaylist() {
        viewModelScope.launch {
            val state = _uiState.value
            val songs = state.aiGeneratedSongs
            if (songs.isEmpty()) {
                _uiState.update { it.copy(aiCreateError = "没有可添加的歌曲") }
                return@launch
            }

            val playlistName = state.aiGeneratedPlaylistName.ifBlank { "AI歌单" }

            try {
                val playlistId = playlistRepository.createPlaylist(playlistName)
                val songIds = songs.map { it.id }
                playlistRepository.addSongsToPlaylist(playlistId, songIds)
                handleDismissAiCreateDialog()
                _uiEffect.emit(MyMusicEffect.ShowToast("歌单已创建"))
            } catch (e: Exception) {
                Timber.e(e, "handleConfirmAiPlaylist failed")
                _uiState.update { it.copy(aiCreateError = "创建歌单失败: ${e.message}") }
            }
        }
    }
}
