package com.cycling.presentation.search

import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import androidx.annotation.StringRes
import com.cycling.domain.model.LyricsMatch
import com.cycling.domain.model.Song
import com.cycling.domain.online.model.MusicInfo

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
    val error: String? = null,
    /** 在线音源搜到的歌：带平台歌曲 id，点一条就去取链播放。 */
    val onlineResults: List<MusicInfo> = emptyList(),
    val onlineSearching: Boolean = false,
    /** 已翻到第几页（每页 5 个平台 × 10 条）。 */
    val onlinePage: Int = 1,
    val onlineHasMore: Boolean = false,
    val onlineLoadingMore: Boolean = false,
    /** false 表示当前没有可用音源，空结果要引导去设置页而不是换关键词。 */
    val onlineHasSources: Boolean = true,
    /** 正在取链的那一条（[MusicInfo] 的平台键:id），用来在行尾显示转圈。 */
    val resolvingOnlineKey: String? = null
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
    data class OnlineTrackClicked(val track: MusicInfo) : SearchIntent
    data object LoadMoreOnline : SearchIntent
    data object ClearSearch : SearchIntent
}

sealed interface SearchEffect {
    data class NavigateToPlayer(val songId: Long) : SearchEffect
    data class NavigateToAlbumDetail(val albumId: Long) : SearchEffect
    data class NavigateToArtistDetail(val artistId: Long) : SearchEffect
    data class ShowToast(@StringRes val message: Int, val arg: String? = null) : SearchEffect
}

/** 在线结果行的稳定 key：同一平台同一 id 就是同一首歌。 */
val MusicInfo.onlineKey: String get() = "${source.lxKey}:$id"
