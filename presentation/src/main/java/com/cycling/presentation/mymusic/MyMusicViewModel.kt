package com.cycling.presentation.mymusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import com.cycling.domain.online.OnlinePlaylistRepository
import com.cycling.domain.repository.AiRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.ImportOnlinePlaylistOutcome
import com.cycling.domain.usecase.ImportOnlinePlaylistUseCase
import com.cycling.domain.usecase.SongQueryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.cycling.presentation.R
import timber.log.Timber
import javax.inject.Inject

private const val FAVORITES_PLAYLIST_NAME = "我喜欢"
private const val FAVORITES_PLAYLIST_THEME = "收藏的歌曲"

@HiltViewModel
class MyMusicViewModel @Inject constructor(
    private val songQueryUseCase: SongQueryUseCase,
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository,
    private val playlistRepository: PlaylistRepository,
    private val aiRepository: AiRepository,
    private val onlinePlaylistRepository: OnlinePlaylistRepository,
    private val importOnlinePlaylistUseCase: ImportOnlinePlaylistUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyMusicUiState())
    val uiState: StateFlow<MyMusicUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<MyMusicEffect>()
    val uiEffect: SharedFlow<MyMusicEffect> = _uiEffect.asSharedFlow()

    private var favoriteDescriptionJob: Job? = null
    private var requestedFavoriteSongsKey: String? = null

    init {
        loadData()
    }

    fun handleIntent(intent: MyMusicIntent) {
        when (intent) {
            is MyMusicIntent.FavoriteSongsClick -> handleFavoriteSongsClick()
            is MyMusicIntent.BackToPlaylists -> handleBackToPlaylists()
            is MyMusicIntent.ToggleFavoriteDescription -> {
                _uiState.update { it.copy(isFavoriteDescriptionExpanded = !it.isFavoriteDescriptionExpanded) }
            }
            is MyMusicIntent.ShuffleFavoriteSongs -> handleShuffleFavoriteSongs()
            is MyMusicIntent.SongClick -> handleSongClick(intent.song)
            is MyMusicIntent.ToggleFavorite -> toggleFavorite(intent.song)
            is MyMusicIntent.PlaylistClick -> handlePlaylistClick(intent.playlist)
            is MyMusicIntent.CreatePlaylist -> handleCreatePlaylist(intent.name)
            is MyMusicIntent.DeletePlaylist -> handleDeletePlaylist(intent.playlist)
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
            is MyMusicIntent.SetImportInput -> {
                _uiState.update { it.copy(importInput = intent.text, importError = null) }
            }
            is MyMusicIntent.ImportOnlinePlaylist -> handleImportOnlinePlaylist()
            is MyMusicIntent.LoadMyPlaylists -> loadMyPlaylists()
            is MyMusicIntent.ImportMyPlaylist -> handleImportMyPlaylist(intent.playlistId)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            launch {
                songQueryUseCase.favorites().collectLatest { songs ->
                    _uiState.update { it.copy(favoriteSongs = songs) }
                    if (_uiState.value.showFavoriteSongs) {
                        loadFavoritePlaylistDescription(songs)
                    }
                }
            }

            launch {
                // isLoading 是由这个 Flow 负责清掉的，所以失败也必须在这里清，
                // 否则整个"我的音乐"会永远停在载入态。
                playlistRepository.getAllPlaylists()
                    .catch { error ->
                        Timber.e(error, "loadData: playlists failed")
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEffect.emit(MyMusicEffect.ShowToast(R.string.mymusic_vm_load_failed))
                    }
                    .collectLatest { playlists ->
                        _uiState.update { it.copy(playlists = playlists, isLoading = false) }
                    }
            }

            launch {
                playerRepository.playerState.collectLatest { playerState ->
                    _uiState.update { it.copy(currentSongId = playerState.currentSong?.id) }
                }
            }
        }
    }

    private fun handleFavoriteSongsClick() {
        _uiState.update { it.copy(showFavoriteSongs = true) }
        loadFavoritePlaylistDescription(_uiState.value.favoriteSongs)
    }

    private fun handleBackToPlaylists() {
        favoriteDescriptionJob?.cancel()
        favoriteDescriptionJob = null
        requestedFavoriteSongsKey = null
        _uiState.update {
            it.copy(
                showFavoriteSongs = false,
                isFavoriteDescriptionGenerating = false
            )
        }
    }

    private fun loadFavoritePlaylistDescription(songs: List<Song>) {
        if (songs.isEmpty()) {
            favoriteDescriptionJob?.cancel()
            favoriteDescriptionJob = null
            requestedFavoriteSongsKey = null
            _uiState.update {
                it.copy(
                    favoriteDescription = "",
                    isFavoriteDescriptionExpanded = false,
                    isFavoriteDescriptionGenerating = false
                )
            }
            return
        }

        val requestKey = songs.joinToString(separator = "|") { "${it.id}:${it.title}:${it.artist}" }
        if (requestKey == requestedFavoriteSongsKey) return

        favoriteDescriptionJob?.cancel()
        requestedFavoriteSongsKey = requestKey
        _uiState.update {
            it.copy(
                favoriteDescription = "",
                isFavoriteDescriptionExpanded = false,
                isFavoriteDescriptionGenerating = true
            )
        }

        favoriteDescriptionJob = viewModelScope.launch {
            val cachedDescription = try {
                aiRepository.getCachedFavoritePlaylistDescription(songs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "loadFavoritePlaylistDescription cache lookup failed")
                null
            }

            if (!isCurrentFavoriteDescriptionRequest(requestKey)) return@launch
            if (!cachedDescription.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        favoriteDescription = cachedDescription,
                        isFavoriteDescriptionGenerating = false
                    )
                }
                return@launch
            }

            val generationResult = try {
                aiRepository.generatePlaylistDescription(
                    playlistName = FAVORITES_PLAYLIST_NAME,
                    theme = FAVORITES_PLAYLIST_THEME,
                    songs = songs
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }

            generationResult.onSuccess { description ->
                val cleanDescription = description.trim()
                if (!isCurrentFavoriteDescriptionRequest(requestKey)) return@onSuccess
                if (cleanDescription.isBlank()) {
                    requestedFavoriteSongsKey = null
                    _uiState.update { it.copy(isFavoriteDescriptionGenerating = false) }
                    _uiEffect.emit(MyMusicEffect.ShowToast(R.string.playlist_detail_desc_failed))
                    return@onSuccess
                }

                try {
                    aiRepository.cacheFavoritePlaylistDescription(songs, cleanDescription)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.e(error, "cacheFavoritePlaylistDescription failed")
                }
                _uiState.update {
                    it.copy(
                        favoriteDescription = cleanDescription,
                        isFavoriteDescriptionExpanded = true,
                        isFavoriteDescriptionGenerating = false
                    )
                }
            }.onFailure { error ->
                Timber.e(error, "generate favorite playlist description failed")
                if (isCurrentFavoriteDescriptionRequest(requestKey)) {
                    requestedFavoriteSongsKey = null
                    _uiState.update { it.copy(isFavoriteDescriptionGenerating = false) }
                    _uiEffect.emit(MyMusicEffect.ShowToast(R.string.playlist_detail_desc_failed))
                }
            }
        }
    }

    private fun currentFavoriteSongsKey(): String =
        _uiState.value.favoriteSongs.joinToString(separator = "|") { "${it.id}:${it.title}:${it.artist}" }

    private fun isCurrentFavoriteDescriptionRequest(requestKey: String): Boolean =
        _uiState.value.showFavoriteSongs && requestKey == currentFavoriteSongsKey()

    private fun handleShuffleFavoriteSongs() {
        val shuffledSongs = _uiState.value.favoriteSongs.shuffled()
        val firstSong = shuffledSongs.firstOrNull() ?: return
        viewModelScope.launch {
            playerRepository.playSong(firstSong, shuffledSongs)
            _uiEffect.emit(MyMusicEffect.NavigateToPlayer(firstSong.id))
        }
    }

    private fun handleSongClick(song: Song) {
        viewModelScope.launch {
            playerRepository.playSong(song, _uiState.value.favoriteSongs)
            _uiEffect.emit(MyMusicEffect.NavigateToPlayer(song.id))
        }
    }

    private fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            songRepository.toggleFavorite(song.id)
            _uiEffect.emit(MyMusicEffect.ShowToast(R.string.mymusic_vm_removed_from_favorites))
        }
    }

    private fun handlePlaylistClick(playlist: Playlist) {
        viewModelScope.launch {
            _uiEffect.emit(MyMusicEffect.NavigateToPlaylistDetail(playlist.id))
        }
    }

    private fun handleCreatePlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
            _uiEffect.emit(MyMusicEffect.ShowToast(R.string.mymusic_vm_playlist_created))
        }
    }

    /**
     * 从分享链接导入在线歌单。
     *
     * 抓取与写入都在用例里，这里只管三件事：把进度推到状态上、成功就跳详情页、失败给一条
     * 能看懂的原因（链接不对 / 歌单读不到 / 网络挂了 —— 三者对用户来说下一步动作完全不同）。
     */
    private fun handleImportOnlinePlaylist() {
        val input = _uiState.value.importInput.trim()
        if (input.isEmpty() || _uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isImporting = true, importError = null, importDone = 0, importTotal = 0)
            }
            val outcome = try {
                importOnlinePlaylistUseCase(input) { done, total ->
                    _uiState.update { it.copy(importDone = done, importTotal = total) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "handleImportOnlinePlaylist: failed")
                _uiState.update {
                    it.copy(isImporting = false, importError = R.string.mymusic_import_error_network)
                }
                return@launch
            }
            applyImportOutcome(outcome)
        }
    }

    /**
     * 拉「我的歌单」。
     *
     * 只拉一次：列表变化不频繁，而每开一次对话框就打一次接口既慢又没必要。失败也标记成
     * 「已加载」—— 空列表和读失败在界面上是同一种表现（不显示这一块），重试没有意义。
     */
    private fun loadMyPlaylists() {
        val state = _uiState.value
        if (state.myPlaylistsLoaded || state.isLoadingMyPlaylists) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMyPlaylists = true) }
            val playlists = try {
                onlinePlaylistRepository.fetchMyPlaylists()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "loadMyPlaylists: failed")
                emptyList()
            }
            _uiState.update {
                it.copy(
                    myPlaylists = playlists,
                    myPlaylistsLoaded = true,
                    isLoadingMyPlaylists = false
                )
            }
        }
    }

    /** 导入列表里点中的那一张：取数走 id，落库与链接导入完全一致。 */
    private fun handleImportMyPlaylist(playlistId: String) {
        if (playlistId.isBlank() || _uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isImporting = true, importError = null, importDone = 0, importTotal = 0)
            }
            val outcome = try {
                importOnlinePlaylistUseCase.importById(playlistId) { done, total ->
                    _uiState.update { it.copy(importDone = done, importTotal = total) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "handleImportMyPlaylist: failed")
                _uiState.update {
                    it.copy(isImporting = false, importError = R.string.mymusic_import_error_network)
                }
                return@launch
            }
            applyImportOutcome(outcome)
        }
    }

    /** 两种导入入口（链接 / 我的歌单）共用的收尾：进度、跳转、失败原因都在这里定。 */
    private suspend fun applyImportOutcome(outcome: ImportOnlinePlaylistOutcome) {
        when (outcome) {
            is ImportOnlinePlaylistOutcome.Imported -> {
                Timber.d("import: ${outcome.playlistName} / ${outcome.imported}")
                _uiState.update { it.copy(isImporting = false, importInput = "") }
                _uiEffect.emit(MyMusicEffect.NavigateToPlaylistDetail(outcome.playlistId))
            }

            ImportOnlinePlaylistOutcome.InvalidLink -> _uiState.update {
                it.copy(isImporting = false, importError = R.string.mymusic_import_error_link)
            }

            ImportOnlinePlaylistOutcome.NotAccessible -> _uiState.update {
                it.copy(isImporting = false, importError = R.string.mymusic_import_error_unavailable)
            }

            is ImportOnlinePlaylistOutcome.Failed -> _uiState.update {
                it.copy(isImporting = false, importError = R.string.mymusic_import_error_network)
            }
        }
    }

    private fun handleDeletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist.id)
            _uiEffect.emit(MyMusicEffect.ShowToast(R.string.mymusic_vm_playlist_deleted))
        }
    }

    private fun handleShowRenameDialog(playlist: Playlist) {
        _uiState.update { it.copy(playlistToRename = playlist, showRenameDialog = true) }
    }

    private fun handleRenamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, newName)
            _uiState.update { it.copy(showRenameDialog = false, playlistToRename = null) }
            _uiEffect.emit(MyMusicEffect.ShowToast(R.string.mymusic_vm_playlist_renamed))
        }
    }

    private fun handleDismissRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = false, playlistToRename = null) }
    }

    // ── AI Playlist Creation ──

    private fun handleShowAiCreateDialog() {
        _uiState.update { it.copy(showAiCreateDialog = true, hasApiKey = true) }
    }

    private fun handleDismissAiCreateDialog() {
        _uiState.update {
            it.copy(
                showAiCreateDialog = false,
                aiCreateMode = AiCreateMode.RANDOM,
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
        _uiState.update { it.copy(aiCreateMode = mode, aiCreateError = null) }
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
                    _uiState.update { it.copy(isAiGenerating = false, aiCreateError = R.string.mymusic_vm_no_songs_available) }
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
                                aiGeneratedPlaylistName = ""
                            )
                        }
                    }
                    AiCreateMode.THEME -> {
                        val theme = state.themeInput.trim()
                        if (theme.isBlank()) {
                            _uiState.update { it.copy(isAiGenerating = false, aiCreateError = R.string.mymusic_vm_enter_theme) }
                            return@launch
                        }

                        val result = aiRepository.generatePlaylistByTheme(theme, allSongs, state.selectedSongCount)
                        result.fold(
                            onSuccess = { songIds ->
                                val selectedSongs = allSongs.filter { it.id in songIds }
                                if (selectedSongs.isEmpty()) {
                                    _uiState.update { it.copy(isAiGenerating = false, aiCreateError = R.string.mymusic_vm_no_matching_songs) }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                        isAiGenerating = false,
                                        aiGeneratedSongs = selectedSongs,
                                        aiGeneratedPlaylistName = "${theme}歌单"
                                        )
                                    }
                                }
                            },
                            onFailure = { error ->
                                Timber.e(error, "generatePlaylistByTheme failed")
                                _uiState.update { it.copy(isAiGenerating = false, aiCreateError = R.string.mymusic_vm_generate_failed) }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "handleGenerateAiPlaylist failed")
                _uiState.update { it.copy(isAiGenerating = false, aiCreateError = R.string.mymusic_vm_generate_failed) }
            }
        }
    }

    private fun handleConfirmAiPlaylist() {
        viewModelScope.launch {
            val state = _uiState.value
            val songs = state.aiGeneratedSongs
            if (songs.isEmpty()) {
                _uiState.update { it.copy(aiCreateError = R.string.mymusic_vm_no_songs_to_add) }
                return@launch
            }

            val playlistName = state.aiGeneratedPlaylistName.ifBlank { "AI歌单" }

            try {
                // 介绍据已选定的曲目生成，所以放在选曲之后、建单之前。
                // 生成失败不阻断建单 —— 歌单本身比介绍重要，介绍留空即可。
                val description = aiRepository
                    .generatePlaylistDescription(
                        playlistName = playlistName,
                        theme = state.themeInput.ifBlank { playlistName },
                        songs = songs
                    )
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }

                val playlistId = playlistRepository.createPlaylist(playlistName, description)
                val songIds = songs.map { it.id }
                playlistRepository.addSongsToPlaylist(playlistId, songIds)
                handleDismissAiCreateDialog()
                _uiEffect.emit(MyMusicEffect.ShowToast(R.string.mymusic_vm_playlist_created_success))
            } catch (e: Exception) {
                Timber.e(e, "handleConfirmAiPlaylist failed")
                _uiState.update { it.copy(aiCreateError = R.string.mymusic_vm_create_failed) }
            }
        }
    }
}
