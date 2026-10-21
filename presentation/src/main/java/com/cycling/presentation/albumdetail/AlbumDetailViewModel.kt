package com.cycling.presentation.albumdetail

import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.AlbumRepository
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

@HiltViewModel(assistedFactory = AlbumDetailViewModel.Factory::class)
class AlbumDetailViewModel @AssistedInject constructor(
    private val albumRepository: AlbumRepository,
    private val songRepository: SongRepository,
    playlistRepository: PlaylistRepository,
    playerRepository: PlayerRepository,
    @Assisted private val route: LibraryDestination.AlbumDetail
) : CollectionDetailViewModel(playlistRepository, songRepository, playerRepository) {

    private val albumId: Long = route.albumId

    init {
        loadCollection()
        observePlayerState()
    }

    override fun loadCollection() {
        viewModelScope.launch {
            try {
                val album = albumRepository.getAlbumById(albumId)
                _uiState.update {
                    it.copy(
                        title = album?.name,
                        // 头图副标题放歌手名；空名由界面方回落"未知歌手"。
                        subtitle = album?.artist?.takeIf { name -> name.isNotBlank() },
                        artwork = album?.albumArt
                    )
                }

                songRepository.getSongsByAlbum(albumId).collect { songs ->
                    Timber.d("loadAlbum: loaded ${songs.size} songs")
                    _uiState.update { it.copy(songs = songs, isLoading = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // 不兜这一下，isLoading 会停在默认的 true —— 页面永远转圈，没有任何出口。
                Timber.e(error, "loadAlbum: failed for albumId=$albumId")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(CollectionDetailEffect.ShowToast(R.string.collection_vm_load_failed))
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: LibraryDestination.AlbumDetail): AlbumDetailViewModel
    }
}
