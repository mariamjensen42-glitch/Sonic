package com.cycling.presentation.collectiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Song
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.presentation.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 歌曲集合详情页（歌单 / 专辑 / 歌手）的公共 ViewModel 基类。
 *
 * 三个页面结构完全相同，差别只在数据来源与少量特有意图：子类实现 [loadCollection]
 * 灌入实体与曲目即可，多选、批量收藏、随机播放、添加到歌单这套完全一致的逻辑
 * 全部收在这里，由 [handleCollectionIntent] 统一入口分发。
 *
 * 注意：基类 init 不调用 [loadCollection] —— Kotlin 基类构造先于子类字段初始化，
 * 在那里调用 open 方法会读到未初始化的子类状态；由子类在自己的 init 里调用。
 */
abstract class CollectionDetailViewModel(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    protected val _uiState = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _uiState.asStateFlow()

    // protected：子类（歌单页）要发自己的 Toast 类副作用，与基类共用同一条流。
    protected val _uiEffect = MutableSharedFlow<CollectionDetailEffect>()
    val uiEffect: SharedFlow<CollectionDetailEffect> = _uiEffect.asSharedFlow()

    /** 由子类在自己的 init 中调用：把实体与曲目灌进 [_uiState]。 */
    protected abstract fun loadCollection()

    /** 由子类在自己的 init 中调用：跟随播放器高亮当前曲目。 */
    protected fun observePlayerState() {
        viewModelScope.launch {
            playerRepository.playerState.collect { state ->
                _uiState.update { it.copy(currentSongId = state.currentSong?.id) }
            }
        }
    }

    fun handleCollectionIntent(intent: SongCollectionIntent) {
        when (intent) {
            is SongCollectionIntent.SongClick -> onSongClick(intent.song)
            is SongCollectionIntent.ShufflePlay -> shufflePlay()
            is SongCollectionIntent.AddSongsToPlaylist -> showAddToPlaylistDialog()
            is SongCollectionIntent.DismissAddToPlaylistDialog -> dismissAddToPlaylistDialog()
            is SongCollectionIntent.AddSongsToPlaylistTo -> addSongsTo(intent.playlistId)
            is SongCollectionIntent.CreatePlaylistAndAddSongs ->
                createPlaylistAndAddSongs(intent.name)
            is SongCollectionIntent.EnterSelectionMode ->
                _uiState.update { it.copy(selectionMode = true, selectedSongIds = emptySet()) }
            is SongCollectionIntent.ExitSelectionMode ->
                // 退出时一并清空勾选：留着的话下次进来会看到上一轮的选择，而用户以为那是空的。
                _uiState.update { it.copy(selectionMode = false, selectedSongIds = emptySet()) }
            is SongCollectionIntent.ToggleSelection -> _uiState.update { state ->
                val selected = state.selectedSongIds
                state.copy(
                    selectedSongIds = if (intent.songId in selected) {
                        selected - intent.songId
                    } else {
                        selected + intent.songId
                    }
                )
            }
            is SongCollectionIntent.SelectAll ->
                _uiState.update { it.copy(selectedSongIds = it.songs.map { song -> song.id }.toSet()) }
            is SongCollectionIntent.ClearSelection ->
                _uiState.update { it.copy(selectedSongIds = emptySet()) }
            is SongCollectionIntent.BatchSetFavorite -> batchSetFavorite(intent.isFavorite)
        }
    }

    private fun onSongClick(song: Song) {
        viewModelScope.launch {
            _uiEffect.emit(CollectionDetailEffect.NavigateToPlayer(song.id))
        }
    }

    /**
     * 随机播放：整个集合打乱后作为队列下发，首曲取打乱结果的第一首。
     * 真正的"顺序随机"由播放器的随机模式接管，这里只负责换一首起点。
     */
    private fun shufflePlay() {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        viewModelScope.launch {
            _uiEffect.emit(CollectionDetailEffect.PlayShuffled(songs.shuffled()))
        }
    }

    private fun showAddToPlaylistDialog() {
        viewModelScope.launch {
            // 只取一次快照，不需要常驻订阅歌单表。
            val playlists = playlistRepository.getAllPlaylists().first()
            _uiState.update {
                it.copy(showAddToPlaylistDialog = true, playlists = playlists)
            }
        }
    }

    private fun dismissAddToPlaylistDialog() {
        _uiState.update { it.copy(showAddToPlaylistDialog = false) }
    }

    private fun addSongsTo(targetPlaylistId: Long) {
        val songIds = _uiState.value.songs.map { it.id }
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            playlistRepository.addSongsToPlaylist(targetPlaylistId, songIds)
            dismissAddToPlaylistDialog()
            _uiEffect.emit(CollectionDetailEffect.ShowToast(R.string.collection_detail_added_toast))
        }
    }

    private fun createPlaylistAndAddSongs(name: String) {
        val songIds = _uiState.value.songs.map { it.id }
        viewModelScope.launch {
            val newId = playlistRepository.createPlaylist(name)
            if (songIds.isNotEmpty()) {
                playlistRepository.addSongsToPlaylist(newId, songIds)
            }
            dismissAddToPlaylistDialog()
            _uiEffect.emit(CollectionDetailEffect.ShowToast(R.string.collection_detail_added_toast))
        }
    }

    private fun batchSetFavorite(isFavorite: Boolean) {
        val songIds = _uiState.value.selectedSongIds.toList()
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            songRepository.setFavoriteBatch(songIds, isFavorite)
            _uiState.update { it.copy(selectionMode = false, selectedSongIds = emptySet()) }
            _uiEffect.emit(
                CollectionDetailEffect.ShowToast(
                    message = if (isFavorite) {
                        R.string.songs_batch_favorited
                    } else {
                        R.string.songs_batch_unfavorited
                    },
                    arg = songIds.size
                )
            )
        }
    }
}
