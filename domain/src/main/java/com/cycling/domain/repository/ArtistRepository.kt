package com.cycling.domain.repository

import com.cycling.domain.model.Artist
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    fun getAllArtists(): Flow<List<Artist>>

    suspend fun getArtistById(id: Long): Artist?

    suspend fun getArtistCount(): Int

    fun searchArtists(query: String): Flow<List<Artist>>
}
