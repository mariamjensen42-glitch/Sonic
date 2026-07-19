package com.cycling.presentation.artistdetail

import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

data class ArtistDetailUiState(
    val artist: Artist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val currentSongId: Long? = null,
    val isPlaying: Boolean = false,
    val likedSongIds: Set<Long> = emptySet()
)

sealed interface ArtistDetailIntent {
    data class LoadArtist(val artistId: Long) : ArtistDetailIntent
    data class SongClick(val song: Song) : ArtistDetailIntent
    data class ToggleFavorite(val songId: Long) : ArtistDetailIntent
}

sealed interface ArtistDetailEffect {
    data class NavigateToPlayer(val songId: Long) : ArtistDetailEffect
}
