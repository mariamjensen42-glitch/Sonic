package com.cycling.presentation.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Song
import com.cycling.domain.model.SortOrder
import com.cycling.domain.model.ViewMode
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.repository.SongsPreferencesRepository
import com.cycling.domain.usecase.ScanMusicUseCase
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val songsPreferencesRepository: SongsPreferencesRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerRepository: PlayerRepository,
    private val scanMusicUseCase: ScanMusicUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongsUiState())
    val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SongsEffect>()
    val uiEffect: SharedFlow<SongsEffect> = _uiEffect.asSharedFlow()

    init {
        loadPreferences()
        loadSongs()
        loadPlaylists()
        observePlayerState()
        observeScanProgress()
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            playerRepository.playerState.collect { state ->
                _uiState.update { it.copy(currentSongId = state.currentSong?.id) }
            }
        }
    }

    fun handleIntent(intent: SongsIntent) {
        when (intent) {
            is SongsIntent.LoadSongs -> loadSongs()
            is SongsIntent.ScanLibrary -> scanLibrary()
            is SongsIntent.ToggleViewMode -> toggleViewMode()
            is SongsIntent.ChangeSortOrder -> changeSortOrder(intent.sortOrder)
            is SongsIntent.SongClick -> onSongClick(intent.song)
            is SongsIntent.ShowAddToPlaylistDialog -> showAddToPlaylistDialog(intent.song)
            is SongsIntent.AddToPlaylist -> addToPlaylist(intent.playlistId)
            is SongsIntent.DismissAddToPlaylistDialog -> dismissAddToPlaylistDialog()
            is SongsIntent.CreatePlaylistAndAddSong -> createPlaylistAndAddSong(intent.name)
            is SongsIntent.ToggleFavorite -> toggleFavorite(intent.song)
            is SongsIntent.EnterSelectionMode -> enterSelectionMode()
            is SongsIntent.ExitSelectionMode -> exitSelectionMode()
            is SongsIntent.ToggleSelection -> toggleSelection(intent.songId)
            is SongsIntent.SelectAll -> selectAll()
            is SongsIntent.ClearSelection -> _uiState.update { it.copy(selectedSongIds = emptySet()) }
            is SongsIntent.ShowBatchAddToPlaylistDialog -> showBatchAddToPlaylistDialog()
            is SongsIntent.DismissBatchAddToPlaylistDialog ->
                _uiState.update { it.copy(showBatchAddToPlaylistDialog = false) }
            is SongsIntent.BatchAddToPlaylist -> batchAddToPlaylist(intent.playlistId)
            is SongsIntent.CreatePlaylistAndAddSelected -> createPlaylistAndAddSelected(intent.name)
            is SongsIntent.BatchSetFavorite -> batchSetFavorite(intent.isFavorite)
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val viewMode = songsPreferencesRepository.getViewMode()
            val sortOrder = songsPreferencesRepository.getSortOrder()
            val sortAscending = songsPreferencesRepository.getSortAscending()
            _uiState.update { it.copy(viewMode = viewMode, sortOrder = sortOrder, sortAscending = sortAscending) }
        }
    }

    private fun loadSongs() {
        Timber.d("loadSongs: starting")
        viewModelScope.launch {
            songRepository.getAllSongs()
                .catch { error ->
                    // 少了这一段，这一页会永远停在载入态：Flow 挂了就再没人把 isLoading 置回去。
                    Timber.e(error, "loadSongs: failed")
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(SongsEffect.ShowToast(R.string.songs_vm_load_failed))
                }
                .collect { songs ->
                    Timber.d("loadSongs: loaded ${songs.size} songs")
                    _uiState.update { it.copy(songs = songs, isLoading = false) }
                }
        }
    }

    /**
     * 扫描状态取自扫描器本身而不是本地标志位：首次启动那次自动扫描是在这个界面之外发起的，
     * 用本地标志位的话，用户在扫描进行中切到歌曲页，看到的会是一个空列表加一个可点的按钮。
     */
    private fun observeScanProgress() {
        scanMusicUseCase.progress()
            .onEach { progress ->
                _uiState.update { it.copy(isScanning = progress.isScanning) }
            }
            .launchIn(viewModelScope)
    }

    private fun scanLibrary() {
        viewModelScope.launch {
            scanMusicUseCase.scan()
                .onFailure { _uiEffect.emit(SongsEffect.ShowToast(R.string.scan_failed)) }
            // 列表本身是 Room 的 Flow，扫描写入之后会自己更新，这里不用再查一遍
        }
    }

    private fun toggleViewMode() {
        viewModelScope.launch {
            val newViewMode = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            songsPreferencesRepository.saveViewMode(newViewMode)
            _uiState.update { it.copy(viewMode = newViewMode) }
        }
    }

    private fun changeSortOrder(newSortOrder: SortOrder) {
        viewModelScope.launch {
            val currentSortOrder = _uiState.value.sortOrder
            val newSortAscending = if (currentSortOrder == newSortOrder) {
                !_uiState.value.sortAscending
            } else {
                true
            }
            songsPreferencesRepository.saveSortOrder(newSortOrder)
            songsPreferencesRepository.saveSortAscending(newSortAscending)
            _uiState.update { it.copy(sortOrder = newSortOrder, sortAscending = newSortAscending) }
        }
    }

    private fun onSongClick(song: Song) {
        viewModelScope.launch {
            _uiEffect.emit(SongsEffect.NavigateToPlayer(song.id))
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun showAddToPlaylistDialog(song: Song) {
        _uiState.update { it.copy(songToAdd = song, showAddToPlaylistDialog = true) }
    }

    private fun dismissAddToPlaylistDialog() {
        _uiState.update { it.copy(songToAdd = null, showAddToPlaylistDialog = false) }
    }

    private fun addToPlaylist(playlistId: Long) {
        val song = _uiState.value.songToAdd ?: return
        viewModelScope.launch {
            val added = playlistRepository.addSongToPlaylist(playlistId, song.id)
            _uiEffect.emit(
                SongsEffect.ShowToast(
                    if (added > 0) R.string.playlist_added_to else R.string.playlist_song_already_added
                )
            )
            dismissAddToPlaylistDialog()
        }
    }

    /** 行菜单里的收藏：直接按 song.id 改库，和播放器当前放的是哪首无关。 */
    private fun toggleFavorite(song: Song) {
        viewModelScope.launch { songRepository.toggleFavorite(song.id) }
    }

    private fun createPlaylistAndAddSong(name: String) {
        val song = _uiState.value.songToAdd ?: return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addSongToPlaylist(playlistId, song.id)
            _uiEffect.emit(SongsEffect.ShowToast(R.string.playlist_created_and_added))
            dismissAddToPlaylistDialog()
        }
    }

    // region 多选

    private fun enterSelectionMode() {
        _uiState.update { it.copy(selectionMode = true, selectedSongIds = emptySet()) }
    }

    /**
     * 退出多选时把已勾选项一起清掉：留着的话，下次进多选会看到上一轮的选择，
     * 而用户以为那是空的。
     */
    private fun exitSelectionMode() {
        _uiState.update {
            it.copy(
                selectionMode = false,
                selectedSongIds = emptySet(),
                showBatchAddToPlaylistDialog = false
            )
        }
    }

    private fun toggleSelection(songId: Long) {
        _uiState.update { state ->
            val selected = state.selectedSongIds
            state.copy(
                selectedSongIds = if (songId in selected) selected - songId else selected + songId
            )
        }
    }

    /** 全选当前列表里的每一首。列表被排序/筛选过也仍是同一批 id，不需要再拿界面上的顺序。 */
    private fun selectAll() {
        _uiState.update { it.copy(selectedSongIds = it.songs.map { song -> song.id }.toSet()) }
    }

    private fun showBatchAddToPlaylistDialog() {
        if (_uiState.value.selectedSongIds.isEmpty()) return
        _uiState.update { it.copy(showBatchAddToPlaylistDialog = true) }
    }

    private fun batchAddToPlaylist(playlistId: Long) {
        val songIds = _uiState.value.selectedSongIds.toList()
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            // 报真正写进去的数量：勾了 10 首、其中 3 首本来就在这张单里，只会写进 7 行。
            val added = playlistRepository.addSongsToPlaylist(playlistId, songIds)
            _uiState.update { it.copy(showBatchAddToPlaylistDialog = false) }
            _uiEffect.emit(
                if (added > 0) {
                    SongsEffect.ShowToast(R.string.songs_batch_added_to_playlist, added)
                } else {
                    SongsEffect.ShowToast(R.string.playlist_song_already_added)
                }
            )
            exitSelectionMode()
        }
    }

    private fun createPlaylistAndAddSelected(name: String) {
        val songIds = _uiState.value.selectedSongIds.toList()
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            val added = if (songIds.isEmpty()) {
                0
            } else {
                playlistRepository.addSongsToPlaylist(playlistId, songIds)
            }
            _uiState.update { it.copy(showBatchAddToPlaylistDialog = false) }
            _uiEffect.emit(
                if (added > 0) {
                    SongsEffect.ShowToast(R.string.songs_batch_added_to_playlist, added)
                } else {
                    SongsEffect.ShowToast(R.string.playlist_created_and_added)
                }
            )
            exitSelectionMode()
        }
    }

    private fun batchSetFavorite(isFavorite: Boolean) {
        val songIds = _uiState.value.selectedSongIds.toList()
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            songRepository.setFavoriteBatch(songIds, isFavorite)
            _uiEffect.emit(
                SongsEffect.ShowToast(
                    message = if (isFavorite) {
                        R.string.songs_batch_favorited
                    } else {
                        R.string.songs_batch_unfavorited
                    },
                    arg = songIds.size
                )
            )
            exitSelectionMode()
        }
    }

    // endregion
}
