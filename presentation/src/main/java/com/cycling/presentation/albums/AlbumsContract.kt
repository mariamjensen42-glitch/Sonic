package com.cycling.presentation.albums

import com.cycling.domain.model.Album

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface AlbumsIntent {
    data class AlbumClick(val album: Album) : AlbumsIntent
}

sealed interface AlbumsEffect {
    data class NavigateToAlbumDetail(val albumId: Long) : AlbumsEffect
}
