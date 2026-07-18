package com.cycling.presentation.artistdetail

import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

data class ArtistDetailUiState(
    val artist: Artist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface ArtistDetailIntent {
    data class LoadArtist(val artistId: Long) : ArtistDetailIntent
    data class SongClick(val song: Song) : ArtistDetailIntent
}

sealed interface ArtistDetailEffect {
    data class NavigateToPlayer(val songId: Long) : ArtistDetailEffect
}
