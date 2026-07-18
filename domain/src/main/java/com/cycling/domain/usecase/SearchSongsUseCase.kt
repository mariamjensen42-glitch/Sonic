package com.cycling.domain.usecase

import com.cycling.domain.model.Song
import com.cycling.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchSongsUseCase @Inject constructor(
    private val repository: SongRepository
) {
    operator fun invoke(query: String): Flow<List<Song>> {
        return repository.searchSongs(query)
    }
}
