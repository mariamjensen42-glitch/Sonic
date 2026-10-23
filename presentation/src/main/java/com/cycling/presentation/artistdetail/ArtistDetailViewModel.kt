package com.cycling.presentation.artistdetail

import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.AlbumRepository
import com.cycling.domain.repository.ArtistRepository
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

@HiltViewModel(assistedFactory = ArtistDetailViewModel.Factory::class)
class ArtistDetailViewModel @AssistedInject constructor(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val songRepository: SongRepository,
    playlistRepository: PlaylistRepository,
    playerRepository: PlayerRepository,
    @Assisted private val route: LibraryDestination.ArtistDetail
) : CollectionDetailViewModel(playlistRepository, songRepository, playerRepository) {

    private val artistId: Long = route.artistId

    init {
        loadCollection()
        observePlayerState()
    }

    fun handleIntent(intent: ArtistDetailIntent) {
        when (intent) {
            is ArtistDetailIntent.ToggleFavorite -> toggleFavorite(intent.songId)
        }
    }

    override fun loadCollection() {
        viewModelScope.launch {
            try {
                val artist = artistRepository.getArtistById(artistId)
                _uiState.update {
                    it.copy(title = artist?.name, artwork = artist?.artistArt)
                }

                // 歌手页页尾的「专辑」横滑：这条查询独立于曲目表，失败不应拖垮整页。
                launch {
                    albumRepository.getAlbumsByArtist(artistId).collect { albums ->
                        _uiState.update { it.copy(albums = albums) }
                    }
                }

                songRepository.getSongsByArtist(artistId).collect { songs ->
                    Timber.d("loadArtist: loaded ${songs.size} songs")
                    val sortedSongs = songs.sortedBy { it.title }
                    _uiState.update {
                        it.copy(
                            songs = sortedSongs,
                            isLoading = false,
                            likedSongIds = sortedSongs
                                .filter { song -> song.isFavorite }
                                .map { song -> song.id }
                                .toSet()
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // 不兜这一下，isLoading 会停在默认的 true —— 页面永远转圈，没有任何出口。
                Timber.e(error, "loadArtist: failed for artistId=$artistId")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(CollectionDetailEffect.ShowToast(R.string.collection_vm_load_failed))
            }
        }
    }

    private fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val newState = songRepository.toggleFavorite(songId)
            _uiState.update { state ->
                val liked = state.likedSongIds
                state.copy(
                    likedSongIds = if (newState) liked + songId else liked - songId
                )
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: LibraryDestination.ArtistDetail): ArtistDetailViewModel
    }
}
