package com.cycling.presentation.albumdetail

import com.cycling.domain.model.Album
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

data class AlbumDetailUiState(
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface AlbumDetailIntent {
    data class LoadAlbum(val albumId: Long) : AlbumDetailIntent
    data class SongClick(val song: Song) : AlbumDetailIntent
}

sealed interface AlbumDetailEffect {
    data class NavigateToPlayer(val songId: Long) : AlbumDetailEffect
}
