package com.cycling.domain.usecase

import com.cycling.domain.model.SearchResult
import com.cycling.domain.repository.AlbumRepository
import com.cycling.domain.repository.ArtistRepository
import com.cycling.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchAllUseCase @Inject constructor(
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) {
    operator fun invoke(query: String): Flow<SearchResult> {
        if (query.isBlank()) {
            return flowOf(SearchResult())
        }
        return combine(
            songRepository.searchSongs(query),
            albumRepository.searchAlbums(query),
            artistRepository.searchArtists(query)
        ) { songs, albums, artists ->
            SearchResult(songs, albums, artists)
        }
    }
}
