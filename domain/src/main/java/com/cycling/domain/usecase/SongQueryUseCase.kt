package com.cycling.domain.usecase

import com.cycling.domain.model.Song
import com.cycling.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SongQueryUseCase @Inject constructor(
    private val repository: SongRepository
) {
    fun all(): Flow<List<Song>> = repository.getAllSongs()

    fun favorites(): Flow<List<Song>> = repository.getFavoriteSongs()

    fun mostPlayed(): Flow<List<Song>> = repository.getMostPlayedSongs()

    fun recentlyPlayed(): Flow<List<Song>> = repository.getRecentlyPlayedSongs()
}
