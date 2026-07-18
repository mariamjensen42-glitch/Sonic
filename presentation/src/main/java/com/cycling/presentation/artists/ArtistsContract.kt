package com.cycling.presentation.artists

import com.cycling.domain.model.Artist

data class ArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface ArtistsIntent {
    data class ArtistClick(val artist: Artist) : ArtistsIntent
}

sealed interface ArtistsEffect {
    data class NavigateToArtistDetail(val artistId: Long) : ArtistsEffect
}
