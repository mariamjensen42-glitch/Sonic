package com.cycling.presentation.playlistdetail

import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface PlaylistDetailIntent {
    data class SongClick(val song: Song) : PlaylistDetailIntent
    data class RemoveSong(val songId: Long) : PlaylistDetailIntent
}

sealed interface PlaylistDetailEffect {
    data class NavigateToPlayer(val songId: Long) : PlaylistDetailEffect
    data class ShowToast(val message: String) : PlaylistDetailEffect
}
