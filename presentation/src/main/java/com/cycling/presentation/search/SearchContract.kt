package com.cycling.presentation.search

import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import androidx.annotation.StringRes
import com.cycling.domain.model.LyricsMatch
import com.cycling.domain.model.Song

data class SearchUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    /** Songs matched by their lyric text; the row shows a snippet instead of metadata. */
    val lyricsMatches: List<LyricsMatch> = emptyList(),
    val selectedTab: SearchTab = SearchTab.ALL,
    val error: String? = null
)

enum class SearchTab {
    ALL, SONGS, ALBUMS, ARTISTS
}

sealed interface SearchIntent {
    data class SearchQueryChanged(val query: String) : SearchIntent
    data class SearchSubmitted(val query: String) : SearchIntent
    data class HistoryItemClicked(val query: String) : SearchIntent
    data object ClearHistory : SearchIntent
    data class TabSelected(val tab: SearchTab) : SearchIntent
    data class SongClicked(val song: Song) : SearchIntent
    data class ToggleFavorite(val song: Song) : SearchIntent
    data class AlbumClicked(val album: Album) : SearchIntent
    data class ArtistClicked(val artist: Artist) : SearchIntent
    data object ClearSearch : SearchIntent
}

sealed interface SearchEffect {
    data class NavigateToPlayer(val songId: Long) : SearchEffect
    data class NavigateToAlbumDetail(val albumId: Long) : SearchEffect
    data class NavigateToArtistDetail(val artistId: Long) : SearchEffect
    data class ShowToast(@StringRes val message: Int) : SearchEffect
}
