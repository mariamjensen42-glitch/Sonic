package com.cycling.presentation.home

import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song

data class HomeUiState(
    val isLoading: Boolean = false,
    val recentlyPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val topAlbums: List<Album> = emptyList(),
    val topArtists: List<Artist> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val error: String? = null
)

sealed interface HomeIntent {
    data object LoadData : HomeIntent
    data class SongClick(val song: Song) : HomeIntent
    data class AlbumClick(val album: Album) : HomeIntent
    data class ArtistClick(val artist: Artist) : HomeIntent
    data object NavigateToSongs : HomeIntent
    data object NavigateToAlbums : HomeIntent
    data object NavigateToArtists : HomeIntent
    data object NavigateToPlaylists : HomeIntent
    data object NavigateToSettings : HomeIntent
    data object NavigateToMyMusic : HomeIntent
    data object NavigateToSearch : HomeIntent
    data object NavigateToFolders : HomeIntent
    data object PlayPause : HomeIntent
    data object MiniPlayerClick : HomeIntent
}

sealed interface HomeEffect {
    data class ShowToast(val message: String) : HomeEffect
    data class NavigateToAlbumDetail(val albumId: Long) : HomeEffect
    data class NavigateToArtistDetail(val artistId: Long) : HomeEffect
    data class NavigateToPlayer(val songId: Long) : HomeEffect
    data object NavigateToSongs : HomeEffect
    data object NavigateToAlbums : HomeEffect
    data object NavigateToArtists : HomeEffect
    data object NavigateToPlaylists : HomeEffect
    data object NavigateToSettings : HomeEffect
    data object NavigateToMyMusic : HomeEffect
    data object NavigateToSearch : HomeEffect
    data object NavigateToFolders : HomeEffect
}
