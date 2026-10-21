package com.cycling.presentation.playlistdetail

import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.AiRepository
import com.cycling.domain.repository.LibraryTransferRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.presentation.R
import com.cycling.presentation.collectiondetail.CollectionDetailEffect
import com.cycling.presentation.collectiondetail.CollectionDetailViewModel
import com.cycling.presentation.navigation.LibraryDestination
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel(assistedFactory = PlaylistDetailViewModel.Factory::class)
class PlaylistDetailViewModel @AssistedInject constructor(
    private val playlistRepository: PlaylistRepository,
    songRepository: SongRepository,
    playerRepository: PlayerRepository,
    private val aiRepository: AiRepository,
    private val libraryTransferRepository: LibraryTransferRepository,
    @Assisted private val route: LibraryDestination.PlaylistDetail
) : CollectionDetailViewModel(playlistRepository, songRepository, playerRepository) {

    private val playlistId: Long = route.playlistId

    /**
     * 是否已发起过介绍生成。
     *
     * 曲目 Flow 会随增删反复发射，不能靠"当前有没有 description"判断 ——
     * 那样会在每次移除歌曲时重复触发生成。
     */
    private var descriptionRequested = false

    init {
        loadCollection()
        observePlayerState()
    }

    fun handleIntent(intent: PlaylistDetailIntent) {
        when (intent) {
            is PlaylistDetailIntent.RemoveSong -> removeSong(intent.songId)
            is PlaylistDetailIntent.RemoveSelectedSongs -> removeSelectedSongs()
            is PlaylistDetailIntent.MoveSong -> moveSong(intent.fromIndex, intent.toIndex)
            is PlaylistDetailIntent.ExportM3u -> exportM3u(intent.targetUri)
            is PlaylistDetailIntent.ToggleDescription ->
                _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
            is PlaylistDetailIntent.GenerateDescription -> generateDescription()
        }
    }

    override fun loadCollection() {
        viewModelScope.launch {
            // 歌单本体必须是 Flow：介绍生成后会回写 playlists 表，
            // 一次性查询会让 UI 一直拿着写入前的快照，只能靠重新进页面才刷新。
            playlistRepository.observePlaylistById(playlistId).collect { playlist ->
                _uiState.update {
                    it.copy(
                        title = playlist?.name,
                        description = playlist?.description
                    )
                }
                maybeGenerateDescription()
            }
        }

        viewModelScope.launch {
            try {
                playlistRepository.getSongsInPlaylist(playlistId).collect { songs ->
                    Timber.d("loadPlaylist: loaded ${songs.size} songs")
                    // 配色由主题层按当前封面统一派生，这里不再单独取色。
                    _uiState.update {
                        it.copy(
                            songs = songs,
                            isLoading = false,
                            // 歌单封面取第一首曲目的封面，与旧版界面行为一致。
                            artwork = songs.firstOrNull()?.albumArt
                        )
                    }
                    maybeGenerateDescription()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // 不兜这一下，isLoading 会停在默认的 true —— 页面永远转圈，没有任何出口。
                Timber.e(error, "loadPlaylist: failed for playlistId=$playlistId")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(CollectionDetailEffect.ShowToast(R.string.collection_vm_load_failed))
            }
        }
    }

    /**
     * 已有介绍就没什么可做的；只有在"名字已到位、曲目已到位、尚未发起过"时才补触发生成，
     * 兜住两个 Flow 的先后竞速。
     */
    private fun maybeGenerateDescription() {
        val state = _uiState.value
        val hasDescription = !state.description.isNullOrBlank()
        val ready = !state.title.isNullOrBlank() &&
            state.songs.isNotEmpty() &&
            !hasDescription &&
            !descriptionRequested
        if (ready) {
            descriptionRequested = true
            generateDescription()
        }
    }

    private fun removeSong(songId: Long) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
            _uiEffect.emit(CollectionDetailEffect.ShowToast(R.string.playlist_detail_removed_toast))
        }
    }

    /**
     * 拖拽排序。界面只给最终位置，中间过程不写库 —— 否则一次拖拽会写几十遍。
     * 列表本身是 `getSongsInPlaylist` 的 Flow 驱动的，写完自动按新 position 重排。
     */
    private fun moveSong(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            playlistRepository.moveSong(playlistId, fromIndex, toIndex)
        }
    }

    /** 异步写文件；SAF 的 uri 在写之前一直有效，整段放到 IO 里做。 */
    private fun exportM3u(targetUri: String) {
        viewModelScope.launch {
            runCatching { libraryTransferRepository.exportPlaylistM3u(playlistId, targetUri) }
                .onSuccess { count ->
                    _uiEffect.emit(CollectionDetailEffect.ShowToast(R.string.playlist_detail_exported, count))
                }
                .onFailure { error ->
                    Timber.e(error, "exportM3u failed")
                    _uiEffect.emit(CollectionDetailEffect.ShowToast(R.string.playlist_detail_export_failed))
                }
        }
    }

    private fun removeSelectedSongs() {
        val songIds = _uiState.value.selectedSongIds.toList()
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            playlistRepository.removeSongsFromPlaylist(playlistId, songIds)
            _uiState.update { it.copy(selectionMode = false, selectedSongIds = emptySet()) }
            _uiEffect.emit(
                CollectionDetailEffect.ShowToast(
                    message = R.string.playlist_detail_batch_removed,
                    arg = songIds.size
                )
            )
        }
    }

    /**
     * 生成介绍并落库。存量歌单没有介绍时才走这里 —— AI 创建的歌单一并写好了。
     * 失败只提示，不重试：这一轮没生成出来，下次进页面还会再试。
     */
    private fun generateDescription() {
        if (_uiState.value.isDescriptionGenerating) return
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        // 歌单 Flow 可能还没首帧到达，此时没有名字可用；这种情况下先不生成，
        // 等 observePlaylistById 首次发射后由 maybeGenerateDescription 补触发。
        val name = _uiState.value.title
        if (name.isNullOrBlank()) {
            descriptionRequested = false
            return
        }

        _uiState.update { it.copy(isDescriptionGenerating = true) }
        viewModelScope.launch {
            aiRepository.generatePlaylistDescription(name, name, songs)
                .onSuccess { description ->
                    playlistRepository.updatePlaylistDescription(playlistId, description)
                    _uiState.update {
                        it.copy(
                            isDescriptionGenerating = false,
                            isDescriptionExpanded = true
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "generateDescription failed")
                    _uiState.update { it.copy(isDescriptionGenerating = false) }
                    _uiEffect.emit(
                        CollectionDetailEffect.ShowToast(R.string.playlist_detail_desc_failed)
                    )
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: LibraryDestination.PlaylistDetail): PlaylistDetailViewModel
    }
}
