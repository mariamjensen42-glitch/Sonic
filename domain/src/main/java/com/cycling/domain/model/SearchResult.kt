package com.cycling.domain.model

data class SearchResult(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    /**
     * Songs whose lyrics contain the query. Kept separate from [songs] because the row shows a
     * snippet instead of the usual metadata, and a song may appear in both lists.
     */
    val lyricsMatches: List<LyricsMatch> = emptyList()
)
